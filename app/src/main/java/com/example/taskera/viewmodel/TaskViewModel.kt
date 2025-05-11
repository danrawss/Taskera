package com.example.taskera.viewmodel

import androidx.lifecycle.*
import com.example.taskera.data.Task
import com.example.taskera.repository.TaskRepository
import com.example.taskera.viewmodel.Stats
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.lifecycle.map
import java.time.*
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

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

    private val todayWindow by lazy {
        val cal = Calendar.getInstance().apply{
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.apply{
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val end = cal.timeInMillis
        start to end
    }

    val todayStats: LiveData<Stats> = repository
        .getTasksByDate(todayWindow.first, todayWindow.second)
        .map { list ->
            Stats(
                completedCount = list.count { it.isCompleted },
                totalCount     = list.size
            )
        }

    // ─── ➊ Helpers to compute epoch millis for start/end of a LocalDate ────────────────────
    private fun LocalDate.startOfDayMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun LocalDate.endOfDayMillis(): Long =
        atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val oneWeekWindow by lazy {
        val today = LocalDate.now()
        val startDate = today.minusDays(6)
        val startMillis = startDate.startOfDayMillis()
        val endMillis   = today.endOfDayMillis()
        startMillis to endMillis
    }

    // ─── ➋ Weekly categories: Tasks due from Monday through Sunday of current week ─────────
    val weeklyCategoryStats: LiveData<Map<String, Int>> =
        repository
            .getTasksByDate(
                todayWindow.first,
                todayWindow.second
            )
            .map { list ->
                list.groupingBy { it.category }
                    .eachCount()
            }

    // ─── ➌ 7-day trend: count of tasks *due* on each of the last 7 days ─────────────────
    val oneWeekTrend: LiveData<List<Pair<LocalDate, Int>>> =
        repository
            .getTasksByDate(
                // compute these once in your todayWindow or similar helper
                oneWeekWindow.first,
                oneWeekWindow.second
            )
            .map { list ->
                val today = LocalDate.now()
                val startDate = today.minusDays(6)
                // group tasks by their due-date
                val counts: Map<LocalDate, Int> = list
                    .mapNotNull {
                        it.dueDate
                            ?.toInstant()
                            ?.atZone(ZoneId.systemDefault())
                            ?.toLocalDate()
                    }
                    .groupingBy { it }
                    .eachCount()

                // produce exactly 7 days with zero-fill
                (0..6).map { offset ->
                    val date = startDate.plusDays(offset.toLong())
                    date to (counts[date] ?: 0)
                }
            }

    // sealed event type
    sealed class Event {
        object CloseDialogs : Event()
    }
}
