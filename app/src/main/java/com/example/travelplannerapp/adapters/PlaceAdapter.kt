package com.example.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.R
import com.example.travelplannerapp.models.Place
import com.squareup.picasso.Picasso

class PlaceAdapter(
    private var placeList: List<Place>,
    private val onItemClick: (Place) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = placeList[position]

        holder.placeName.text = place.name
        holder.placeDescription.text = place.description

        if (place.imageUrl.isNotEmpty()) {
            Picasso.get().load(place.imageUrl).into(holder.placeImage)
        } else {
            holder.placeImage.setImageResource(R.drawable.ic_placeholder)
        }

        holder.itemView.setOnClickListener {
            onItemClick(place)
        }
    }

    override fun getItemCount(): Int = placeList.size

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.place_name)
        val placeDescription: TextView = itemView.findViewById(R.id.place_description)
        val placeImage: ImageView = itemView.findViewById(R.id.place_image)
    }
}