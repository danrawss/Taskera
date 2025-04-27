package com.example.taskera.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskera.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    task: Task,
    onDismiss: () -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(4.dp))
                Text(task.description ?: "No description", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Due: ${task.dueDate?.let { dateFmt.format(it) } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text("Priority: ${task.priority}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("Category: ${task.category}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onEdit(task)
                onDismiss()
            }) {
                Text("Edit")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDelete(task)
                onDismiss()
            }) {
                Text("Delete")
            }
        }
    )
}
