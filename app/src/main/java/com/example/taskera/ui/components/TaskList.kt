package com.example.taskera.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.taskera.data.Task

@Composable
fun TaskList(
    tasks: List<Task>,
    onItemClick: (Task) -> Unit,
    onTaskStatusChanged: (Task, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(tasks) { task ->
            TaskItem(
                task = task,
                onItemClick = { onItemClick(task) },
                onTaskStatusChanged = {_, isChecked -> onTaskStatusChanged(task, isChecked) }
            )
        }
    }
}
