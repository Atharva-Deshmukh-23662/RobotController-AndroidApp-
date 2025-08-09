package com.example.robotcontroller.actions

import com.example.robotcontroller.bluetooth.BluetoothManager
import kotlinx.coroutines.delay
import android.util.Log

/**
 * RobotActions class contains all basic movement functions for the robot
 * Each function sends appropriate commands to FPGA via Bluetooth at specified intervals
 */
class RobotActions(private val bluetoothManager: BluetoothManager) {

    companion object {
        private const val COMMAND_INTERVAL_MS = 50L // Send commands every 50ms for smooth movement
        private const val TAG = "RobotActions"

        // UART Command Protocol
        private const val CMD_FORWARD = "MOV-FD-#"
        private const val CMD_BACKWARD = "MOV-BD-#"
        private const val CMD_LEFT = "MOV-LD-#"
        private const val CMD_RIGHT = "MOV-RD-#"
        private const val CMD_STOP = "STOP-#"

        // Default timing for turns (milliseconds)
        private const val DEFAULT_TURN_DURATION = 1000L
    }

    /**
     * Move robot straight forward for specified duration
     * @param durationMs Duration in milliseconds to move forward
     */
    suspend fun goStraight(durationMs: Long) {
        Log.d(TAG, "Starting go_straight for ${durationMs}ms")
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_FORWARD)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
        Log.d(TAG, "Completed go_straight")
    }

    /**
     * Move robot backward for specified duration
     * @param durationMs Duration in milliseconds to move backward
     */
    suspend fun goBackward(durationMs: Long) {
        Log.d(TAG, "Starting go_backward for ${durationMs}ms")
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_BACKWARD)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
        Log.d(TAG, "Completed go_backward")
    }

    /**
     * Turn robot left (default duration ~1 second)
     * @param durationMs Optional duration for the turn, defaults to 1 second
     */
    suspend fun turnLeft(durationMs: Long = DEFAULT_TURN_DURATION) {
        Log.d(TAG, "Starting turn_left for ${durationMs}ms")
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_LEFT)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
        Log.d(TAG, "Completed turn_left")
    }

    /**
     * Turn robot right (default duration ~1 second)
     * @param durationMs Optional duration for the turn, defaults to 1 second
     */
    suspend fun turnRight(durationMs: Long = DEFAULT_TURN_DURATION) {
        Log.d(TAG, "Starting turn_right for ${durationMs}ms")
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_RIGHT)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
        Log.d(TAG, "Completed turn_right")
    }

    /**
     * Stop robot immediately
     */
    suspend fun stop() {
        Log.d(TAG, "Sending stop command")
        bluetoothManager.send(CMD_STOP)
    }

    /**
     * Execute a custom movement pattern
     * @param commands List of command-duration pairs
     * Example: listOf(CMD_FORWARD to 1000L, CMD_LEFT to 500L)
     */
    suspend fun executeCustomPattern(commands: List<Pair<String, Long>>) {
        Log.d(TAG, "Starting custom pattern with ${commands.size} commands")
        for ((command, duration) in commands) {
            if (command == CMD_STOP) {
                bluetoothManager.send(command)
            } else {
                val endTime = System.currentTimeMillis() + duration
                while (System.currentTimeMillis() < endTime) {
                    bluetoothManager.send(command)
                    delay(COMMAND_INTERVAL_MS)
                }
            }
        }
        // Always stop at the end
        bluetoothManager.send(CMD_STOP)
        Log.d(TAG, "Completed custom pattern")
    }

    /**
     * Move in a square pattern
     * @param sideLength Duration for each side in milliseconds
     */
    suspend fun moveInSquare(sideLength: Long = 2000L) {
        Log.d(TAG, "Starting square pattern with ${sideLength}ms sides")
        repeat(4) {
            goStraight(sideLength)
            delay(200) // Brief pause between movements
            turnRight(DEFAULT_TURN_DURATION)
            delay(200)
        }
        Log.d(TAG, "Completed square pattern")
    }

    /**
     * Perform a U-turn (180-degree turn)
     * @param direction "left" or "right" for turn direction
     */
    suspend fun performUturn(direction: String = "left") {
        val turnDuration = DEFAULT_TURN_DURATION * 2 // Double duration for 180-degree turn
        Log.d(TAG, "Starting U-turn ${direction} for ${turnDuration}ms")

        when (direction.lowercase()) {
            "left" -> turnLeft(turnDuration)
            "right" -> turnRight(turnDuration)
            else -> turnLeft(turnDuration) // Default to left
        }
        Log.d(TAG, "Completed U-turn")
    }
}