package com.example.taskera.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class EditTaskDialogFragment(private val task: Task) : DialogFragment() {

    private val taskViewModel: TaskViewModel by activityViewModels()
    private var selectedDate: Date? = task.dueDate
    private var selectedStartTime: LocalTime? = task.startTime
    private var selectedEndTime: LocalTime? = task.endTime
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater

        // Inflate layout (re-using dialog_add_task.xml)
        val dialogView = inflater.inflate(R.layout.dialog_add_task, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etTaskDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        // New buttons for time selection
        val btnSelectStartTime = dialogView.findViewById<Button>(R.id.btnSelectStartTime)
        val btnSelectEndTime = dialogView.findViewById<Button>(R.id.btnSelectEndTime)

        // Set initial values
        etTitle.setText(task.title)
        etDescription.setText(task.description)

        // Set up Priority Dropdown
        val priorities = arrayOf("Low", "Medium", "High")
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        spinnerPriority.adapter = priorityAdapter
        spinnerPriority.setSelection(priorities.indexOf(task.priority))

        // Set up Category Dropdown
        val categories = arrayOf("Personal", "Work", "Study", "Health", "Finance", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter
        spinnerCategory.setSelection(categories.indexOf(task.category))

        // Set up Date Picker
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

        // Set up Start Time Picker
        selectedStartTime?.let {
            btnSelectStartTime.text = it.format(timeFormatter)
        }
        btnSelectStartTime.setOnClickListener {
            val currentTime = selectedStartTime ?: LocalTime.now()
            val timePicker = TimePickerDialog(requireContext(),
                { _, hour, minute ->
                    selectedStartTime = LocalTime.of(hour, minute)
                    btnSelectStartTime.text = selectedStartTime?.format(timeFormatter)
                },
                currentTime.hour,
                currentTime.minute,
                true
            )
            timePicker.show()
        }

        // Set up End Time Picker
        selectedEndTime?.let {
            btnSelectEndTime.text = it.format(timeFormatter)
        }
        btnSelectEndTime.setOnClickListener {
            val currentTime = selectedEndTime ?: LocalTime.now()
            val timePicker = TimePickerDialog(requireContext(),
                { _, hour, minute ->
                    selectedEndTime = LocalTime.of(hour, minute)
                    btnSelectEndTime.text = selectedEndTime?.format(timeFormatter)
                },
                currentTime.hour,
                currentTime.minute,
                true
            )
            timePicker.show()
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
                        startTime = selectedStartTime,
                        endTime = selectedEndTime,
                        category = category
                        // Note: userEmail remains unchanged
                    )
                    taskViewModel.updateTask(updatedTask)
                    dismiss()
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
