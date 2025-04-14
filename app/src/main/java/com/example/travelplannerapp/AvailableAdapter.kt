package com.example.travelplannerapp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.utils.TripImageLoader

class AvailableAdapter(
    private val context: Context,
    private var availableTrips: List<Trip>
) : RecyclerView.Adapter<AvailableAdapter.AvailableTripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableTripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.available_trip_item, parent, false)
        return AvailableTripViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvailableTripViewHolder, position: Int) {
        val trip = availableTrips[position]
        holder.bind(trip)

        // Add debug logging to track binding
        Log.d("AvailableAdapter", "Binding trip at position $position: ${trip.name}, imageUrl: ${trip.imageUrl}, imageResId: ${trip.imageResId}")

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                // Pass individual trip properties as extras, matching TripAdapter
                putExtra("trip_name", trip.name)
                putExtra("trip_location", trip.location)
                putExtra("trip_description", trip.description)
                putExtra("trip_image_url", trip.imageUrl)
                putExtra("trip_image_res_id", trip.imageResId ?: 0)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        // Add logging to track item count
        Log.d("AvailableAdapter", "getItemCount called, returning ${availableTrips.size} items")
        return availableTrips.size
    }

    fun updateTrips(newTrips: List<Trip>) {
        Log.d("AvailableAdapter", "updateTrips called with ${newTrips.size} trips")
        availableTrips = newTrips
        notifyDataSetChanged()
    }

    class AvailableTripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripImage: ImageView = itemView.findViewById(R.id.tripImageView)
        private val tripName: TextView = itemView.findViewById(R.id.availableTripsTitle)
        private val tripLocation: TextView = itemView.findViewById(R.id.tripLocationTextView)
        private val tripDescription: TextView = itemView.findViewById(R.id.tripDescriptionTextView)

        fun bind(trip: Trip) {
            tripName.text = trip.name
            tripLocation.text = trip.location
            tripDescription.text = trip.description

            // Add logging before image loading
            Log.d("AvailableTripViewHolder", "Binding trip: ${trip.name}, imageUrl: ${trip.imageUrl}, imageResId: ${trip.imageResId}")

            // Use TripImageLoader to handle image loading - same as TripAdapter
            TripImageLoader.loadTripImage(
                itemView.context,
                tripImage,
                trip.imageUrl,
                trip.imageResId,
                trip.name
            )
        }
    }
}
