package com.example.taskera.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Convert a LocalDate to epoch millis at 00:00 of that day. */
fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Convert a LocalDate to epoch millis at 23:59:59.999 of that day. */
fun LocalDate.endOfDayMillis(): Long =
    atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
