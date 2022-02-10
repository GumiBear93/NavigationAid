package com.example.navigationaid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.navigationaid.data.TaskItem
import com.example.navigationaid.databinding.TaskItemViewBinding
import com.example.navigationaid.model.StudyDataViewModel

class StudyManagerAdapter(private val dataViewModel: StudyDataViewModel, private val taskClickListener: OnTaskClickListener) :
    ListAdapter<TaskItem, StudyManagerAdapter.TaskViewHolder>(DiffCallback) {

    interface OnTaskClickListener {
        fun onTaskClicked(taskId: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder(
            TaskItemViewBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            ),
            dataViewModel
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, taskClickListener)
    }

    class TaskViewHolder(
        private var binding: TaskItemViewBinding,
        private val dataViewModel: StudyDataViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(taskItem: TaskItem, taskClickListener: OnTaskClickListener) {
            binding.buttonTask.text = dataViewModel.getTaskButtonLabel(taskItem.taskId)
            binding.buttonTask.setOnClickListener {
                taskClickListener.onTaskClicked(taskItem.taskId)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TaskItem>() {
            override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean {
                return oldItem.taskId == newItem.taskId
            }
        }
    }
}