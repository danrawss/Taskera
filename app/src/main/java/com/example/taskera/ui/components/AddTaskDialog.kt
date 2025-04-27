package com.example.taskera.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        description: String?,
        dueDate: Date?,
        startTime: LocalTime?,
        endTime: LocalTime?,
        priority: String,
        category: String
    ) -> Unit
) {
    // Local state for each field
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Date?>(null) }
    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }
    var priority by remember { mutableStateOf("Low") }
    var category by remember { mutableStateOf("Personal") }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
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

                // Due Date picker
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance()
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

                // Start Time picker
                OutlinedButton(onClick = {
                    val now = LocalTime.now()
                    TimePickerDialog(
                        context,
                        { _, h, min ->
                            startTime = LocalTime.of(h, min)
                        }, now.hour, now.minute, true
                    ).show()
                }) {
                    Text(startTime?.format(timeFormatter) ?: "Select Start Time")
                }
                Spacer(Modifier.height(8.dp))

                // End Time picker
                OutlinedButton(onClick = {
                    val now = LocalTime.now()
                    TimePickerDialog(
                        context,
                        { _, h, min ->
                            endTime = LocalTime.of(h, min)
                        }, now.hour, now.minute, true
                    ).show()
                }) {
                    Text(endTime?.format(timeFormatter) ?: "Select End Time")
                }
                Spacer(Modifier.height(8.dp))

                // Priority dropdown
                PriorityDropdown(selected = priority, onSelect = { priority = it })
                Spacer(Modifier.height(8.dp))

                // Category dropdown
                CategoryDropdown(selected = category, onSelect = { category = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title,
                            description.takeIf { it.isNotBlank() },
                            dueDate,
                            startTime,
                            endTime,
                            priority,
                            category
                        )
                        onDismiss()
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("Low", "Medium", "High")
    var expanded by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Priority") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    Modifier.clickable { expanded = !expanded }
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(4.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(140.dp)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                    modifier = Modifier.height(40.dp)  // more compact items
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("Personal", "Work", "Study", "Health", "Finance", "Other")
    var expanded by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    Modifier.clickable { expanded = !expanded }
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(4.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(160.dp)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}