package com.example.taskera.utils

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.*

object CalendarUtils {
    /**
     * Obtain a Calendar service instance using the currently signed-in account.
     */
    fun getCalendarService(context: Context): Calendar? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf("https://www.googleapis.com/auth/calendar")
        )
        credential.selectedAccount = account.account

        return Calendar.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Taskera")
            .build()
    }

    /**
     * Creates a calendar event using the given details.
     *
     * @param context The application context.
     * @param title The event title.
     * @param description The event description.
     * @param startMillis The event start time in milliseconds.
     * @param endMillis The event end time in milliseconds.
     * @return The event ID if creation was successful, or null otherwise.
     */
    fun createCalendarEvent(
        context: Context,
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long
    ): String? {
        val service = getCalendarService(context) ?: return null

        val event = Event().apply {
            summary = title
            this.description = description
        }

        val timeZone = TimeZone.getDefault().id

        val startDateTime = EventDateTime()
            .setDateTime(DateTime(startMillis))
            .setTimeZone(timeZone)
        event.start = startDateTime

        val endDateTime = EventDateTime()
            .setDateTime(DateTime(endMillis))
            .setTimeZone(timeZone)
        event.end = endDateTime

        return try {
            val createdEvent = service.events().insert("primary", event).execute()
            createdEvent.id // Return the created event's ID
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Updates an existing calendar event identified by eventId with new details.
     * Returns the updated event ID if successful, or null otherwise.
     */
    fun updateCalendarEvent(
        context: Context,
        eventId: String,
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long
    ): String? {
        val service = getCalendarService(context) ?: return null

        // Create an event object with the new details
        val event = Event().apply {
            summary = title
            this.description = description
        }

        val timeZone = TimeZone.getDefault().id

        val startDateTime = EventDateTime()
            .setDateTime(DateTime(startMillis))
            .setTimeZone(timeZone)
        event.start = startDateTime

        val endDateTime = EventDateTime()
            .setDateTime(DateTime(endMillis))
            .setTimeZone(timeZone)
        event.end = endDateTime

        return try {
            val updatedEvent = service.events().update("primary", eventId, event).execute()
            updatedEvent.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
