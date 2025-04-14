package com.example.travelplannerapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class PropertyAdapter(private val context: Context, private var properties: List<PropertyListing> = listOf()) : 
    RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ShapeableImageView = itemView.findViewById(R.id.propertyImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.propertyTitleTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.propertyLocationTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.propertyPriceTextView)
        val guestsTextView: TextView = itemView.findViewById(R.id.propertyGuestsTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        
        holder.titleTextView.text = property.title
        holder.locationTextView.text = property.location
        holder.priceTextView.text = "$${property.pricePerNight} / night"
        holder.guestsTextView.text = "${property.maxGuests} guests"
        
        // Load the first image if available, otherwise use placeholder
        if (property.imageUrls.isNotEmpty()) {
            Picasso.get()
                .load(property.imageUrls[0])
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .fit()
                .centerCrop()
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image)
        }
        
        // Set click listener to open property details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PropertyDetailActivity::class.java).apply {
                putExtra("PROPERTY_ID", property.id)
                putExtra("PROPERTY", property)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = properties.size

    fun updateProperties(newProperties: List<PropertyListing>) {
        properties = newProperties
        notifyDataSetChanged()
    }
}