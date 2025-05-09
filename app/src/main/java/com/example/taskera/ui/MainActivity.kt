package com.example.taskera.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.taskera.ui.components.TasksByDateDialog
import java.util.Date

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


            TaskeraTheme(darkTheme = isDarkMode) {
                // 4) Collect live task list
                val tasks by vm.allTasks.observeAsState(initial = emptyList<Task>())

                // 5) Render the entire screen
                MainScreen(
                    drawerState = drawerState,
                    tasks               = tasks,
                    onItemClick         = { task ->
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
                    onDeleteTask        = { task ->
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
                        GoogleSignIn.getClient(this, gso).signOut()
                            .addOnCompleteListener {
                                startActivity(
                                    Intent(this, ComposeLoginActivity::class.java)
                                )
                                finish()
                            }
                    }
                )
                if (isDialogOpen) {
                    TaskDialog(
                        task      = dialogTask,
                        onDismiss = { isDialogOpen = false },
                        onSubmit  = { result ->
                            if (dialogTask == null) {
                                // “add” mode — supply the signed-in user’s email
                                val email = GoogleSignIn
                                    .getLastSignedInAccount(this@MainActivity)
                                    ?.email.orEmpty()
                                vm.insertTask(result.copy(userEmail = email))
                            } else {
                                // “edit” mode
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
                            dialogTask   = task
                            isDialogOpen = true
                            dateWindow   = null
                        },
                        onTaskStatusChanged = { task, done ->
                            vm.updateTask(task.copy(isCompleted = done))
                        }
                    )
                }

            }
        }
    }
}
