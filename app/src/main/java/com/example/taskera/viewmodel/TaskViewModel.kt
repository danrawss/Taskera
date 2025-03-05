package com.example.taskera.viewmodel

import androidx.lifecycle.*
import com.example.taskera.data.Task
import com.example.taskera.repository.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: LiveData<List<Task>> = repository.allTasks

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun getTaskById(taskId: Int): LiveData<Task> {
        return repository.getTaskById(taskId)
    }

    fun getTasksByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>> {
        return repository.getTasksByDate(startOfDay, endOfDay)
    }

    fun getDistinctTaskDates(): LiveData<List<String>> {
        return repository.getDistinctTaskDates()
    }

    fun getTasksSortedByPriority(): LiveData<List<Task>> {
        return repository.getTasksSortedByPriority()
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) = viewModelScope.launch {
        repository.updateTaskCompletion(taskId, isCompleted)
    }
}
