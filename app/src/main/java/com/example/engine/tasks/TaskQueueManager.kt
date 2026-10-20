package com.example.engine.tasks

import com.example.data.remote.ToolActionRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    SKIPPED,
    WAITING_CONFIRMATION
}

enum class QueueState {
    IDLE,
    RUNNING,
    PAUSED
}

data class QueuedTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val action: ToolActionRequest,
    var status: TaskStatus = TaskStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    var resultMessage: String? = null
)

class TaskQueueManager(
    private val scope: CoroutineScope,
    private val executeAction: suspend (ToolActionRequest, (Boolean) -> Unit) -> String
) {
    private val _tasks = MutableStateFlow<List<QueuedTask>>(emptyList())
    val tasks: StateFlow<List<QueuedTask>> = _tasks.asStateFlow()

    private val _currentTask = MutableStateFlow<QueuedTask?>(null)
    val currentTask: StateFlow<QueuedTask?> = _currentTask.asStateFlow()

    private val _queueState = MutableStateFlow(QueueState.IDLE)
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private var executionJob: Job? = null

    fun enqueueTasks(actions: List<ToolActionRequest>, originalPrompt: String? = null) {
        if (actions.isEmpty()) return

        val newTasks = actions.mapIndexed { index, action ->
            val title = getActionTitle(action)
            val desc = getActionDescription(action)
            QueuedTask(
                title = title,
                description = desc,
                action = action,
                status = TaskStatus.PENDING
            )
        }

        val updated = _tasks.value.toMutableList().apply { addAll(newTasks) }
        _tasks.value = updated

        if (_queueState.value != QueueState.RUNNING) {
            startExecutionLoop()
        }
    }

    private fun startExecutionLoop() {
        if (_queueState.value == QueueState.PAUSED) return

        executionJob?.cancel()
        executionJob = scope.launch(Dispatchers.Main) {
            _queueState.value = QueueState.RUNNING

            while (true) {
                if (_queueState.value == QueueState.PAUSED) break

                val pendingTask = _tasks.value.firstOrNull { it.status == TaskStatus.PENDING }
                if (pendingTask == null) {
                    _currentTask.value = null
                    _queueState.value = QueueState.IDLE
                    break
                }

                _currentTask.value = pendingTask
                updateTaskStatus(pendingTask.id, TaskStatus.RUNNING)

                try {
                    var isConfirmed = true
                    val resultMessage = executeAction(pendingTask.action) { confirmed ->
                        isConfirmed = confirmed
                    }

                    if (!isConfirmed) {
                        updateTaskStatus(pendingTask.id, TaskStatus.CANCELLED, "User cancelled confirmation")
                    } else {
                        updateTaskStatus(pendingTask.id, TaskStatus.COMPLETED, resultMessage)
                    }
                } catch (e: Exception) {
                    updateTaskStatus(pendingTask.id, TaskStatus.FAILED, "Error: ${e.message}")
                }

                // Short breathing pause between sequential tasks so user can see progress and UI settles
                delay(900)
            }
        }
    }

    fun pauseQueue(): String {
        _queueState.value = QueueState.PAUSED
        executionJob?.cancel()
        _currentTask.value?.let {
            if (it.status == TaskStatus.RUNNING) {
                updateTaskStatus(it.id, TaskStatus.PENDING)
            }
        }
        _currentTask.value = null
        return "Queue paused. Aap 'Queue resume karo' bol sakte hain."
    }

    fun resumeQueue(): String {
        if (_queueState.value == QueueState.PAUSED) {
            _queueState.value = QueueState.RUNNING
            startExecutionLoop()
            return "Queue resume kar di gayi hai."
        }
        return "Queue pehle se active hai."
    }

    fun cancelCurrentTask(): String {
        val current = _currentTask.value
        return if (current != null) {
            updateTaskStatus(current.id, TaskStatus.CANCELLED, "Cancelled by user")
            _currentTask.value = null
            executionJob?.cancel()
            startExecutionLoop()
            "Current task '${current.title}' cancel kar diya gaya hai."
        } else {
            "Koi task abhi run nahi ho raha hai."
        }
    }

    fun skipCurrentTask(): String {
        val current = _currentTask.value
        return if (current != null) {
            updateTaskStatus(current.id, TaskStatus.SKIPPED, "Skipped by user")
            _currentTask.value = null
            executionJob?.cancel()
            startExecutionLoop()
            "Task '${current.title}' skip kiya gaya hai."
        } else {
            "Koi task skip karne ke liye nahi hai."
        }
    }

    fun clearQueue(): String {
        executionJob?.cancel()
        _currentTask.value = null
        _tasks.value = emptyList()
        _queueState.value = QueueState.IDLE
        return "Task queue clear kar di gayi hai."
    }

    fun getCurrentStatusSummary(): String {
        val current = _currentTask.value
        val remaining = _tasks.value.count { it.status == TaskStatus.PENDING }
        return when {
            current != null && remaining > 0 ->
                "Sir, abhi '${current.title}' execute ho raha hai aur $remaining tasks pending hain."
            current != null ->
                "Sir, abhi '${current.title}' execute ho raha hai."
            remaining > 0 ->
                "Sir, $remaining tasks queue me hain."
            else ->
                "Sir, koi active task nahi chal raha hai. Queue idle hai."
        }
    }

    private fun updateTaskStatus(taskId: String, status: TaskStatus, result: String? = null) {
        val currentList = _tasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = currentList[index]
            currentList[index] = task.copy(status = status, resultMessage = result ?: task.resultMessage)
            _tasks.value = currentList
        }
    }

    companion object {
        fun getActionTitle(action: ToolActionRequest): String {
            return when (action) {
                is ToolActionRequest.ToggleFlashlight -> if (action.enable) "Torch Turn ON" else "Torch Turn OFF"
                is ToolActionRequest.AdjustVolume -> "Volume Adjust: ${action.direction}"
                is ToolActionRequest.GetBatteryStatus -> "Battery Status Check"
                is ToolActionRequest.LaunchApp -> "Open App: ${action.appName.replaceFirstChar { it.uppercase() }}"
                is ToolActionRequest.YouTubeSearch -> "YouTube Search: ${action.query}"
                is ToolActionRequest.YouTubeControl -> "YouTube Control: ${action.action}"
                is ToolActionRequest.WhatsAppSend -> "WhatsApp Message to ${action.recipient}"
                is ToolActionRequest.WhatsAppOpenChat -> "WhatsApp Chat: ${action.recipient}"
                is ToolActionRequest.WhatsAppSearch -> "WhatsApp Search: ${action.query}"
                is ToolActionRequest.DialPhone -> "Call/Dial: ${action.contactName?.takeIf { it.isNotBlank() } ?: action.phoneNumber}"
                is ToolActionRequest.SetTimer -> "Set Timer (${action.seconds}s)"
                is ToolActionRequest.SetAlarm -> "Set Alarm (${action.hour}:${action.minute})"
                is ToolActionRequest.ActiveAppContextAction -> "Context Action: ${action.action}"
                is ToolActionRequest.DeviceNavigation -> "Device Navigation: ${action.action}"
                is ToolActionRequest.SaveNote -> "Save Voice Note"
                is ToolActionRequest.OpenSettings -> "Open Settings: ${action.settingType}"
                else -> "Autonomous Task"
            }
        }

        fun getActionDescription(action: ToolActionRequest): String {
            return when (action) {
                is ToolActionRequest.ToggleFlashlight -> if (action.enable) "Turning device flashlight on" else "Turning device flashlight off"
                is ToolActionRequest.AdjustVolume -> "Adjusting device media volume"
                is ToolActionRequest.GetBatteryStatus -> "Reading battery level and charging state"
                is ToolActionRequest.LaunchApp -> "Launching application package for ${action.appName}"
                is ToolActionRequest.YouTubeSearch -> "Searching YouTube for '${action.query}'"
                is ToolActionRequest.YouTubeControl -> "Executing playback action '${action.action}'"
                is ToolActionRequest.WhatsAppSend -> "Composing message '${action.message}' to ${action.recipient}"
                is ToolActionRequest.WhatsAppOpenChat -> "Opening direct chat screen with ${action.recipient}"
                is ToolActionRequest.WhatsAppSearch -> "Searching WhatsApp conversations for '${action.query}'"
                is ToolActionRequest.DialPhone -> "Initiating phone call to ${action.contactName?.takeIf { it.isNotBlank() } ?: action.phoneNumber}"
                is ToolActionRequest.SetTimer -> "Starting timer for ${action.seconds / 60} minutes"
                is ToolActionRequest.SetAlarm -> "Scheduling system alarm"
                is ToolActionRequest.ActiveAppContextAction -> "Executing in-app action: ${action.action}"
                is ToolActionRequest.DeviceNavigation -> "System gesture action: ${action.action}"
                is ToolActionRequest.SaveNote -> "Saving note into local database"
                is ToolActionRequest.OpenSettings -> "Opening ${action.settingType} settings"
                else -> "Sequential background task execution"
            }
        }
    }
}
