package com.example.navigationaid

import android.annotation.SuppressLint
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
    private val sharedViewModel: RoutesViewModel, private val itemClickListener: OnItemClickListener
) :
    ListAdapter<RouteItem, RoutesAdapter.RoutesViewHolder>(DiffCallback) {
    interface OnItemClickListener {
        fun onItemClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutesViewHolder {
        return RoutesViewHolder(
            RouteItemViewBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            ),
            sharedViewModel
        )
    }

    override fun onBindViewHolder(holder: RoutesViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClicked()
            val action =
                RoutesFragmentDirections.actionRoutesFragmentToRouteOverviewFragment(position)
            it.findNavController().navigate(action)
        }
        holder.bind(current)
    }

    class RoutesViewHolder(
        private var binding: RouteItemViewBinding,
        private val sharedViewModel: RoutesViewModel
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(routeItem: RouteItem) {
            // load resources and bind them to UI
            val duration = sharedViewModel.getFormattedDuration(routeItem.duration)
            val distance = sharedViewModel.getFormattedDistance(routeItem.distance)

            binding.apply {
                textViewItemNumber.text = (adapterPosition + 1).toString()
                textViewDuration.text = duration
                textViewDistance.text = distance
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
