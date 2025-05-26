@file:Suppress("MissingPermission")
package com.example.taskera.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationManagerCompat
import com.example.taskera.R

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("taskId", -1)
        val title  = inputData.getString("title") ?: "Task Reminder"

        // ➊ Build the notification using the fully‐qualified Builder
        val notification =
            androidx.core.app.NotificationCompat.Builder(
                applicationContext,
                "reminders"
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Upcoming Task")
                .setContentText(title)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .build()

        // ➋ Show it
        NotificationManagerCompat
            .from(applicationContext)
            .notify(taskId, notification)

        return Result.success()
    }
}
