package com.example.robotcontroller

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.robotcontroller.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*
import kotlin.collections.toList
import android.os.Build

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

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

        binding.btnConnect.setOnClickListener { requestPermissionsAndConnect() }

        binding.btnForward.setOnClickListener { sendCommand("F") }
        binding.btnBack.setOnClickListener { sendCommand("B") }
        binding.btnLeft.setOnClickListener { sendCommand("L") }
        binding.btnRight.setOnClickListener { sendCommand("R") }
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

        // ✅ Permission check before accessing bondedDevices
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
                // ✅ Check permission before calling protected APIs
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Bluetooth connect permission denied",
                            Toast.LENGTH_SHORT
                        ).show()
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
                socket.outputStream.write(command.toByteArray())
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
