package com.example.navigationaid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.navigationaid.databinding.RouteItemViewBinding
import com.example.navigationaid.routing.RoadDifficulty
import com.example.navigationaid.routing.RouteItem

class RoutesAdapter(private val durationPlaceholder: String) :
    ListAdapter<RouteItem, RoutesAdapter.RoutesViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutesViewHolder {
        return RoutesViewHolder(
            RouteItemViewBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            ),
            durationPlaceholder
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
        private var durationPlaceholder: String
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(routeItem: RouteItem) {
            val imageResource = when (routeItem.roadDifficulty) {
                RoadDifficulty.DIFFICULTY_1 -> R.drawable.ic_difficulty_1
                RoadDifficulty.DIFFICULTY_2 -> R.drawable.ic_difficulty_2
                RoadDifficulty.DIFFICULTY_3 -> R.drawable.ic_difficulty_3
                RoadDifficulty.DIFFICULTY_4 -> R.drawable.ic_difficulty_4
                else -> R.drawable.ic_difficulty_5
            }

            binding.apply {
                textViewItemNumber.text = (adapterPosition + 1).toString()
                textViewDuration.text =
                    String.format(durationPlaceholder, (routeItem.duration / 60.0).toInt())
                imageViewDifficulty.setImageResource(imageResource)
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
