package com.example.robotcontroller



import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.robotcontroller.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import com.example.robotcontroller.ai.AiManager
import com.example.robotcontroller.bluetooth.BluetoothManager
import com.example.robotcontroller.voice.WakeWordDetector
import com.example.robotcontroller.voice.SpeechRecognizerManager
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch



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
        setupHoldToSend(binding.btnForward, "MOV-FD-#")
        setupHoldToSend(binding.btnBack, "MOV-FD-#")
        setupHoldToSend(binding.btnLeft, "MOV-LD-#")
        setupHoldToSend(binding.btnRight, "MOV-RD-#")


    }
    private var moveJob: Job? = null

    private fun setupHoldToSend(button: View, command: String) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moveJob?.cancel()  // stop any previous job
                    moveJob = lifecycleScope.launch {
                        while (isActive) {
                            btMgr.send(command)
                            delay(700) // send every 0.7 sec
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    moveJob?.cancel()
                }
            }
            true
        }
    }

    private fun onWake() {
        runOnUiThread { speech.startListening() }
    }

    private fun onSpeech(text: String) {
        Toast.makeText(this, "Heard: $text", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val response = ai.interpret(text)
            Toast.makeText(this@MainActivity, response, Toast.LENGTH_LONG).show()
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

