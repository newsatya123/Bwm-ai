package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.GeminiAssistantAgent
import com.example.data.remote.ToolActionRequest
import com.example.engine.voice.ModularWakeWordEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BWM AI", appName)
    }

    @Test
    fun `wake word engine detects hey bwm activation and extracts attached commands`() {
        // Pure wake word
        val res1 = ModularWakeWordEngine.checkWakeWord("hey bwm")
        assertTrue(res1.isDetected)
        assertNull(res1.remainingCommand)

        // Wake word + command
        val res2 = ModularWakeWordEngine.checkWakeWord("Hey BWM turn on flashlight")
        assertTrue(res2.isDetected)
        assertEquals("turn on flashlight", res2.remainingCommand)

        // Hindi wake word variation + command
        val res3 = ModularWakeWordEngine.checkWakeWord("suno bwm whatsapp kholo")
        assertTrue(res3.isDetected)
        assertEquals("whatsapp kholo", res3.remainingCommand)

        // Common speech misrecognition for BWM
        val res4 = ModularWakeWordEngine.checkWakeWord("ok bmw battery status")
        assertTrue(res4.isDetected)
        assertEquals("battery status", res4.remainingCommand)

        // Negative case
        val res5 = ModularWakeWordEngine.checkWakeWord("good morning what is the weather today")
        assertFalse(res5.isDetected)
    }

    @Test
    fun `offline nlp parses flashlight commands in hindi and english`() {
        val agent = GeminiAssistantAgent()

        val resp1 = agent.parseWithOfflineNlp("turn on flashlight")
        assertTrue(resp1.toolAction is ToolActionRequest.ToggleFlashlight)
        assertEquals(true, (resp1.toolAction as ToolActionRequest.ToggleFlashlight).enable)

        val resp2 = agent.parseWithOfflineNlp("torch band karo")
        assertTrue(resp2.toolAction is ToolActionRequest.ToggleFlashlight)
        assertEquals(false, (resp2.toolAction as ToolActionRequest.ToggleFlashlight).enable)
    }

    @Test
    fun `offline nlp parses app launch commands`() {
        val agent = GeminiAssistantAgent()

        val resp = agent.parseWithOfflineNlp("whatsapp kholo")
        assertTrue(resp.toolAction is ToolActionRequest.LaunchApp)
        assertEquals("whatsapp", (resp.toolAction as ToolActionRequest.LaunchApp).appName)
    }

    @Test
    fun `offline nlp parses battery status commands`() {
        val agent = GeminiAssistantAgent()

        val resp = agent.parseWithOfflineNlp("battery kitni hai")
        assertNotNull(resp.toolAction)
        assertTrue(resp.toolAction is ToolActionRequest.GetBatteryStatus)
    }

    @Test
    fun `offline nlp parses whatsapp voice commands`() {
        val agent = GeminiAssistantAgent()

        // 1. WhatsApp send command: "Raman ko hii bhejo"
        val resp1 = agent.parseWithOfflineNlp("Raman ko hii bhejo")
        assertTrue("Expected WhatsAppSend action", resp1.toolAction is ToolActionRequest.WhatsAppSend)
        val sendAction = resp1.toolAction as ToolActionRequest.WhatsAppSend
        assertEquals("Raman", sendAction.recipient)
        assertEquals("hii", sendAction.message)

        // 2. WhatsApp send with detailed message: "Raman ko message bhejo: main ghar aa raha hoon"
        val resp2 = agent.parseWithOfflineNlp("Raman ko message bhejo: main ghar aa raha hoon")
        assertTrue("Expected WhatsAppSend action", resp2.toolAction is ToolActionRequest.WhatsAppSend)
        val sendAction2 = resp2.toolAction as ToolActionRequest.WhatsAppSend
        assertEquals("Raman", sendAction2.recipient)
        assertEquals("main ghar aa raha hoon", sendAction2.message)

        // 3. WhatsApp open chat: "Raman ki chat kholo"
        val resp3 = agent.parseWithOfflineNlp("Raman ki chat kholo")
        assertTrue("Expected WhatsAppOpenChat action", resp3.toolAction is ToolActionRequest.WhatsAppOpenChat)
        val openAction = resp3.toolAction as ToolActionRequest.WhatsAppOpenChat
        assertEquals("Raman", openAction.recipient)

        // 4. WhatsApp search: "WhatsApp mein search karo"
        val resp4 = agent.parseWithOfflineNlp("WhatsApp mein search karo office group")
        assertTrue("Expected WhatsAppSearch action", resp4.toolAction is ToolActionRequest.WhatsAppSearch)
        val searchAction = resp4.toolAction as ToolActionRequest.WhatsAppSearch
        assertEquals("office group", searchAction.query)
    }

    @Test
    fun `offline nlp parses context-aware active app commands`() {
        val agent = GeminiAssistantAgent()

        // Scrolling: "Chat mein neeche scroll karo"
        val resp1 = agent.parseWithOfflineNlp("Chat mein neeche scroll karo")
        assertTrue("Expected ActiveAppContextAction", resp1.toolAction is ToolActionRequest.ActiveAppContextAction)
        val action1 = resp1.toolAction as ToolActionRequest.ActiveAppContextAction
        assertEquals("scroll_down", action1.action)

        // Scrolling up: "upar scroll karo"
        val resp2 = agent.parseWithOfflineNlp("upar scroll karo")
        assertTrue(resp2.toolAction is ToolActionRequest.ActiveAppContextAction)
        assertEquals("scroll_up", (resp2.toolAction as ToolActionRequest.ActiveAppContextAction).action)

        // Playback: "pause karo"
        val resp3 = agent.parseWithOfflineNlp("pause karo")
        assertTrue(resp3.toolAction is ToolActionRequest.ActiveAppContextAction)
        assertEquals("pause", (resp3.toolAction as ToolActionRequest.ActiveAppContextAction).action)

        // Navigation: "back jao"
        val resp4 = agent.parseWithOfflineNlp("back jao")
        assertTrue(resp4.toolAction is ToolActionRequest.ActiveAppContextAction)
        assertEquals("back", (resp4.toolAction as ToolActionRequest.ActiveAppContextAction).action)
    }

    @Test
    fun `contacts manager normalizes phone numbers correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.engine.actions.ContactsManager(context)

        assertEquals("919876543210", manager.normalizePhoneNumber("+91 98765 43210"))
        assertEquals("919876543210", manager.normalizePhoneNumber("9876543210"))
        assertEquals("919876543210", manager.normalizePhoneNumber("09876543210"))
        assertEquals("919876543210", manager.normalizePhoneNumber("+91-987-654-3210"))
    }
}
