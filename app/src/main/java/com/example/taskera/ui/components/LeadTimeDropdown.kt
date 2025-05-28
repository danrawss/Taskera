package com.example.taskera.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadTimeDropdown(
    options: List<Duration>,
    selected: Duration,
    includeNoReminder: Boolean = true,
    onSelect: (Duration) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayOptions = remember(options, includeNoReminder) {
        buildList {
            if (includeNoReminder) add(Duration.ZERO)
            addAll(options)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        val text = if (selected == Duration.ZERO) "No Reminder" else
            when {
                selected.toMinutes() < 60 -> "${selected.toMinutes()} min"
                selected.toMinutes() % 60 == 0L -> "${selected.toHours()} h"
                else -> {
                    val hours = selected.toHours()
                    val mins = selected.minus(hours, ChronoUnit.HOURS).toMinutes()
                    "${hours}h ${mins}m"
                }
            }

        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text("Reminder") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            displayOptions.forEach { duration ->
                val optionText = if (duration == Duration.ZERO) "No Reminder" else
                    when {
                        duration.toMinutes() < 60 -> "${duration.toMinutes()} min"
                        duration.toMinutes() % 60 == 0L -> "${duration.toHours()} h"
                        else -> {
                            val hours = duration.toHours()
                            val mins = duration.minus(hours, ChronoUnit.HOURS).toMinutes()
                            "${hours}h ${mins}m"
                        }
                    }
                DropdownMenuItem(
                    text = { Text(optionText) },
                    onClick = {
                        onSelect(duration)
                        expanded = false
                    }
                )
            }
        }
    }
}
