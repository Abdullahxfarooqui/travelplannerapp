package com.example.travelplannerapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class PropertyAdapter(private val context: Context, private var properties: List<PropertyListing> = listOf()) : 
    RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ShapeableImageView = itemView.findViewById(R.id.propertyImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.propertyTitleTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.propertyLocationTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.propertyPriceTextView)
        val guestsTextView: TextView = itemView.findViewById(R.id.propertyGuestsTextView)
        val ownerBadge: TextView = itemView.findViewById(R.id.ownerBadgeTextView)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deletePropertyButton)
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
        
        // Show owner badge if this is the user's own property
        if (property.isOwnProperty) {
            holder.ownerBadge.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.VISIBLE
            
            // Set up delete button click listener
            holder.deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(property)
            }
        } else {
            holder.ownerBadge.visibility = View.GONE
            holder.deleteButton.visibility = View.GONE
        }
        
        // Load the first image if available, otherwise use placeholder
        if (property.imageUrls.isNotEmpty()) {
            val imageUrl = property.imageUrls[0]
            // Check if this is a database reference URL
            if (imageUrl.startsWith("db://")) {
                // Use our custom database image loader
                com.example.travelplannerapp.utils.ImageDatabaseLoader.loadImage(holder.imageView, imageUrl)
            } else {
                // Regular URL, use Picasso
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fit()
                    .centerCrop()
                    .into(holder.imageView)
            }
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
    
    private fun showDeleteConfirmationDialog(property: PropertyListing) {
        AlertDialog.Builder(context)
            .setTitle("Delete Property")
            .setMessage("Are you sure you want to delete '${property.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteProperty(property)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteProperty(property: PropertyListing) {
        val database = FirebaseDatabase.getInstance().reference
        
        // Delete the property from the database
        database.child("properties").child(property.id).removeValue()
            .addOnSuccessListener {
                // Also delete associated images if they exist
                if (property.imageUrls.isNotEmpty()) {
                    // Delete images from the database
                    database.child("property_images").child(property.id).removeValue()
                }
                
                Toast.makeText(context, "Property deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete property: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}