package com.example.taskera.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskera.R
import com.example.taskera.data.Task
import com.example.taskera.ui.components.TasksByDateDialog
import java.util.Date

class TaskListDialogFragment(
    private val date: Date,
    private val tasks: List<Task>,
    private val onStatusChanged: (Task, Boolean) -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(ComposeView(requireContext()).apply {
            setContent {
                TasksByDateDialog(
                    date = date,
                    tasks = tasks,
                    onDismiss = { dismiss() },
                    onItemClick = { task ->
                        // open detail or edit
                    },
                    onTaskStatusChanged = onStatusChanged
                )
            }
        })
        return dialog
    }
}
