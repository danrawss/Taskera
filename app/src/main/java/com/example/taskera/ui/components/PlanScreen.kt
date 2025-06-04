package com.example.taskera.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskera.data.Task
import com.example.taskera.ui.utils.createTodayPlanPdf
import com.example.taskera.utils.endOfDayMillis
import com.example.taskera.utils.startOfDayMillis
import com.example.taskera.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter

// 1) Define a sealed class to represent free‐time vs. task segments
private sealed class DaySegment {
    data class Free(val start: LocalTime, val end: LocalTime) : DaySegment()
    data class TaskItem(val task: Task) : DaySegment()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 2) Compute today’s start/end millis
    val todayRange: Pair<Long, Long> = remember {
        val today = LocalDate.now()
        val startOfDay = today.startOfDayMillis()
        val endOfDay   = today.endOfDayMillis()
        startOfDay to endOfDay
    }

    // 3) Observe tasks due today
    val tasksForToday by viewModel.getTasksByDate(
        todayRange.first, todayRange.second
    ).observeAsState(initial = emptyList())

    // 4) From tasksForToday, build a list of “segments” (Free / Task)
    val segments = remember(tasksForToday) {
        buildTodaySegments(tasksForToday)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top “Back” row
        SmallTopAppBar(
            title = { Text("Daily Plan") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(16.dp))

        if (segments.isEmpty()) {
            // No timed tasks at all
            Text(
                text = "No timed tasks for today.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* no tasks to PDFify, so maybe disable this? */ },
                enabled = false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Generate PDF")
            }
        } else {
            // 5) Show segments in a LazyColumn
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(segments) { segment ->
                    when (segment) {
                        is DaySegment.Free -> {
                            FreeSegmentCard(segment)
                        }
                        is DaySegment.TaskItem -> {
                            TaskSegmentCard(segment)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 6) “Generate PDF” button (same as before)
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val pdfFile = createTodayPlanPdf(
                                context = context,
                                tasksForToday = tasksForToday,
                                date = LocalDate.now()
                            )
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        context,
                                        "PDF saved: ${pdfFile.absolutePath}",
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        context,
                                        "Error generating PDF: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Generate PDF")
            }
        }
    }
}

// 7) Build the list of segments for “today”
private fun buildTodaySegments(tasks: List<Task>): List<DaySegment> {
    // Filter only tasks that have both a startTime and endTime:
    val timedTasks = tasks.filter { it.startTime != null && it.endTime != null }
        .sortedBy { it.startTime }

    if (timedTasks.isEmpty()) {
        return emptyList()
    }

    val segments = mutableListOf<DaySegment>()

    // Define the day’s bounds
    val dayStart: LocalTime = LocalTime.MIDNIGHT         // 00:00
    val dayEnd: LocalTime   = LocalTime.MAX              // 23:59:59.999...

    // 1) First free interval: 00:00 to firstTask.startTime
    val first = timedTasks.first()
    if (first.startTime!!.isAfter(dayStart)) {
        segments.add(DaySegment.Free(dayStart, first.startTime!!))
    }

    // 2) For each task, add TaskItem—and then a Free block between tasks
    for (i in timedTasks.indices) {
        val task = timedTasks[i]
        segments.add(DaySegment.TaskItem(task))

        // If there's a next task, create a Free segment between them
        if (i < timedTasks.size - 1) {
            val next = timedTasks[i + 1]
            if (next.startTime!!.isAfter(task.endTime!!)) {
                segments.add(DaySegment.Free(task.endTime!!, next.startTime!!))
            }
        }
    }

    // 3) Last free interval: lastTask.endTime to 23:59:59
    val last = timedTasks.last()
    if (last.endTime!!.isBefore(dayEnd)) {
        segments.add(DaySegment.Free(last.endTime!!, dayEnd))
    }

    return segments
}

// 8) Composable to show a “Free” interval
@Composable
private fun FreeSegmentCard(segment: DaySegment.Free) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val duration = Duration.between(segment.start, segment.end)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Free time: ${segment.start.format(fmt)} – ${segment.end.format(fmt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Text(
                text = " (${hours}h ${minutes}m)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// 9) Composable to show a “Task” interval
@Composable
private fun TaskSegmentCard(segment: DaySegment.TaskItem) {
    val task = segment.task
    val fmt = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (task.startTime != null && task.endTime != null) {
                    "${task.startTime.format(fmt)} – ${task.endTime.format(fmt)}"
                } else {
                    ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (!task.category.isNullOrBlank()) {
                Text(
                    text = "Category: ${task.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
