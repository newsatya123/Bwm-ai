package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BwmAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BwmAccessibility"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private var instance: BwmAccessibilityService? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun isAccessibilityEnabled(context: Context): Boolean {
            if (instance != null && _isServiceActive.value) return true
            return try {
                val expectedServiceName = "${context.packageName}/${BwmAccessibilityService::class.java.canonicalName}"
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                enabledServices.contains(expectedServiceName) || enabledServices.contains(context.packageName)
            } catch (_: Exception) {
                instance != null
            }
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun performGlobal(action: String): Boolean {
            val service = instance ?: return false
            return when (action.lowercase()) {
                "back" -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                "home" -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                "notifications", "notification" -> service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "quick_settings", "quicksettings" -> service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "recents", "recent_apps" -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "lock", "lock_screen" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    } else false
                }
                else -> false
            }
        }

        fun clickFirstVideoResult(): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndClickFirstVideoNode(root)
        }

        fun scheduleFirstVideoClick(delayMs: Long = 2000) {
            mainHandler.postDelayed({
                val clicked = clickFirstVideoResult()
                Log.d(TAG, "Scheduled YouTube first video click result: $clicked")
                if (!clicked) {
                    mainHandler.postDelayed({
                        clickFirstVideoResult()
                    }, 1500)
                }
            }, delayMs)
        }

        fun clickPlayPause(): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndClickPlayPauseNode(root)
        }

        fun clickNextVideo(): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndClickNextNode(root)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
        Log.i(TAG, "BWM Accessibility Service connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "BWM Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    private fun findAndClickFirstVideoNode(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 25) return false

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""

        val isYouTubeVideoCandidate = (
            desc.contains("video", ignoreCase = true) ||
            desc.contains("views", ignoreCase = true) ||
            desc.contains("ago", ignoreCase = true) ||
            viewId.contains("thumbnail", ignoreCase = true) ||
            viewId.contains("video_item", ignoreCase = true) ||
            viewId.contains("compact_video", ignoreCase = true) ||
            viewId.contains("reel", ignoreCase = true)
        ) && !desc.contains("shorts", ignoreCase = true) && !text.contains("shorts", ignoreCase = true)

        if (isYouTubeVideoCandidate && node.isClickable) {
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                Log.i(TAG, "Clicked YouTube video node: '$desc'")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickFirstVideoNode(child, depth + 1)) {
                return true
            }
        }

        if (isYouTubeVideoCandidate) {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                parent = parent.parent
            }
        }

        return false
    }

    private fun findAndClickPlayPauseNode(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 20) return false

        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""

        val matchesPlayPause = desc.contains("play video", ignoreCase = true) ||
                desc.contains("pause video", ignoreCase = true) ||
                desc.equals("play", ignoreCase = true) ||
                desc.equals("pause", ignoreCase = true) ||
                viewId.contains("play_pause", ignoreCase = true) ||
                text.equals("play", ignoreCase = true) ||
                text.equals("pause", ignoreCase = true)

        if (matchesPlayPause && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickPlayPauseNode(child, depth + 1)) return true
        }

        return false
    }

    private fun findAndClickNextNode(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 20) return false

        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""

        val matchesNext = desc.contains("next video", ignoreCase = true) ||
                desc.contains("next", ignoreCase = true) ||
                viewId.contains("next_button", ignoreCase = true) ||
                text.contains("next", ignoreCase = true)

        if (matchesNext && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNextNode(child, depth + 1)) return true
        }

        return false
    }
}
