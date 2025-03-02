package com.example.taskera.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.viewmodel.TaskViewModel

class AddTaskDialogFragment : DialogFragment() {

    // Share the TaskViewModel with the activity
    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater

        // Note: change the layout resource name here:
        val dialogView = inflater.inflate(R.layout.task_item, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.tvTaskTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.tvTaskDescription)

        builder.setView(dialogView)
            .setTitle("Add New Task")
            .setPositiveButton("Add") { dialog, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                if (title.isNotEmpty()) {
                    val newTask = Task(
                        title = title,
                        description = if (description.isEmpty()) null else description
                    )
                    taskViewModel.insertTask(newTask)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }
}
