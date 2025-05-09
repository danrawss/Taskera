package com.example.taskera.viewmodel

import android.app.usage.UsageEvents.Event
import androidx.lifecycle.*
import com.example.taskera.data.Task
import com.example.taskera.repository.TaskRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // 1️⃣ Define the event stream
    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    // Repository already filters tasks based on userEmail
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

    // sealed event type
    sealed class Event {
        object CloseDialogs : Event()
    }
}
