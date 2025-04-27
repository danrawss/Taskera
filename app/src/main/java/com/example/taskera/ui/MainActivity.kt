package com.example.taskera.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.data.TaskDatabase
import com.example.taskera.repository.TaskRepository
import com.example.taskera.viewmodel.TaskViewModel
import com.example.taskera.viewmodel.TaskViewModelFactory
import com.example.taskera.ui.components.NavHeader
import com.example.taskera.ui.components.TaskList
import com.example.taskera.ui.theme.TaskeraTheme
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.taskera.ui.components.TaskDetailDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.taskera.ui.components.TasksByDateDialog
import kotlinx.coroutines.flow.collectLatest
import java.util.*

enum class SortType {
    DEFAULT, PRIORITY, DATE
}

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private var isUserInitiatedClick = true
    private var currentSortType = SortType.DEFAULT
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var tasksDateMillis by  mutableStateOf<Long?>(null)
    private var tasksForDate by mutableStateOf<List<Task>>(emptyList())

    private val taskViewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getInstance(this)
        val currentUserEmail = GoogleSignIn.getLastSignedInAccount(this)?.email ?: ""
        val repository = TaskRepository(database.taskDao(), currentUserEmail)
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme preference on startup
        val isDarkMode = getSharedPreferences("TaskeraPrefs", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // *** MODIFIED: Setup ComposeView for Compose UI content with observed tasks ***
        val composeView = findViewById<ComposeView>(R.id.composeViewTaskList)
        composeView.setContent {
            TaskeraTheme {
                // 1) observe tasks
                val tasks by taskViewModel.allTasks.observeAsState(emptyList())

                // 2) local UI state
                var selectedTask by remember { mutableStateOf<Task?>(null) }

                // 3) collect events from VM
                LaunchedEffect(Unit) {
                    taskViewModel.events.collectLatest { event ->
                        when (event) {
                            TaskViewModel.Event.CloseDialogs -> {
                                // clear *all* dialog triggers
                                selectedTask   = null
                                tasksDateMillis = null
                            }
                        }
                    }
                }

                // 4) your TaskList
                TaskList(
                    tasks = tasks,
                    onItemClick = { task ->
                        selectedTask = task
                    },
                    onTaskStatusChanged = { task, checked ->
                        taskViewModel.updateTask(task.copy(isCompleted = checked))
                    }
                )

                // 5) detail‐dialog
                selectedTask?.let { task ->
                    TaskDetailDialog(
                        task = task,
                        onDismiss = { selectedTask = null },
                        onEdit    = { toEdit ->
                            EditTaskDialogFragment(toEdit)
                                .show(supportFragmentManager, "EditTaskDialog")
                        },
                        onDelete  = { toDelete ->
                            taskViewModel.deleteTask(toDelete)
                        }
                    )
                }

                // 6) date‐filtered dialog
                tasksDateMillis?.let { millis ->
                    TasksByDateDialog(
                        date                = Date(millis),
                        tasks               = tasksForDate,
                        onDismiss           = { tasksDateMillis = null },
                        onItemClick         = { task -> selectedTask = task },
                        onTaskStatusChanged = { task, checked ->
                            taskViewModel.updateTask(task.copy(isCompleted = checked))
                        }
                    )
                }
            }
        }
        // *** END MODIFIED ***

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup Drawer Layout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val composeHeader = ComposeView(this).apply {
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.WRAP_CONTENT
            )
            setContent {
                NavHeader()  // Your composable function for the navigation header
            }
        }
        navView.addHeaderView(composeHeader)

        // Enable Menu Icon (☰) to Open Navigation Drawer
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Ensure navView is interactive
        navView.bringToFront()

        // Find the menu item
        val menu = navView.menu
        val darkModeItem = menu.findItem(R.id.nav_dark_mode)
        val switchView = darkModeItem.actionView as? SwitchCompat

        // Load saved theme preference
        val sharedPreferences = getSharedPreferences("TaskeraPrefs", MODE_PRIVATE)
        switchView?.isChecked = isDarkMode

        switchView?.setOnCheckedChangeListener { _, isChecked ->
            // Update switch to prevent UI delay issues
            switchView.isChecked = isChecked

            // Save theme choice to SharedPreferences
            with(sharedPreferences.edit()) {
                putBoolean("dark_mode", isChecked)
                apply()
            }

            // Set the night mode
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // Recreate the activity to apply theme changes
            recreate()
        }

        // Logout functionality
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    signOutUser() // Call the sign-out function
                    true
                }
                // Handle other items if needed
                else -> false
            }
        }

        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)


        // Populate Sorting Dropdown
        val sortDropdown: AutoCompleteTextView = findViewById(R.id.sortDropdown)
        val sortDropdownArrow: ImageView = findViewById(R.id.sortDropdownArrow)

        val sortingOptions = arrayOf("Default", "Priority", "Due Date")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortingOptions)
        sortDropdown.setAdapter(adapter)

        val openDropdown = {
            sortDropdown.post {
                sortDropdown.dropDownHorizontalOffset = sortDropdownArrow.left - sortDropdown.left
                sortDropdown.dropDownWidth = sortDropdownArrow.width * 4
                sortDropdown.showDropDown()
            }
            sortDropdownArrow.setImageResource(R.drawable.ic_arrow_down)
        }

        // Click listener for both dropdown text and arrow
        val dropdownClickListener = View.OnClickListener { openDropdown() }
        sortDropdown.setOnClickListener(dropdownClickListener)
        sortDropdownArrow.setOnClickListener(dropdownClickListener)

        // Handle selection
