package com.example.robotcontroller.actions

import com.example.robotcontroller.bluetooth.BluetoothManager
import kotlinx.coroutines.delay

/**
 * RobotActions class contains all basic movement functions for the robot
 * Each function sends appropriate commands to FPGA via Bluetooth at specified intervals
 */
class RobotActions(private val bluetoothManager: BluetoothManager) {

    companion object {
        private const val COMMAND_INTERVAL_MS = 100L // Send commands every 50ms for smooth movement

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
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_FORWARD)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
    }

    /**
     * Move robot backward for specified duration
     * @param durationMs Duration in milliseconds to move backward
     */
    suspend fun goBackward(durationMs: Long) {
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_BACKWARD)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
    }

    /**
     * Turn robot left (default duration ~1 second)
     * @param durationMs Optional duration for the turn, defaults to 1 second
     */
    suspend fun turnLeft(durationMs: Long = DEFAULT_TURN_DURATION) {
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_LEFT)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
    }

    /**
     * Turn robot right (default duration ~1 second)
     * @param durationMs Optional duration for the turn, defaults to 1 second
     */
    suspend fun turnRight(durationMs: Long = DEFAULT_TURN_DURATION) {
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            bluetoothManager.send(CMD_RIGHT)
            delay(COMMAND_INTERVAL_MS)
        }

        // Send stop command at the end
        bluetoothManager.send(CMD_STOP)
    }

    /**
     * Stop robot immediately
     */
    suspend fun stop() {
        bluetoothManager.send(CMD_STOP)
    }

    /**
     * Execute a custom movement pattern
     * @param commands List of command-duration pairs
     * Example: listOf(CMD_FORWARD to 1000L, CMD_LEFT to 500L)
     */
    suspend fun executeCustomPattern(commands: List<Pair<String, Long>>) {
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
    }

    /**
     * Move in a square pattern
     * @param sideLength Duration for each side in milliseconds
     */
    suspend fun moveInSquare(sideLength: Long = 2000L) {
        repeat(4) {
            goStraight(sideLength)
            delay(200) // Brief pause between movements
            turnRight(DEFAULT_TURN_DURATION)
            delay(200)
        }
    }

    /**
     * Perform a U-turn (180-degree turn)
     * @param direction "left" or "right" for turn direction
     */
    suspend fun performUturn(direction: String = "left") {
        val turnDuration = DEFAULT_TURN_DURATION * 2 // Double duration for 180-degree turn

        when (direction.lowercase()) {
            "left" -> turnLeft(turnDuration)
            "right" -> turnRight(turnDuration)
            else -> turnLeft(turnDuration) // Default to left
        }
    }
}