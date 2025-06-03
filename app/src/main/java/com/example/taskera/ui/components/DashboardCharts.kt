package com.example.taskera.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun WeeklyCategoryPieChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().coerceAtLeast(1)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer
    )
    Canvas(modifier = modifier) {
        // Compute the diameter based on the smaller dimension
        val diameter = size.minDimension

        // Figure out the top-left corner so the circle is centered
        val left   = (size.width - diameter) / 2f
        val top    = (size.height - diameter) / 2f
        val diameterSize = Size(diameter, diameter)

        var startAngle = -90f
        data.entries.forEachIndexed { idx, (_, count) ->
            val sweep = (count / total.toFloat()) * 360f

            drawArc(
                color      = colors[idx % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter  = true,
                topLeft    = Offset(left, top),
                size       = diameterSize
            )
            startAngle += sweep
        }
    }
}

@Composable
fun SevenDayTrendLineChart(
    data: List<Pair<LocalDate, Int>>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary

    val maxCount = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val wStep = size.width / (data.size - 1).coerceAtLeast(1)
        val h     = size.height

        // Build points
        val points = data.mapIndexed { i, pair ->
            Offset(
                x = i * wStep,
                y = h - (pair.second / maxCount.toFloat()) * h
            )
        }

        // Draw the connecting line
        drawPath(
            path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            },
            color = lineColor,
            style = Stroke(width = 3f)
        )

        // Draw the circles
        points.forEach {
            drawCircle(
                color  = lineColor,
                radius = 4f,
                center = it
            )
        }
    }
}
