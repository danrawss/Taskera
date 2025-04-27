package com.example.taskera.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.taskera.viewmodel.TaskViewModel
import com.example.taskera.ui.components.AddTaskDialog
import com.example.taskera.data.Task
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.example.taskera.utils.CalendarUtils
import com.example.taskera.utils.combineDateAndTime
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddTaskDialogFragment : DialogFragment() {

    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a ComposeView to render our dialog in Compose
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                AddTaskDialog(
                    onDismiss = { dismiss() },
                    onAdd = { title, desc, dueDate, start, end, priority, category ->
                        // ← pull the user’s email right here
                        val userEmail = GoogleSignIn.getLastSignedInAccount(requireContext())
                            ?.email.orEmpty()

                        val newTask = Task(
                            title       = title,
                            description = desc,
                            dueDate     = dueDate,
                            startTime   = start,
                            endTime     = end,
                            priority    = priority,
                            category    = category,
                            isCompleted = false,
                            userEmail   = userEmail
                        )
                        taskViewModel.insertTask(newTask)

                        if (dueDate != null && start != null && end != null) {
                            val startMillis = combineDateAndTime(dueDate, start)
                            val endMillis   = combineDateAndTime(dueDate, end)

                            // Launch on IO dispatcher
                            lifecycleScope.launch(Dispatchers.IO) {
                                val eventId = CalendarUtils.createCalendarEvent(
                                    requireContext(),
                                    title,
                                    desc.orEmpty(),
                                    startMillis,
                                    endMillis
                                )
                                Log.d("AddTaskDialog", "Calendar event created with ID: $eventId")
                            }
                        }

                        dismiss()
                    }
                )
            }
        }

        // Wrap the ComposeView in a regular Dialog
        return Dialog(requireContext()).apply {
            setContentView(composeView)
        }
    }
}
