package com.Travelplannerfyp.travelplannerapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.Travelplannerfyp.travelplannerapp.R

class ImageAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    
    private val imageUris = mutableListOf<Uri>()
    
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
        
        init {
            removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick(position)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_thumbnail, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = imageUris[position]
        
        // Load image using Glide with proper options
        val requestOptions = RequestOptions()
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
        
        Glide.with(holder.itemView.context)
            .load(uri)
            .apply(requestOptions)
            .into(holder.imageView)
    }
    
    override fun getItemCount(): Int = imageUris.size
    
    fun addImage(uri: Uri) {
        imageUris.add(uri)
        notifyItemInserted(imageUris.size - 1)
    }
    
    fun removeImage(position: Int) {
        if (position in 0 until imageUris.size) {
            imageUris.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    fun getImageUris(): List<Uri> = imageUris.toList()
    
    fun clearImages() {
        val size = imageUris.size
        imageUris.clear()
        notifyItemRangeRemoved(0, size)
    }
    
    fun setImages(uris: List<Uri>) {
        imageUris.clear()
        imageUris.addAll(uris)
        notifyDataSetChanged()
    }
}