package com.example.taskera.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.taskera.R
import com.example.taskera.data.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onItemClick: (Task) -> Unit,
    private val onTaskStatusChanged: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var taskList = emptyList<Task>()

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val checkBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        holder.tvTitle.text = task.title
        holder.tvDueDate.text = task.dueDate?.let { "Due: ${dateFormatter.format(it)}" } ?: "No due date"
        holder.tvPriority.text = "Priority: ${task.priority}"
        holder.tvCategory.text = "Category: ${task.category}"

        // Remove previous listener before setting a new one
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.isCompleted

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val updatedTask = task.copy(isCompleted = isChecked)
            holder.checkBox.isEnabled = false

            onTaskStatusChanged(updatedTask)

            holder.checkBox.postDelayed({
                holder.checkBox.isEnabled = true
            }, 200)
        }


        val context = holder.itemView.context

        // Set priority colors
        val priorityColors = mapOf(
            "High" to R.color.red,
            "Medium" to R.color.orange,
            "Low" to R.color.green
        )
        holder.tvPriority.setTextColor(ContextCompat.getColor(context, priorityColors[task.priority] ?: R.color.dark_gray))

        // Set category colors
        val categoryColors = mapOf(
            "Work" to R.color.blue,
            "Study" to R.color.purple,
            "Personal" to R.color.pink,
            "Health" to R.color.yellow,
            "Finance" to R.color.cyan
        )
        holder.tvCategory.setTextColor(ContextCompat.getColor(context, categoryColors[task.category] ?: R.color.dark_gray))

        // Handle task click event to open details
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = taskList.size

    fun setData(newTasks: List<Task>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = taskList.size
            override fun getNewListSize() = newTasks.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = taskList[oldPos].id == newTasks[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = taskList[oldPos] == newTasks[newPos]
        })
        taskList = newTasks
        diffResult.dispatchUpdatesTo(this)
    }
}
