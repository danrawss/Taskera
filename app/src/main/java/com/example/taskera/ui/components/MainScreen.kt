package com.example.taskera.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import com.applandeo.materialcalendarview.CalendarView as ApplandeoCalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.utils.AppearanceUtils
import com.example.taskera.data.Task
import java.util.Calendar
import java.util.*
import com.example.taskera.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    drawerState: DrawerState,
    onDashboard: () -> Unit,
    tasks: List<Task>,
    onItemClick: (Task) -> Unit,
    onTaskStatusChanged: (Task, Boolean) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onDateClick: (Date, Long, Long) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onHome:    () -> Unit,
    onAccount: () -> Unit,
    onSettings:() -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Keep track of whether we are currently waiting for a “which date?” response
    var waitingForDate by remember { mutableStateOf(false) }
    // If the user has said “show tasks” and we asked “Do you want all or for a specific day?”,
    // this flag tells us that the next spoken result should be interpreted as a date or “all”.
    // Once we handle it, we set waitingForDate = false.

    // UI State
    var sortExpanded by remember { mutableStateOf(false) }
    var sortSelection by remember { mutableStateOf("Default") }
    val sortOptions = listOf("Default", "Priority", "Due Date")

    var selectedTask by remember { mutableStateOf<Task?>(null) }

    // Theme Colors
    val headerBg        = MaterialTheme.colorScheme.primaryContainer
    val headerLabel     = MaterialTheme.colorScheme.onPrimaryContainer
    val pagesBg         = MaterialTheme.colorScheme.secondaryContainer
    val abbreviationsBg = pagesBg.toArgb()
    val abbreviationsFg = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()

    // Microphone Permission
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Speech Recognition Setup
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

            // If we are currently waiting for the date after “show tasks”
            if (waitingForDate) {
                // Reset the flag immediately so normal commands resume next time
                waitingForDate = false

                // Interpret this spoken phrase as either “all” or a date
                val spokenDate = spoken  // e.g. “today”, “tomorrow”, “06/03/2025”

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
                    // Try parsing “today” / “tomorrow”
                    val today = LocalDate.now()
                    val targetDate: LocalDate? = when (spokenDate) {
                        "today" -> today
                        "tomorrow" -> today.plusDays(1)
                        else -> {
                            // Try parsing with a known pattern (e.g. MM/dd/yyyy)
                            try {
                                LocalDate.parse(spokenDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
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
                            task.dueDate?.toInstant()
                                ?.atZone(TimeZone.getDefault().toZoneId())
                                ?.toLocalDate() == targetDate
                        }
                        if (matchesOnDate.isEmpty()) {
                            Toast.makeText(
                                context,
                                "No tasks found for ${targetDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // List their titles
                            val titlesForDate = matchesOnDate.joinToString(", ") { it.title }
                            Toast.makeText(
                                context,
                                "Tasks on ${targetDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}: $titlesForDate",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                return@rememberLauncherForActivityResult
            }

            when {
                // Add task
                "add task"  in spoken -> onAddTask()

                // Delete task
                spoken.startsWith("delete task") -> {
                    // Strip out the exact prefix "delete task"
                    val remainder = spoken.removePrefix("delete task").trim()
                    if (remainder.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Please say “delete task [task name]”",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Search for matching task titles
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
                                // Exactly one match → delete it
                                onDeleteTask(matches[0])
                                Toast.makeText(
                                    context,
                                    "Deleted task “${matches[0].title}”",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                // Multiple matches → ask user to be more specific
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

                // Edit task
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

                // Show tasks
                spoken.startsWith("show tasks") -> {
                    val remainder = spoken.removePrefix("show tasks").trim()
                    if (remainder.isEmpty()) {
                        // Step 2: ask follow‐up question
                        Toast.makeText(
                            context,
                            "Do you want all tasks or tasks for a specific day? Say “all” or a date (e.g. MM/dd/yyyy).",
                            Toast.LENGTH_LONG
                        ).show()
                        waitingForDate = true
                    } else {
                        // If the user said “show tasks 06/03/2025” or “show tasks today”
                        // we handle it immediately (same code as the follow‐up branch)
                        val spokenDate = remainder  // e.g. “06/03/2025” or “today”
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
                                    "No tasks found for ${targetDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))}",
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

                "settings"  in spoken -> onSettings()
                "dashboard" in spoken -> onDashboard()
                else -> Toast
                    .makeText(context, "Sorry, I didn’t catch that.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Build the speech intent once
    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Commands: work with tasks"
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Taskera",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()

                // Drawer items
                NavigationDrawerItem(
                    label    = { Text("Dashboard") },
                    selected = false,
                    onClick  = {
                        onDashboard()
                        scope.launch { drawerState.close() }
                    },
                    icon     = { Icon(painterResource(R.drawable.ic_dashboard), null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label    = { Text("Home") },
                    selected = false,
                    onClick  = {
                        onHome()
                        scope.launch { drawerState.close() }
                    },
                    icon     = {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = "Home",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label    = { Text("Account Info") },
                    selected = false,
                    onClick  = {
                        onAccount()
                        scope.launch { drawerState.close() }
                    },
                    icon     = {
                        Icon(
                            painter = painterResource(R.drawable.ic_account),
                            contentDescription = "Account Info",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label    = { Text("Settings") },
                    selected = false,
                    onClick  = {
                        onSettings()
                        scope.launch { drawerState.close() }
                    },
                    icon     = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Dark Mode toggle in drawer
                NavigationDrawerItem(
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dark Mode")
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onToggleDarkMode(it) }
                            )
                        }
                    },
                    selected = isDarkMode,
                    onClick  = { onToggleDarkMode(!isDarkMode) },
                    icon     = {
                        Icon(
                            painter = painterResource(R.drawable.ic_dark_mode),
                            contentDescription = "Dark Mode",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label    = { Text("Logout") },
                    selected = false,
                    onClick  = {
                        onLogout()
                        scope.launch { drawerState.close() }
                    },
                    icon     = {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = "Logout"
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    SmallTopAppBar(
                        title = { Text("Taskera") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = onAddTask) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                },
                content = { padding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Calendar View
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            AndroidView<ApplandeoCalendarView>(
                                factory = { ctx ->
                                    ApplandeoCalendarView(ctx, null).apply {
                                        AppearanceUtils.setHeaderColor(this, headerBg.toArgb())
                                        AppearanceUtils.setHeaderLabelColor(this, headerLabel.toArgb())
                                        AppearanceUtils.setPagesColor(this, pagesBg.toArgb())
                                        AppearanceUtils.setAbbreviationsBarColor(this, abbreviationsBg)
                                        AppearanceUtils.setAbbreviationsLabelsColor(this, abbreviationsFg)
                                    }
                                },
                                update = { cv ->
                                    // Show markers for tasks with due dates
                                    val events = tasks.mapNotNull { t ->
                                        t.dueDate?.let {
                                            val cal = Calendar.getInstance().apply { time = it }
                                            EventDay(cal, R.drawable.ic_task_marker)
                                        }
                                    }
                                    cv.setEvents(events)

                                    // When a date is clicked, send its start/end millis
                                    cv.setOnDayClickListener { eventDay ->
                                        val cal = eventDay.calendar.apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        val start = cal.timeInMillis
                                        cal.apply {
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                            set(Calendar.SECOND, 59)
                                            set(Calendar.MILLISECOND, 999)
                                        }
                                        val end = cal.timeInMillis
                                        onDateClick(eventDay.calendar.time, start, end)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Sort Dropdown
                        ExposedDropdownMenuBox(
                            expanded = sortExpanded,
                            onExpandedChange = { sortExpanded = !sortExpanded },
                            modifier = Modifier.width(240.dp)
                        ) {
                            TextField(
                                readOnly = true,
                                value = "Sort: $sortSelection",
                                onValueChange = {},
                                label = { Text("Sort by") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false }
                            ) {
                                sortOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            sortSelection = opt
                                            sortExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Task List (sorted)
                        val displayed = when (sortSelection) {
                            "Priority" -> tasks.sortedBy {
                                when (it.priority) {
                                    "High" -> 1
                                    "Medium" -> 2
                                    else -> 3
                                }
                            }
                            "Due Date" -> tasks.sortedBy { it.dueDate ?: Date(Long.MAX_VALUE) }
                            else -> tasks
                        }

                        TaskList(
                            tasks = displayed,
                            onItemClick = { selectedTask = it },
                            onTaskStatusChanged = onTaskStatusChanged
                        )
                    }

                    // Task Detail Dialog
                    selectedTask?.let { task ->
                        TaskDetailDialog(
                            task = task,
                            onDismiss = { selectedTask = null },
                            onEdit = { onEditTask(task) },
                            onDelete = {
                                onDeleteTask(task)
                                selectedTask = null
                            }
                        )
                    }
                }
            )

            // Voice-command FAB
            FloatingActionButton(
                onClick = { voiceLauncher.launch(speechIntent) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Command")
            }
        }
    }
}
