package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.Place

class NearbyAttractionAdapter(
    private var attractions: List<Place>,
    private val onItemClick: (Place, Int) -> Unit,
    private val onDirectionsClick: (Place, Int) -> Unit
) : RecyclerView.Adapter<NearbyAttractionAdapter.AttractionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttractionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nearby_attraction, parent, false)
        return AttractionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttractionViewHolder, position: Int) {
        val place = attractions[position]
        holder.name.text = place.name
        holder.type.text = place.type
        holder.distance.text = if (place.distance >= 1000) {
            String.format("%.2f km", place.distance / 1000)
        } else {
            String.format("%.0f m", place.distance)
        }
        holder.address.text = place.address
        holder.itemView.setOnClickListener { onItemClick(place, position) }
        holder.directionsButton.setOnClickListener { onDirectionsClick(place, position) }
    }

    override fun getItemCount(): Int = attractions.size

    fun updateData(newAttractions: List<Place>) {
        attractions = newAttractions
        notifyDataSetChanged()
    }

    inner class AttractionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.attractionNameTextView)
        val type: TextView = itemView.findViewById(R.id.attractionTypeTextView)
        val distance: TextView = itemView.findViewById(R.id.attractionDistanceTextView)
        val address: TextView = itemView.findViewById(R.id.attractionAddressTextView)
        val directionsButton: View = itemView.findViewById(R.id.getDirectionsButton)
    }
} 