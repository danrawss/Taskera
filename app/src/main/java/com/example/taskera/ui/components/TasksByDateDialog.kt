// TasksByDateDialog.kt
package com.example.taskera.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskera.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksByDateDialog(
    date: Date,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onTaskStatusChanged: (Task, Boolean) -> Unit,
    onItemClick: (Task) -> Unit
) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tasks for ${fmt.format(date)}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                if (tasks.isEmpty()) {
                    Text("No tasks on this date", Modifier.padding(8.dp))
                } else {
                    LazyColumn {
                        items(tasks) { task ->
                            TaskItem(
                                task = task,
                                onItemClick = { onItemClick(task) },
                                onTaskStatusChanged = { _, isChecked -> onTaskStatusChanged(task, isChecked) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
