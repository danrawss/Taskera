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

    // Obtain a Calendar service instance using the currently signed-in account.
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
     * Creates a new event in “primary” calendar.
     * Returns the new event’s String ID on success, or null on failure.
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
            createdEvent.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Updates an existing event (by its eventId) in the user’s “primary” calendar.
     * If anything goes wrong (service is null or update fails), this is a no-op.
     */
    fun updateCalendarEvent(
        context: Context,
        eventId: String,
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long
    ) {
        val service = getCalendarService(context) ?: return

        try {
            // First, fetch the existing event
            val existing = service.events()
                .get("primary", eventId)
                .execute()

            // Modify its fields
            existing.summary = title
            existing.description = description

            val timeZone = TimeZone.getDefault().id

            existing.start = EventDateTime()
                .setDateTime(DateTime(startMillis))
                .setTimeZone(timeZone)

            existing.end = EventDateTime()
                .setDateTime(DateTime(endMillis))
                .setTimeZone(timeZone)

            // Push the update
            service.events()
                .update("primary", eventId, existing)
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes a Calendar event by its eventId from the user’s “primary” calendar.
     * If the service is null or delete fails, this is a no-op.
     */
    fun deleteCalendarEvent(
        context: Context,
        eventId: String
    ) {
        val service = getCalendarService(context) ?: return

        try {
            service.events()
                .delete("primary", eventId)
                .execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
