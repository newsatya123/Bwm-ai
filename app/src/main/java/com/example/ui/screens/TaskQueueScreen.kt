package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.engine.tasks.QueueState
import com.example.engine.tasks.QueuedTask
import com.example.engine.tasks.TaskStatus
import com.example.ui.theme.CardBorderNavy
import com.example.ui.theme.DeepNavyDark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.SurfaceVariantNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun TaskQueueScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.taskQueueManager.tasks.collectAsState()
    val currentTask by viewModel.taskQueueManager.currentTask.collectAsState()
    val queueState by viewModel.taskQueueManager.queueState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavyDark)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("task_queue_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Autonomous Task Queue", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Sequential Multi-Task Engine", color = TextSecondary, fontSize = 12.sp)
                }
            }

            if (tasks.isNotEmpty()) {
                IconButton(onClick = { viewModel.taskQueueManager.clearQueue() }) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear Queue", tint = TextMuted)
                }
            }
        }

        // Status Banner & Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderNavy),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (queueState) {
                                        QueueState.RUNNING -> RadiantEmerald
                                        QueueState.PAUSED -> WarningAmber
                                        QueueState.IDLE -> TextMuted
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (queueState) {
                                QueueState.RUNNING -> "Executing Sequentially"
                                QueueState.PAUSED -> "Queue Paused"
                                QueueState.IDLE -> "Queue Idle"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Surface(
                        color = SurfaceVariantNavy,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${tasks.count { it.status == TaskStatus.COMPLETED }}/${tasks.size} Completed",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Queue Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (queueState == QueueState.RUNNING) {
                        OutlinedButton(
                            onClick = { viewModel.taskQueueManager.pauseQueue() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 12.sp)
                        }
                    } else if (queueState == QueueState.PAUSED) {
                        Button(
                            onClick = { viewModel.taskQueueManager.resumeQueue() },
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald, contentColor = Color(0xFF001F25)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (currentTask != null) {
                        OutlinedButton(
                            onClick = { viewModel.taskQueueManager.skipCurrentTask() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skip Task", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.taskQueueManager.cancelCurrentTask() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Task Queue is Empty",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Say multi-step compound commands like:\n\"Torch on karo aur YouTube chalao\"\nor \"Play Store kholo, Instagram search karo\"",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    TaskItemRow(task = task, isCurrentlyExecuting = currentTask?.id == task.id)
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(task: QueuedTask, isCurrentlyExecuting: Boolean) {
    val borderColor = when {
        isCurrentlyExecuting -> NeonCyan
        task.status == TaskStatus.COMPLETED -> RadiantEmerald
        task.status == TaskStatus.FAILED -> ErrorRed
        task.status == TaskStatus.CANCELLED || task.status == TaskStatus.SKIPPED -> TextMuted
        else -> CardBorderNavy
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyExecuting) SurfaceVariantNavy else SurfaceNavy
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        when (task.status) {
                            TaskStatus.RUNNING -> NeonCyan.copy(alpha = 0.2f)
                            TaskStatus.COMPLETED -> RadiantEmerald.copy(alpha = 0.2f)
                            TaskStatus.FAILED -> ErrorRed.copy(alpha = 0.2f)
                            TaskStatus.WAITING_CONFIRMATION -> WarningAmber.copy(alpha = 0.2f)
                            else -> SurfaceVariantNavy
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (task.status) {
                        TaskStatus.RUNNING -> Icons.Default.Sync
                        TaskStatus.COMPLETED -> Icons.Default.CheckCircle
                        TaskStatus.FAILED -> Icons.Default.Cancel
                        TaskStatus.WAITING_CONFIRMATION -> Icons.Default.HourglassTop
                        else -> Icons.Default.HourglassTop
                    },
                    contentDescription = null,
                    tint = when (task.status) {
                        TaskStatus.RUNNING -> NeonCyan
                        TaskStatus.COMPLETED -> RadiantEmerald
                        TaskStatus.FAILED -> ErrorRed
                        TaskStatus.WAITING_CONFIRMATION -> WarningAmber
                        else -> TextMuted
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = task.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                if (!task.resultMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Result: ${task.resultMessage}",
                        color = if (task.status == TaskStatus.FAILED) ErrorRed else RadiantEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                color = when (task.status) {
                    TaskStatus.RUNNING -> NeonCyan.copy(alpha = 0.15f)
                    TaskStatus.COMPLETED -> RadiantEmerald.copy(alpha = 0.15f)
                    TaskStatus.FAILED -> ErrorRed.copy(alpha = 0.15f)
                    TaskStatus.WAITING_CONFIRMATION -> WarningAmber.copy(alpha = 0.15f)
                    TaskStatus.SKIPPED, TaskStatus.CANCELLED -> TextMuted.copy(alpha = 0.15f)
                    else -> SurfaceVariantNavy
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = task.status.name,
                    color = when (task.status) {
                        TaskStatus.RUNNING -> NeonCyan
                        TaskStatus.COMPLETED -> RadiantEmerald
                        TaskStatus.FAILED -> ErrorRed
                        TaskStatus.WAITING_CONFIRMATION -> WarningAmber
                        else -> TextSecondary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
