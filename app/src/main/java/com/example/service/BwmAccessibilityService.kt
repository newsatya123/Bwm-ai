package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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

        private val _currentForegroundPackage = MutableStateFlow<String>("")
        val currentForegroundPackage: StateFlow<String> = _currentForegroundPackage.asStateFlow()

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

        fun getActivePackage(): String {
            val service = instance ?: return ""
            val activeRoot = service.rootInActiveWindow
            val rootPkg = activeRoot?.packageName?.toString() ?: ""
            if (rootPkg.isNotBlank()) {
                _currentForegroundPackage.value = rootPkg
                return rootPkg
            }
            return _currentForegroundPackage.value
        }

        // --- GLOBAL SYSTEM ACTIONS ---
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

        // --- CONTEXT-AWARE SCROLLING ---
        fun scrollActiveWindow(forward: Boolean = true): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndScrollNode(root, forward)
        }

        // --- TEXT INPUT INTO ACTIVE WINDOW ---
        fun typeTextIntoActiveField(text: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndSetText(root, text)
        }

        // --- WHATSAPP SPECIFIC AUTOMATION ---
        fun clickWhatsAppSendButton(): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndClickWhatsAppSendNode(root)
        }

        fun scheduleWhatsAppSend(delayMs: Long = 1000, onComplete: ((Boolean) -> Unit)? = null) {
            mainHandler.postDelayed({
                val clicked = clickWhatsAppSendButton()
                Log.d(TAG, "WhatsApp Send click result: $clicked")
                if (!clicked) {
                    mainHandler.postDelayed({
                        val retryClicked = clickWhatsAppSendButton()
                        Log.d(TAG, "WhatsApp Send retry click result: $retryClicked")
                        onComplete?.invoke(retryClicked)
                    }, 1000)
                } else {
                    onComplete?.invoke(true)
                }
            }, delayMs)
        }

        fun scheduleWhatsAppTypeAndSend(
            message: String,
            autoSend: Boolean,
            delayMs: Long = 1200,
            onComplete: ((Boolean) -> Unit)? = null
        ) {
            mainHandler.postDelayed({
                val typed = typeTextIntoActiveField(message)
                Log.d(TAG, "Typed WhatsApp message: $typed")
                if (autoSend) {
                    scheduleWhatsAppSend(delayMs = 600, onComplete = onComplete)
                } else {
                    onComplete?.invoke(typed)
                }
            }, delayMs)
        }

        fun searchInWhatsApp(query: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            // 1. Click search icon
            val searchClicked = service.findAndClickNodeByCandidate(root, listOf("search", "menuitem_search", "खोजें"))
            if (searchClicked) {
                mainHandler.postDelayed({
                    val activeRoot = service.rootInActiveWindow ?: return@postDelayed
                    service.findAndSetText(activeRoot, query)
                }, 500)
                return true
            }
            return false
        }

        // --- YOUTUBE SPECIFIC AUTOMATION ---
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

        fun isYouTubePlaying(): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            val pkg = root.packageName?.toString() ?: getActivePackage()
            if (!pkg.contains("youtube", ignoreCase = true)) return false

            fun checkPauseNode(node: AccessibilityNodeInfo, depth: Int): Boolean {
                if (depth > 25) return false
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                if (desc.contains("pause video") || text.contains("pause video") || desc == "pause") {
                    return true
                }
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    if (checkPauseNode(child, depth + 1)) return true
                }
                return false
            }

            return checkPauseNode(root, 0)
        }

        // --- SCREEN-AWARE TEXT READING ---
        fun extractActiveScreenContent(): ScreenContent? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return null
            val pkg = root.packageName?.toString() ?: getActivePackage()
            val textList = mutableListOf<String>()
            val clickableList = mutableListOf<String>()
            val editableList = mutableListOf<String>()

            fun traverse(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 30) return
                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()

                if (!text.isNullOrBlank()) {
                    textList.add(text)
                } else if (!desc.isNullOrBlank() && (node.isClickable || desc.length > 3)) {
                    textList.add(desc)
                }

                if (node.isClickable) {
                    val label = text ?: desc
                    if (!label.isNullOrBlank() && !clickableList.contains(label)) {
                        clickableList.add(label)
                    }
                }
                if (node.isEditable) {
                    val label = text ?: desc ?: node.viewIdResourceName ?: "Input field"
                    editableList.add(label)
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    traverse(child, depth + 1)
                }
            }

            traverse(root, 0)

            val cleanTexts = textList.distinct().filter { it.isNotBlank() }
            val title = cleanTexts.firstOrNull() ?: pkg
            val summary = cleanTexts.joinToString("\n")
            return ScreenContent(
                packageName = pkg,
                title = title,
                textNodes = cleanTexts,
                clickableElements = clickableList.distinct(),
                editableElements = editableList.distinct(),
                rawSummaryText = summary
            )
        }

        fun getActiveWhatsAppContactName(): String? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return null
            var found: String? = null
            fun find(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 15 || found != null) return
                val id = node.viewIdResourceName ?: ""
                if (id.contains("conversation_contact_name", ignoreCase = true) || id.contains("contact_name", ignoreCase = true)) {
                    val t = node.text?.toString()?.trim()
                    if (!t.isNullOrBlank()) {
                        found = t
                        return
                    }
                }
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    find(child, depth + 1)
                }
            }
            find(root, 0)
            return found
        }

        // --- WHATSAPP CONVERSATION READING ---
        fun extractVisibleWhatsAppMessages(maxCount: Int = 3): List<WhatsAppVisibleMessage> {
            val service = instance ?: return emptyList()
            val root = service.rootInActiveWindow ?: return emptyList()
            val messages = mutableListOf<WhatsAppVisibleMessage>()

            var activeChatContact = "Contact"

            fun findContactName(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 15) return
                val id = node.viewIdResourceName ?: ""
                if (id.contains("conversation_contact_name", ignoreCase = true) || id.contains("contact_name", ignoreCase = true)) {
                    val text = node.text?.toString()?.trim()
                    if (!text.isNullOrBlank()) {
                        activeChatContact = text
                        return
                    }
                }
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    findContactName(child, depth + 1)
                }
            }
            findContactName(root, 0)

            fun traverseMessages(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 35) return
                val id = node.viewIdResourceName ?: ""
                val text = node.text?.toString()?.trim() ?: ""
                val desc = node.contentDescription?.toString()?.trim() ?: ""

                val isMessageNode = id.contains("message_text", ignoreCase = true) ||
                        id.contains("conversation_text", ignoreCase = true) ||
                        (text.isNotBlank() && id.contains("message", ignoreCase = true))

                if (isMessageNode && text.isNotBlank()) {
                    val isOutgoing = desc.contains("Delivered", ignoreCase = true) ||
                            desc.contains("Read", ignoreCase = true) ||
                            desc.contains("Sent", ignoreCase = true) ||
                            id.contains("outgoing", ignoreCase = true)

                    val sender = if (isOutgoing) "Aap" else activeChatContact
                    messages.add(
                        WhatsAppVisibleMessage(
                            sender = sender,
                            message = text,
                            timestamp = null,
                            isOutgoing = isOutgoing
                        )
                    )
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    traverseMessages(child, depth + 1)
                }
            }

            traverseMessages(root, 0)

            if (messages.isEmpty()) {
                val allTexts = mutableListOf<String>()
                fun fallback(node: AccessibilityNodeInfo, depth: Int) {
                    if (depth > 30) return
                    val t = node.text?.toString()?.trim()
                    if (!t.isNullOrBlank() && t.length > 1 &&
                        !t.contains("Type a message", ignoreCase = true) &&
                        !t.contains("Search", ignoreCase = true) &&
                        !t.contains("Online", ignoreCase = true) &&
                        !t.contains("typing...", ignoreCase = true)
                    ) {
                        allTexts.add(t)
                    }
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i) ?: continue
                        fallback(child, depth + 1)
                    }
                }
                fallback(root, 0)
                allTexts.distinct().takeLast(maxCount).forEach { t ->
                    messages.add(WhatsAppVisibleMessage(sender = activeChatContact, message = t))
                }
            }

            return messages.takeLast(maxCount)
        }

        fun clickFirstSearchResult(): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return service.findAndClickFirstSearchResultNode(root)
        }
    }

