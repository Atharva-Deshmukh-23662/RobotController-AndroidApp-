package com.example.robotcontroller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.robotcontroller.ai.AiManager
import com.example.robotcontroller.ai.AiResponse
import com.example.robotcontroller.bluetooth.BluetoothManager
import com.example.robotcontroller.databinding.ActivityMainBinding
import com.example.robotcontroller.tasks.TaskHandler
import com.example.robotcontroller.voice.SpeechRecognizerManager
import com.example.robotcontroller.voice.WakeWordDetector
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
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    private val YOUR_WAKEWORD_KEY = "I9J2ohbN5LKHHCDY+7VHWvFbAr07qTnqXE5pmUiza0m+0nEXz3CzfA=="
    private val YOUR_API_KEY = "AIzaSyC45yM8KxDGP7fvJWWI2DlItbuZu-O_75c"

    companion object {
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1001
    }
    private fun checkAndRequestBluetoothPermissions() {
        val missing = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make the app full-screen
        window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        checkAndRequestBluetoothPermissions()

        if (checkPermissions()) {
            initializeComponents()
        } else {
            requestPermissions()
        }

        setupUIListeners()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeComponents()
            } else {
                Toast.makeText(this, "Permission denied. App may not function correctly.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeComponents() {
        btMgr = BluetoothManager(this) {}
        wake = WakeWordDetector(this, YOUR_WAKEWORD_KEY, this::onWake)
        speech = SpeechRecognizerManager(this, this::onSpeech)
        ai = AiManager(this, YOUR_API_KEY)
        taskHandler = TaskHandler(this, btMgr, lifecycleScope)

        wake.initialize()
        wake.start()
    }

    private fun setupUIListeners() {
        binding.menuIcon.setOnClickListener {
            binding.menuLayout.visibility = if (binding.menuLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnConnect.setOnClickListener {
            btMgr.initialize()
            btMgr.connect()
        }

        binding.btnSpeak.setOnClickListener {
            speech.startListening()
        }

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
                            btMgr.send(command)
                            delay(100)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    moveJob?.cancel()
                    true
                }
                else -> false
            }
        }
    }

    private fun onWake() {
        lifecycleScope.launch {
            wake.stop()
            delay(200)
            runOnUiThread {
                animateListening(true)
                binding.heardText.text = "Listening..."
                speech.startListening()
            }
        }
    }

    private fun onSpeech(text: String) {
        lifecycleScope.launch {
            runOnUiThread {
                animateListening(false)
                binding.heardText.text = "Heard: $text"
            }

            val aiResponse = ai.processInput(text)
            when (aiResponse) {
                is AiResponse.Actions -> {
                    taskHandler.executeTaskSequence(
                        aiResponse.actions,
                        onProgress = { current, total, action ->
                            runOnUiThread {
                                binding.geminiResponseText.text = "Executing action $current of $total: $action"
                            }
                        },
                        onComplete = {
                            runOnUiThread {
                                binding.geminiResponseText.text = "All actions completed!"
                            }
                        },
                        onError = {
                            runOnUiThread {
                                binding.geminiResponseText.text = "An error occurred during execution."
                            }
                        }
                    )
                    runOnUiThread { binding.geminiResponseText.text = "Executing actions..." }
                }
                is AiResponse.Conversation -> {
                    runOnUiThread { binding.geminiResponseText.text = aiResponse.message }
                }
            }

            delay(1000)
            wake.start()
        }
    }

    private fun animateListening(isListening: Boolean) {
        val scale = if (isListening) 1.2f else 1.0f
        val anim = ScaleAnimation(1.0f, scale, 1.0f, scale, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        anim.duration = 300
        anim.fillAfter = true
        binding.leftEye.startAnimation(anim)
        binding.rightEye.startAnimation(anim)
    }

    override fun onDestroy() {
        super.onDestroy()
        wake.release()
        btMgr.close()
        speech.destroy()
        taskHandler.cancelCurrentTask()
    }
}
