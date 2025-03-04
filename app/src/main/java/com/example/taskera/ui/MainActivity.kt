package com.example.taskera.ui

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.example.taskera.R
import com.example.taskera.data.TaskDatabase
import com.example.taskera.data.Task
import com.example.taskera.repository.TaskRepository
import com.example.taskera.viewmodel.TaskViewModel
import com.example.taskera.viewmodel.TaskViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var taskAdapter: TaskAdapter
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val taskViewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getInstance(this)
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup Drawer Layout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // Enable Menu Icon (☰) to Open Navigation Drawer
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // RecyclerView for displaying tasks
        val recyclerView = findViewById<RecyclerView>(R.id.rvTaskList)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)

        taskAdapter = TaskAdapter { task ->
            val dialog = TaskDetailDialogFragment(task)
            dialog.show(supportFragmentManager, "TaskDetailDialog")
        }
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Observe All Tasks Initially
        taskViewModel.allTasks.observe(this, Observer { tasks ->
            taskAdapter.setData(tasks)
            highlightTaskDates(calendarView, tasks) // Highlight task dates
        })

        // ✅ Open a dialog with tasks when clicking on a date
        calendarView.setOnDayClickListener { eventDay ->
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

            // ✅ Remove previous observers to prevent multiple triggers
            taskViewModel.getTasksByDate(startOfDay, endOfDay).removeObservers(this)

            taskViewModel.getTasksByDate(startOfDay, endOfDay).observe(this, Observer { tasks ->
                val existingDialog = supportFragmentManager.findFragmentByTag("TaskListDialog") as? TaskListDialogFragment

                if (tasks.isNotEmpty()) {
                    if (existingDialog != null) {
                        // ✅ If dialog is already open, update its contents
                        existingDialog.updateTasks(tasks)
                    } else {
                        // ✅ If dialog is NOT open, create and show a new one
                        val dialog = TaskListDialogFragment.newInstance(tasks)
                        dialog.show(supportFragmentManager, "TaskListDialog")
                    }
                } else {
                    // ✅ Close the dialog if there are no tasks left
                    existingDialog?.dismiss()
                }
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
                events.add(EventDay(calendar, R.drawable.ic_task_marker)) // Add a small dot/icon
            }
        }
        calendarView.setEvents(events)
    }

    // ✅ Method to Show a Simple Dialog
    private fun showNoTasksDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Tasks")
        builder.setMessage("There are no tasks scheduled for this date.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}
