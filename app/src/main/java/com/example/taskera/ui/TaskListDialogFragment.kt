package com.example.taskera.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskera.R
import com.example.taskera.data.Task

class TaskListDialogFragment(
    private var tasks: List<Task>,
    private val onTaskStatusChanged: (Task) -> Unit
) : DialogFragment() {

    private lateinit var taskAdapter: TaskAdapter

    companion object {
        fun newInstance(
            tasks: List<Task>,
            onTaskStatusChanged: (Task) -> Unit
        ): TaskListDialogFragment {
            return TaskListDialogFragment(tasks, onTaskStatusChanged)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_task_list, null)

        // Initialize RecyclerView and adapter
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvTaskList)
        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                // Open TaskDetailDialogFragment for the selected task
                val dialog = TaskDetailDialogFragment(task)
                dialog.show(parentFragmentManager, "TaskDetailDialog")
            },
            onTaskStatusChanged = { updatedTask ->
                onTaskStatusChanged(updatedTask)
            }
        )
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set initial data
        taskAdapter.setData(tasks)

        builder.setView(view)
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    fun updateTasks(updatedTasks: List<Task>) {
        if (updatedTasks.isEmpty()) {
            dismiss()
        } else {
            tasks = updatedTasks
            taskAdapter.setData(updatedTasks)
            taskAdapter.notifyDataSetChanged()
        }
    }
}
