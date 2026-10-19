package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class ToolActionRequest {
    data class LaunchApp(val appName: String) : ToolActionRequest()
    data class ToggleFlashlight(val enable: Boolean) : ToolActionRequest()
    data class AdjustVolume(val direction: String, val levelPercent: Int? = null) : ToolActionRequest()
    object GetBatteryStatus : ToolActionRequest()
    data class SetTimer(val seconds: Int, val label: String) : ToolActionRequest()
    data class SetAlarm(val hour: Int, val minute: Int, val message: String) : ToolActionRequest()
    data class OpenSettings(val settingType: String) : ToolActionRequest()
    data class SaveNote(val title: String, val content: String) : ToolActionRequest()
    object GetNotes : ToolActionRequest()
    data class DialPhone(val phoneNumber: String, val contactName: String? = null) : ToolActionRequest()
    data class OpenUrl(val url: String) : ToolActionRequest()
    data class DeviceNavigation(val action: String) : ToolActionRequest()
    data class YouTubeSearch(val query: String, val autoPlayFirst: Boolean = false) : ToolActionRequest()
    data class YouTubeControl(val action: String) : ToolActionRequest()
    data class WhatsAppSend(val recipient: String, val message: String, val directSend: Boolean = false) : ToolActionRequest()
    data class WhatsAppOpenChat(val recipient: String) : ToolActionRequest()
    data class WhatsAppSearch(val query: String) : ToolActionRequest()
    data class ActiveAppContextAction(val action: String, val parameter: String? = null) : ToolActionRequest()
}

data class AgentResponse(
    val responseText: String,
    val toolActions: List<ToolActionRequest> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
) {
    // Backwards compatibility constructor & property
    constructor(
        responseText: String,
        toolAction: ToolActionRequest?,
        requiresConfirmation: Boolean = false,
        confirmationPrompt: String? = null
    ) : this(
        responseText = responseText,
        toolActions = if (toolAction != null) listOf(toolAction) else emptyList(),
        requiresConfirmation = requiresConfirmation,
        confirmationPrompt = confirmationPrompt
    )

    val toolAction: ToolActionRequest? get() = toolActions.firstOrNull()
}

class GeminiAssistantAgent {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processCommand(
        userPrompt: String,
        recentHistory: List<Pair<String, String>> = emptyList()
    ): AgentResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Check if API key is configured and valid
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("BwmAgent", "Using local NLP parser (Gemini API key not configured or default)")
            return@withContext parseWithOfflineNlp(userPrompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestJson = buildRequestBody(userPrompt, recentHistory)

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                Log.w("BwmAgent", "Gemini API call failed with code: ${response.code}, falling back to local NLP")
                return@withContext parseWithOfflineNlp(userPrompt)
            }