//        sortDropdown.setOnItemClickListener { _, _, position, _ ->
//            sortDropdownArrow.setImageResource(R.drawable.ic_arrow_up)
//            when (position) {
//                0 -> updateSorting(SortType.DEFAULT)
//                1 -> updateSorting(SortType.PRIORITY)
//                2 -> updateSorting(SortType.DATE)
//            }
//        }

        // Detect when dropdown is dismissed and reset the arrow
        sortDropdown.setOnDismissListener {
            sortDropdownArrow.setImageResource(R.drawable.ic_arrow_up)
        }

        // Observe All Tasks Initially
        taskViewModel.allTasks.observe(this, Observer { tasks ->
            //applySorting(tasks)
            highlightTaskDates(calendarView, tasks)
        })

        // Open a dialog with tasks when clicking on a date
        calendarView.setOnDayClickListener { eventDay ->
            // build the 0:00–23:59 window for the clicked date
            val cal = eventDay.calendar.apply {
                set(Calendar.HOUR_OF_DAY,   0)
                set(Calendar.MINUTE,        0)
                set(Calendar.SECOND,        0)
                set(Calendar.MILLISECOND,   0)
            }
            val startOfDay = cal.timeInMillis

            cal.apply {
                set(Calendar.HOUR_OF_DAY,  23)
                set(Calendar.MINUTE,       59)
                set(Calendar.SECOND,       59)
                set(Calendar.MILLISECOND,  999)
            }
            val endOfDay = cal.timeInMillis

            // observe once and update our Compose state
            taskViewModel
                .getTasksByDate(startOfDay, endOfDay)
                .observe(this) { list ->
                    tasksForDate    = list
                    tasksDateMillis = startOfDay
                }
        }

        // Floating Action Button Click
        fabAddTask.setOnClickListener {
            val dialog = AddTaskDialogFragment()
            dialog.show(supportFragmentManager, "AddTaskDialog")
        }
    }

    private fun highlightTaskDates(calendarView: CalendarView, tasks: List<Task>) {
        val events = mutableListOf<EventDay>()
        for (task in tasks) {
            task.dueDate?.let { date ->
                val calendar = Calendar.getInstance().apply { time = date }
                events.add(EventDay(calendar, R.drawable.ic_task_marker))
            }
        }
        calendarView.setEvents(events)
    }

//    private fun updateSorting(sortType: SortType) {
//        currentSortType = sortType
//        taskViewModel.allTasks.value?.let { applySorting(it) }
//    }

//    private fun applySorting(tasks: List<Task>) {
//        val sortedTasks = when (currentSortType) {
//            SortType.DEFAULT -> tasks
//            SortType.PRIORITY -> tasks.sortedBy { task ->
//                when (task.priority) {
//                    "High" -> 1
//                    "Medium" -> 2
//                    "Low" -> 3
//                    else -> 4
//                }
//            }
//            SortType.DATE -> tasks.sortedBy { it.dueDate ?: Date(Long.MAX_VALUE) }
//        }
//        taskAdapter.setData(sortedTasks)
//    }

    private fun signOutUser() {
        // Configure default Google Sign-In options for logout
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // After sign-out, navigate back to ComposeLoginActivity
            startActivity(Intent(this, ComposeLoginActivity::class.java))
            finish()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
    }
}
