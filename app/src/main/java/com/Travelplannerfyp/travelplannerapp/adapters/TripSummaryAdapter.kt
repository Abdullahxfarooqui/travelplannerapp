package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.TripSummary

class TripSummaryAdapter(
    private val tripList: List<TripSummary>,
    private val onTripClick: (TripSummary) -> Unit // <-- Add this line
) : RecyclerView.Adapter<TripSummaryAdapter.TripViewHolder>() {

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripImage: ImageView = itemView.findViewById(R.id.trip_image)
        private val tripName: TextView = itemView.findViewById(R.id.trip_name)
        private val tripDates: TextView = itemView.findViewById(R.id.trip_dates)

        fun bind(trip: TripSummary) {
            tripName.text = trip.placeName
            tripDates.text = "${trip.startDate} - ${trip.endDate}"

            // Assuming imageName is stored as resource name like "paris", "london"
            val resId = itemView.context.resources.getIdentifier(
                trip.placeImageUrl.lowercase(),
                "drawable",
                itemView.context.packageName
            )
            if (resId != 0) {
                tripImage.setImageResource(resId)
            } else {
                tripImage.setImageResource(R.drawable.ic_placeholder) // fallback
            }

            itemView.setOnClickListener {
                onTripClick(trip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.trips_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripList[position])
    }

    override fun getItemCount(): Int = tripList.size
}