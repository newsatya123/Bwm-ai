package com.example.engine.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.Settings

data class ToolExecutionResult(
    val isSuccess: Boolean,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

class DeviceController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    companion object {
        var isFlashlightOn = false
            private set
    }

    fun toggleFlashlight(turnOn: Boolean): ToolExecutionResult {
        if (cameraManager == null) {
            return ToolExecutionResult(false, "Flashlight / CameraManager is not available on this device.")
        }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                hasFlash && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                ToolExecutionResult(false, "No camera with flash unit detected.")
            } else {
                cameraManager.setTorchMode(cameraId, turnOn)
                isFlashlightOn = turnOn
                vibrate(50)
                val statusText = if (turnOn) "Flashlight turned ON" else "Flashlight turned OFF"
                ToolExecutionResult(true, statusText, mapOf("torchState" to turnOn))
            }
        } catch (e: CameraAccessException) {
            ToolExecutionResult(false, "Flashlight error: ${e.localizedMessage ?: "Camera in use"}")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to switch flashlight: ${e.message}")
        }
    }

    fun adjustVolume(direction: String, levelPercent: Int? = null): ToolExecutionResult {
        return try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val newVol: Int = when {
                levelPercent != null -> {
                    val clamped = levelPercent.coerceIn(0, 100)
                    (maxVol * (clamped / 100f)).toInt()
                }
                direction.equals("up", ignoreCase = true) || direction.equals("badhao", ignoreCase = true) -> {
                    (currentVol + (maxVol * 0.15f).toInt().coerceAtLeast(1)).coerceAtMost(maxVol)
                }
                direction.equals("down", ignoreCase = true) || direction.equals("ghatao", ignoreCase = true) || direction.equals("kam", ignoreCase = true) -> {
                    (currentVol - (maxVol * 0.15f).toInt().coerceAtLeast(1)).coerceAtLeast(0)
                }
                direction.equals("mute", ignoreCase = true) || direction.equals("silent", ignoreCase = true) -> {
                    0
                }
                direction.equals("max", ignoreCase = true) -> {
                    maxVol
                }
                else -> currentVol
            }

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
            val percent = if (maxVol > 0) ((newVol.toFloat() / maxVol) * 100).toInt() else 0
            vibrate(40)
            ToolExecutionResult(true, "Media volume set to $percent% ($newVol/$maxVol)", mapOf("volumePercent" to percent))
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to adjust volume: ${e.message}")
        }
    }

    fun getBatteryStatus(): ToolExecutionResult {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
            } else {
                false
            }
            val statusDesc = if (isCharging) "Battery is at $level% (Charging ⚡)" else "Battery is at $level%"
            ToolExecutionResult(true, statusDesc, mapOf("level" to level, "isCharging" to isCharging))
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not read battery status: ${e.message}")
        }
    }

    fun setTimer(seconds: Int, label: String = "BWM Assistant Timer"): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val min = seconds / 60
                val sec = seconds % 60
                val timeStr = if (min > 0) "$min minute(s) ${if (sec > 0) "$sec sec" else ""}" else "$sec seconds"
                ToolExecutionResult(true, "Timer set for $timeStr: '$label'")
            } else {
                ToolExecutionResult(false, "No timer or clock app available on this device.")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to set timer: ${e.message}")
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "BWM AI Alarm"): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val formatted = String.format("%02d:%02d", hour, minute)
                ToolExecutionResult(true, "Alarm set for $formatted ($message)")
            } else {
                ToolExecutionResult(false, "No alarm application found.")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to set alarm: ${e.message}")
        }
    }

    fun openSettings(settingType: String): ToolExecutionResult {
        return try {
            val action = when (settingType.lowercase().trim()) {
                "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
                "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                "date", "time" -> Settings.ACTION_DATE_SETTINGS
                "app_info", "app_details" -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    return ToolExecutionResult(true, "Opening App settings")
                }
                else -> Settings.ACTION_SETTINGS
            }

            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opened ${settingType.replaceFirstChar { it.uppercase() }} Settings")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to open settings: ${e.message}")
        }
    }

    fun openDialer(phoneNumber: String): ToolExecutionResult {
        return try {
            val sanitized = phoneNumber.replace(Regex("[^0-9+*#]"), "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$sanitized")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opened phone dialer for $sanitized")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Could not open dialer: ${e.message}")
        }
    }

    fun openUrl(url: String): ToolExecutionResult {
        return try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opened URL: $validUrl")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to open URL: ${e.message}")
        }
    }

    fun openYouTube(query: String? = null, autoPlayFirst: Boolean = false): ToolExecutionResult {
        return try {
            val targetUrl = if (!query.isNullOrBlank()) {
                val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                "https://www.youtube.com/results?search_query=$encoded"
            } else {
                "https://www.youtube.com"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                setPackage("com.google.android.youtube")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val canResolve = context.packageManager.resolveActivity(intent, 0) != null
            if (canResolve) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }

            try {
                com.example.service.VoiceAssistantService.start(context)
            } catch (_: Exception) {}

            if (autoPlayFirst) {
                com.example.service.BwmAccessibilityService.scheduleFirstVideoClick(2000)
            }

            val message = if (!query.isNullOrBlank()) {
                "YouTube par \"$query\" search kiya ja raha hai${if (autoPlayFirst) " aur video play ho rahi hai" else ""}..."
            } else {
                "YouTube khol raha hoon..."
            }
            ToolExecutionResult(true, message, mapOf("query" to (query ?: "")))
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to open YouTube: ${e.message}")
        }
    }

    fun controlYouTube(action: String): ToolExecutionResult {
        return try {
            when (action.lowercase()) {
                "pause" -> {
                    val clicked = com.example.service.BwmAccessibilityService.clickPlayPause()
                    if (clicked) {
                        ToolExecutionResult(true, "YouTube video pause kar di hai.")
                    } else {
                        sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                        ToolExecutionResult(true, "Sent pause command.")
                    }
                }
                "play", "resume", "chalao" -> {
                    val clicked = com.example.service.BwmAccessibilityService.clickPlayPause()
                    if (clicked) {
                        ToolExecutionResult(true, "YouTube video resume kar di hai.")
                    } else {
                        sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                        ToolExecutionResult(true, "Sent play/resume command.")
                    }
                }
                "next", "agla" -> {
                    val clicked = com.example.service.BwmAccessibilityService.clickNextVideo()
                    if (clicked) {
                        ToolExecutionResult(true, "Next video play kar rahe hain.")
                    } else {
                        sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                        ToolExecutionResult(true, "Sent next video command.")
                    }
                }
                "close", "band" -> {
                    com.example.service.BwmAccessibilityService.performGlobal("home")
                    ToolExecutionResult(true, "YouTube band kar diya hai.")
                }
                else -> ToolExecutionResult(false, "Unknown YouTube command '$action'")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "YouTube control error: ${e.message}")
        }
    }

    fun openWhatsAppChat(
        phoneNumber: String,
        message: String? = null,
        autoSend: Boolean = false,
        recipientName: String? = null
    ): ToolExecutionResult {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").removePrefix("+")
            val encodedMessage = if (!message.isNullOrBlank()) {
                java.net.URLEncoder.encode(message.trim(), "UTF-8")
            } else ""

            val waUri = if (cleanNumber.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber${if (encodedMessage.isNotBlank()) "&text=$encodedMessage" else ""}")
            } else {
                Uri.parse("whatsapp://send${if (encodedMessage.isNotBlank()) "?text=$encodedMessage" else ""}")
            }

            val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val canResolve = context.packageManager.resolveActivity(intent, 0) != null
            if (canResolve) {
                context.startActivity(intent)
            } else {
                // Try WhatsApp Business package
                val intentW4b = Intent(Intent.ACTION_VIEW, waUri).apply {
                    setPackage("com.whatsapp.w4b")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (context.packageManager.resolveActivity(intentW4b, 0) != null) {
                    context.startActivity(intentW4b)
                } else {
                    // Fallback to browser or generic handler
                    val browserIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                }
            }

            // Ensure background voice service stays listening
            try {
                com.example.service.VoiceAssistantService.start(context)
            } catch (_: Exception) {}

            val targetDesc = recipientName ?: cleanNumber
            if (autoSend && !message.isNullOrBlank()) {
                com.example.service.BwmAccessibilityService.scheduleWhatsAppSend(1400)
                ToolExecutionResult(
                    true,
                    "WhatsApp par $targetDesc ko message bheja ja raha hai: \"$message\"",
                    mapOf("recipient" to targetDesc, "message" to message, "autoSend" to true)
                )
            } else {
                val actionDesc = if (!message.isNullOrBlank()) {
                    "WhatsApp par $targetDesc ka chat open kar diya hai with message prefilled."
                } else {
                    "WhatsApp par $targetDesc ka chat khol diya hai."
                }
                ToolExecutionResult(
                    true,
                    actionDesc,
                    mapOf("recipient" to targetDesc, "message" to (message ?: ""), "autoSend" to false)
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "WhatsApp open karne me error: ${e.message}")
        }
    }

    fun searchWhatsApp(query: String): ToolExecutionResult {
        return try {
            val waIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
            if (waIntent != null) {
                waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(waIntent)
                com.example.service.BwmAccessibilityService.searchInWhatsApp(query)
                ToolExecutionResult(true, "WhatsApp mein \"$query\" search kar rahe hain...")
            } else {
                ToolExecutionResult(false, "WhatsApp phone me install nahi mila.")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "WhatsApp search failed: ${e.message}")
        }
    }

    fun controlActiveApp(action: String, parameter: String? = null): ToolExecutionResult {
        val activePkg = com.example.service.BwmAccessibilityService.getActivePackage().lowercase()
        return when (action.lowercase()) {
            "scroll_down", "neeche_scroll", "scroll" -> {
                val scrolled = com.example.service.BwmAccessibilityService.scrollActiveWindow(forward = true)
                if (scrolled) {
                    ToolExecutionResult(true, "Scrolled down.")
                } else {
                    ToolExecutionResult(false, "Could not scroll current screen.")
                }
            }
            "scroll_up", "upar_scroll" -> {
                val scrolled = com.example.service.BwmAccessibilityService.scrollActiveWindow(forward = false)
                if (scrolled) {
                    ToolExecutionResult(true, "Scrolled up.")
                } else {
                    ToolExecutionResult(false, "Could not scroll current screen.")
                }
            }
            "pause", "roko" -> {
                if (activePkg.contains("youtube")) {
                    controlYouTube("pause")
                } else {
                    sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                    ToolExecutionResult(true, "Paused media.")
                }
            }
            "play", "resume", "chalao" -> {
                if (activePkg.contains("youtube")) {
                    controlYouTube("play")
                } else {
                    sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
                    ToolExecutionResult(true, "Playing media.")
                }
            }
            "next", "agla" -> {
                if (activePkg.contains("youtube")) {
                    controlYouTube("next")
                } else {
                    sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                    ToolExecutionResult(true, "Skipped to next.")
                }
            }
            "back", "peeche" -> {
                val success = com.example.service.BwmAccessibilityService.performGlobal("back")
                ToolExecutionResult(success, if (success) "Going back..." else "Failed to go back.")
            }
            "home" -> {
                val success = com.example.service.BwmAccessibilityService.performGlobal("home")
                ToolExecutionResult(success, if (success) "Home screen..." else "Failed to go home.")
            }
            "send", "bhejo" -> {
                val clicked = com.example.service.BwmAccessibilityService.clickWhatsAppSendButton()
                ToolExecutionResult(clicked, if (clicked) "Message sent." else "Could not find send button.")
            }
            else -> ToolExecutionResult(false, "Unsupported context action: $action")
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(eventDown)
            am.dispatchMediaKeyEvent(eventUp)
        } catch (_: Exception) {}
    }

    fun vibrate(durationMs: Long = 50) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
