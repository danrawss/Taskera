package com.example.taskera.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.utils.CalendarUtils
import com.example.taskera.viewmodel.TaskViewModel
import com.example.taskera.utils.combineDateAndTime
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class AddTaskDialogFragment : DialogFragment() {

    private val taskViewModel: TaskViewModel by activityViewModels()
    private var selectedDate: Date? = null
    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater

        // Inflate the custom dialog layout
        val dialogView = inflater.inflate(R.layout.dialog_add_task, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTaskTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etTaskDescription)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        // New buttons for time selection
        val btnSelectStartTime = dialogView.findViewById<Button>(R.id.btnSelectStartTime)
        val btnSelectEndTime = dialogView.findViewById<Button>(R.id.btnSelectEndTime)

        // Set up Priority Dropdown
        val priorities = arrayOf("Low", "Medium", "High")
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)
        spinnerPriority.adapter = priorityAdapter

        // Set up Category Dropdown
        val categories = arrayOf("Personal", "Work", "Study", "Health", "Finance", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter

        // Set up Date Picker
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
        btnSelectStartTime.setOnClickListener {
            val currentTime = LocalTime.now()
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
        btnSelectEndTime.setOnClickListener {
            val currentTime = LocalTime.now()
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

        builder.setView(dialogView)
            .setTitle("Add New Task")
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val priority = spinnerPriority.selectedItem.toString()
                val category = spinnerCategory.selectedItem.toString()

                if (title.isNotEmpty()) {
                    // Retrieve current signed-in user's email
                    val currentUserEmail = GoogleSignIn.getLastSignedInAccount(requireContext())?.email ?: ""

                    val newTask = Task(
                        title = title,
                        description = if (description.isEmpty()) null else description,
                        dueDate = selectedDate,
                        startTime = selectedStartTime,
                        endTime = selectedEndTime,
                        priority = priority,
                        category = category,
                        isCompleted = false,
                        userEmail = currentUserEmail
                    )
                    taskViewModel.insertTask(newTask)

                    if (selectedDate != null && selectedStartTime != null && selectedEndTime != null) {
                        val startMillis = combineDateAndTime(selectedDate!!, selectedStartTime!!)
                        val endMillis = combineDateAndTime(selectedDate!!, selectedEndTime!!)
                        lifecycleScope.launch(Dispatchers.IO) {
                            val eventId = CalendarUtils.createCalendarEvent(
                                requireContext(),
                                title,
                                description, // or any description you want
                                startMillis,
                                endMillis
                            )
                            Log.d("AddTaskDialog", "Calendar event created with ID: $eventId")
                            // Optionally, update the task with eventId if needed
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }
}
