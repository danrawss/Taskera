package com.example.taskera.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskera.data.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskItem(
    task: Task,
    onItemClick: () -> Unit,
    onTaskStatusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium
                )
                // Format and display due date if available
                task.dueDate?.let {
                    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text(
                        text = "Due: ${dateFormatter.format(it)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Display priority and category
                Text(
                    text = "Priority: ${task.priority}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Category: ${task.category}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Checkbox for marking task as completed
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { checked -> onTaskStatusChanged(checked) }
            )
        }
    }
}
