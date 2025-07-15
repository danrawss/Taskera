package com.example.taskera.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import com.example.taskera.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: TaskViewModel,
    drawerState: DrawerState,
    onMenuClick: () -> Unit,
    onDailyPlan: () -> Unit,
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
    onSettings:() -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Keep track of whether we are currently waiting for a “which date?” response
    var waitingForDate by remember { mutableStateOf(false) }
    var showTasksDialog by remember { mutableStateOf(false) }
    var tasksForThatDate by remember { mutableStateOf<List<Task>>(emptyList()) }
    var dialogDate by remember { mutableStateOf(LocalDate.now()) }
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

    // Current local date/time for logic
    val nowLocalDate  = LocalDate.now()
    val nowLocalTime  = LocalTime.now()
    val todayDate = Date()

    // Pick a different set of gradient stops depending on light vs. dark mode:
    val backgroundBrush = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f),
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        // In light mode, use stronger (more opaque) container hues so the gradient shows:
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.10f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SmallTopAppBar(
                    title = { Text("Taskera") },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
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
                                    AppearanceUtils.setHeaderLabelColor(
                                        this,
                                        headerLabel.toArgb()
                                    )
                                    AppearanceUtils.setPagesColor(this, pagesBg.toArgb())
                                    AppearanceUtils.setAbbreviationsBarColor(
                                        this,
                                        abbreviationsBg
                                    )
                                    AppearanceUtils.setAbbreviationsLabelsColor(
                                        this,
                                        abbreviationsFg
                                    )
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
                            onValueChange = { /* no-op */ },
                            label = { Text("Sort by") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    sortExpanded
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                // make the background fill whatever you like:
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                // remove all underlines/indicators:
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.6f
                                ),
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.6f
                                ),
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
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

                        "Due Date" -> {
                            // Split out tasks with a due‐date vs. without
                            val withDate    = tasks.filter { it.dueDate != null }
                            val withoutDate = tasks.filter { it.dueDate == null }

                            // Sort the dated tasks by abs(dueDate - today)
                            val nearestFirst = withDate.sortedBy { task ->
                                abs(task.dueDate!!.time - todayDate.time)
                            }

                            // Append the tasks missing a due‐date at the end
                            nearestFirst + withoutDate
                        }
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
        SpeechFab(
            tasks = tasks,
            waitingForDate = waitingForDate,
            setWaitingForDate = { waitingForDate = it },
            onAddTask = onAddTask,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onSettings = onSettings,
            onDashboard = onDashboard,
            onDailyPlan = onDailyPlan,
            onMarkTodayBeforeNow = {
                // Launch a coroutine to update all matching tasks:
                CoroutineScope(Dispatchers.IO).launch {
                    tasks
                        // 1) Only tasks whose dueDate == today’s date
                        .filter { task ->
                            task.dueDate?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDate() == nowLocalDate
                        }
                        // 2) And whose startTime (or midnight if null) is <= now
                        .filter { task ->
                            val taskStart = task.startTime ?: LocalTime.MIDNIGHT
                            taskStart <= nowLocalTime
                        }
                        // 3) And not already completed
                        .filter { task -> !task.isCompleted }
                        // 4) Mark each as completed
                        .forEach { task ->
                            vm.updateTask(task.copy(isCompleted = true))
                        }
                }
            },
            onShowTasksForDate = { targetDate ->
                // Filter the current 'tasks' list for that LocalDate
                val matches = tasks.filter { task ->
                    task.dueDate?.toInstant()
                        ?.atZone(ZoneId.systemDefault())
                        ?.toLocalDate() == targetDate
                }
                tasksForThatDate = matches
                dialogDate = targetDate
                showTasksDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp)
        )

        if (showTasksDialog) {
            AlertDialog(
                onDismissRequest = { showTasksDialog = false },
                title = {
                    Text(
                        text = "Tasks on ${
                            dialogDate.format(
                                DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
                            )
                        }"
                    )
                },
                text = {
                    if (tasksForThatDate.isEmpty()) {
                        Text("No tasks found for this date.")
                    } else {
                        Column {
                            tasksForThatDate.forEach { t ->
                                Text("• ${t.title}")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTasksDialog = false }) {
                        Text("OK")
                    }
                }
            )


        }
    }
}
