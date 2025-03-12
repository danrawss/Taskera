package com.example.taskera.ui

import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

enum class SortType {
    DEFAULT, PRIORITY, DATE
}

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var taskAdapter: TaskAdapter
    private var isUserInitiatedClick = true
    private var currentSortType = SortType.DEFAULT
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val taskViewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getInstance(this)
        val repository = TaskRepository(database.taskDao())
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

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup Drawer Layout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.bringToFront()

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

        // RecyclerView for displaying tasks
        val recyclerView = findViewById<RecyclerView>(R.id.rvTaskList)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        // Initialize task adapter
        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                val dialog = TaskDetailDialogFragment(task)
                dialog.show(supportFragmentManager, "TaskDetailDialog")
            },
            onTaskStatusChanged = { updatedTask ->
                taskViewModel.updateTask(updatedTask)
            }
        )

        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

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
        sortDropdown.setOnItemClickListener { _, _, position, _ ->
            sortDropdownArrow.setImageResource(R.drawable.ic_arrow_up)
            when (position) {
                0 -> updateSorting(SortType.DEFAULT)
                1 -> updateSorting(SortType.PRIORITY)
                2 -> updateSorting(SortType.DATE)
            }
        }

        // Detect when dropdown is dismissed and reset the arrow
        sortDropdown.setOnDismissListener {
            sortDropdownArrow.setImageResource(R.drawable.ic_arrow_up)
        }

        // Observe All Tasks Initially
        taskViewModel.allTasks.observe(this, Observer { tasks ->
            applySorting(tasks)
            highlightTaskDates(calendarView, tasks)
        })

        // Open a dialog with tasks when clicking on a date
        calendarView.setOnDayClickListener { eventDay ->
            isUserInitiatedClick = true

            val selectedCalendar = eventDay.calendar
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = selectedCalendar.timeInMillis

            selectedCalendar.set(Calendar.HOUR_OF_DAY, 23)
            selectedCalendar.set(Calendar.MINUTE, 59)
            selectedCalendar.set(Calendar.SECOND, 59)
            selectedCalendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = selectedCalendar.timeInMillis

            taskViewModel.getTasksByDate(startOfDay, endOfDay).removeObservers(this)

            taskViewModel.getTasksByDate(startOfDay, endOfDay).observe(this, Observer { tasks ->
                val existingDialog = supportFragmentManager.findFragmentByTag("TaskListDialog") as? TaskListDialogFragment

                if (isUserInitiatedClick) {
                    if (tasks.isNotEmpty()) {
                        if (existingDialog != null) {
                            existingDialog.updateTasks(tasks)
                        } else {
                            val dialog = TaskListDialogFragment.newInstance(tasks) { updatedTask ->
                                taskViewModel.updateTask(updatedTask)
                            }
                            dialog.show(supportFragmentManager, "TaskListDialog")
                        }
                    } else {
                        existingDialog?.dismiss()
                    }
                } else {
                    if (tasks.isEmpty()) {
                        existingDialog?.dismiss()
                    }
                }
                isUserInitiatedClick = false
            })
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

    private fun updateSorting(sortType: SortType) {
        currentSortType = sortType
        taskViewModel.allTasks.value?.let { applySorting(it) }
    }

    private fun applySorting(tasks: List<Task>) {
        val sortedTasks = when (currentSortType) {
            SortType.DEFAULT -> tasks
            SortType.PRIORITY -> tasks.sortedBy { task ->
                when (task.priority) {
                    "High" -> 1
                    "Medium" -> 2
                    "Low" -> 3
                    else -> 4
                }
            }
            SortType.DATE -> tasks.sortedBy { it.dueDate ?: Date(Long.MAX_VALUE) }
        }
        taskAdapter.setData(sortedTasks)
    }
}
