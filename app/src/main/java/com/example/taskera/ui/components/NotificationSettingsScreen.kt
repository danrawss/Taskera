package com.example.taskera.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height

import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface

import com.example.taskera.viewmodel.SettingsViewModel

import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled by viewModel.remindersEnabled.observeAsState(true)
    val leadMin by viewModel.defaultLeadMin.observeAsState(30)

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)  // inset for status bar / app bar
                    .padding(16.dp)         // your own content padding
            ) {
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable reminders", Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.setRemindersEnabled(it) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Default lead time",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(8.dp))

                LeadTimeDropdown(
                    options = listOf(5, 10, 15, 30, 60).map { Duration.ofMinutes(it.toLong()) },
                    selected = Duration.ofMinutes(leadMin.toLong()),
                    includeNoReminder = false,
                    onSelect = { viewModel.setDefaultLeadMin(it.toMinutes().toInt()) }
                )
            }
        }
    )
}
