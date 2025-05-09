package com.example.taskera.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
fun TaskDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onSubmit: (Task) -> Unit
) {
    // Prepare a working copy (if editing) or defaults (if adding)
    val isEdit = task != null
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val context = LocalContext.current

    // State, saved across rotations
    var title       by rememberSaveable { mutableStateOf(task?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(task?.description.orEmpty()) }
    var dueDate     by rememberSaveable { mutableStateOf(task?.dueDate) }
    var startTime   by rememberSaveable { mutableStateOf(task?.startTime) }
    var endTime     by rememberSaveable { mutableStateOf(task?.endTime) }
    var priority    by rememberSaveable { mutableStateOf(task?.priority ?: "Low") }
    var category    by rememberSaveable { mutableStateOf(task?.category ?: "Personal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Edit Task" else "Add New Task")
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(Modifier.height(8.dp))

                // Due Date picker
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        time = dueDate ?: Date()
                    }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(y, m, d)
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

                // Time pickers row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Time
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val now = startTime ?: LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, h, min -> startTime = LocalTime.of(h, min) },
                            now.hour, now.minute, true
                        ).show()
                    }) {
                        Text(startTime?.format(timeFormatter) ?: "Start Time")
                    }
                    // End Time
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                        val now = endTime ?: LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, h, min -> endTime = LocalTime.of(h, min) },
                            now.hour, now.minute, true
                        ).show()
                    }) {
                        Text(endTime?.format(timeFormatter) ?: "End Time")
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Priority dropdown
                DropdownField(
                    label = "Priority",
                    options = listOf("Low", "Medium", "High"),
                    selected = priority,
                    onSelect = { priority = it }
                )
                Spacer(Modifier.height(8.dp))

                // Category dropdown
                DropdownField(
                    label = "Category",
                    options = listOf("Personal", "Work", "Study", "Health", "Finance", "Other"),
                    selected = category,
                    onSelect = { category = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        // Build Task object (keep id for edits)
                        val result = task?.copy(
                            title       = title,
                            description = description.takeIf { it.isNotBlank() },
                            dueDate     = dueDate,
                            startTime   = startTime,
                            endTime     = endTime,
                            priority    = priority,
                            category    = category
                        ) ?: Task(
                            title       = title,
                            description = description.takeIf { it.isNotBlank() },
                            dueDate     = dueDate,
                            startTime   = startTime,
                            endTime     = endTime,
                            priority    = priority,
                            category    = category,
                            userEmail   = "" // fill with current user
                        )
                        onSubmit(result)
                        onDismiss()
                    }
                }
            ) {
                Text(if (isEdit) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
