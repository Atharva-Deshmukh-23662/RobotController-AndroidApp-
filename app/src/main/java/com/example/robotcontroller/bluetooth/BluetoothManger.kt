package com.example.robotcontroller.bluetooth

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
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

    fun connect() {
        if (adapter == null || !adapter.isEnabled) return

        val missing = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_DENIED
        if (missing) {
            // Inform caller to request permission via ActivityCompat.requestPermissions(...)
            return
        }

        val devices = adapter.bondedDevices.toList()
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Select HC-05 Device")
            .setItems(names) { _, i -> connectTo(devices[i]) }
            .show()
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

    fun send(message: String) {
        try {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission missing—inform caller
                return
            }
            socket?.outputStream?.write(message.toByteArray())
        } catch (e: SecurityException) {
            // Handle user revoking permission mid-session
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (_: IOException) { }
    }
}
