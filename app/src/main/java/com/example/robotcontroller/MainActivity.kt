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
import com.example.robotcontroller.ai.AiResponse
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
import android.util.Log

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
        private const val TAG = "MainActivity"
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
                    initializeComponentsWithoutVoice()
                }
            }
        }
    }

    private fun initializeComponents() {
        try {
            btMgr = BluetoothManager(this) { /* handle FPGA messages if needed */ }
            wake = WakeWordDetector(this, YOUR_WAKEWORD_KEY) { onWake() }
            speech = SpeechRecognizerManager(this) { onSpeech(it) }
            ai = AiManager(this, YOUR_API_KEY)
            taskHandler = TaskHandler(this, btMgr, lifecycleScope)

            wake.initialize()
            wake.start()

            Toast.makeText(this, "Voice recognition initialized successfully!", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "All components initialized successfully")

        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing voice components: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error initializing components", e)
        }
    }

    private fun initializeComponentsWithoutVoice() {
        try {
            btMgr = BluetoothManager(this) { /* handle FPGA messages if needed */ }
            ai = AiManager(this, YOUR_API_KEY)
            taskHandler = TaskHandler(this, btMgr, lifecycleScope)

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
                    moveJob?.cancel()
                    moveJob = lifecycleScope.launch {
                        while (isActive) {
                            if (::btMgr.isInitialized) {
                                btMgr.send(command)
                            }
                            delay(100)
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
                wake.stop()
            }

            delay(200)

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
        Log.d(TAG, "Speech input received: $text")

        lifecycleScope.launch {
            if (::ai.isInitialized && ::taskHandler.isInitialized) {
                try {
                    // Single AI call that handles both actions and conversations
                    val aiResponse = ai.processInput(text)
                    Log.d(TAG, "AI response type: ${aiResponse::class.simpleName}")

                    when (aiResponse) {
                        is AiResponse.Actions -> {
                            // Handle movement commands
                            Log.d(TAG, "Executing actions: ${aiResponse.actions.map { it.action }}")

                            val actionsText = aiResponse.actions.joinToString(" → ") { action ->
                                when (action.action) {
                                    "go_straight" -> "Forward ${action.params.firstOrNull()}ms"
                                    "go_backward" -> "Backward ${action.params.firstOrNull()}ms"
                                    "turn_left" -> "Turn Left"
                                    "turn_right" -> "Turn Right"
                                    "stop" -> "Stop"
                                    else -> action.action
                                }
                            }

                            // Show recent conversation context
                            val recentConversations = ai.getConversationMemory().getRecentConversations(2)
                            val contextText = if (recentConversations.isNotEmpty()) {
                                "\nRecent context:\n" + recentConversations.joinToString("\n") {
                                    "• ${it.userInput} → ${if (it.isMovementCommand) "[Action executed]" else it.aiResponse}"
                                }
                            } else ""

                            runOnUiThread {
                                binding.geminiResponseText.text =
                                    "Command: $text\n\nPlanned Actions:\n$actionsText\n\nExecuting...$contextText"
                            }

                            // Execute the action sequence
                            taskHandler.executeTaskSequence(
                                actions = aiResponse.actions,
                                onProgress = { current, total, action ->
                                    runOnUiThread {
                                        binding.geminiResponseText.text =
                                            "Command: $text\n\nProgress: $current/$total\nCurrent: $action$contextText"
                                    }
                                },
                                onComplete = {
                                    runOnUiThread {
                                        binding.geminiResponseText.text =
                                            "Command: $text\n\nTask completed successfully! ✅$contextText"
                                    }
                                },
                                onError = { error ->
                                    runOnUiThread {
                                        binding.geminiResponseText.text =
                                            "Command: $text\n\nError: $error ❌$contextText"
                                    }
                                }
                            )
                        }

                        is AiResponse.Conversation -> {
                            // Handle conversational responses
                            Log.d(TAG, "Conversational response: ${aiResponse.message}")

                            // Show conversation history
                            val recentConversations = ai.getConversationMemory().getRecentConversations(3)
                            val historyText = if (recentConversations.size > 1) {
                                "\n\n--- Recent conversation ---\n" +
                                        recentConversations.dropLast(1).joinToString("\n") {
                                            "You: ${it.userInput}\nRobo: ${if (it.isMovementCommand) "[Executed ${it.aiResponse}]" else it.aiResponse}"
                                        }
                            } else ""

                            runOnUiThread {
                                binding.geminiResponseText.text =
                                    "You: $text\n\nRobo: ${aiResponse.message}$historyText"
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing speech input", e)
                    runOnUiThread {
                        binding.geminiResponseText.text = "Error processing command: ${e.message}"
                    }
                }
            }

            // Restart wake word detection
            delay(1000)
            if (::wake.isInitialized) {
                Log.d(TAG, "Restarting wake word detection")
                wake.start()
            }
        }
    }

    /**
     * Clear conversation memory
     */
    fun clearConversationMemory() {
        if (::ai.isInitialized) {
            ai.clearMemory()
            runOnUiThread {
                binding.geminiResponseText.text = "Conversation memory cleared! Starting fresh."
                Toast.makeText(this, "Memory cleared", Toast.LENGTH_SHORT).show()
            }
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