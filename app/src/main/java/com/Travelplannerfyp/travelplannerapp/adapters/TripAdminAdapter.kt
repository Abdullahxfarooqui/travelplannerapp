package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.model.TripAdmin
import android.util.Log

class TripAdminAdapter(
    private var trips: List<TripAdmin>,
    private val onDeleteClick: (TripAdmin) -> Unit,
    private val onFeatureClick: (TripAdmin) -> Unit
) : RecyclerView.Adapter<TripAdminAdapter.TripViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    private fun getSeatsAvailable(trip: TripAdmin): Int {
        return when (trip.seatsAvailable) {
            is Number -> (trip.seatsAvailable as Number).toInt()
            is String -> (trip.seatsAvailable as String).toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun getPrice(trip: TripAdmin): String {
        val p = trip.price
        val d = p.toDoubleOrNull()
        return if (!p.isNullOrEmpty() && d != null && d > 0.0) {
            "Rs. %,.0f".format(d)
        } else {
            "Price: Not set"
        }
    }

    // Add an interface for edit callback
    interface OnEditTripListener {
        fun onEditTrip(trip: TripAdmin)
    }

    var onEditTripListener: OnEditTripListener? = null

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]
        holder.title.text = trip.title.ifEmpty { "No Title" }
        holder.organizer.text = "Organizer: ${trip.organizerName}"
        holder.price.text = if (trip.price.isNotEmpty() && trip.price != "0") "Rs. ${trip.price}" else "Price Not Set"
        holder.status.text = "Status: ${trip.status}"
        holder.seatsAvailable.text = "Seats: ${getSeatsAvailable(trip)}"
        Log.d("TripDebug", "Trip Title: ${trip.title}, Price: ${trip.price}")
        holder.btnDelete.setOnClickListener { onDeleteClick(trip) }
        holder.btnFeature.setOnClickListener { onFeatureClick(trip) }
        holder.btnEdit?.setOnClickListener { onEditTripListener?.onEditTrip(trip) }
    }

    override fun getItemCount(): Int = trips.size

    fun updateData(newTrips: List<TripAdmin>) {
        trips = newTrips
        notifyDataSetChanged()
    }

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tripTitleTextView)
        val organizer: TextView = itemView.findViewById(R.id.tripOrganizer)
        val price: TextView = itemView.findViewById(R.id.tripPriceTextView)
        val status: TextView = itemView.findViewById(R.id.tripStatus)
        val seatsAvailable: TextView = itemView.findViewById(R.id.seatsAvailableTextView)
        val btnDelete: View = itemView.findViewById(R.id.btnDeleteTrip)
        val btnFeature: View = itemView.findViewById(R.id.btnFeatureTrip)
        val btnEdit: View? = itemView.findViewById(R.id.btnEditTrip)
    }
} 