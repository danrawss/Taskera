// File: ui/components/SpeechFab.kt
package com.example.taskera.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taskera.data.Task
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.TimeZone

/**
 * A reusable Composable that:
 *   1) asks for RECORD_AUDIO permission on first composition
 *   2) shows a Mic‐FAB which, when clicked, launches speech recognition
 *   3) contains all of your “waitingForDate” / “show tasks” / “add/edit/delete” logic
 *
 * @param tasks             the list of tasks (needed to filter by date)
 * @param waitingForDate    “are we currently expecting the next spoken result to be a date?”
 * @param setWaitingForDate setter for that flag (usually from MainScreen’s state)
 * @param onAddTask         Called when “add task” is spoken
 * @param onEditTask        Called when “edit task [name]” is spoken and a match is found
 * @param onDeleteTask      Called when “delete task [name]” is spoken and a match is found
 * @param onSettings        Called when “settings” is spoken
 * @param onDashboard       Called when “dashboard” is spoken
 * @param modifier          Allows the parent to position this FAB (e.g. .align(…) + .padding(…))
 */
@Composable
fun SpeechFab(
    tasks: List<Task>,
    waitingForDate: Boolean,
    setWaitingForDate: (Boolean) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onDailyPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Ask for RECORD_AUDIO permission on first composition
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Prepare the speech‐recognition Intent
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Commands: add task, delete task, edit task, show tasks, settings, dashboard"
            )
        }
    }

    // Create the launcher to receive speech results
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        if (result.resultCode == Activity.RESULT_OK && intent != null) {
            val spoken = intent
                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                ?.lowercase()
                .orEmpty()

            // If we are currently waiting to interpret “all” or a date:
            if (waitingForDate) {
                // Reset the flag so next time we go back to normal commands
                setWaitingForDate(false)

                val spokenDate = spoken  // e.g. “today” or “06/03/2025”

                if ("all" in spokenDate) {
                    // Show all task titles
                    if (tasks.isEmpty()) {
                        Toast.makeText(context, "No tasks to show", Toast.LENGTH_SHORT).show()
                    } else {
                        val allTitles = tasks.joinToString(", ") { it.title }
                        Toast.makeText(
                            context,
                            "All tasks: $allTitles",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Try parsing “today” / “tomorrow” / “MM/dd/yyyy”
                    val today = LocalDate.now()
                    val targetDate: LocalDate? = when (spokenDate) {
                        "today" -> today
                        "tomorrow" -> today.plusDays(1)
                        else -> {
                            try {
                                LocalDate.parse(
                                    spokenDate,
                                    DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                )
                            } catch (e: DateTimeParseException) {
                                null
                            }
                        }
                    }

                    if (targetDate == null) {
                        Toast.makeText(
                            context,
                            "Sorry, couldn’t parse \"$spokenDate\" as a date",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Filter tasks whose dueDate matches targetDate
                        val matchesOnDate = tasks.filter { task ->
                            task.dueDate
                                ?.toInstant()
                                ?.atZone(TimeZone.getDefault().toZoneId())
                                ?.toLocalDate() == targetDate
                        }
                        if (matchesOnDate.isEmpty()) {
                            Toast.makeText(
                                context,
                                "No tasks found for ${targetDate.format(
                                    DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                )}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val titlesForDate = matchesOnDate.joinToString { it.title }
                            Toast.makeText(
                                context,
                                "Tasks on ${targetDate.format(
                                    DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                )}: $titlesForDate",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                return@rememberLauncherForActivityResult
            }

            // Normal commands (when waitingForDate == false)
            when {
                "add task" in spoken -> {
                    onAddTask()
                }

                spoken.startsWith("delete task") -> {
                    val remainder = spoken.removePrefix("delete task").trim()
                    if (remainder.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Please say “delete task [task name]”",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val matches = tasks.filter { task ->
                            task.title.lowercase().contains(remainder)
                        }
                        when (matches.size) {
                            0 -> {
                                Toast.makeText(
                                    context,
                                    "No task named “$remainder” found",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            1 -> {
                                onDeleteTask(matches[0])
                                Toast.makeText(
                                    context,
                                    "Deleted task “${matches[0].title}”",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val titles = matches.joinToString { it.title }
                                Toast.makeText(
                                    context,
                                    "Multiple matches: $titles. Be more specific.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                spoken.startsWith("edit task") -> {
                    val remainder = spoken.removePrefix("edit task").trim()
                    if (remainder.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Please say “edit task [task name]”",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val matches = tasks.filter { task ->
                            task.title.lowercase().contains(remainder)
                        }
                        when (matches.size) {
                            0 -> {
                                Toast.makeText(
                                    context,
                                    "No task named “$remainder” found",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            1 -> {
                                onEditTask(matches[0])
                                Toast.makeText(
                                    context,
                                    "Editing task “${matches[0].title}”",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                val titles = matches.joinToString { it.title }
                                Toast.makeText(
                                    context,
                                    "Multiple matches: $titles. Be more specific.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                spoken.startsWith("show tasks") -> {
                    val remainder = spoken.removePrefix("show tasks").trim()
                    if (remainder.isEmpty()) {
                        // If they just said “show tasks” → ask the follow‐up
                        Toast.makeText(
                            context,
                            "Do you want all tasks or tasks for a specific day? Say “all” or a date (e.g. MM/dd/yyyy).",
                            Toast.LENGTH_LONG
                        ).show()
                        setWaitingForDate(true)
                    } else {
                        // If they said “show tasks 06/03/2025” or “show tasks today”
                        val spokenDate = remainder
                        val today = LocalDate.now()
                        val targetDate: LocalDate? = when (spokenDate) {
                            "today" -> today
                            "tomorrow" -> today.plusDays(1)
                            else -> {
                                try {
                                    LocalDate.parse(
                                        spokenDate,
                                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                    )
                                } catch (e: DateTimeParseException) {
                                    null
                                }
                            }
                        }

                        if (targetDate == null) {
                            Toast.makeText(
                                context,
                                "Sorry, couldn’t parse \"$spokenDate\" as a date",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val matchesOnDate = tasks.filter { task ->
                                task.dueDate
                                    ?.toInstant()
                                    ?.atZone(TimeZone.getDefault().toZoneId())
                                    ?.toLocalDate() == targetDate
                            }
                            if (matchesOnDate.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "No tasks found for ${targetDate.format(
                                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                    )}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val titlesForDate = matchesOnDate.joinToString { it.title }
                                Toast.makeText(
                                    context,
                                    "Tasks on ${targetDate.format(
                                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                    )}: $titlesForDate",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                "settings" in spoken -> {
                    onSettings()
                }

                "dashboard" in spoken -> {
                    onDashboard()
                }

                "daily plan" in spoken -> {
                    onDailyPlan()
                }

                else -> {
                    Toast.makeText(context, "Sorry, I didn’t catch that.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    // Finally, show the Mic‐FAB itself:
    FloatingActionButton(
        onClick = {
            scope.launch {
                try {
                    voiceLauncher.launch(speechIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Voice recognition not available.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Mic,
            contentDescription = "Voice Command"
        )
    }
}
