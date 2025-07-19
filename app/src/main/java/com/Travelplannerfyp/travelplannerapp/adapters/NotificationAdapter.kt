package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.model.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private var notifications: List<Notification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.body.text = notification.body
        holder.audience.text = "Audience: ${notification.audience}"
        val date = if (notification.timestamp > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(notification.timestamp)) else "Unknown"
        holder.timestamp.text = "Sent: $date"
        if (notification.status.isNotEmpty()) {
            holder.status.text = notification.status.capitalize()
            holder.status.visibility = View.VISIBLE
        } else {
            holder.status.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.notificationTitleTextView)
        val body: TextView = itemView.findViewById(R.id.notificationBodyTextView)
        val audience: TextView = itemView.findViewById(R.id.notificationAudienceTextView)
        val timestamp: TextView = itemView.findViewById(R.id.notificationTimestampTextView)
        val status: TextView = itemView.findViewById(R.id.notificationStatusTextView)
    }
} 