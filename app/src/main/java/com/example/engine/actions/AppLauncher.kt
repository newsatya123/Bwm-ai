package com.example.engine.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null
)

class AppLauncher(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getInstalledLauncherApps(): List<InstalledAppInfo> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos: List<ResolveInfo> = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }

            resolveInfos.mapNotNull { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                // Filter out ourselves if desired, or keep
                val label = resolveInfo.loadLabel(packageManager).toString()
                val icon = try { resolveInfo.loadIcon(packageManager) } catch (_: Exception) { null }
                InstalledAppInfo(label, pkgName, icon)
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun launchAppByName(query: String): ToolExecutionResult {
        val cleanQuery = query.trim().lowercase()
            .replace("kholo", "")
            .replace("chalao", "")
            .replace("open", "")
            .replace("launch", "")
            .replace("app", "")
            .trim()

        if (cleanQuery.isEmpty()) {
            return ToolExecutionResult(false, "Please specify an app name to launch.")
        }

        val installedApps = getInstalledLauncherApps()

        // 1. Exact or contains match on app label
        var matched = installedApps.firstOrNull { it.appName.lowercase() == cleanQuery }
            ?: installedApps.firstOrNull { it.appName.lowercase().contains(cleanQuery) }

        // 2. Common aliases (Hindi/English common packages)
        if (matched == null) {
            val aliasMap = mapOf(
                "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
                "youtube" to listOf("com.google.android.youtube"),
                "chrome" to listOf("com.android.chrome"),
                "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera", "camera"),
                "calculator" to listOf("calculator", "com.google.android.calculator"),
                "calendar" to listOf("calendar", "com.google.android.calendar"),
                "clock" to listOf("deskclock", "clock", "com.google.android.deskclock"),
                "maps" to listOf("maps", "com.google.android.apps.maps"),
                "gallery" to listOf("gallery", "photos", "com.google.android.apps.photos"),
                "photos" to listOf("photos", "com.google.android.apps.photos"),
                "contacts" to listOf("contacts", "com.google.android.contacts"),
                "dialer" to listOf("dialer", "com.google.android.dialer", "phone"),
                "phone" to listOf("dialer", "com.google.android.dialer", "phone"),
                "messages" to listOf("messaging", "messages", "com.google.android.apps.messaging"),
                "settings" to listOf("com.android.settings")
            )

            for ((keyword, packages) in aliasMap) {
                if (cleanQuery.contains(keyword) || keyword.contains(cleanQuery)) {
                    matched = installedApps.firstOrNull { app ->
                        packages.any { app.packageName.lowercase().contains(it) || app.appName.lowercase().contains(keyword) }
                    }
                    if (matched != null) break
                }
            }
        }

        // 3. Match against package name contains
        if (matched == null) {
            matched = installedApps.firstOrNull { it.packageName.lowercase().contains(cleanQuery) }
        }

        return if (matched != null) {
            launchPackage(matched.packageName, matched.appName)
        } else {
            ToolExecutionResult(
                false,
                "Could not find '$cleanQuery' among installed apps. Available apps include: " +
                        installedApps.take(5).joinToString { it.appName } + "..."
            )
        }
    }

    fun launchPackage(packageName: String, appName: String? = null): ToolExecutionResult {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val displayName = appName ?: packageName
                ToolExecutionResult(true, "Launching $displayName...", mapOf("packageName" to packageName))
            } else {
                ToolExecutionResult(false, "Cannot launch package '$packageName' (no launcher activity).")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "Error launching app: ${e.message}")
        }
    }
}
