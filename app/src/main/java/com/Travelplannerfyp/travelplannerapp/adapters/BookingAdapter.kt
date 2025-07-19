package com.Travelplannerfyp.travelplannerapp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.google.android.material.button.MaterialButton

class BookingAdapter(
    private var bookings: List<Booking>,
    private val updateBookingStatus: (String, String) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    companion object {
        private const val TAG = "BookingAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        
        // Log the booking data for debugging
        Log.d(TAG, "Binding booking: ID=${booking.id}, Type=${booking.bookingType}, Amount=${booking.totalAmount}")
        
        // Bind item name (trip name or item name)
        val itemName = if (booking.tripName.isNotEmpty()) booking.tripName else booking.itemName
        holder.itemNameTextView.text = itemName.ifEmpty { "Unnamed Item" }
        
        // Bind user name
        holder.userNameTextView.text = booking.userName.ifEmpty { "Unknown User" }
        
        // Bind status with proper formatting
        holder.statusTextView.text = booking.status.replaceFirstChar { it.uppercase() }
        
        // Bind booking type
        val bookingType = when (booking.bookingType.uppercase()) {
            "TRIP" -> "Trip"
            "PROPERTY" -> "Property"
            "HOTEL" -> "Hotel"
            else -> booking.bookingType.ifEmpty { "Unknown" }
        }
        holder.bookingTypeTextView.text = bookingType
        
        // Bind confirmation code
        val confirmationCode = if (booking.confirmationCode.isNotEmpty()) {
            "Code: ${booking.confirmationCode}"
        } else {
            "Code: Not Set"
        }
        holder.confirmationCodeTextView.text = confirmationCode
        
        // Bind date range
        val dateRange = if (booking.startDate.isNotEmpty() && booking.endDate.isNotEmpty()) {
            "${booking.startDate} - ${booking.endDate}"
        } else {
            "Dates: Not Set"
        }
        holder.dateTextView.text = dateRange
        
        // Bind amount with proper formatting
        val formattedAmount = if (booking.totalAmount > 0) {
            "PKR ${String.format("%,.0f", booking.totalAmount)}"
        } else {
            "Price: Not Set"
        }
        holder.amountTextView.text = formattedAmount
        
        // Update UI based on status
        when (booking.status.lowercase()) {
            "approved", "confirmed" -> {
                holder.approveButton.isEnabled = false
                holder.rejectButton.isEnabled = false
                holder.approveButton.text = "Approved"
                holder.rejectButton.text = "Reject"
            }
            "rejected", "cancelled" -> {
                holder.approveButton.isEnabled = false
                holder.rejectButton.isEnabled = false
                holder.approveButton.text = "Approve"
                holder.rejectButton.text = "Rejected"
            }
            "pending" -> {
                holder.approveButton.isEnabled = true
                holder.rejectButton.isEnabled = true
                holder.approveButton.text = "Approve"
                holder.rejectButton.text = "Reject"
            }
            else -> {
                holder.approveButton.isEnabled = true
                holder.rejectButton.isEnabled = true
                holder.approveButton.text = "Approve"
                holder.rejectButton.text = "Reject"
            }
        }

        holder.approveButton.setOnClickListener {
            Log.d(TAG, "Approving booking: ${booking.id}")
            updateBookingStatus(booking.id, "approved")
            booking.status = "approved"
            notifyItemChanged(position)
        }

        holder.rejectButton.setOnClickListener {
            Log.d(TAG, "Rejecting booking: ${booking.id}")
            updateBookingStatus(booking.id, "rejected")
            booking.status = "rejected"
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings
        Log.d(TAG, "Updated bookings data: ${bookings.size} bookings")
        notifyDataSetChanged()
    }

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val bookingTypeTextView: TextView = itemView.findViewById(R.id.bookingTypeTextView)
        val confirmationCodeTextView: TextView = itemView.findViewById(R.id.confirmationCodeTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        val approveButton: MaterialButton = itemView.findViewById(R.id.approveBookingButton)
        val rejectButton: MaterialButton = itemView.findViewById(R.id.rejectBookingButton)
    }
} 