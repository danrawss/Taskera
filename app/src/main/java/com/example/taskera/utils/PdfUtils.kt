package com.example.taskera.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.taskera.data.Task
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun createTodayPlanPdf(
    context: Context,
    tasksForToday: List<Task>,
    date: LocalDate
): Uri? {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val c = page.canvas

    // 1) Title paint
    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textSize = 20f
        color = Color.BLACK
    }
    // 2) Axis & label paint
    val axisPaint = Paint().apply {
        strokeWidth = 2f
        color = Color.DKGRAY
    }
    val labelPaint = Paint().apply {
        textSize = 12f
        color = Color.DKGRAY
    }
    // 3) Block paint (varying colors)
    val blockPaints = listOf(
        Paint().apply { color = Color.parseColor("#FF8A65") },
        Paint().apply { color = Color.parseColor("#4DB6AC") },
        Paint().apply { color = Color.parseColor("#81C784") }
    )

    // Draw header
    val df = DateTimeFormatter.ofPattern("MM_dd_yyyy")
    c.drawText("Daily Plan – ${date.format(df)}", 40f, 40f, titlePaint)

    // Timeline bounds
    val top = 80f
    val bottom = pageInfo.pageHeight - 40f
    val left = 80f
    val hours = 24
    val hourH = (bottom - top) / hours

    // Draw vertical axis
    c.drawLine(left, top, left, bottom, axisPaint)
    // Ticks & hour labels
    for (h in 0..hours) {
        val y = top + h * hourH
        c.drawLine(left - 8, y, left + 8, y, axisPaint)
        c.drawText(String.format("%02d:00", h), 40f, y + 4f, labelPaint)
    }

    // Sort tasks by startTime
    val sorted = tasksForToday
        .filter { it.startTime != null && it.endTime != null }
        .sortedBy { it.startTime }

    // Draw each task block
    sorted.forEachIndexed { idx, task ->
        val start = task.startTime!!
        val end = task.endTime!!
        // Y positions
        val yStart = top + (start.hour + start.minute / 60f) * hourH
        val yEnd   = top + (end.hour   + end.minute   / 60f) * hourH
        val blockTop = yStart
        val blockBottom = yEnd.coerceAtMost(bottom)

        // Pick a paint (cycle through list)
        val paint = blockPaints[idx % blockPaints.size]
        // Draw rectangle
        val rect = RectF(left + 16f, blockTop, pageInfo.pageWidth - 40f, blockBottom - 2f)
        c.drawRoundRect(rect, 8f, 8f, paint)

        // Draw task title inside block
        labelPaint.color = Color.WHITE
        labelPaint.textSize = 12f
        val text = task.title
        c.drawText(text, rect.left + 8f, rect.top + 16f, labelPaint)
        // Reset label color
        labelPaint.color = Color.DKGRAY
    }

    pdf.finishPage(page)

    // Save file
    val filename = "daily_plan_${date.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))}.pdf"
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        // RELATIVE_PATH only works on Android Q+; on older you’ll need WRITE_EXTERNAL_STORAGE
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val itemUri = resolver.insert(collection, contentValues)
    itemUri?.let { uri ->
        resolver.openOutputStream(uri).use { out ->
            pdf.writeTo(out!!)
        }
    }

    pdf.close()
    return itemUri
}
