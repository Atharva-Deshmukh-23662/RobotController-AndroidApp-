package com.example.robotcontroller

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.DisplayContext.LENGTH_SHORT
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.robotcontroller.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.*
import com.google.ai.client.generativeai.*
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import com.example.robotcontroller.ai.AiManager
import com.example.robotcontroller.bluetooth.BluetoothManager
import com.example.robotcontroller.voice.WakeWordDetector
import com.example.robotcontroller.voice.SpeechRecognizerManager


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var btMgr: BluetoothManager
    private lateinit var wake: WakeWordDetector
    private lateinit var speech: SpeechRecognizerManager
    private lateinit var ai: AiManager

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Instantiate managers
        val YOUR_WAKEWORD_KEY = "YEWpY2uu/ejh97A66zakeL/RP8q3/su52qIU8Xy/BDdQsBgxnw/YkQ=="
        val YOUR_API_KEY= "Iz**aSyDDWp7kM3kLNZehIXpWonZ92IGxa1_I2_E"
        btMgr = BluetoothManager(this) { /* handle FPGA messages if needed */ }
        wake = WakeWordDetector(this, YOUR_WAKEWORD_KEY) { onWake() }
        speech = SpeechRecognizerManager(this) { onSpeech(it) }

        ai = AiManager(this, YOUR_API_KEY)

        // Initialize and start wake-word loop
        wake.initialize(); wake.start()

        binding.btnConnect.setOnClickListener { btMgr.initialize(); btMgr.connect() }
        binding.btnSpeak.setOnClickListener { speech.startListening() }
        // Movement buttons can call btMgr.send(...)
    }

    private fun onWake() {
        runOnUiThread { speech.startListening() }
    }

    private fun onSpeech(text: String) {
        Toast.makeText(this, "Heard: $text", LENGTH_SHORT).show()
        lifecycleScope.launch {
            val response = ai.interpret(text)
            Toast.makeText(this@MainActivity, response, LENGTH_LONG).show()
            // Map commands to btMgr.send(...)
            wake.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wake.stop(); wake.release()
        btMgr.close()
    }
}

