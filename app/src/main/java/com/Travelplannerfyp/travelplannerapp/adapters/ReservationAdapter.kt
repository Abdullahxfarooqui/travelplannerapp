package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.Booking

class ReservationAdapter(private val items: List<Booking>) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {
    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        val bookingTypeTextView: TextView = itemView.findViewById(R.id.bookingTypeTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val confirmationCodeTextView: TextView = itemView.findViewById(R.id.confirmationCodeTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        val item = items[position]
        holder.itemNameTextView.text = if (item.tripName.isNotEmpty()) item.tripName else item.itemName
        holder.bookingTypeTextView.text = if (item.bookingType == "TRIP") "Trip" else "Property"
        holder.statusTextView.text = item.status.replaceFirstChar { it.uppercase() }
        holder.confirmationCodeTextView.text = "Code: ${item.confirmationCode}"
        holder.dateTextView.text = "${item.startDate} - ${item.endDate}"
        holder.amountTextView.text = "$${String.format("%,.2f", item.totalAmount)}"
    }

    override fun getItemCount(): Int = items.size
} 