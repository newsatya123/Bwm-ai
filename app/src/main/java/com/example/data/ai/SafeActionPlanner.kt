package com.example.data.ai

import android.util.Log
import com.example.data.remote.AgentResponse
import com.example.data.remote.ToolActionRequest
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

class SafeActionPlanner {

    companion object {
        private const val TAG = "SafeActionPlanner"
    }

    /**
     * Evaluates whether a prompt can be completely and confidently handled by the local offline NLP engine.
     * Returns true if local execution should be preferred (or if offline mode is enforced).
     */
    fun canHandleLocally(prompt: String, activeApp: String? = null): Boolean {
        val p = prompt.lowercase().trim()

        // 1. Core Task Queue & Playback Commands
        if (p == "stop" || p == "cancel" || p == "pause" || p == "resume" || p == "repeat" ||
            p.contains("task queue") || p.contains("status batao") || p.contains("padhna band") ||
            p.contains("reading pause") || p.contains("reading resume") || p.contains("dobara padho") ||
            p.contains("agla message") || p.contains("assistant band karo")
        ) {
            return true
        }

        // 2. Hardware & Device Toggles
        if (p.contains("flashlight") || p.contains("torch") || p.contains("battery") ||
            p.contains("volume") || p.contains("aawaz") || p.contains("timer") || p.contains("alarm") ||
            p.contains("wifi") || p.contains("bluetooth")
        ) {
            return true
        }

        // 3. Direct App Launching
        if (p.startsWith("open ") || p.startsWith("kholo ") || p.endsWith(" kholo") ||
            p.endsWith(" open karo") || p.endsWith(" launch karo")
        ) {
            return true
        }

        // 4. WhatsApp Direct Actions (including active chat follow-ups and media sharing)
        if (p.contains("whatsapp") || p.contains("message") || p.contains("bhejo") ||
            p.contains("isse pucho") || p.contains("usse bolo") || p.contains("ab bolo") ||
            p.contains("photo bhejo") || p.contains("screenshot bhejo") || p.contains("pdf bhejo")
        ) {
            return true
        }

        // 5. Calling
        if (p.startsWith("call ") || p.startsWith("dial ") || p.endsWith(" ko call karo") ||
            p.endsWith(" ko phone lagao") || p.endsWith(" ko dial karo")
        ) {
            return true
        }

        // 6. Notes
        if (p.startsWith("note ") || p.startsWith("save note") || p.contains("note likho") ||
            p == "notes" || p.contains("show notes")
        ) {
            return true
        }

        // 7. Screen Reading & YouTube
        if (p.contains("screen par kya") || p.contains("ye text padho") || p.contains("pdf kholo") ||
            p.contains("youtube par") || p.contains("video chalao")
        ) {
            return true
        }

        return false
    }

    /**
     * Parses and validates raw model text (which might be JSON, conversational text, or mixed)
     * into a safe AgentResponse with strictly allowed actions.
     */
    fun parseModelResponse(rawText: String, userPrompt: String): AgentResponse {
        val clean = rawText.trim()

        // 1. Check if the model returned a JSON structure
        val jsonStart = clean.indexOf('{')
        val jsonEnd = clean.lastIndexOf('}')

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            try {
                val jsonSubstring = clean.substring(jsonStart, jsonEnd + 1)
                val json = JSONObject(jsonSubstring)
                return parseValidatedJsonPlan(json, clean, userPrompt)
            } catch (e: Exception) {
                Log.d(TAG, "Not a strict JSON object, parsing as conversational text: ${e.message}")
            }
        }

