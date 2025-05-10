package com.example.taskera.ui.components

import android.content.Context
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.drawerlayout.widget.DrawerLayout
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    drawerState: DrawerState,
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
    // 1) UI state for sorting
    var sortExpanded by remember { mutableStateOf(false) }
    var sortSelection by remember { mutableStateOf("Default") }
    val sortOptions = listOf("Default", "Priority", "Due Date")

    // 2) State for which task is selected in the detail‐dialog
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    // 3) State for date‐filtered dialog
    var tasksForDate by remember { mutableStateOf<List<Task>>(emptyList()) }
    var currentDateWindow by remember { mutableStateOf<Pair<Date, Pair<Long, Long>>?>(null) }
    // Pair<clickedDate, Pair<startMillis,endMillis>>

    // 4) Drawer state
    val scope       = rememberCoroutineScope()

    val cardBg        = MaterialTheme.colorScheme.surfaceVariant
    val headerBg      = MaterialTheme.colorScheme.primaryContainer
    val headerLabel   = MaterialTheme.colorScheme.onPrimaryContainer
    val pagesBg       = MaterialTheme.colorScheme.secondaryContainer
    val abbreviationsBg = MaterialTheme.colorScheme.secondaryContainer.toArgb()
    val abbreviationsFg = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()

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

                // Home
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

                // Account Info
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

                // Settings
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

                // Dark Mode Toggle
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

                // Logout
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
                    // 1) CalendarView via AndroidView
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
                                    AppearanceUtils.setHeaderColor(       this, headerBg.toArgb())
                                    AppearanceUtils.setHeaderLabelColor(  this, headerLabel.toArgb())
                                    AppearanceUtils.setPagesColor(        this, pagesBg.toArgb())
                                    //  Week‐day row
                                    AppearanceUtils.setAbbreviationsBarColor(this, abbreviationsBg)
                                    AppearanceUtils.setAbbreviationsLabelsColor(this, abbreviationsFg)
                                }
                            },
                            update = { cv: ApplandeoCalendarView ->
                                val events = tasks.mapNotNull { t ->
                                    t.dueDate?.let {
                                        val cal = Calendar.getInstance().apply { time = it }
                                        EventDay(cal, R.drawable.ic_task_marker)
                                    }
                                }
                                cv.setEvents(events)

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

                    // 2) Sort dropdown
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
                            modifier = Modifier.menuAnchor().fillMaxWidth().padding(horizontal = 16.dp)
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

                    // 3) Task list
                    val displayed = when (sortSelection) {
                        "Priority" -> tasks.sortedBy { when (it.priority) {
                            "High"   -> 1; "Medium" -> 2; else -> 3 } }
                        "Due Date" -> tasks.sortedBy { it.dueDate ?: Date(Long.MAX_VALUE) }
                        else       -> tasks
                    }

                    TaskList(
                        tasks               = displayed,
                        onItemClick         = { selectedTask = it },
                        onTaskStatusChanged = onTaskStatusChanged
                    )
                }

                // — detail dialog —
                selectedTask?.let { task ->
                    TaskDetailDialog(
                        task      = task,
                        onDismiss = { selectedTask = null },
                        onEdit    = { onEditTask(task) },
                        onDelete  = {
                            onDeleteTask(task)
                            selectedTask = null
                        }
                    )
                }

                // — date‐filtered dialog —
                currentDateWindow?.let { (date, range) ->
                    TasksByDateDialog(
                        date                = date,
                        tasks               = tasksForDate,
                        onDismiss           = { currentDateWindow = null },
                        onItemClick         = { selectedTask = it },
                        onTaskStatusChanged = onTaskStatusChanged
                    )
                }
            }
        )
    }
}
