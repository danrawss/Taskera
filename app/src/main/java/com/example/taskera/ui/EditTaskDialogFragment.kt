package com.example.taskera.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

class EditTaskDialogFragment(private val task: Task) : DialogFragment() {

    private val taskViewModel: TaskViewModel by activityViewModels()
    private var selectedDate: Date? = task.dueDate

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater

        // Inflate layout
        val dialogView = inflater.inflate(R.layout.dialog_add_task, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etTaskDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        // Set initial values
        etTitle.setText(task.title)
        etDescription.setText(task.description)

        // Priority dropdown setup
        val priorities = arrayOf("Low", "Medium", "High")
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        spinnerPriority.adapter = priorityAdapter
        spinnerPriority.setSelection(priorities.indexOf(task.priority))

        // Category dropdown setup
        val categories = arrayOf("Personal", "Work", "Study", "Health", "Finance", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter
        spinnerCategory.setSelection(categories.indexOf(task.category))

        // Date Picker setup
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        task.dueDate?.let {
            btnSelectDate.text = dateFormatter.format(it)
        }

        btnSelectDate.setOnClickListener {
            val datePicker = DatePickerDialog(requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    btnSelectDate.text = dateFormatter.format(selectedDate!!)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Save Changes
        builder.setView(dialogView)
            .setTitle("Edit Task")
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val priority = spinnerPriority.selectedItem.toString()
                val category = spinnerCategory.selectedItem.toString()

                if (title.isNotEmpty()) {
                    val updatedTask = task.copy(
                        title = title,
                        description = if (description.isEmpty()) null else description,
                        priority = priority,
                        dueDate = selectedDate,
                        category = category
                    )
                    taskViewModel.updateTask(updatedTask)

                    // Dismiss Edit Dialog
                    dismiss()

                    // Close the TaskDetailDialog if it's open
                    parentFragmentManager.findFragmentByTag("TaskDetailDialog")?.let { fragment ->
                        (fragment as DialogFragment).dismiss()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }
}
