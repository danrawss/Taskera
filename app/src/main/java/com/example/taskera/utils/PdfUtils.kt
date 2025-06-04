// PdfUtils.kt
package com.example.taskera.ui.utils

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.example.taskera.data.Task
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun createTodayPlanPdf(
    context: Context,
    tasksForToday: List<Task>,
    date: LocalDate
): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(
        /* width= */ 595,
        /* height= */ 842,
        /* pageNumber= */ 1
    ).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    // Paint for Title
    val titlePaint = android.graphics.Paint().apply {
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
        textSize = 18f
        color = android.graphics.Color.BLACK
    }
    // Paint for Items
    val itemPaint = android.graphics.Paint().apply {
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
        textSize = 14f
        color = android.graphics.Color.DKGRAY
    }

    // Draw header
    val dateFormatter = DateTimeFormatter.ofPattern("MMddyyyy")
    val titleText = "Daily Plan – ${date.format(dateFormatter)}"
    canvas.drawText(titleText, 40f, 50f, titlePaint)

    // Draw each task, starting at y=80f, increment by 24f
    var yPosition = 80f
    tasksForToday.forEach { task ->
        val timeRange = if (task.startTime != null && task.endTime != null) {
            "[${task.startTime} – ${task.endTime}] "
        } else {
            ""
        }
        val categoryPart = if (!task.category.isNullOrBlank()) {
            " (${task.category})"
        } else {
            ""
        }
        val line = "• $timeRange${task.title}$categoryPart"
        canvas.drawText(line, 40f, yPosition, itemPaint)
        yPosition += 24f
        if (yPosition > pageInfo.pageHeight - 40) {
            // For simplicity, we stop if we run out of space.
            return@forEach
        }
    }

    pdfDocument.finishPage(page)

    // Save to cache directory
    val fileName = "daily_plan_${date.format(dateFormatter)}.pdf"

    val outputFile = File(context.cacheDir, fileName)
    FileOutputStream(outputFile).use { out ->
        pdfDocument.writeTo(out)
    }
    pdfDocument.close()
    return outputFile
}
