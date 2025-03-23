package com.example.taskera.data

import androidx.lifecycle.LiveData
import java.util.Date
import androidx.room.*

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Get all tasks for a specific user
    @Query("SELECT * FROM tasks WHERE userEmail = :userEmail ORDER BY id DESC")
    fun getAllTasks(userEmail: String): LiveData<List<Task>>

    // Get a single task by id for a specific user
    @Query("SELECT * FROM tasks WHERE id = :taskId AND userEmail = :userEmail")
    fun getTaskById(taskId: Int, userEmail: String): LiveData<Task>

    // Get tasks by date range for a specific user
    @Query("SELECT * FROM tasks WHERE userEmail = :userEmail AND dueDate BETWEEN :startOfDay AND :endOfDay")
    fun getTasksByDate(userEmail: String, startOfDay: Long, endOfDay: Long): LiveData<List<Task>>

    // Get distinct task dates for a specific user
    @Query("SELECT DISTINCT strftime('%Y-%m-%d', dueDate / 1000, 'unixepoch') FROM tasks WHERE userEmail = :userEmail AND dueDate IS NOT NULL")
    fun getDistinctTaskDates(userEmail: String): LiveData<List<String>>

    // Get tasks sorted by priority for a specific user
    @Query("""
        SELECT * FROM tasks 
        WHERE userEmail = :userEmail
        ORDER BY 
            CASE priority 
                WHEN 'High' THEN 1
                WHEN 'Medium' THEN 2
                WHEN 'Low' THEN 3
            END, dueDate ASC
    """)
    fun getTasksSortedByPriority(userEmail: String): LiveData<List<Task>>

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean)
}
