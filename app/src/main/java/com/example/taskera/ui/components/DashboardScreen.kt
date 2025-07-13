package com.example.taskera.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import com.example.taskera.viewmodel.Stats
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = DateTimeFormatter.ofPattern("MM/dd")

    Column(modifier = modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = { Text("Dashboard") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
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
            }
        }

        Spacer(Modifier.height(24.dp))

        // This Week by Category (Pie + Legend)
        Text(
            "This Week by Category",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        WeeklyCategoryPieChart(
            data = weeklyCategories,
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        // Legend
        Column(modifier = Modifier.padding(horizontal = 32.dp)) {
            // Use same color palette as pie chart
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

        // Last 7-Day Due Trend (Line + Data Row)
        Text(
            "Last 7-Day Due Trend",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        SevenDayTrendLineChart(
            data = oneWeekTrend,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(start = 16.dp, end = 32.dp)
        )

        Spacer(Modifier.height(8.dp))
        // Exact numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            oneWeekTrend.forEach { (date, count) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(date.format(dateFmt), style = MaterialTheme.typography.bodySmall)
                    Text("$count", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}