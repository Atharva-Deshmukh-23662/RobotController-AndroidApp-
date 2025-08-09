package com.example.robotcontroller.bluetooth

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class BluetoothManager(
    private val context: Context,
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"),
    private val onMessage: (String) -> Unit
) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null

    fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun initialize(): Boolean {
        if (adapter == null) {
            return false // Bluetooth not supported
        }
        if (!adapter.isEnabled) {
            return false // Bluetooth not enabled
        }

        return hasRequiredPermissions()
    }
    fun connect() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Safe Bluetooth calls
            val devices = adapter?.bondedDevices?.toList() ?: return
            val names = devices.map { it.name ?: it.address }.toTypedArray()
            AlertDialog.Builder(context)
                .setTitle("Select HC-05 Device")
                .setItems(names) { _, i -> connectTo(devices[i]) }
                .show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private fun connectTo(device: BluetoothDevice) {
        Thread {
            try {
                // Double-check permission before calling createRfcommSocket
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@Thread
                }
                socket = device.createRfcommSocketToServiceRecord(uuid)
                adapter?.cancelDiscovery()
                socket!!.connect()
            } catch (e: SecurityException) {
                // The user revoked permission at runtime; notify or recover
                e.printStackTrace()
            } catch (e: IOException) {
                // Connection I/O error
                e.printStackTrace()
            }
        }.start()
    }

    // In BluetoothManager.kt
    fun send(message: String) {
        try {
            Log.d("BluetoothManager", "Sending message: $message")
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BluetoothManager", "Missing BLUETOOTH_CONNECT permission")
                return
            }
            val os = socket?.outputStream
            if (os == null) {
                Log.e("BluetoothManager", "Socket outputStream is null")
            } else {
                os.write(message.toByteArray())
                Log.d("BluetoothManager", "Message sent successfully")
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Send failed", e)
        }
    }


    fun close() {
        try {
            socket?.close()
        } catch (_: IOException) { }
    }
}
