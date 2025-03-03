package com.example.taskera.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskera.R
import com.example.taskera.data.TaskDatabase
import com.example.taskera.repository.TaskRepository
import com.example.taskera.viewmodel.TaskViewModel
import com.example.taskera.viewmodel.TaskViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var darkModeSwitch: SwitchCompat? = null  // ✅ Ensure it's nullable

    private val taskViewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getInstance(this)
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadTheme() // Load the saved theme before setting the layout
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

        // ✅ Find the Dark Mode switch from the Navigation Menu
        val menuItem: MenuItem = navView.menu.findItem(R.id.nav_dark_mode)
        val actionView: View? = menuItem.actionView
        darkModeSwitch = actionView?.findViewById(R.id.switch_dark_mode) as? SwitchCompat

        darkModeSwitch?.apply {
            isChecked = getSavedTheme() // ✅ Load saved theme state
            setOnCheckedChangeListener { _, isChecked ->
                toggleDarkMode(isChecked)
            }
        }

        // Prevent closing the menu when clicking the switch
        actionView?.setOnClickListener {
            darkModeSwitch?.toggle()
        }

        // Handle Navigation Drawer Item Clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_settings -> {
                    // Open settings activity (if needed)
                }
            }
            true
        }

        // Initialize the adapter and handle clicks on tasks
        taskAdapter = TaskAdapter { task ->
            val dialog = TaskDetailDialogFragment(task)
            dialog.show(supportFragmentManager, "TaskDetailDialog")
        }

        // Setup RecyclerView with Grid Layout (2 columns)
        val recyclerView = findViewById<RecyclerView>(R.id.rvTaskList)
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Observe LiveData from ViewModel
        taskViewModel.allTasks.observe(this, Observer { tasks ->
            taskAdapter.setData(tasks)
        })

        // Floating Action Button for Adding a Task
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            val dialog = AddTaskDialogFragment()
            dialog.show(supportFragmentManager, "AddTaskDialog")
        }
    }

    private fun toggleDarkMode(enableDarkMode: Boolean) {
        if (enableDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        saveTheme(enableDarkMode)
        recreate() // Restart the activity to apply the theme change
    }

    private fun saveTheme(isDarkMode: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("dark_mode", isDarkMode)
        editor.apply()
    }

    private fun loadTheme() {
        sharedPreferences = getSharedPreferences("TaskeraPrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun getSavedTheme(): Boolean {
        sharedPreferences = getSharedPreferences("TaskeraPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("dark_mode", false)
    }
}
