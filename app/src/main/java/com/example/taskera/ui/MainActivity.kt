package com.example.taskera.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import com.example.taskera.ui.TaskListDialogFragment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

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
            var isDarkMode by remember { mutableStateOf(initialDark) }

            TaskeraTheme(darkTheme = isDarkMode) {
                // 3) Obtain ViewModel
                val vm: TaskViewModel = viewModel(factory = factory)

                // 4) Collect live task list
                val tasks by vm.allTasks.observeAsState(initial = emptyList<Task>())

                // 5) Render the entire screen
                MainScreen(
                    tasks               = tasks,
                    onItemClick         = { task ->
                        // show your existing Edit dialog
                        EditTaskDialogFragment(task)
                            .show(supportFragmentManager, "EditTask")
                    },
                    onTaskStatusChanged = { task, done ->
                        vm.updateTask(task.copy(isCompleted = done))
                    },
                    onAddTask           = {
                        AddTaskDialogFragment()
                            .show(supportFragmentManager, "AddTask")
                    },
                    onEditTask          = { task ->
                        EditTaskDialogFragment(task)
                            .show(supportFragmentManager, "EditTask")
                    },
                    onDeleteTask        = { task ->
                        vm.deleteTask(task)
                    },
                    onDateClick         = { date, start, end ->
                        vm.getTasksByDate(start, end)
                            .observe(this@MainActivity) { list ->
                                TaskListDialogFragment(
                                    date = date,
                                    tasks = list,
                                    onStatusChanged = { task, done ->
                                        vm.updateTask(task.copy(isCompleted = done))
                                    }
                                ).show(supportFragmentManager, "TaskList")
                            }
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
            }
        }
    }
}
