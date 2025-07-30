package com.example.robotcontroller.wakeword

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.*

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
