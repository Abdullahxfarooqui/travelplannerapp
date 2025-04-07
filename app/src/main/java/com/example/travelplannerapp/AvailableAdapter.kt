package com.example.travelplannerapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class AvailableAdapter(private val context: Context, private var availableTrips: List<Trip>) :
    RecyclerView.Adapter<AvailableAdapter.AvailableTripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableTripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.available_trip_item, parent, false)
        return AvailableTripViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvailableTripViewHolder, position: Int) {
        val trip = availableTrips[position]
        holder.bind(trip)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("trip_name", trip.name)
                putExtra("trip_location", trip.location)
                putExtra("trip_description", trip.description)
                putExtra("trip_image_url", trip.imageUrl ?: "")
                putExtra("trip_image_res_id", trip.imageResId ?: 0)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = availableTrips.size

    fun updateTrips(newTrips: List<Trip>) {
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

            when {
                !trip.imageUrl.isNullOrEmpty() -> {
                    // Load image from URL using Picasso
                    Picasso.get()
                        .load(trip.imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(tripImage)
                }
                trip.imageResId != null && trip.imageResId != 0 -> {

                    tripImage.setImageResource(trip.imageResId)
                }
                else -> {

                    tripImage.setImageResource(R.drawable.placeholder_image)
                }
            }
        }
    }
}
