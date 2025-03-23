package com.example.taskera.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

class TaskDetailDialogFragment(private val task: Task) : DialogFragment() {

    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_task_details, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTaskDetailTitle)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvTaskDetailDescription)
        val tvDueDate = dialogView.findViewById<TextView>(R.id.tvTaskDetailDueDate)
        val tvStartTime = dialogView.findViewById<TextView>(R.id.tvTaskDetailStartTime)
        val tvEndTime = dialogView.findViewById<TextView>(R.id.tvTaskDetailEndTime)
        val tvPriority = dialogView.findViewById<TextView>(R.id.tvTaskDetailPriority)
        val tvCategory = dialogView.findViewById<TextView>(R.id.tvTaskDetailCategory)
        val btnEdit = dialogView.findViewById<Button>(R.id.btnEditTask)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDeleteTask)

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        // Formatter for LocalTime (start and end times)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        // Set the basic details
        tvTitle.text = task.title
        tvDescription.text = task.description ?: "No description"
        tvDueDate.text = task.dueDate?.let { "Due Date: ${dateFormatter.format(it)}" } ?: "No due date"
        tvPriority.text = "Priority: ${task.priority}"
        tvCategory.text = "Category: ${task.category}"

        // Set start time if available
        tvStartTime.text = task.startTime?.let {
            "Start Time: ${it.format(timeFormatter)}"
        } ?: "No start time"

        // Set end time if available
        tvEndTime.text = task.endTime?.let {
            "End Time: ${it.format(timeFormatter)}"
        } ?: "No end time"

        // Set priority color
        val priorityColors = mapOf(
            "High" to R.color.red,
            "Medium" to R.color.orange,
            "Low" to R.color.green
        )
        val context = dialogView.context
        tvPriority.setTextColor(ContextCompat.getColor(context, priorityColors[task.priority] ?: R.color.dark_gray))

        btnEdit.setOnClickListener {
            val editDialog = EditTaskDialogFragment(task)
            editDialog.show(parentFragmentManager, "EditTaskDialog")
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete") { _, _ ->
                    taskViewModel.deleteTask(task)
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        builder.setView(dialogView)
        return builder.create()
    }
}
