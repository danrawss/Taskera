package com.example.taskera.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.viewmodel.TaskViewModel

class TaskListDialogFragment(private var tasks: List<Task>) : DialogFragment() {

    companion object {
        fun newInstance(tasks: List<Task>): TaskListDialogFragment {
            return TaskListDialogFragment(tasks)
        }
    }

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_task_list, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvTaskList)
        taskAdapter = TaskAdapter { task ->
            val dialog = TaskDetailDialogFragment(task)
            dialog.show(parentFragmentManager, "TaskDetailDialog")
        }
        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Set initial data
        taskAdapter.setData(tasks)

        builder.setView(view)
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    // ✅ Method to update tasks dynamically
    fun updateTasks(updatedTasks: List<Task>) {
        if (updatedTasks.isEmpty()) {
            dismiss() // ✅ Close dialog if no tasks left
        } else {
            tasks = updatedTasks
            taskAdapter.setData(updatedTasks)
            taskAdapter.notifyDataSetChanged()
        }
    }
}




