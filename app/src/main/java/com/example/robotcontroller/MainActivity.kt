package com.example.robotcontroller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.robotcontroller.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import com.example.robotcontroller.ai.AiManager
import com.example.robotcontroller.ai.RobotAction
import com.example.robotcontroller.bluetooth.BluetoothManager
import com.example.robotcontroller.voice.WakeWordDetector
import com.example.robotcontroller.voice.SpeechRecognizerManager
import com.example.robotcontroller.tasks.TaskHandler
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
    private lateinit var taskHandler: TaskHandler

    val YOUR_WAKEWORD_KEY = "YEWpY2uu/ejh97A66zakeL/RP8q3/su52qIU8Xy/BDdQsBgxnw/YkQ=="
    val YOUR_API_KEY = "AIzaSyDDWp7kM3kLNZehIXpWonZ92IGxa1_I2_E"

    companion object {
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1001
        private const val BLUETOOTH_PERMISSIONS_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check and request permissions first
        if (checkPermissions()) {
            initializeComponents()
        } else {
            requestPermissions()
        }

        // Setup UI listeners
        setupUIListeners()
    }

    private fun checkPermissions(): Boolean {
        val recordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val bluetoothConnectPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)

        return recordAudioPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_AUDIO_PERMISSION_REQUEST_CODE -> {
                var allGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }

                if (allGranted) {
                    Toast.makeText(this, "Permissions granted! Initializing voice recognition...", Toast.LENGTH_SHORT).show()
                    initializeComponents()
                } else {
                    Toast.makeText(this, "Permissions denied. Voice recognition will not work.", Toast.LENGTH_LONG).show()
                    // Initialize components without voice features
                    initializeComponentsWithoutVoice()
                }
            }
        }
    }

    private fun initializeComponents() {
        try {
            // Instantiate managers
            btMgr = BluetoothManager(this) { /* handle FPGA messages if needed */ }
            wake = WakeWordDetector(this, YOUR_WAKEWORD_KEY) { onWake() }
            speech = SpeechRecognizerManager(this) { onSpeech(it) }
            ai = AiManager(this, YOUR_API_KEY)
            taskHandler = TaskHandler(this, btMgr, lifecycleScope)

            // Initialize and start wake-word loop
            wake.initialize()
            wake.start()

            Toast.makeText(this, "Voice recognition initialized successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing voice components: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeComponentsWithoutVoice() {
        try {
            btMgr = BluetoothManager(this) { /* handle FPGA messages if needed */ }
            ai = AiManager(this, YOUR_API_KEY)
            taskHandler = TaskHandler(this, btMgr, lifecycleScope)

            // Disable voice-related buttons
            binding.btnSpeak.isEnabled = false
            binding.btnSpeak.alpha = 0.5f

            Toast.makeText(this, "Initialized without voice features", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUIListeners() {
        binding.btnConnect.setOnClickListener {
            if (::btMgr.isInitialized) {
                btMgr.initialize()
                btMgr.connect()
            }
        }

        binding.btnSpeak.setOnClickListener {
            if (::speech.isInitialized) {
                speech.startListening()
            } else {
                Toast.makeText(this, "Voice recognition not initialized. Please grant permissions and restart.", Toast.LENGTH_LONG).show()
            }
        }

        // Add stop task button listener (you may need to add this button to your layout)
        // binding.btnStopTask.setOnClickListener {
        //     if (::taskHandler.isInitialized) {
        //         taskHandler.cancelCurrentTask()
        //     }
        // }

        // Movement buttons for manual control
        setupHoldToSend(binding.btnForward, "MOV-FD-#")
        setupHoldToSend(binding.btnBack, "MOV-BD-#")
        setupHoldToSend(binding.btnLeft, "MOV-LD-#")
        setupHoldToSend(binding.btnRight, "MOV-RD-#")
    }

    private var moveJob: Job? = null

    private fun setupHoldToSend(button: View, command: String) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moveJob?.cancel() // stop any previous job
                    moveJob = lifecycleScope.launch {
                        while (isActive) {
                            if (::btMgr.isInitialized) {
                                btMgr.send(command)
                            }
                            delay(100) // send every 0.7 sec
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
        lifecycleScope.launch {
            if (::wake.isInitialized) {
                wake.stop() // release mic
            }

            delay(200) // give Android a moment to free the mic

            if (::speech.isInitialized) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Wake word detected! Listening...", Toast.LENGTH_SHORT).show()
                        speech.startListening()
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        RECORD_AUDIO_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun onSpeech(text: String) {
        Toast.makeText(this, "Heard: $text", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            if (::ai.isInitialized && ::taskHandler.isInitialized) {
                try {
                    // Get structured actions from AI
                    val actions = ai.interpretToActions(text)

                    if (actions.isNotEmpty()) {
                        // Display the planned actions
                        val actionsText = actions.joinToString(" → ") { action ->
                            when (action.action) {
                                "go_straight" -> "Forward ${action.params.firstOrNull()}ms"
                                "go_backward" -> "Backward ${action.params.firstOrNull()}ms"
                                "turn_left" -> "Turn Left"
                                "turn_right" -> "Turn Right"
                                "stop" -> "Stop"
                                else -> action.action
                            }
                        }

                        runOnUiThread {
                            binding.geminiResponseText.text = "Command: $text\n\nPlanned Actions:\n$actionsText\n\nExecuting..."
                        }

                        // Execute the action sequence
                        taskHandler.executeTaskSequence(
                            actions = actions,
                            onProgress = { current, total, action ->
                                runOnUiThread {
                                    binding.geminiResponseText.text =
                                        "Command: $text\n\nProgress: $current/$total\nCurrent: $action"
                                }
                            },
                            onComplete = {
                                runOnUiThread {
                                    binding.geminiResponseText.text =
                                        "Command: $text\n\nTask completed successfully! ✅"
                                }
                            },
                            onError = { error ->
                                runOnUiThread {
                                    binding.geminiResponseText.text =
                                        "Command: $text\n\nError: $error ❌"
                                }
                            }
                        )

                    } else {
                        // No actions generated, provide conversational response
                        val response = ai.interpret(text)
                        runOnUiThread {
                            binding.geminiResponseText.text = "You: $text\n\nAI: $response"
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        binding.geminiResponseText.text = "Error processing command: ${e.message}"
                    }
                }
            }

            // Restart wake word detection
            if (::wake.isInitialized) {
                wake.start()
            }
        }
    }

    /**
     * Manual method to execute predefined sequences for testing
     */
    private fun testTaskSequence() {
        if (::taskHandler.isInitialized) {
            val testActions = listOf(
                RobotAction("go_straight", listOf(2000)),  // Forward 2 seconds
                RobotAction("turn_left", emptyList()),      // Turn left
                RobotAction("go_straight", listOf(1000)),   // Forward 1 second
                RobotAction("turn_right", emptyList()),     // Turn right
                RobotAction("stop", emptyList())            // Stop
            )

            taskHandler.executeTaskSequence(testActions)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wake.isInitialized) {
            wake.stop()
            wake.release()
        }
        if (::btMgr.isInitialized) {
            btMgr.close()
        }
        if (::speech.isInitialized) {
            speech.destroy()
        }
        if (::taskHandler.isInitialized) {
            taskHandler.cancelCurrentTask()
        }
    }
}