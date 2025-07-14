package com.example.taskera.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import com.example.taskera.viewmodel.Stats
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    stats: Stats,
    weeklyCategories: Map<String, Int>,
    oneWeekTrend: List<Pair<LocalDate, Int>>,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = DateTimeFormatter.ofPattern("MM/dd")

    Scaffold(
        modifier = modifier,
        topBar = {
            SmallTopAppBar(
                title = { Text("Dashboard") },
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
                    // 1) inset for the app-bar's height & any system bars
                    .padding(innerPadding)
                    // 2) your own content padding
                    .padding(16.dp)
            ) {
                // --- Today's Tasks ---
                Text("Today's Tasks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("${stats.completedCount} of ${stats.totalCount} done")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (stats.totalCount > 0)
                        stats.completedCount.toFloat() / stats.totalCount
                    else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Spacer(Modifier.height(24.dp))

                // --- This Week by Category ---
                Text("This Week by Category", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                WeeklyCategoryPieChart(
                    data = weeklyCategories,
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Legend
                Column {
                    val colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                    weeklyCategories.entries.forEachIndexed { idx, (cat, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(colors[idx % colors.size], shape = MaterialTheme.shapes.small)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("$cat: $count", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(32.dp))

                // --- Last 7-Day Due Trend ---
                Text("Last 7-Day Due Trend", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SevenDayTrendLineChart(
                    data = oneWeekTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Exact numbers row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    oneWeekTrend.forEach { (date, count) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.format(dateFmt), style = MaterialTheme.typography.bodySmall)
                            Text("$count", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    )
}
