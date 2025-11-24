
package com.example.robotcontroller.TTS

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*
import android.speech.tts.Voice

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onInitCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val TAG = "TtsManager"
        const val UTTERANCE_ID_AI_RESPONSE = "ai_response"
        const val UTTERANCE_ID_ACTION = "action"
    }

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { textToSpeech ->
                // Set language to US English
                val result = textToSpeech.setLanguage(Locale.US)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                    isInitialized = false
                    onInitCallback?.invoke(false)
                } else {
                    try {
                        val voices = textToSpeech.voices
                        val robotic = voices.firstOrNull {
                            it.name.contains("en-us-x-sfg", ignoreCase = true) ||
                                    it.name.contains("en-us-x-sfp", ignoreCase = true) ||
                                    it.name.contains("loc", ignoreCase = true) ||
                                    it.quality == Voice.QUALITY_HIGH
                        }

                        robotic?.let {
                            textToSpeech.voice = it
                            Log.d(TAG, "Selected robotic voice: ${it.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Voice selection failed: ${e.message}")
                    }
                    // Configure speech parameters
                    textToSpeech.setPitch(0.95f)  // Slightly lower pitch for robot feel
                    textToSpeech.setSpeechRate(1.2f)  // Slower speech rate

                    // Set up progress listener
                    textToSpeech.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                Log.d(TAG, "TTS started: $utteranceId")
                            }

                            override fun onDone(utteranceId: String?) {
                                Log.d(TAG, "TTS completed: $utteranceId")
                            }

                            override fun onError(utteranceId: String?) {
                                Log.e(TAG, "TTS error: $utteranceId")
                            }
                        }
                    )

                    isInitialized = true
                    onInitCallback?.invoke(true)
                    Log.d(TAG, "TTS initialized successfully")
                }
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
            isInitialized = false
            onInitCallback?.invoke(false)
        }
    }

    /**
     * Speak AI response from Gemini
     */
    fun speakAiResponse(text: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "Empty text provided")
            return
        }

        // Clean the text (remove special characters that don't sound good)
        val cleanText = text
            .replace("*", "")  // Remove asterisks from markdown
            .replace("#", "")
            .trim()

        speak(cleanText, UTTERANCE_ID_AI_RESPONSE)
    }

    /**
     * Speak action confirmation (e.g., "Moving forward", "Turning left")
     */
    fun speakAction(action: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        val actionText = when(action.uppercase()) {
            "FORWARD", "MOV-FD" -> "Moving forward"
            "BACKWARD", "MOV-BD" -> "Moving backward"
            "LEFT", "MOV-LD" -> "Turning left"
            "RIGHT", "MOV-RD" -> "Turning right"
            "STOP" -> "Stopping"
            "PICK", "GRIP" -> "Picking object"
            "RELEASE" -> "Releasing object"
            else -> action
        }

        speak(actionText, UTTERANCE_ID_ACTION)
    }

    /**
     * Speak custom text
     */
    fun speak(text: String, utteranceId: String = "custom") {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized")
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    /**
     * Stop current speech
     */
    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    /**
     * Set speech rate (0.5 = slow, 1.0 = normal, 2.0 = fast)
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    /**
     * Set pitch (0.5 = low, 1.0 = normal, 2.0 = high)
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    /**
     * Call this in Activity's onDestroy
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        Log.d(TAG, "TTS shutdown")
    }

    /**
     * Set callback for initialization status
     */
    fun setOnInitCallback(callback: (Boolean) -> Unit) {
        this.onInitCallback = callback
    }
}
