package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser
import java.text.SimpleDateFormat
import java.util.*

class EnrolledUserAdapter(private val users: List<EnrolledUser>) :
    RecyclerView.Adapter<EnrolledUserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.enrolled_user_name)
        val userEmail: TextView = itemView.findViewById(R.id.enrolled_user_email)
        val userPhone: TextView = itemView.findViewById(R.id.enrolled_user_phone)
        val userSeats: TextView = itemView.findViewById(R.id.enrolled_user_seats)
        val userBookingTime: TextView = itemView.findViewById(R.id.enrolled_user_booking_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_enrolled_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.name
        holder.userEmail.text = user.email
        holder.userPhone.text = "Phone: ${user.phone}"
        holder.userSeats.text = "Seats: ${user.seats}"
        holder.userBookingTime.text = "Booked: ${formatDate(user.bookingTime)}"
    }

    override fun getItemCount(): Int = users.size

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "-"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}