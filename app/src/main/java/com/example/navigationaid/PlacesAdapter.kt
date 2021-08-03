package com.example.navigationaid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlacesAdapter :
    RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>(){

    //placeholder for size of list of places, empty right now
    private val placesList = listOf<Int>()

    class PlaceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        //name of place
        val textView = view.findViewById<TextView>(R.id.textView)
        //image of place
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        //if last item in list, it can be clicked to add new place
    }

    override fun getItemCount(): Int {
        //return number of places plus one for adding new places
        return placesList.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.route_item_view, parent, false)
        return PlaceViewHolder(layout)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        if (position >= placesList.size) {
            holder.textView.setText(R.string.add_new_place)
            holder.imageView.setImageResource(R.drawable.ic_baseline_add_circle_24)
        } else {
            holder.textView.text = "Sample Text"
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }
}