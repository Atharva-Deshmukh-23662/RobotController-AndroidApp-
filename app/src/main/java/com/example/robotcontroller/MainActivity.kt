package com.example.robotcontroller

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.robotcontroller.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*
import com.example.robotcontroller.wakeword.WakeWordManager // ✅ Import your WakeWordManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
    private lateinit var wakeWordManager: WakeWordManager


    // ✅ For speech recognition
    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.get(0)?.lowercase(Locale.getDefault()) ?: ""
            Toast.makeText(this, "Heard: $spokenText", Toast.LENGTH_LONG).show()

            when (spokenText) {
                "start" -> sendCommand("CSL-WU-#")
                "forward" -> sendCommand("CSL-WU-#")
                "back" -> sendCommand("B")
                "left" -> sendCommand("L")
                "right" -> sendCommand("R")
                else -> Toast.makeText(this, "Sorry, I can only execute movement commands.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    // ✅ Permission launcher for Bluetooth
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            showDevicePicker()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wakeWordManager = WakeWordManager(this) {
            runOnUiThread {
                Toast.makeText(this, "Wake word detected!", Toast.LENGTH_SHORT).show()
                startVoiceRecognition()
            }
        }
        wakeWordManager.initialize("YEWpY2uu/ejh97A66zakeL/RP8q3/su52qIU8Xy/BDdQsBgxnw/YkQ==")
        wakeWordManager.start()

        // ✅ Request RECORD_AUDIO permission if not already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // ✅ Button click listeners
        binding.btnConnect.setOnClickListener { requestPermissionsAndConnect() }
        binding.btnForward.setOnClickListener { sendCommand("CSL-WU-#") }
        binding.btnBack.setOnClickListener { sendCommand("B") }
        binding.btnLeft.setOnClickListener { sendCommand("L") }
        binding.btnRight.setOnClickListener { sendCommand("R") }

        // 🎙️ Speak button for voice input
        binding.btnSpeak.setOnClickListener {
            startVoiceRecognition()
        }
    }

    // ✅ Start voice input using built-in recognizer
    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        voiceLauncher.launch(intent)
    }

    private fun requestPermissionsAndConnect() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        permissionLauncher.launch(perms)
    }

    private fun showDevicePicker() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Bluetooth connect permission denied", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices.toList()
        val deviceNames = pairedDevices.map { it.name ?: it.address }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select HC-05 Device")
            .setItems(deviceNames) { _, i ->
                connectToDevice(pairedDevices[i])
            }
            .show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread {
                        Toast.makeText(this, "Bluetooth connect permission denied", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                runOnUiThread {
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendCommand(command: String) {
        try {
            val socket = bluetoothSocket
            if (socket != null && socket.isConnected) {
                socket.outputStream.write(command.toByteArray()) // send full string
                Toast.makeText(this, "Sent: $command", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        bluetoothSocket?.close()
        super.onDestroy()
        wakeWordManager.stop()
        wakeWordManager.release()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            showDevicePicker()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}
