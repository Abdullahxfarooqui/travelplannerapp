package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.Announcement
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AnnouncementAdapter(
    private var announcements: List<Announcement>,
    private val onDelete: (Announcement) -> Unit
) : RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_announcement, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]
        holder.title.text = announcement.title
        holder.message.text = announcement.message
        holder.date.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(announcement.createdAt))
        holder.deleteButton.setOnClickListener { onDelete(announcement) }
    }

    override fun getItemCount(): Int = announcements.size

    fun updateData(newAnnouncements: List<Announcement>) {
        announcements = newAnnouncements
        notifyDataSetChanged()
    }

    class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.announcementTitle)
        val message: TextView = itemView.findViewById(R.id.announcementMessage)
        val date: TextView = itemView.findViewById(R.id.announcementDate)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteAnnouncementButton)
    }
} 