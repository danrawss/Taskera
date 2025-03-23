package com.example.taskera.repository

import androidx.lifecycle.LiveData
import com.example.taskera.data.Task
import com.example.taskera.data.TaskDao

class TaskRepository(
    private val taskDao: TaskDao,
    private val userEmail: String  // Added parameter for current user's email
) {

    // Get all tasks only for this user
    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks(userEmail)

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    // Get a specific task for this user
    fun getTaskById(taskId: Int): LiveData<Task> {
        return taskDao.getTaskById(taskId, userEmail)
    }

    // Get tasks by date range for this user
    fun getTasksByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>> {
        return taskDao.getTasksByDate(userEmail, startOfDay, endOfDay)
    }

    // Get distinct task dates for this user
    fun getDistinctTaskDates(): LiveData<List<String>> {
        return taskDao.getDistinctTaskDates(userEmail)
    }

    // Get tasks sorted by priority for this user
    fun getTasksSortedByPriority(): LiveData<List<Task>> {
        return taskDao.getTasksSortedByPriority(userEmail)
    }

    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted)
    }
}
