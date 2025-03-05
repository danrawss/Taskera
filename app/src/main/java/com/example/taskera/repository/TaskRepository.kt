package com.example.taskera.repository

import androidx.lifecycle.LiveData
import com.example.taskera.data.Task
import com.example.taskera.data.TaskDao

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    fun getTaskById(taskId: Int): LiveData<Task> {
        return taskDao.getTaskById(taskId)
    }

    fun getTasksByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>> {
        return taskDao.getTasksByDate(startOfDay, endOfDay)
    }

    fun getDistinctTaskDates(): LiveData<List<String>> {
        return taskDao.getDistinctTaskDates()
    }

    fun getTasksSortedByPriority(): LiveData<List<Task>> {
        return taskDao.getTasksSortedByPriority()
    }

    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted)
    }
}
