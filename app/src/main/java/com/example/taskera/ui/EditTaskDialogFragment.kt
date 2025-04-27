package com.example.taskera.ui

import android.app.Dialog
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.taskera.data.Task
import com.example.taskera.ui.components.EditTaskDialog
import com.example.taskera.utils.CalendarUtils
import com.example.taskera.utils.combineDateAndTime
import com.example.taskera.viewmodel.TaskViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class EditTaskDialogFragment(private val task: Task) : DialogFragment() {
    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val fragment = this

        val compose = ComposeView(requireContext()).apply {
            setContent {
                EditTaskDialog(
                    task = task,
                    onDismiss = {
                        fragment.dismiss()

                        parentFragmentManager
                            .findFragmentByTag("TaskDetailDialog")
                            ?.let { (it as DialogFragment).dismiss() } },
                    onSave = { updated ->
                        taskViewModel.updateTask(updated)

                        // Close the edit dialog
                        fragment.dismiss()

                        // Close the details dialog beneath
                        parentFragmentManager
                            .findFragmentByTag("TaskDetailDialog")
                            ?.let { (it as DialogFragment).dismiss() }
                    }
                )
            }
        }

        dialog.setContentView(compose)
        return dialog
    }
}