        // 2. Fallback: Pure conversational assistant response
        return AgentResponse(
            responseText = clean.ifBlank { "Sir, I am standing by." },
            toolActions = emptyList(),
            requiresConfirmation = false
        )
    }

    /**
     * Validates JSON against the strict allowlist of supported actions.
     */
    private fun parseValidatedJsonPlan(json: JSONObject, originalText: String, userPrompt: String): AgentResponse {
        val spokenResponse = json.optString("response", "").ifBlank {
            json.optString("thought", originalText)
        }

        val intent = json.optString("intent", "").lowercase().trim()
        val actions = mutableListOf<ToolActionRequest>()
        var requiresConfirmation = json.optBoolean("requires_confirmation", false)
        var confirmationPrompt: String? = null

        when (intent) {
            "send_message", "whatsapp_send" -> {
                val recipient = json.optString("recipient", "").trim()
                val message = json.optString("message", "").trim()
                if (recipient.isNotBlank() && message.isNotBlank()) {
                    actions.add(ToolActionRequest.WhatsAppSend(recipient, message))
                    if (recipient.equals("unknown", true) || recipient.length < 2) {
                        requiresConfirmation = true
                        confirmationPrompt = "Sir, kya aap \"$recipient\" ko \"$message\" bhejna chahte hain?"
                    }
                }
            }

            "send_media", "whatsapp_send_media" -> {
                val recipient = json.optString("recipient", "").trim()
                val mediaType = json.optString("media_type", "latest_photo").trim()
                val caption = json.optString("caption", "").trim()
                if (recipient.isNotBlank()) {
                    actions.add(ToolActionRequest.WhatsAppSendMedia(recipient, mediaType, caption.ifBlank { null }))
                    requiresConfirmation = true
                    confirmationPrompt = "Sir, $recipient ko $mediaType bhejne se pehle confirmation chahiye."
                }
            }

            "launch_app", "open_app" -> {
                val appName = json.optString("app_name", "").ifBlank {
                    json.optString("target_app", "")
                }.trim()
                if (appName.isNotBlank()) {
                    actions.add(ToolActionRequest.LaunchApp(appName))
                }
            }

            "toggle_flashlight" -> {
                val enable = json.optBoolean("enable", true)
                actions.add(ToolActionRequest.ToggleFlashlight(enable))
            }

            "adjust_volume" -> {
                val direction = json.optString("direction", "up").trim()
                val level = json.optInt("level_percent", -1).takeIf { it in 0..100 }
                actions.add(ToolActionRequest.AdjustVolume(direction, level))
            }

            "get_battery_status" -> {
                actions.add(ToolActionRequest.GetBatteryStatus)
            }

            "set_timer" -> {
                val seconds = json.optInt("seconds", 60)
                val label = json.optString("label", "Timer")
                actions.add(ToolActionRequest.SetTimer(seconds, label))
            }

            "set_alarm" -> {
                val hour = json.optInt("hour", 7)
                val minute = json.optInt("minute", 0)
                val message = json.optString("message", "Alarm")
                actions.add(ToolActionRequest.SetAlarm(hour, minute, message))
            }

            "dial_phone", "call" -> {
                val contact = json.optString("contact_name", "").ifBlank {
                    json.optString("recipient", "")
                }.trim()
                val number = json.optString("phone_number", "").trim()
                actions.add(ToolActionRequest.DialPhone(number, contact.ifBlank { null }))
                requiresConfirmation = true
                confirmationPrompt = "Sir, ${contact.ifBlank { number }} ko call connect karein?"
            }

            "youtube_search" -> {
                val query = json.optString("query", "").trim()
                val autoPlay = json.optBoolean("auto_play", true)
                if (query.isNotBlank()) {
                    actions.add(ToolActionRequest.YouTubeSearch(query, autoPlay))
                }
            }

            "youtube_control" -> {
                val action = json.optString("action", "play").trim()
                actions.add(ToolActionRequest.YouTubeControl(action))
            }

            "save_note" -> {
                val title = json.optString("title", "Voice Note").trim()
                val content = json.optString("content", "").trim()
                if (content.isNotBlank()) {
                    actions.add(ToolActionRequest.SaveNote(title, content))
                }
            }

            "read_screen" -> {
                val mode = json.optString("mode", "read").trim()
                val query = json.optString("query").takeIf { it.isNotBlank() }?.trim()
                actions.add(ToolActionRequest.ReadScreen(query, mode))
            }

            "read_whatsapp_chat" -> {
                val recipient = json.optString("recipient").takeIf { it.isNotBlank() }?.trim()
                val count = json.optInt("count", 1)
                val query = json.optString("query").takeIf { it.isNotBlank() }?.trim()
                actions.add(ToolActionRequest.ReadWhatsAppChat(recipient, count, query))
            }

            "search_and_summarize_web" -> {
                val query = json.optString("query", userPrompt).trim()
                val openFirst = json.optBoolean("open_first_result", false)
                actions.add(ToolActionRequest.SearchAndSummarizeWeb(query, openFirst))
            }

            "open_pdf_document" -> {
                val fileName = json.optString("file_name").takeIf { it.isNotBlank() }?.trim()
                actions.add(ToolActionRequest.OpenPdfDocument(fileName))
            }

            "read_pdf" -> {
                val page = json.optInt("page_number", -1).takeIf { it > 0 }
                val action = json.optString("action", "read").trim()
                actions.add(ToolActionRequest.ReadPdf(page, action))
            }

            "control_reading" -> {
                val cmd = json.optString("command", "stop").trim()
                actions.add(ToolActionRequest.ControlReading(cmd))
            }
        }

        return AgentResponse(
            responseText = spokenResponse.ifBlank { "Action prepared: $intent" },
            toolActions = actions,
            requiresConfirmation = requiresConfirmation,
            confirmationPrompt = confirmationPrompt
        )
    }
}
