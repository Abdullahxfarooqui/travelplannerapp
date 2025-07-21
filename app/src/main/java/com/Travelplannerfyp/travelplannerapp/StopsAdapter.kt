package com.Travelplannerfyp.travelplannerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.models.Stop
import android.widget.ImageView
import com.squareup.picasso.Picasso

class StopsAdapter(
    private val stops: MutableList<Stop>,
    private val onEdit: (Int, Stop) -> Unit,
    private val onRemove: (Int, Stop) -> Unit
) : RecyclerView.Adapter<StopsAdapter.StopViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stop, parent, false)
        return StopViewHolder(view)
    }
    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stops[position]
        holder.stopName.text = stop.stopName
        holder.arrivalTime.text = "Arrival: ${stop.arrivalTime}"
        holder.durationMinutes.text = "Duration: ${stop.durationMinutes} min"
        holder.attractions.text = "Attractions: ${stop.attractions.joinToString(", ")}"
        // Load stop image
        val stopImageView = holder.itemView.findViewById<ImageView>(R.id.stopImage)
        if (!stop.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(stop.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .fit()
                .centerCrop()
                .into(stopImageView)
        } else {
            stopImageView.setImageResource(R.drawable.placeholder_image)
        }
        holder.itemView.setOnClickListener { onEdit(position, stop) }
        holder.itemView.setOnLongClickListener { onRemove(position, stop); true }
    }
    override fun getItemCount() = stops.size
    class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stopName: TextView = itemView.findViewById(R.id.stopName)
        val arrivalTime: TextView = itemView.findViewById(R.id.arrivalTime)
        val durationMinutes: TextView = itemView.findViewById(R.id.durationMinutes)
        val attractions: TextView = itemView.findViewById(R.id.attractions)
    }
} 