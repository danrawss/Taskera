package com.example.taskera.viewmodel

import androidx.lifecycle.*
import com.example.taskera.data.Task
import com.example.taskera.repository.TaskRepository
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.lifecycle.map
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.taskera.data.NotificationPrefs
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import com.example.taskera.utils.combineDateAndTime
import com.example.taskera.workers.ReminderWorker
import java.time.*


class TaskViewModel(
    application: Application,
    private val repository: TaskRepository)
    : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    // Repository already filters tasks based on userEmail
    val allTasks: LiveData<List<Task>> = repository.allTasks

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
        scheduleReminder(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
        scheduleReminder(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
        cancelReminder(task)
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

    // Helpers to compute epoch millis for start/end of a LocalDate
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

    // Weekly categories: Tasks due from Monday through Sunday of current week
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

    // 7-day trend: count of tasks *due* on each of the last 7 days
    val oneWeekTrend: LiveData<List<Pair<LocalDate, Int>>> =
        repository
            .getTasksByDate(
                // Compute these once in your todayWindow or similar helper
                oneWeekWindow.first,
                oneWeekWindow.second
            )
            .map { list ->
                val today = LocalDate.now()
                val startDate = today.minusDays(6)
                // Group tasks by their due-date
                val counts: Map<LocalDate, Int> = list
                    .mapNotNull {
                        it.dueDate
                            ?.toInstant()
                            ?.atZone(ZoneId.systemDefault())
                            ?.toLocalDate()
                    }
                    .groupingBy { it }
                    .eachCount()

                // Produce exactly 7 days with zero-fill
                (0..6).map { offset ->
                    val date = startDate.plusDays(offset.toLong())
                    date to (counts[date] ?: 0)
                }
            }

    // Scheduling helpers
    private suspend fun scheduleReminder(task: Task) {
        val workName = "reminder-${task.id}"
        val wm = WorkManager.getInstance(context)

        // Cancel any existing
        wm.cancelUniqueWork(workName)

        // Read prefs
        val enabled = NotificationPrefs.isEnabled(context).first()
        val leadMin = task.leadTimeMin
            ?: NotificationPrefs.defaultLeadMin(context).first()

        if (!enabled || task.isCompleted || task.dueDate == null) {
            Log.d("Reminders", "Skipping scheduling for task ${task.id}: " +
                    "enabled=$enabled, completed=${task.isCompleted}, dueDate=${task.dueDate}")
            return
        }

        // Combine date + time into millis
        val dueMillis = combineDateAndTime(task.dueDate, task.startTime ?: LocalTime.MIDNIGHT)
        val triggerAt = dueMillis - leadMin * 60_000L
        val now       = System.currentTimeMillis()

        // Only schedule if trigger is still in the future
        if (triggerAt <= now) {
            Log.d("Reminders", "Not scheduling past reminder for task ${task.id} (triggerAt=$triggerAt, now=$now)")
            return
        }

        val delay = triggerAt - now
        Log.d("Reminders", "Scheduling reminder for task ${task.id} in $delay ms (lead $leadMin min)")

        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "taskId" to task.id,
                    "title"  to task.title
                )
            )
            .build()

        wm.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, work)
    }

    private fun cancelReminder(task: Task) {
        WorkManager
        .getInstance(context)
        .cancelUniqueWork("reminder-${task.id}")
        }

    // Sealed event type
    sealed class Event {
        object CloseDialogs : Event()
    }
}
