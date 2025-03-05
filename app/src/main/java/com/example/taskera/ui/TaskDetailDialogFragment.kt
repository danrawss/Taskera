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
        val tvCategory = dialogView.findViewById<TextView>(R.id.tvTaskDetailCategory)
        val tvPriority = dialogView.findViewById<TextView>(R.id.tvTaskDetailPriority)
        val btnEdit = dialogView.findViewById<Button>(R.id.btnEditTask)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDeleteTask)

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        tvTitle.text = task.title
        tvDescription.text = task.description ?: "No description"
        tvDueDate.text = task.dueDate?.let { "Due Date: ${dateFormatter.format(it)}" } ?: "No due date"
        tvCategory.text = "Category: ${task.category}"
        tvPriority.text = "Priority: ${task.priority}"

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
