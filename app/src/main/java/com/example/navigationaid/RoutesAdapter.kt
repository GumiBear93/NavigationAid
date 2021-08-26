package com.example.navigationaid

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.navigationaid.data.RouteItem
import com.example.navigationaid.databinding.RouteItemViewBinding
import com.example.navigationaid.model.RoutesViewModel

class RoutesAdapter(
    private val context: Context,
    private val sharedViewModel: RoutesViewModel
) :
    ListAdapter<RouteItem, RoutesAdapter.RoutesViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutesViewHolder {
        return RoutesViewHolder(
            RouteItemViewBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            ),
            context,
            sharedViewModel
        )
    }

    override fun onBindViewHolder(holder: RoutesViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            val action =
                RoutesFragmentDirections.actionRoutesFragmentToRouteOverviewFragment(position)
            it.findNavController().navigate(action)
        }
        holder.bind(current)
    }

    class RoutesViewHolder(
        private var binding: RouteItemViewBinding,
        private val context: Context,
        private val sharedViewModel: RoutesViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(routeItem: RouteItem) {
            val imageResource =
                sharedViewModel.getDifficultyImageResourceId(routeItem.roadDifficulty)
            val duration = sharedViewModel.getFormattedDuration(routeItem.duration, context)
            val imageDescription =
                sharedViewModel.getDifficultyImageDescription(routeItem.roadDifficulty, context)

            binding.apply {
                textViewItemNumber.text = (adapterPosition + 1).toString()
                textViewDuration.text = duration
                imageViewDifficulty.setImageResource(imageResource)
                imageViewDifficulty.contentDescription = imageDescription
            }

        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RouteItem>() {
            override fun areItemsTheSame(oldItem: RouteItem, newItem: RouteItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: RouteItem, newItem: RouteItem): Boolean {
                return oldItem.road.mNodes == newItem.road.mNodes
            }
        }
    }
}
