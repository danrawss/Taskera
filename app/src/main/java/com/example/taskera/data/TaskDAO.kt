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

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Int): LiveData<Task>

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startOfDay AND :endOfDay")
    fun getTasksByDate(startOfDay: Long, endOfDay: Long): LiveData<List<Task>>

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', dueDate / 1000, 'unixepoch') FROM tasks WHERE dueDate IS NOT NULL")
    fun getDistinctTaskDates(): LiveData<List<String>>
}
