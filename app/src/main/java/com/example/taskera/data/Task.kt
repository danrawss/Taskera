package com.example.taskera.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val dueDate: Date? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val priority: String = "Low", // "Low", "Medium", or "High"
    val category: String = "General", // e.g. "Work", "Personal"
    val isCompleted: Boolean = false,
    val userEmail: String
)
