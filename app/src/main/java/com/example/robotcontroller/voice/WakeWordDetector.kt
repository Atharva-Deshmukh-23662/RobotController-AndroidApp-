package com.example.robotcontroller.voice

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.Context
import android.util.Log

class WakeWordDetector(
    private val context: Context,
    private val apiKey: String,
    private val onWake: () -> Unit
) {
    private val manager = WakeWordManager(context) { onWake() }

    fun initialize() {
        manager.initialize(apiKey)
    }

    fun start() = manager.start()
    fun stop() = manager.stop()
    fun release() = manager.release()
}

class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {

    private var porcupineManager: PorcupineManager? = null

    fun initialize(accessKey: String) {
        try {
            val callback = PorcupineManagerCallback { keywordIndex ->
                Log.d("WakeWord", "Detected keyword index: $keywordIndex")
                onWakeWordDetected()
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
//              .setKeywords(arrayOf(Porcupine.BuiltInKeyword.PORCUPINE, Porcupine.BuiltInKeyword.BUMBLEBEE))
                .setKeywordPaths(arrayOf("hey-Robo.ppn"))
                .build(this.context, callback)

        } catch (e: Exception) {
            Log.e("WakeWord", "Failed to initialize Porcupine: ${e.message}")
        }
    }

    fun start() {
        try {
            porcupineManager?.start()
            Log.d("WakeWord", "Porcupine started")
        } catch (e: Exception) {
            Log.e("WakeWord", "Error starting Porcupine: ${e.message}")
        }
    }

    fun stop() {
        try {
            porcupineManager?.stop()
            Log.d("WakeWord", "Porcupine stopped")
        } catch (e: Exception) {
            Log.e("WakeWord", "Error stopping Porcupine: ${e.message}")
        }
    }

    fun release() {
        porcupineManager?.delete()
        porcupineManager = null
    }
}