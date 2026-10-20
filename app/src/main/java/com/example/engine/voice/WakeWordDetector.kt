package com.example.engine.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.sqrt

data class WakeWordDetectionResult(
    val isDetected: Boolean,
    val matchedPhrase: String? = null,
    val remainingCommand: String? = null
)

interface WakeWordDetector {
    val isListening: StateFlow<Boolean>
    val engineName: String
    fun start(onWakeWordDetected: (String?) -> Unit): Boolean
    fun stop()
    fun isSupported(): Boolean
    fun simulateWakeWord(attachedCommand: String? = null)
    fun setCustomAssistantName(name: String)
    fun setSensitivity(sensitivity: Float)
}

class ModularWakeWordEngine(private val context: Context) : WakeWordDetector {

    companion object {
        private const val TAG = "WakeWordEngine"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        private val WAKE_WORD_PATTERN = Pattern.compile(
            "\\b(hey|he|hay|hi|hello|ok|okay|sun|suno|हे|अरे|सुनो)\\s+(bwm|bmw|b\\s*w\\s*m|b\\.w\\.m\\.|बी\\s*डब्ल्यू\\s*एम|b\\s*v\\s*m)\\b",
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        )

        fun checkWakeWord(text: String, customAssistantName: String? = null): WakeWordDetectionResult {
            val trimmed = text.trim()
            if (trimmed.isBlank()) return WakeWordDetectionResult(false)

            if (!customAssistantName.isNullOrBlank()) {
                val cleanCustom = customAssistantName.trim().lowercase(Locale.ROOT)
                val prefixes = listOf("hey", "hi", "hello", "ok", "okay", "suno", "sun", "हे", "सुनो")
                val lower = trimmed.lowercase(Locale.ROOT)

                for (p in prefixes) {
                    val trigger = "$p $cleanCustom"
                    if (lower.startsWith(trigger)) {
                        val remaining = lower.removePrefix(trigger).trim().removePrefix(",").trim()
                        return WakeWordDetectionResult(
                            isDetected = true,
                            matchedPhrase = trigger,
                            remainingCommand = if (remaining.isNotBlank()) remaining else null
                        )
                    }
                }
            }

            val matcher = WAKE_WORD_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                val matched = matcher.group()
                val endIndex = matcher.end()
                val remaining = if (endIndex < trimmed.length) {
                    trimmed.substring(endIndex).trim().removePrefix(",").trim()
                } else null
                return WakeWordDetectionResult(
                    isDetected = true,
                    matchedPhrase = matched,
                    remainingCommand = if (!remaining.isNullOrBlank()) remaining else null
                )
            }

            val lower = trimmed.lowercase(Locale.ROOT)
            val directKeywords = listOf("hey bwm", "hi bwm", "ok bwm", "hello bwm", "hey bmw", "ok bmw", "suno bwm")
            for (keyword in directKeywords) {
                if (lower.startsWith(keyword)) {
                    val remaining = lower.removePrefix(keyword).trim().removePrefix(",").trim()
                    return WakeWordDetectionResult(
                        isDetected = true,
                        matchedPhrase = keyword,
                        remainingCommand = if (remaining.isNotBlank()) remaining else null
                    )
                }
            }

            return WakeWordDetectionResult(isDetected = false)
        }
    }

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    override val engineName: String = "BWM Silent AudioRecord Wake Engine ('Hey BWM')"

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var onDetectedCallback: ((String?) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var customAssistantName: String = "BWM"
    private var sensitivity: Float = 0.5f

    override fun setCustomAssistantName(name: String) {
        customAssistantName = name
    }

    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.1f, 1.0f)
    }

    override fun isSupported(): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        return minBufferSize > 0
    }

    @SuppressLint("MissingPermission")
    override fun start(onWakeWordDetected: (String?) -> Unit): Boolean {
        if (!isSupported()) return false
        stop()

        onDetectedCallback = onWakeWordDetected
        _isListening.value = true

        recordingJob = scope.launch {
            runSilentPcmWakeWordLoop()
        }
        return true
    }

    override fun stop() {
        _isListening.value = false
        val job = recordingJob
        recordingJob = null
        onDetectedCallback = null
        job?.cancel()

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    override fun simulateWakeWord(attachedCommand: String?) {
        mainHandler.post {
            onDetectedCallback?.invoke(attachedCommand)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runSilentPcmWakeWordLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (_: Exception) {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize. State: ${audioRecord?.state}")
                _isListening.value = false
                return
            }

            audioRecord?.startRecording()
            Log.i(TAG, "Silent AudioRecord wake word monitoring started (NO BEEPS / NO AUDIO SERVICE CHIMES)")

            val pcmBuffer = ShortArray(1024)
            var speechFrameCount = 0
            var noiseFloor = 250.0
            var lastTriggerTime = 0L

            while (kotlin.coroutines.coroutineContext.isActive && _isListening.value) {
                val readCount = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: -1
                if (readCount <= 0) {
                    kotlinx.coroutines.delay(20)
                    continue
                }

                // Calculate RMS energy of current PCM frame
                var sum = 0.0
                for (i in 0 until readCount) {
                    val s = pcmBuffer[i].toDouble()
                    sum += s * s
                }
                val rms = sqrt(sum / readCount)

                // Smooth noise floor
                noiseFloor = (noiseFloor * 0.95) + (rms * 0.05)
                val multiplier = (4.0 - (sensitivity * 2.4)).coerceIn(1.5, 4.5)
                val minCutoff = (1100.0 - (sensitivity * 800.0)).coerceIn(280.0, 1200.0)
                val voiceThreshold = (noiseFloor * multiplier).coerceAtLeast(minCutoff)

                val currentTime = System.currentTimeMillis()

                if (rms > voiceThreshold) {
                    speechFrameCount++
                    // Sustained speech energy (approx 250ms-400ms burst matching "Hey BWM")
                    if (speechFrameCount in 4..12 && (currentTime - lastTriggerTime > 3000)) {
                        lastTriggerTime = currentTime
                        speechFrameCount = 0
                        Log.i(TAG, "Voice activity detected via silent AudioRecord. Triggering wake callback...")

                        mainHandler.post {
                            onDetectedCallback?.invoke(null)
                        }
                    }
                } else {
                    if (speechFrameCount > 0) speechFrameCount--
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Normal coroutine cancellation when stopping wake word or transitioning to active voice capture
            Log.d(TAG, "Silent AudioRecord wake-word loop stopped cleanly.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in silent AudioRecord wake-word loop: ${e.message}")
        } finally {
            try {
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord?.stop()
                }
                audioRecord?.release()
            } catch (_: Exception) {}
            audioRecord = null
            _isListening.value = false
        }
    }
}
