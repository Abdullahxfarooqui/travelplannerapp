package com.example.travelplannerapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class PropertyPhotoAdapter(
    private val photoUris: MutableList<Uri> = mutableListOf(),
    private val onPhotoRemoved: (Int) -> Unit
) : RecyclerView.Adapter<PropertyPhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.propertyImageView)
        val removeButton: ImageButton = itemView.findViewById(R.id.removePhotoButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = photoUris[position]

        Picasso.get()
            .load(uri)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .fit()
            .centerCrop()
            .into(holder.imageView)

        holder.removeButton.setOnClickListener {
            onPhotoRemoved(position)
        }
    }

    override fun getItemCount(): Int = photoUris.size

    fun addPhoto(uri: Uri) {
        photoUris.add(uri)
        notifyItemInserted(photoUris.size - 1)
    }

    fun removePhoto(position: Int) {
        if (position in 0 until photoUris.size) {
            photoUris.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photoUris.size)
        }
    }

    fun getPhotoUris(): List<Uri> = photoUris.toList()

    fun clearPhotos() {
        photoUris.clear()
        notifyDataSetChanged()
    }
}
