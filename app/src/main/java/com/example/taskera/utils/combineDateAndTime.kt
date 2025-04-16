package com.example.taskera.utils

import java.time.LocalTime
import java.util.Calendar
import java.util.Date

fun combineDateAndTime(date: Date, time: LocalTime): Long {
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.HOUR_OF_DAY, time.hour)
    calendar.set(Calendar.MINUTE, time.minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
