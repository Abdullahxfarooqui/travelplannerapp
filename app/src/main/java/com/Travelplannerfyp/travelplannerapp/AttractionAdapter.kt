package com.Travelplannerfyp.travelplannerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.models.Attraction
import com.squareup.picasso.Picasso

class AttractionAdapter(private val attractions: List<Attraction>) : RecyclerView.Adapter<AttractionAdapter.AttractionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttractionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attraction, parent, false)
        return AttractionViewHolder(view)
    }
    override fun onBindViewHolder(holder: AttractionViewHolder, position: Int) {
        val attraction = attractions[position]
        holder.name.text = attraction.name
        holder.type.text = attraction.type
        holder.distance.text = attraction.distance
        if (!attraction.iconUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(attraction.iconUrl)
                .placeholder(R.drawable.ic_location)
                .error(R.drawable.ic_location)
                .into(holder.icon)
        } else {
            holder.icon.setImageResource(R.drawable.ic_location)
        }
    }
    override fun getItemCount() = attractions.size
    class AttractionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.attractionName)
        val type: TextView = itemView.findViewById(R.id.attractionType)
        val distance: TextView = itemView.findViewById(R.id.attractionDistance)
        val icon: ImageView = itemView.findViewById(R.id.attractionIcon)
    }
} 