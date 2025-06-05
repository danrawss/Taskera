// File: ui/components/SpeechFab.kt
package com.example.taskera.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.taskera.data.Task
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
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
    onMarkTodayBeforeNow: () -> Unit,
    onShowTasksForDate: (LocalDate) -> Unit,
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
                ?.lowercase(Locale.getDefault())
                .orEmpty()

            // If we’re currently “waiting for date,” interpret this result as a date or “all”:
            if (waitingForDate) {
                setWaitingForDate(false) // turn the flag off immediately

                // 1) Handle the special “all” keyword:
                if ("all" in spoken) {
                    onShowTasksForDate(LocalDate.now())
                    return@rememberLauncherForActivityResult
                }

                // 2) Normalize: remove ordinal suffixes (st, nd, rd, th) from numbers
                //    e.g. “june fifth 2025” → “june five 2025”
                val cleaned = spoken.replace(
                    """\b(\d+)(st|nd|rd|th)\b""".toRegex(), "$1"
                )

                // 3) Try parsing “today” / “tomorrow”:
                val today = LocalDate.now()
                val targetDate: LocalDate? = when (cleaned) {
                    "today"    -> today
                    "tomorrow" -> today.plusDays(1)
                    else       -> {
                        // 4) Try “MMMM d yyyy” (e.g. “june 5 2025”)
                        val fmtFull  = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.getDefault())
                        val fmtShort = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
                        try {
                            LocalDate.parse(cleaned, fmtFull)
                        } catch (e1: DateTimeParseException) {
                            // 5) If that fails, try “MMMM d” (e.g. “june 5”) → assume current year
                            try {
                                val withoutYear = LocalDate.parse(cleaned, fmtShort)
                                // The parser above without a year will default to year=1970, so we override:
                                withoutYear.withYear(today.year)
                            } catch (e2: DateTimeParseException) {
                                // 6) Last fallback: try numeric “MM/dd/yyyy” if spoken was numbers
                                try {
                                    LocalDate.parse(
                                        cleaned,
                                        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault())
                                    )
                                } catch (e3: DateTimeParseException) {
                                    null
                                }
                            }
                        }
                    }
                }

                if (targetDate == null) {
                    Toast.makeText(
                        context,
                        "Sorry, couldn’t parse \"$spoken\" as a date",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    onShowTasksForDate(targetDate)
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

                // “show tasks” → ask follow‐up
                spoken.startsWith("show tasks")  -> {
                    val remainder = spoken.removePrefix("show tasks").trim()
                    if (remainder.isEmpty()) {
                        // “show tasks” alone: ask follow-up
                        Toast.makeText(
                            context,
                            "Do you want ALL tasks or tasks for a SPECIFIC day? Say “all” or a date (e.g. June 5 2025).",
                            Toast.LENGTH_LONG
                        ).show()
                        setWaitingForDate(true)
                    } else {
                        // “show tasks June 5 2025” or “show tasks today”
                        // Step 1: check “all”
                        if ("all" in remainder) {
                            onShowTasksForDate(LocalDate.now())
                        } else {
                            val cleaned = remainder.replace(
                                """\b(\d+)(st|nd|rd|th)\b""".toRegex(),
                                "$1"
                            )
                            val today = LocalDate.now()
                            val targetDate: LocalDate? = when (cleaned) {
                                "today"    -> today
                                "tomorrow" -> today.plusDays(1)
                                else       -> {
                                    // Try “MMMM d yyyy” first:
                                    val fmtFull  = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.getDefault())
                                    val fmtShort = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
                                    try {
                                        LocalDate.parse(cleaned, fmtFull)
                                    } catch (e1: DateTimeParseException) {
                                        try {
                                            // If “June 5” was spoken (no year), parse with current year
                                            LocalDate.parse(cleaned, fmtShort)
                                                .withYear(today.year)
                                        } catch (e2: DateTimeParseException) {
                                            try {
                                                // Fallback to numeric MM/dd/yyyy
                                                LocalDate.parse(
                                                    cleaned,
                                                    DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault())
                                                )
                                            } catch (e3: DateTimeParseException) {
                                                null
                                            }
                                        }
                                    }
                                }
                            }

                            if (targetDate == null) {
                                Toast.makeText(
                                    context,
                                    "Couldn’t parse \"$remainder\" as a date",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                // Immediately show the dialog for that date:
                                onShowTasksForDate(targetDate)
                            }
                        }
                    }
                    return@rememberLauncherForActivityResult
                }

                // look for “mark” + “today” + “task” + “done” in any order:
                ("mark" in spoken || "finish" in spoken)
                        && "today" in spoken
                        && "task"  in spoken
                        && ("done" in spoken || "complete" in spoken) ->
                    onMarkTodayBeforeNow()

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
