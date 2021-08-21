package com.example.navigationaid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.navigationaid.data.PlaceItem
import com.example.navigationaid.databinding.PlaceItemViewBinding
import java.io.File

class PlacesAdapter(private val filesDir: File) : ListAdapter<PlaceItem, PlacesAdapter.PlaceViewHolder>(DiffCallback) {
    override fun getItemCount(): Int {
        //return number of places plus one for adding new places
        return currentList.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        return PlaceViewHolder(
            PlaceItemViewBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        if (position >= currentList.size) {
            holder.textView.setText(R.string.add_new_place)
            holder.imageView.setImageResource(R.drawable.ic_baseline_add_circle_24)

            //clicking last element will open editor for new place
            holder.imageView.setOnClickListener {
                val action = PlacesFragmentDirections.actionPlacesFragmentToPlaceEditorFragment()
                it.findNavController().navigate(action)
            }
        } else {
            val current = getItem(position)
            holder.bind(current, filesDir)

            //clicking will open list of routes
            holder.imageView.setOnClickListener {
                val action = PlacesFragmentDirections.actionPlacesFragmentToRoutesFragment(current.id)
                it.findNavController().navigate(action)
            }
            //long clicking will open editor of clicked place
            holder.imageView.setOnLongClickListener {
                val action = PlacesFragmentDirections.actionPlacesFragmentToPlaceEditorFragment(current.id)
                it.findNavController().navigate(action)
                true
            }
        }
    }

    class PlaceViewHolder(private var binding: PlaceItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        //name of place
        val textView = binding.textView
        //image of place
        val imageView = binding.imageView

        fun bind(placeItem: PlaceItem, filesDir: File) {
            val file = File(filesDir, placeItem.imageName)
            val image: Bitmap? = try {
                BitmapFactory.decodeFile(file.path)
            } catch (e: Exception) {
                Log.d("PlaceAdapter", "bind: Error reading image")
                null
            }

            binding.apply {
                textView.text = placeItem.name
                if (image == null) {
                    imageView.setImageResource(R.drawable.home)
                } else {
                    imageView.setImageBitmap(image)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PlaceItem>() {
            override fun areItemsTheSame(oldItem: PlaceItem, newItem: PlaceItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: PlaceItem, newItem: PlaceItem): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}