package com.example.robotcontroller.tasks

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.robotcontroller.ai.RobotAction
import com.example.robotcontroller.actions.RobotActions
import com.example.robotcontroller.bluetooth.BluetoothManager
import kotlinx.coroutines.*

class TaskHandler(
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    private var currentTaskJob: Job? = null
    private val robotActions = RobotActions(bluetoothManager)

    /**
     * Execute a sequence of robot actions one by one
     * @param actions List of RobotAction to execute sequentially
     * @param onProgress Callback for progress updates (actionIndex, totalActions, currentAction)
     * @param onComplete Callback when all actions are completed
     * @param onError Callback when an error occurs
     */
    fun executeTaskSequence(
        actions: List<RobotAction>,
        onProgress: ((Int, Int, String) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // Cancel any existing task
        cancelCurrentTask()
        Log.d("TaskHandler", "executeTaskSequence() called with ${actions.size} actions")
        Log.d("TaskHandler", "Actions: ${actions.joinToString { it.action }}")


        if (actions.isEmpty()) {
            onError?.invoke("No actions to execute")
            Toast.makeText(context, "No actions to execute", Toast.LENGTH_SHORT).show()
            Log.w("TaskHandler", "No actions provided for task sequence")
            return
        }

        currentTaskJob = lifecycleScope.launch {
            try {
                Log.d("TaskHandler", "Starting executeActionsSequentially() with ${actions.size} actions, currentTaskJob: $currentTaskJob, isActive: ${currentTaskJob?.isActive}, isCompleted: ${currentTaskJob?.isCompleted}")
                executeActionsSequentially(actions, onProgress)

                // Task completed successfully
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Task sequence completed!", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                }

            } catch (e: CancellationException) {
                // Task was cancelled - don't show toast here as it's already shown in cancelCurrentTask()
                withContext(Dispatchers.Main) {
                    // Only show if it wasn't manually cancelled
                    // Toast.makeText(context, "Task cancelled", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Task failed
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Task failed: ${e.message}", Toast.LENGTH_LONG).show()
                    onError?.invoke(e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun executeActionsSequentially(
        actions: List<RobotAction>,
        onProgress: ((Int, Int, String) -> Unit)?
    ) {
        Log.d("TaskHandler", "Executing actions sequentially, total actions: ${actions.size}")
        for ((index, action) in actions.withIndex()) {

            Log.d("TaskHandler", "Executing action $index: ${action.action} with params ${action.params}")

            // FIXED: Proper null-safe check for task cancellation
            val job = currentTaskJob

            if (job == null || !job.isActive) {
                throw CancellationException("Task was cancelled")
            }

            // Update progress
            withContext(Dispatchers.Main) {
                onProgress?.invoke(index + 1, actions.size, action.action)
            }

            // Execute the action
            Log.d("TaskHandler", "Executing executeAction(): ${action.action} with params ${action.params}")
            executeAction(action)

            // Small delay between actions for smoother execution
            delay(100)
        }
    }

    private suspend fun executeAction(action: RobotAction) {
        Log.d("TaskHandler", "excuteAction(): ${action.action} with params ${action.params}")
        when (action.action) {
            "go_straight" -> {
                val duration = action.params.firstOrNull() as? Number ?: 1000
                robotActions.goStraight(duration.toLong())
            }
            "go_backward" -> {
                val duration = action.params.firstOrNull() as? Number ?: 1000
                robotActions.goBackward(duration.toLong())
            }
            "turn_left" -> {
                robotActions.turnLeft()
            }
            "turn_right" -> {
                robotActions.turnRight()
            }
            "stop" -> {
                robotActions.stop()
            }
            else -> {
                throw IllegalArgumentException("Unknown action: ${action.action}")
            }
        }
    }

    /**
     * Cancel the currently executing task sequence
     */
    fun cancelCurrentTask() {
        val job = currentTaskJob
        if (job != null && job.isActive) {
            job.cancel()
            currentTaskJob = null

            // Stop robot movement immediately
            lifecycleScope.launch {
                robotActions.stop()
            }

            Toast.makeText(context, "Task cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Check if a task is currently running
     */
    fun isTaskRunning(): Boolean {
        return currentTaskJob?.isActive == true
    }

    /**
     * Get current task progress information
     */
    fun getTaskStatus(): TaskStatus {
        return when {
            currentTaskJob == null -> TaskStatus.IDLE
            currentTaskJob?.isActive == true -> TaskStatus.RUNNING
            currentTaskJob?.isCancelled == true -> TaskStatus.CANCELLED
            currentTaskJob?.isCompleted == true -> TaskStatus.COMPLETED
            else -> TaskStatus.ERROR
        }
    }
}

enum class TaskStatus {
    IDLE,
    RUNNING,
    CANCELLED,
    COMPLETED,
    ERROR
}