            parseGeminiResponse(responseBody, userPrompt)
        } catch (e: Exception) {
            Log.e("BwmAgent", "Exception in Gemini API call: ${e.message}, using local NLP fallback")
            parseWithOfflineNlp(userPrompt)
        }
    }

    private fun buildRequestBody(userPrompt: String, history: List<Pair<String, String>>): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstruction = JSONObject()
        val systemParts = JSONArray()
        systemParts.put(JSONObject().put("text", """
            You are BWM AI, a native Android Personal Phone Assistant.
            You are smart, polite, concise, and understand English, Hindi, and Hinglish fluently.
            When the user asks you to perform a phone action (like launching an app, turning on/off the flashlight, checking battery, setting timers/alarms, adjusting volume, taking a note, or dialing a phone number), ALWAYS invoke the appropriate function call tool.
            Keep spoken conversational responses short and crisp (1-2 sentences). Respond in the language or mix the user addressed you in (Hindi/Hinglish/English).
            For sensitive actions like dialing a phone number or deleting important notes, make sure to inform the user.
        """.trimIndent()))
        systemInstruction.put("parts", systemParts)
        root.put("systemInstruction", systemInstruction)

        // Contents (Conversation History + Current Prompt)
        val contentsArray = JSONArray()
        for ((role, text) in history.takeLast(6)) {
            val contentObj = JSONObject()
            contentObj.put("role", if (role.equals("user", true)) "user" else "model")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", text))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        val currentContent = JSONObject()
        currentContent.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().put("text", userPrompt))
        currentContent.put("parts", currentParts)
        contentsArray.put(currentContent)

        root.put("contents", contentsArray)

        // Tools Declaration
        val toolsArray = JSONArray()
        val toolsContainer = JSONObject()
        val functionDeclarations = JSONArray()

        functionDeclarations.put(createTool(
            "launch_app",
            "Launch an installed application on the user's phone",
            listOf("app_name" to "Name of the app (e.g. WhatsApp, YouTube, Camera, Calculator, Chrome, Maps, Settings)"),
            listOf("app_name")
        ))

        functionDeclarations.put(createTool(
            "toggle_flashlight",
            "Turn the phone's torch or flashlight on or off",
            listOf("enable" to "true to turn on, false to turn off"),
            listOf("enable")
        ))

        functionDeclarations.put(createTool(
            "adjust_volume",
            "Adjust the media volume up, down, mute, max, or to a percentage",
            listOf(
                "direction" to "Direction: 'up', 'down', 'mute', 'max', or 'set'",
                "level_percent" to "Optional target volume percentage 0 to 100"
            ),
            listOf("direction")
        ))

        functionDeclarations.put(createTool(
            "get_battery_status",
            "Check current battery percentage and charging state of the device",
            emptyList(),
            emptyList()
        ))

        functionDeclarations.put(createTool(
            "set_timer",
            "Set a countdown timer in seconds with a label",
            listOf(
                "seconds" to "Duration in seconds (e.g. 300 for 5 minutes)",
                "label" to "Label or purpose of timer"
            ),
            listOf("seconds")
        ))

        functionDeclarations.put(createTool(
            "set_alarm",
            "Set an alarm for a specific hour and minute",
            listOf(
                "hour" to "Hour in 24-hour format (0-23)",
                "minute" to "Minute (0-59)",
                "message" to "Label or title of the alarm"
            ),
            listOf("hour", "minute")
        ))

        functionDeclarations.put(createTool(
            "open_settings",
            "Open specific Android device settings screen",
            listOf("setting_type" to "wifi, bluetooth, display, sound, battery, accessibility, or general"),
            listOf("setting_type")
        ))

        functionDeclarations.put(createTool(
            "save_note",
            "Save a quick note or reminder into the assistant's local database",
            listOf(
                "title" to "Brief title of the note",
                "content" to "Detailed text of the note"
            ),
            listOf("title", "content")
        ))

        functionDeclarations.put(createTool(
            "get_notes",
            "Retrieve saved notes and reminders",
            emptyList(),
            emptyList()
        ))

        functionDeclarations.put(createTool(
            "dial_phone",
            "Open phone dialer with a phone number (requires confirmation)",
            listOf(
                "phone_number" to "Phone number to dial",
                "contact_name" to "Optional contact name"
            ),
            listOf("phone_number")
        ))

        functionDeclarations.put(createTool(
            "device_navigation",
            "Perform global navigation action on phone: back, home, notifications, quick_settings",
            listOf("action" to "back, home, notifications, or quick_settings"),
            listOf("action")
        ))

        functionDeclarations.put(createTool(
            "youtube_search",
            "Search YouTube for videos, music, tutorials, or topics and optionally autoplay the first video",
            listOf(
                "query" to "Search query topic or song name",
                "auto_play" to "Whether to automatically click and play the first video result (true/false)"
            ),
            listOf("query")
        ))

        functionDeclarations.put(createTool(
            "youtube_control",
            "Control YouTube playback: pause, play, resume, next, or close",
            listOf("action" to "pause, play, resume, next, or close"),
            listOf("action")
        ))

        functionDeclarations.put(createTool(
            "whatsapp_send_message",
            "Send or compose a WhatsApp message to a contact or phone number",
            listOf(
                "recipient" to "Name of contact or phone number (e.g. Raman, Papa, Neha, 9876543210)",
                "message" to "Text message content to send"
            ),
            listOf("recipient", "message")
        ))

        functionDeclarations.put(createTool(
            "whatsapp_open_chat",
            "Open WhatsApp conversation chat with a contact or phone number",
            listOf("recipient" to "Name of contact or phone number (e.g. Raman, Papa, Neha)"),
            listOf("recipient")
        ))

        functionDeclarations.put(createTool(
            "whatsapp_search",
            "Search for a contact, message, or term inside WhatsApp",
            listOf("query" to "Search keyword or contact name"),
            listOf("query")
        ))

        functionDeclarations.put(createTool(
            "active_app_control",
            "Perform context-aware active application actions: scroll_down, scroll_up, pause, play, next, back, home, or send",
            listOf(
                "action" to "scroll_down, scroll_up, pause, play, next, back, home, or send",
                "parameter" to "Optional target or parameter"
            ),
            listOf("action")
        ))

        toolsContainer.put("functionDeclarations", functionDeclarations)
        toolsArray.put(toolsContainer)
        root.put("tools", toolsArray)

        return root
    }

    private fun createTool(
        name: String,
        description: String,
        parameters: List<Pair<String, String>>,
        required: List<String>
    ): JSONObject {
        val tool = JSONObject()
        tool.put("name", name)
        tool.put("description", description)

        val paramsObj = JSONObject()
        paramsObj.put("type", "OBJECT")
        val propsObj = JSONObject()
        for ((pName, pDesc) in parameters) {
            val propDetail = JSONObject()
            propDetail.put("type", if (pName == "seconds" || pName == "hour" || pName == "minute" || pName == "level_percent") "INTEGER" else if (pName == "enable") "BOOLEAN" else "STRING")
            propDetail.put("description", pDesc)
            propsObj.put(pName, propDetail)
        }
        paramsObj.put("properties", propsObj)
        if (required.isNotEmpty()) {
            val reqArray = JSONArray()
            required.forEach { reqArray.put(it) }
            paramsObj.put("required", reqArray)
        }
        tool.put("parameters", paramsObj)
        return tool
    }

    private fun parseGeminiResponse(jsonString: String, originalPrompt: String): AgentResponse {
        val json = JSONObject(jsonString)
        val candidates = json.optJSONArray("candidates") ?: return parseWithOfflineNlp(originalPrompt)
        if (candidates.length() == 0) return parseWithOfflineNlp(originalPrompt)

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return parseWithOfflineNlp(originalPrompt)
        val parts = content.optJSONArray("parts") ?: return parseWithOfflineNlp(originalPrompt)

        var textResponse = ""
        val toolActions = mutableListOf<ToolActionRequest>()
        var requiresConfirmation = false
        var confirmationPrompt: String? = null

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                textResponse += part.getString("text")
            }
            if (part.has("functionCall")) {
                val functionCall = part.getJSONObject("functionCall")
                val toolName = functionCall.getString("name")
                val args = functionCall.optJSONObject("args") ?: JSONObject()

                when (toolName) {
                    "launch_app" -> {
                        val appName = args.optString("app_name", "")
                        toolActions.add(ToolActionRequest.LaunchApp(appName))
                        if (textResponse.isBlank()) textResponse = "Opening $appName..."
                    }
                    "toggle_flashlight" -> {
                        val enable = args.optBoolean("enable", true)
                        toolActions.add(ToolActionRequest.ToggleFlashlight(enable))
                        if (textResponse.isBlank()) textResponse = if (enable) "Turning on flashlight" else "Turning off flashlight"
                    }
                    "adjust_volume" -> {
                        val dir = args.optString("direction", "up")
                        val level = if (args.has("level_percent")) args.getInt("level_percent") else null
                        toolActions.add(ToolActionRequest.AdjustVolume(dir, level))
                        if (textResponse.isBlank()) textResponse = "Adjusting volume..."
                    }
                    "get_battery_status" -> {
                        toolActions.add(ToolActionRequest.GetBatteryStatus)
                        if (textResponse.isBlank()) textResponse = "Checking battery level..."
                    }
                    "set_timer" -> {
                        val seconds = args.optInt("seconds", 60)
                        val label = args.optString("label", "Timer")
                        toolActions.add(ToolActionRequest.SetTimer(seconds, label))
                        if (textResponse.isBlank()) textResponse = "Setting timer for ${seconds / 60} minutes..."
                    }
                    "set_alarm" -> {
                        val hour = args.optInt("hour", 7)
                        val minute = args.optInt("minute", 0)
                        val message = args.optString("message", "Alarm")
                        toolActions.add(ToolActionRequest.SetAlarm(hour, minute, message))
                        if (textResponse.isBlank()) textResponse = "Setting alarm for $hour:$minute..."
                    }
                    "open_settings" -> {
                        val settingType = args.optString("setting_type", "general")
                        toolActions.add(ToolActionRequest.OpenSettings(settingType))
                        if (textResponse.isBlank()) textResponse = "Opening $settingType settings..."
                    }
                    "save_note" -> {
                        val title = args.optString("title", "Voice Note")
                        val noteContent = args.optString("content", "")
                        toolActions.add(ToolActionRequest.SaveNote(title, noteContent))
                        if (textResponse.isBlank()) textResponse = "Saving note: $title"
                    }
                    "get_notes" -> {
                        toolActions.add(ToolActionRequest.GetNotes)
                        if (textResponse.isBlank()) textResponse = "Fetching your saved notes..."
                    }
                    "dial_phone" -> {
                        val phone = args.optString("phone_number", "")
                        val name = args.optString("contact_name", "")
                        toolActions.add(ToolActionRequest.DialPhone(phone, name))
                        requiresConfirmation = true
                        confirmationPrompt = "Do you want to dial ${if (name.isNotEmpty()) name else phone}?"
                        if (textResponse.isBlank()) textResponse = "Preparing to dial $phone..."
                    }
                    "device_navigation" -> {
                        val action = args.optString("action", "back")
                        toolActions.add(ToolActionRequest.DeviceNavigation(action))
                        if (textResponse.isBlank()) textResponse = "Navigating $action..."
                    }
                    "youtube_search" -> {
                        val query = args.optString("query", "")
                        val autoPlay = args.optBoolean("auto_play", false)
                        toolActions.add(ToolActionRequest.YouTubeSearch(query, autoPlay))
                        if (textResponse.isBlank()) textResponse = if (autoPlay) "Searching YouTube for $query and playing..." else "Searching YouTube for $query..."
                    }
                    "youtube_control" -> {
                        val action = args.optString("action", "pause")
                        toolActions.add(ToolActionRequest.YouTubeControl(action))
                        if (textResponse.isBlank()) textResponse = "Executing YouTube $action command..."
                    }
                    "whatsapp_send_message" -> {
                        val recipient = args.optString("recipient", "")
                        val message = args.optString("message", "")
                        toolActions.add(ToolActionRequest.WhatsAppSend(recipient, message))
                        requiresConfirmation = true
                        confirmationPrompt = "Kya aap $recipient ko WhatsApp par \"$message\" bhejna chahte hain?"
                        if (textResponse.isBlank()) textResponse = "Sending WhatsApp message to $recipient..."
                    }
                    "whatsapp_open_chat" -> {
                        val recipient = args.optString("recipient", "")
                        toolActions.add(ToolActionRequest.WhatsAppOpenChat(recipient))
                        if (textResponse.isBlank()) textResponse = "WhatsApp par $recipient ka chat khol raha hoon..."
                    }
                    "whatsapp_search" -> {
                        val query = args.optString("query", "")
                        toolActions.add(ToolActionRequest.WhatsAppSearch(query))
                        if (textResponse.isBlank()) textResponse = "WhatsApp par $query search kar raha hoon..."
                    }
                    "active_app_control" -> {
                        val action = args.optString("action", "scroll_down")
                        val param = args.optString("parameter", "")
                        toolActions.add(ToolActionRequest.ActiveAppContextAction(action, if (param.isNotEmpty()) param else null))
                        if (textResponse.isBlank()) textResponse = "Executing $action..."
                    }
                }
            }
        }

        if (textResponse.isBlank() && toolActions.isEmpty()) {
            textResponse = "Main aapki kya madad kar sakta hoon? (How can I assist you?)"
        }

        return AgentResponse(
            responseText = textResponse.trim(),
            toolActions = toolActions,
            requiresConfirmation = requiresConfirmation,
            confirmationPrompt = confirmationPrompt
        )
    }

    // Multi-tasking NLP engine supporting compound commands (e.g. "turn on torch and open youtube")
    fun parseWithOfflineNlp(prompt: String): AgentResponse {
        val clean = prompt.trim()
        val splitRegex = Regex("(?i)\\s+(and|aur|then|fir|phir|ke baad|baad me|after that)\\s+|\\s*[,;]\\s+")
        val clauses = clean.split(splitRegex).map { it.trim() }.filter { it.length > 2 }

        if (clauses.size > 1) {
            val combinedActions = mutableListOf<ToolActionRequest>()
            val speechParts = mutableListOf<String>()

            for (clause in clauses) {
                val singleResp = parseSingleCommandOffline(clause)
                if (singleResp.toolActions.isNotEmpty()) {
                    combinedActions.addAll(singleResp.toolActions)
                    speechParts.add(singleResp.responseText)
                }
            }

            if (combinedActions.isNotEmpty()) {
                val combinedText = speechParts.joinToString(separator = ", aur ")
                return AgentResponse(
                    responseText = combinedText,
                    toolActions = combinedActions
                )
            }
        }

        return parseSingleCommandOffline(clean)
    }

    private fun parseSingleCommandOffline(prompt: String): AgentResponse {
        val p = prompt.lowercase().trim()

        // 1. Flashlight / Torch
        if (p.contains("flashlight") || p.contains("torch") || p.contains("batti") || p.contains("light")) {
            val turnOff = p.contains("off") || p.contains("band") || p.contains("bujhao") || p.contains("close")
            val turnOn = p.contains("on") || p.contains("chalu") || p.contains("jalao") || p.contains("start") || !turnOff
            val state = turnOn && !turnOff
            return AgentResponse(
                responseText = if (state) "Torch chalu kar raha hoon (Turning on flashlight)" else "Torch band kar raha hoon (Turning off flashlight)",
                toolAction = ToolActionRequest.ToggleFlashlight(state)
            )
        }

        // 2. Battery Status
        if (p.contains("battery") || p.contains("charge") || p.contains("charging") || p.contains("kitni percent")) {
            return AgentResponse(
                responseText = "Battery check kar raha hoon...",
                toolAction = ToolActionRequest.GetBatteryStatus
            )
        }

        // 3. Volume
        if (p.contains("volume") || p.contains("awaz") || p.contains("aawaz") || p.contains("sound")) {
            return when {
                p.contains("mute") || p.contains("silent") || p.contains("chup") -> {
                    AgentResponse("Media volume mute kiya ja raha hai", ToolActionRequest.AdjustVolume("mute"))
                }
                p.contains("badhao") || p.contains("up") || p.contains("tez") || p.contains("increase") -> {
                    AgentResponse("Volume badha raha hoon", ToolActionRequest.AdjustVolume("up"))
                }
                p.contains("kam") || p.contains("ghatao") || p.contains("down") || p.contains("decrease") -> {
                    AgentResponse("Volume kam kar raha hoon", ToolActionRequest.AdjustVolume("down"))
                }
                p.contains("max") || p.contains("full") -> {
                    AgentResponse("Volume full kar raha hoon", ToolActionRequest.AdjustVolume("max"))
                }
                else -> {
                    AgentResponse("Volume badha raha hoon", ToolActionRequest.AdjustVolume("up"))
                }
            }
        }

        // 4. Timer
        if (p.contains("timer") || p.contains("minute") || p.contains("second")) {
            val matcher = Pattern.compile("(\\d+)\\s*(minute|min|sec|second|ghanta|hour)?").matcher(p)
            var totalSec = 60 // default 1 min
            if (matcher.find()) {
                val num = matcher.group(1)?.toIntOrNull() ?: 1
                val unit = matcher.group(2) ?: "min"
                totalSec = when {
                    unit.startsWith("sec") -> num
                    unit.startsWith("hour") || unit.startsWith("ghanta") -> num * 3600
                    else -> num * 60
                }
            }
            val min = totalSec / 60
            return AgentResponse(
                responseText = "${if (min > 0) "$min minute" else "$totalSec second"} ka timer set kar diya hai.",
                toolAction = ToolActionRequest.SetTimer(totalSec, "BWM Assistant Timer")
            )
        }

        // 5. Alarm
        if (p.contains("alarm")) {
            val matcher = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm|baje)?").matcher(p)
            var hour = 7
            var min = 0
            if (matcher.find()) {
                val h = matcher.group(1)?.toIntOrNull() ?: 7
                val m = matcher.group(3)?.toIntOrNull() ?: 0
                val meridian = matcher.group(4) ?: ""
                hour = if (meridian.equals("pm", true) && h < 12) h + 12 else h
                min = m
            }
            return AgentResponse(
                responseText = "$hour:${if (min < 10) "0$min" else "$min"} par alarm lagaya ja raha hai.",
                toolAction = ToolActionRequest.SetAlarm(hour, min, "Morning Alarm")
            )
        }

        // 6. Settings
        if (p.contains("setting") || p.contains("wifi") || p.contains("bluetooth")) {
            val type = when {
                p.contains("wifi") || p.contains("internet") -> "wifi"
                p.contains("bluetooth") || p.contains("bt") -> "bluetooth"
                p.contains("display") || p.contains("brightness") -> "display"
                p.contains("sound") || p.contains("audio") -> "sound"
                p.contains("battery") -> "battery"
                p.contains("accessibility") -> "accessibility"
                else -> "general"
            }
            return AgentResponse(
                responseText = "$type settings khol raha hoon...",
                toolAction = ToolActionRequest.OpenSettings(type)
            )
        }

        // 7. Navigation (Back / Home / Notifications)
        if (p.contains("go back") || p.contains("peeche jao") || p.contains("back karo") || p.contains("back jao") || p == "back") {
            return AgentResponse("Going back...", ToolActionRequest.ActiveAppContextAction("back"))
        }
        if (p.contains("go home") || p.contains("home screen") || p.contains("home jao") || p == "home") {
            return AgentResponse("Going to home screen...", ToolActionRequest.ActiveAppContextAction("home"))
        }
        if (p.contains("notification") || p.contains("shade")) {
            return AgentResponse("Opening notifications...", ToolActionRequest.DeviceNavigation("notifications"))
        }

        // 8. Active App Scrolling & Context Controls
        if (p.contains("upar scroll") || p.contains("scroll up") || p.contains("up scroll")) {
            return AgentResponse("Scrolling up...", ToolActionRequest.ActiveAppContextAction("scroll_up"))
        }
        if (p.contains("neeche scroll") || p.contains("scroll down") || p.contains("down scroll") || p.contains("scroll")) {
            return AgentResponse("Scrolling down...", ToolActionRequest.ActiveAppContextAction("scroll_down"))
        }
        if (p == "pause" || p == "pause karo" || p == "roko" || p == "ruk jao" || p == "stop") {
            return AgentResponse("Pausing active playback...", ToolActionRequest.ActiveAppContextAction("pause"))
        }
        if (p == "play" || p == "play karo" || p == "chalao" || p == "resume" || p == "resume karo") {
            return AgentResponse("Resuming active playback...", ToolActionRequest.ActiveAppContextAction("play"))
        }
        if (p == "next" || p == "next karo" || p == "agla" || p == "agla video" || p == "next video") {
            return AgentResponse("Skipping to next...", ToolActionRequest.ActiveAppContextAction("next"))
        }

        // 9. WhatsApp Voice Features
        // 9a. WhatsApp Search: "WhatsApp mein search karo [query]"
        if (p.contains("whatsapp") && (p.contains("search") || p.contains("khojo") || p.contains("dhoondo"))) {
            val q = p
                .replace(Regex("(?i)\\b(whatsapp\\s*(mein|par|pe)?|search\\s*karo|khojo|dhoondo|please|karo)\\b"), "")
                .trim()
            return AgentResponse(
                responseText = if (q.isNotEmpty()) "WhatsApp mein \"$q\" search kar rahe hain..." else "WhatsApp search khol rahe hain...",
                toolAction = ToolActionRequest.WhatsAppSearch(q)
            )
        }

        // 9b. WhatsApp Send Message:
        // Examples: "Raman ko hii bhejo", "Raman ko message bhejo: main ghar aa raha hoon", "Neha ko WhatsApp par message bhejo: hello"
        val sendPatterns = listOf(
            Pattern.compile("(?i)^([a-zA-Z0-9\u0900-\u097F]+)\\s+ko\\s+(?:whatsapp\\s*(?:par|pe)?\\s+)?(?:message|msg)?\\s*bhejo\\s*[:\\-]?\\s*(.*)$"),
            Pattern.compile("(?i)^([a-zA-Z0-9\u0900-\u097F]+)\\s+ko\\s+(?:whatsapp\\s*(?:par|pe)?\\s+)?message\\s+bhejo[:\\s]*(.*)$"),
            Pattern.compile("(?i)^([a-zA-Z0-9\u0900-\u097F]+)\\s+ko\\s+(?:whatsapp\\s*(?:par|pe)?\\s+)?(.*)\\s+bhejo$"),
            Pattern.compile("(?i)^send\\s+(?:a\\s+)?(?:whatsapp\\s+)?message\\s+to\\s+([a-zA-Z0-9\u0900-\u097F]+)[:\\s]+(.*)$"),
            Pattern.compile("(?i)^send\\s+(?:whatsapp\\s+)?(.*)\\s+to\\s+([a-zA-Z0-9\u0900-\u097F]+)$")
        )

        for (pattern in sendPatterns) {
            val matcher = pattern.matcher(prompt.trim())
            if (matcher.find()) {
                var recipient: String
                var messageText: String

                if (pattern.pattern().startsWith("(?i)^send\\s+(?:whatsapp\\s+)?(.*)\\s+to")) {
                    messageText = matcher.group(1)?.trim() ?: ""
                    recipient = matcher.group(2)?.trim() ?: ""
                } else {
                    recipient = matcher.group(1)?.trim() ?: ""
                    messageText = matcher.group(2)?.trim() ?: ""
                }

                // If message text was empty or just "hii", set default
                if (messageText.isBlank()) {
                    messageText = "Hi"
                }

                // Clean up prefixes from message
                messageText = messageText.removePrefix(":").trim()
                if (messageText.isBlank()) messageText = "Hi"

                val isDirectSend = p.contains("direct") || p.contains("bina confirmation")
                return AgentResponse(
                    responseText = "WhatsApp par $recipient ko \"$messageText\" bhejna hai.",
                    toolAction = ToolActionRequest.WhatsAppSend(recipient, messageText, directSend = isDirectSend),
                    requiresConfirmation = !isDirectSend,
                    confirmationPrompt = "Kya aap $recipient ko WhatsApp par \"$messageText\" bhejna chahte hain?"
                )
            }
        }

        // 9c. WhatsApp Open Chat:
        // Examples: "Raman ki chat kholo", "Papa ka WhatsApp chat kholo", "Raman ka chat kholo", "open chat with Raman"
        val openChatPatterns = listOf(
            Pattern.compile("(?i)^([a-zA-Z0-9\u0900-\u097F]+)\\s+(?:ka|ki|ke)?\\s*(?:whatsapp\\s*)?chat\\s*(?:kholo|open|chalao)"),
            Pattern.compile("(?i)^(?:whatsapp\\s*(?:par|pe)?\\s*)?([a-zA-Z0-9\u0900-\u097F]+)\\s+(?:ka|ki|ke)?\\s*chat\\s*(?:kholo|open)"),
            Pattern.compile("(?i)^open\\s+(?:whatsapp\\s+)?chat\\s+(?:with|of)?\\s*([a-zA-Z0-9\u0900-\u097F]+)")
        )

        for (pattern in openChatPatterns) {
            val matcher = pattern.matcher(prompt.trim())
            if (matcher.find()) {
                val recipient = matcher.group(1)?.trim() ?: ""
                if (recipient.isNotBlank() && recipient.lowercase() != "whatsapp") {
                    return AgentResponse(
                        responseText = "WhatsApp par $recipient ka chat khol raha hoon...",
                        toolAction = ToolActionRequest.WhatsAppOpenChat(recipient)
                    )
                }
            }
        }

        // 10. Notes
        if (p.contains("note") || p.contains("yaad rakhna") || p.contains("likho")) {
            val noteText = p.replace(Regex("note|likho|yaad rakhna|save|karo|banao"), "").trim()
            val finalContent = if (noteText.isNotEmpty()) noteText else "Quick Voice Note: $prompt"
            return AgentResponse(
                responseText = "Note save kar liya hai: $finalContent",
                toolAction = ToolActionRequest.SaveNote("Voice Note", finalContent)
            )
        }

        // 11. Dial Phone
        if (p.contains("dial") || p.contains("call") || p.contains("phone lagao")) {
            val digits = prompt.replace(Regex("[^0-9+]"), "")
            return if (digits.length >= 3) {
                AgentResponse(
                    responseText = "Dialing $digits...",
                    toolAction = ToolActionRequest.DialPhone(digits),
                    requiresConfirmation = true,
                    confirmationPrompt = "Do you want to dial $digits?"
                )
            } else {
                AgentResponse("Opening phone dialer...", ToolActionRequest.OpenSettings("phone"))
            }
        }

        // 10. YouTube Controls & Searches
        if (p.contains("youtube") || p.contains("video")) {
            if (p.contains("next") || p.contains("agla")) {
                return AgentResponse("Playing next video on YouTube...", ToolActionRequest.YouTubeControl("next"))
            }
            if (p.contains("pause") || p.contains("roko") || p.contains("ruk jao")) {
                return AgentResponse("YouTube video pause kar di hai.", ToolActionRequest.YouTubeControl("pause"))
            }
            if (p.contains("resume") || p.contains("unpause") || (p.contains("play") && !p.contains("search") && !p.contains("song"))) {
                return AgentResponse("YouTube video resume kar di hai.", ToolActionRequest.YouTubeControl("play"))
            }
            if (p.contains("band") || p.contains("close") || p.contains("exit")) {
                return AgentResponse("YouTube band kar diya hai.", ToolActionRequest.YouTubeControl("close"))
            }
        }

        val isExplicitSearch = p.contains("search") || p.contains("khojo") || p.contains("dhoondo")
        val isExplicitPlay = p.contains("chalao") || p.contains("play") || p.contains("sunao") || p.contains("lagao")
        if (p.contains("youtube") || (isExplicitSearch && (p.contains("tutorial") || p.contains("song") || p.contains("video") || p.contains("minecraft") || p.contains("coding"))) || (isExplicitPlay && (p.contains("tutorial") || p.contains("song") || p.contains("video")))) {
            val autoPlay = isExplicitPlay
            var cleanQuery = p
                .replace(Regex("(?i)\\b(hey|suno|ok|okay)\\s*(bwm|bmw)?\\b"), "")
                .replace(Regex("(?i)\\b(youtube\\s*par|on\\s*youtube|in\\s*youtube|youtube)\\b"), "")
                .replace(Regex("(?i)\\b(search\\s*karo|search\\s*kijiye|search|khojo|dhoondo)\\b"), "")
                .replace(Regex("(?i)\\b(chalao|play\\s*karo|play|lagao|sunao|kholo|open)\\b"), "")
                .replace(Regex("(?i)\\b(please|mujhe|zara|karo|bhi)\\b"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (cleanQuery.isNotBlank()) {
                val resp = if (autoPlay) {
                    "YouTube par \"$cleanQuery\" search karke play kar raha hoon..."
                } else {
                    "YouTube par \"$cleanQuery\" search kar raha hoon..."
                }
                return AgentResponse(
                    responseText = resp,
                    toolAction = ToolActionRequest.YouTubeSearch(cleanQuery, autoPlayFirst = autoPlay)
                )
            } else if (p.contains("youtube")) {
                return AgentResponse("YouTube khol raha hoon...", ToolActionRequest.LaunchApp("youtube"))
            }
        }

        // 11. Open App (WhatsApp, YouTube, Camera, Calculator, Chrome, Maps, Instagram, etc.)
        val appMatches = listOf(
            "whatsapp", "camera", "calculator", "chrome",
            "maps", "photos", "gallery", "contacts", "clock", "calendar",
            "instagram", "facebook", "telegram", "spotify", "gmail", "settings"
        )
        for (appName in appMatches) {
            if (p.contains(appName)) {
                return AgentResponse(
                    responseText = "$appName khol raha hoon (Launching $appName)...",
                    toolAction = ToolActionRequest.LaunchApp(appName)
                )
            }
        }

        if (p.contains("open") || p.contains("launch") || p.contains("kholo") || p.contains("chalao")) {
            val candidate = p.replace(Regex("open|launch|kholo|chalao|app|please|mujhe"), "").trim()
            if (candidate.isNotEmpty()) {
                return AgentResponse(
                    responseText = "Searching and opening $candidate...",
                    toolAction = ToolActionRequest.LaunchApp(candidate)
                )
            }
        }

        // Default conversational response
        return AgentResponse(
            responseText = "Namaste! Main aapka BWM AI Phone Assistant hoon. Main apps khol sakta hoon, flashlight, volume, battery status, alarm, timer aur notes control kar sakta hoon. Aap kya karna chahte hain?"
        )
    }
}
