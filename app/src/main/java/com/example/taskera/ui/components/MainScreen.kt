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

            when {
                "add task"  in spoken -> onAddTask()
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
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Add task, open settings, or show dashboard")
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
