package com.Travelplannerfyp.travelplannerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.Travelplannerfyp.travelplannerapp.R
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.Travelplannerfyp.travelplannerapp.utils.ImageDatabaseLoader

class PropertyImageAdapter(private val imageUrls: List<String>) : 
    RecyclerView.Adapter<PropertyImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.propertySlideImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_slide, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        
        // Check if this is a database reference URL
        if (imageUrl.startsWith("db://")) {
            // Use our custom database image loader
            ImageDatabaseLoader.loadImage(holder.imageView, imageUrl)
        } else {
            // Regular URL, use Glide
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
} 