data class ScreenContent(
    val packageName: String,
    val title: String,
    val textNodes: List<String>,
    val clickableElements: List<String>,
    val editableElements: List<String>,
    val rawSummaryText: String
)

data class WhatsAppVisibleMessage(
    val sender: String,
    val message: String,
    val timestamp: String? = null,
    val isOutgoing: Boolean = false
)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
        Log.i(TAG, "BWM Accessibility Service connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: ""
            if (pkg.isNotBlank() && pkg != packageName && !pkg.contains("systemui", ignoreCase = true)) {
                _currentForegroundPackage.value = pkg
                Log.d(TAG, "Foreground app switched to: $pkg")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "BWM Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    // --- RECURSIVE NODE TRAVERSAL HELPERS ---

    private fun findAndScrollNode(node: AccessibilityNodeInfo, forward: Boolean, depth: Int = 0): Boolean {
        if (depth > 20) return false

        if (node.isScrollable) {
            val action = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            val scrolled = node.performAction(action)
            if (scrolled) {
                Log.i(TAG, "Scrolled node successfully (forward=$forward)")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndScrollNode(child, forward, depth + 1)) return true
        }
        return false
    }

    private fun findAndSetText(node: AccessibilityNodeInfo, text: String, depth: Int = 0): Boolean {
        if (depth > 20) return false

        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val isTargetField = node.isEditable ||
                className.contains("EditText", ignoreCase = true) ||
                viewId.contains("entry", ignoreCase = true) ||
                viewId.contains("message", ignoreCase = true) ||
                viewId.contains("search_src_text", ignoreCase = true)

        if (isTargetField) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (success) {
                Log.i(TAG, "Successfully entered text '$text' into node (id=$viewId)")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndSetText(child, text, depth + 1)) return true
        }
        return false
    }

    private fun findAndClickWhatsAppSendNode(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 25) return false

        val viewId = node.viewIdResourceName ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""

        val isSendCandidate = viewId.contains("send", ignoreCase = true) ||
                desc.equals("send", ignoreCase = true) ||
                desc.contains("भेजें", ignoreCase = true) ||
                desc.contains("send message", ignoreCase = true) ||
                text.equals("send", ignoreCase = true)

        if (isSendCandidate && node.isClickable) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) {
                Log.i(TAG, "Clicked WhatsApp Send button (id=$viewId, desc=$desc)")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickWhatsAppSendNode(child, depth + 1)) return true
        }

        if (isSendCandidate) {
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

    private fun findAndClickNodeByCandidate(
        node: AccessibilityNodeInfo,
        keywords: List<String>,
        depth: Int = 0
    ): Boolean {
        if (depth > 20) return false

        val viewId = (node.viewIdResourceName ?: "").lowercase()
        val desc = (node.contentDescription?.toString() ?: "").lowercase()
        val text = (node.text?.toString() ?: "").lowercase()

        val matches = keywords.any { kw ->
            viewId.contains(kw) || desc.contains(kw) || text.contains(kw)
        }

        if (matches && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNodeByCandidate(child, keywords, depth + 1)) return true
        }

        if (matches) {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }

        return false
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

    private fun findAndClickFirstSearchResultNode(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 25) return false
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        val isCandidate = (node.isClickable && (text.length > 10 || desc.length > 10)) &&
                !text.contains("Search", ignoreCase = true) &&
                !desc.contains("Search", ignoreCase = true) &&
                !text.contains("Google", ignoreCase = true)

        if (isCandidate) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickFirstSearchResultNode(child, depth + 1)) return true
        }

        if (isCandidate) {
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
}
