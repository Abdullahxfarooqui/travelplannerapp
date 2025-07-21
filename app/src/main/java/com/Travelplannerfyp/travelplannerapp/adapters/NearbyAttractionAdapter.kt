package com.Travelplannerfyp.travelplannerapp.adapters

import android.util.Log
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

    private val TAG = "NearbyAttractionAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttractionViewHolder {
        Log.d(TAG, "Creating view holder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nearby_attraction, parent, false)
        
        // If parent is horizontal RecyclerView, adjust item width
        if (parent is RecyclerView && parent.layoutManager?.canScrollHorizontally() == true) {
            val width = (parent.width * 0.8).toInt() // 80% of parent width
            view.layoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        
        return AttractionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttractionViewHolder, position: Int) {
        val place = attractions[position]
        Log.d(TAG, "Binding view holder for ${place.name} at position $position")
        
        holder.name.text = place.name
        holder.type.text = place.type
        holder.distance.text = if (place.distance >= 1000) {
            String.format("%.2f km", place.distance / 1000)
        } else {
            String.format("%.0f m", place.distance)
        }
        holder.address.text = place.address
        
        holder.itemView.setOnClickListener { 
            Log.d(TAG, "Item clicked: ${place.name}")
            onItemClick(place, position) 
        }
        
        holder.directionsButton.setOnClickListener { 
            Log.d(TAG, "Directions clicked for: ${place.name}")
            onDirectionsClick(place, position) 
        }
    }

    override fun getItemCount(): Int = attractions.size

    fun updateData(newAttractions: List<Place>) {
        Log.d(TAG, "Updating data with ${newAttractions.size} attractions")
        attractions = newAttractions
        notifyDataSetChanged()
    }

    inner class AttractionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById<TextView>(R.id.attractionNameTextView).apply {
            setOnClickListener { itemView.performClick() }
        }
        val type: TextView = itemView.findViewById(R.id.attractionTypeTextView)
        val distance: TextView = itemView.findViewById(R.id.attractionDistanceTextView)
        val address: TextView = itemView.findViewById(R.id.attractionAddressTextView)
        val directionsButton: View = itemView.findViewById(R.id.getDirectionsButton)
    }
} 