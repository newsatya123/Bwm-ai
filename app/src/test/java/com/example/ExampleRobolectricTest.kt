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
}
