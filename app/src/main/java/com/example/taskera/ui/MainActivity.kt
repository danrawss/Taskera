package com.example.taskera.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskera.data.Task
import com.example.taskera.data.TaskDatabase
import com.example.taskera.repository.TaskRepository
import com.example.taskera.ui.components.MainScreen
import com.example.taskera.ui.theme.TaskeraTheme
import com.example.taskera.viewmodel.TaskViewModel
import com.example.taskera.viewmodel.TaskViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.taskera.ui.components.TaskDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.rememberNavController
import com.example.taskera.ui.components.TasksByDateDialog
import com.example.taskera.viewmodel.Stats
import java.util.Date
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.taskera.ui.components.DashboardScreen
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taskera.ui.components.NotificationSettingsScreen
import com.example.taskera.ui.components.PlanScreen
import com.example.taskera.viewmodel.SettingsViewModel
import com.example.taskera.viewmodel.SettingsViewModelFactory
import java.time.Duration

class MainActivity : AppCompatActivity() {
    private val POST_NOTIF_REQ = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIF_REQ
                )
            }
        }

        // Prepare database, repository, and ViewModel factory
        val db      = TaskDatabase.getInstance(this)
        val email   = GoogleSignIn.getLastSignedInAccount(this)?.email.orEmpty()
        val repo    = TaskRepository(db.taskDao(), email)
        val factory = TaskViewModelFactory(
            application = application,
            repository  = repo
        )

        // Load saved dark mode setting
        val prefs       = getSharedPreferences("TaskeraPrefs", MODE_PRIVATE)
        val initialDark = prefs.getBoolean("dark_mode", false)

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(initialDark) }
            TaskeraTheme(darkTheme = isDarkMode) {
                // Dialog and navigation state
                var dialogTask   by remember { mutableStateOf<Task?>(null) }
                var isDialogOpen by rememberSaveable { mutableStateOf(false) }
                var dateWindow   by rememberSaveable { mutableStateOf<Pair<Date, Pair<Long,Long>>?>(null) }
                val drawerState  = rememberDrawerState(DrawerValue.Closed)
                val composeContext = LocalContext.current
                val activity     = composeContext as Activity
                val navController = rememberNavController()

                // ViewModel and live data
                val vm: TaskViewModel = viewModel(factory = factory)
                val tasks by vm.allTasks.observeAsState(initial = emptyList<Task>())
                val stats by vm.todayStats.observeAsState(Stats(0,0))
                val weeklyStats by vm.weeklyCategoryStats.observeAsState(emptyMap())
                val trendData  by vm.oneWeekTrend.observeAsState(emptyList())

                // Settings ViewModel for default lead time
                val settingsFactory = SettingsViewModelFactory(composeContext)
                val settingsVm: SettingsViewModel = viewModel(factory = settingsFactory)
                val defaultLeadMin by settingsVm.defaultLeadMin.observeAsState(initial = 30)
                val defaultLeadDuration = Duration.ofMinutes(defaultLeadMin.toLong())

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        // Render the main calendar/tasks screen
                        MainScreen(
                            vm = vm,
                            drawerState = drawerState,
                            onDashboard = { navController.navigate("dashboard") },
                            onDailyPlan = { navController.navigate("plan")},
                            tasks = tasks,
                            onItemClick = { task ->
                                dialogTask = task
                                isDialogOpen = true
                            },
                            onTaskStatusChanged = { task, done ->
                                vm.updateTask(task.copy(isCompleted = done))
                            },
                            onAddTask = {
                                dialogTask = null
                                isDialogOpen = true
                            },
                            onEditTask = { task ->
                                dialogTask = task
                                isDialogOpen = true
                            },
                            onDeleteTask = { task ->
                                vm.deleteTask(task)
                            },
                            onDateClick = { date, start, end ->
                                dateWindow = date to (start to end)
                            },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { enabled ->
                                isDarkMode = enabled
                                prefs.edit().putBoolean("dark_mode", enabled).apply()
                            },
                            onHome = {},
                            onAccount = {},
                            onSettings = {
                                navController.navigate("settings")
                            },
                            onLogout = {
                                // Sign out and return to login screen
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                GoogleSignIn.getClient(composeContext, gso)
                                    .signOut()
                                    .addOnCompleteListener {
                                        composeContext.startActivity(
                                            Intent(composeContext, ComposeLoginActivity::class.java)
                                        )
                                        activity.finish()
                                    }
                            }
                        )

                        // Show add/edit task dialog
                        if (isDialogOpen) {
                            TaskDialog(
                                task        = dialogTask,
                                defaultLead = defaultLeadDuration,
                                onDismiss   = { isDialogOpen = false },
                                onSubmit    = { result, leadDuration ->
                                    // Build the final Task once, with email + leadTimeMin
                                    val email  = GoogleSignIn.getLastSignedInAccount(composeContext)
                                        ?.email
                                        .orEmpty()
                                    if (dialogTask == null) {
                                        // Adding
                                        // Build a brand-new Task (no calendarEventId yet)
                                        val toSave = result.copy(
                                            userEmail       = email,
                                            leadTimeMin     = leadDuration.toMinutes().toInt(),
                                            calendarEventId = null
                                        )
                                        vm.insertTask(toSave)
                                    } else {
                                        // Editing
                                        // Take the existing Task (dialogTask), and copy over only the edited fields,
                                        // preserving its id and calendarEventId
                                        val original = dialogTask!!
                                        val updated = original.copy(
                                            title           = result.title,
                                            description     = result.description?.takeIf { it.isNotBlank() },
                                            dueDate         = result.dueDate,
                                            startTime       = result.startTime,
                                            endTime         = result.endTime,
                                            priority        = result.priority,
                                            category        = result.category,
                                            leadTimeMin     = leadDuration.toMinutes().toInt(),
                                            userEmail       = email,
                                            calendarEventId = original.calendarEventId
                                        )
                                        vm.updateTask(updated)
                                    }

                                    isDialogOpen = false
                                }
                            )
                        }

                        // Show tasks by date dialog when a day is selected
                        dateWindow?.let { (date, range) ->
                            val tasksForDate by vm.getTasksByDate(range.first, range.second)
                                .observeAsState(initial = emptyList())
                            TasksByDateDialog(
                                date = date,
                                tasks = tasksForDate,
                                onDismiss = { dateWindow = null },
                                onItemClick = { task ->
                                    dialogTask = task
                                    isDialogOpen = true
                                    dateWindow = null
                                },
                                onTaskStatusChanged = { task, done ->
                                    vm.updateTask(task.copy(isCompleted = done))
                                }
                            )
                        }
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            stats = stats,
                            weeklyCategories = weeklyStats,
                            oneWeekTrend = trendData,
                            onClose = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    composable("settings") {
                        NotificationSettingsScreen(
                            viewModel = settingsVm,
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable("plan") {
                        PlanScreen(
                            viewModel = vm,
                            onBack    = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        // Create notification channel for reminders on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId   = "reminders"
            val channelName = "Task Reminders"
            val channelDesc = "Notifications for upcoming task reminders"
            val importance  = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
