package com.example.taskera.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DrawerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.taskera.ui.components.TasksByDateDialog
import com.example.taskera.utils.CalendarUtils
import com.example.taskera.utils.combineDateAndTime
import com.example.taskera.viewmodel.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.taskera.ui.components.DashboardScreen
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Prepare ViewModel factory
        val db      = TaskDatabase.getInstance(this)
        val email   = GoogleSignIn.getLastSignedInAccount(this)?.email.orEmpty()
        val repo    = TaskRepository(db.taskDao(), email)
        val factory = TaskViewModelFactory(repo)

        // 2) Load saved dark-mode flag
        val prefs       = getSharedPreferences("TaskeraPrefs", MODE_PRIVATE)
        val initialDark = prefs.getBoolean("dark_mode", false)

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(initialDark) }
            var dialogTask   by rememberSaveable { mutableStateOf<Task?>(null) }
            var isDialogOpen by rememberSaveable { mutableStateOf(false) }
            val vm: TaskViewModel = viewModel(factory = factory)
            // track which day was clicked (date + start/end millis)
            var dateWindow by rememberSaveable { mutableStateOf<Pair<Date, Pair<Long,Long>>?>(null) }
            val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)

            val composeContext = LocalContext.current
            val ioScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val activity = composeContext as Activity

            val weeklyStats by vm.weeklyCategoryStats.observeAsState(emptyMap())
            val trendData  by vm.oneWeekTrend.observeAsState(emptyList())

            TaskeraTheme(darkTheme = isDarkMode) {
                // 4) Collect live task list
                val tasks by vm.allTasks.observeAsState(initial = emptyList<Task>())
                val stats by vm.todayStats.observeAsState(Stats(0,0))

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    // HOME SCREEN (calendar + tasks)
                    composable("home") {
                        // 5) Render the entire screen
                        MainScreen(
                            drawerState = drawerState,
                            onDashboard = { navController.navigate("dashboard") },
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
                                // just record the clicked day -> trigger Compose dialog
                                dateWindow = date to (start to end)
                            },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { enabled ->
                                isDarkMode = enabled
                                prefs.edit().putBoolean("dark_mode", enabled).apply()
                            },
                            onHome = {

                            },
                            onAccount = {

                            },
                            onSettings = {

                            },
                            onLogout = {
                                // sign out and return to login
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .build()
                                GoogleSignIn.getClient(composeContext, gso)
                                    .signOut()
                                    .addOnCompleteListener {
                                        // start login activity
                                        composeContext.startActivity(
                                            Intent(composeContext, ComposeLoginActivity::class.java)
                                        )
                                        // finish the current Activity
                                        activity.finish()
                                    }
                            }
                        )
                        if (isDialogOpen) {
                            TaskDialog(
                                task = dialogTask,
                                onDismiss = { isDialogOpen = false },
                                onSubmit = { result ->
                                    if (dialogTask == null) {
                                        val email = GoogleSignIn
                                            .getLastSignedInAccount(composeContext)
                                            ?.email
                                            .orEmpty()

                                        // ① Insert into Room
                                        vm.insertTask(result.copy(userEmail = email))

                                        // ② If we got a due‐date plus start/end times, schedule a Calendar event
                                        val dd = result.dueDate
                                        val st = result.startTime
                                        val et = result.endTime
                                        if (dd != null && st != null && et != null) {
                                            val startMillis = combineDateAndTime(dd, st)
                                            val endMillis = combineDateAndTime(dd, et)

                                            ioScope.launch(Dispatchers.IO) {
                                                CalendarUtils.createCalendarEvent(
                                                    composeContext,
                                                    result.title,
                                                    result.description.orEmpty(),
                                                    startMillis,
                                                    endMillis
                                                )
                                            }
                                        }
                                    } else {
                                        // Edit mode
                                        vm.updateTask(result)
                                    }
                                    isDialogOpen = false
                                }
                            )
                        }
                        dateWindow?.let { (date, range) ->
                            // observe the LiveData for this one window
                            val tasksForDate by vm
                                .getTasksByDate(range.first, range.second)
                                .observeAsState(initial = emptyList())

                            TasksByDateDialog(
                                date = date,
                                tasks = tasksForDate,
                                onDismiss = { dateWindow = null },
                                onItemClick = { task ->
                                    // e.g. open your TaskDialog for editing
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

                    // DASHBOARD SCREEN
                    composable("dashboard") {
                        DashboardScreen(
                            stats   = stats,
                            weeklyCategories  = weeklyStats,
                            oneWeekTrend      = trendData,
                            onClose = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
