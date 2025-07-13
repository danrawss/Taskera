@file:Suppress("MissingPermission")
package com.example.taskera.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taskera.R
import com.example.taskera.ui.MainActivity

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("taskId", -1)
        val triggerAt   = inputData.getLong  ("triggerAt", -1L)
        val title  = inputData.getString("title") ?: "Task Reminder"
        val now         = System.currentTimeMillis()

        // If we're more than 5 minutes past the intended trigger, drop it
        if (triggerAt > 0 && now > triggerAt + 5 * 60_000L) {
            return Result.success()
        }

        // Create an Intent to open MainActivity
        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingLaunch = PendingIntent.getActivity(
            applicationContext,
            taskId,  // use taskId to keep it unique
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification, attaching the PendingIntent
        val notification = NotificationCompat.Builder(applicationContext, "reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming Task")
            .setContentText(title)
            .setContentIntent(pendingLaunch)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show it
        NotificationManagerCompat
            .from(applicationContext)
            .notify(taskId, notification)

        return Result.success()
    }
}
