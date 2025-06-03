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
import com.example.taskera.utils.CalendarUtils
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import com.example.taskera.utils.combineDateAndTime
import com.example.taskera.workers.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.time.temporal.TemporalAdjusters


class TaskViewModel(
    application: Application,
    private val repository: TaskRepository)
    : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    // Repository already filters tasks based on userEmail
    val allTasks: LiveData<List<Task>> = repository.allTasks

    // Inserts a new Task into Room, then creates a Calendar event (if date/time exist),
    // and finally updates that Task row with the new eventId.
    fun insertTask(task: Task) {
        viewModelScope.launch {
            // Insert into Room and get the new row ID
            val newRowId: Long = repository.insertTaskReturningId(task)

            // If there is a dueDate/startTime/endTime, create a Calendar event
            if (task.dueDate != null && task.startTime != null && task.endTime != null) {
                val startMillis = combineDateAndTime(task.dueDate, task.startTime)
                val endMillis   = combineDateAndTime(task.dueDate, task.endTime)

                // Call CalendarUtils.createCalendarEvent on IO
                val eventId: String? = withContext(Dispatchers.IO) {
                    CalendarUtils.createCalendarEvent(
                        context,
                        task.title,
                        task.description.orEmpty(),
                        startMillis,
                        endMillis
                    )
                }

                // If we got an eventId back (non-null), update the Task row with it
                if (eventId != null) {
                    val updatedTask = task.copy(
                        id = newRowId.toInt(),
                        calendarEventId = eventId
                    )
                    repository.updateTask(updatedTask)
                }
            }
            scheduleReminder(task.copy(id = newRowId.toInt()))
        }
    }

    //Updates an existing Task in Room, then also updates/creates/deletes its Calendar event.
    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)

            val oldEventId: String? = task.calendarEventId

            // Update existing event if oldEventId != null and dates still present
            if (oldEventId != null
                && task.dueDate != null
                && task.startTime != null
                && task.endTime != null
            ) {
                val startMillis = combineDateAndTime(task.dueDate, task.startTime)
                val endMillis   = combineDateAndTime(task.dueDate, task.endTime)

                withContext(Dispatchers.IO) {
                    CalendarUtils.updateCalendarEvent(
                        context,
                        oldEventId,
                        task.title,
                        task.description.orEmpty(),
                        startMillis,
                        endMillis
                    )
                }
            }
            // Create a new event if none existed before but now dates are present
            else if (oldEventId == null
                && task.dueDate != null
                && task.startTime != null
                && task.endTime != null
            ) {
                val startMillis = combineDateAndTime(task.dueDate, task.startTime)
                val endMillis   = combineDateAndTime(task.dueDate, task.endTime)

                val newEventId: String? = withContext(Dispatchers.IO) {
                    CalendarUtils.createCalendarEvent(
                        context,
                        task.title,
                        task.description.orEmpty(),
                        startMillis,
                        endMillis
                    )
                }
                if (newEventId != null) {
                    repository.updateTask(task.copy(calendarEventId = newEventId))
                }
            }
            // Delete the existing event if oldEventId != null but user removed the dueDate
            else if (oldEventId != null && task.dueDate == null) {
                withContext(Dispatchers.IO) {
                    CalendarUtils.deleteCalendarEvent(context, oldEventId)
                }
                repository.updateTask(task.copy(calendarEventId = null))
            }

            scheduleReminder(task)
        }
    }

    //Deletes a Task from Room. If it has a calendarEventId, delete that event first.
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            // If there is a linked Calendar event, delete it
            task.calendarEventId?.let { eventId ->
                withContext(Dispatchers.IO) {
                    CalendarUtils.deleteCalendarEvent(context, eventId)
                }
            }

            repository.deleteTask(task)
            cancelReminder(task)
        }
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

    /**
     * Returns a Pair where:
     *  first = epoch millis at 00:00 of this week’s Monday,
     *  second = epoch millis at 23:59:59.999 of this week’s Sunday.
     */
    private val thisWeekWindow by lazy {
        val today  = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val startMillis = monday.startOfDayMillis()
        val endMillis   = sunday.endOfDayMillis()
        startMillis to endMillis
    }

    // Weekly categories: Tasks due from Monday through Sunday of current week
    val weeklyCategoryStats: LiveData<Map<String, Int>> =
        repository
            .getTasksByDate(
                thisWeekWindow.first,
                thisWeekWindow.second
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
