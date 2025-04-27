package com.example.taskera.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.taskera.data.Task
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (
        updated: Task
    ) -> Unit
) {
    // Initialize state from the task
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description.orEmpty()) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var startTime by remember { mutableStateOf(task.startTime) }
    var endTime by remember { mutableStateOf(task.endTime) }
    var priority by remember { mutableStateOf(task.priority) }
    var category by remember { mutableStateOf(task.category) }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Description
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(Modifier.height(8.dp))

                // Due Date Picker
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.time = task.dueDate ?: Date()
                    DatePickerDialog(
                        context,
                        { _, y,m,d ->
                            cal.set(y,m,d)
                            dueDate = cal.time
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text(dueDate?.let { dateFormatter.format(it) } ?: "Select Due Date")
                }
                Spacer(Modifier.height(8.dp))

                // Start Time Picker
                OutlinedButton(onClick = {
                    val now = startTime ?: LocalTime.now()
                    TimePickerDialog(
                        context,
                        { _, h,min ->
                            startTime = LocalTime.of(h, min)
                        }, now.hour, now.minute, true
                    ).show()
                }) {
                    Text(startTime?.format(timeFormatter) ?: "Select Start Time")
                }
                Spacer(Modifier.height(8.dp))

                // End Time Picker
                OutlinedButton(onClick = {
                    val now = endTime ?: LocalTime.now()
                    TimePickerDialog(
                        context,
                        { _, h,min ->
                            endTime = LocalTime.of(h, min)
                        }, now.hour, now.minute, true
                    ).show()
                }) {
                    Text(endTime?.format(timeFormatter) ?: "Select End Time")
                }
                Spacer(Modifier.height(8.dp))

                // Priority Dropdown
                PriorityDropdown(selected = priority, onSelect = { priority = it })
                Spacer(Modifier.height(8.dp))

                // Category Dropdown
                CategoryDropdown(selected = category, onSelect = { category = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // build updated Task
                val updated = task.copy(
                    title       = title,
                    description = description.takeIf { it.isNotBlank() },
                    dueDate     = dueDate,
                    startTime   = startTime,
                    endTime     = endTime,
                    priority    = priority,
                    category    = category
                )
                onSave(updated)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
