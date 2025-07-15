package com.Travelplannerfyp.travelplannerapp.utils

import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.Travelplannerfyp.travelplannerapp.R

object HotelImageLoader {
    fun loadHotelImage(imageUrl: String?, imageName: String?, hotelName: String?, imageView: ImageView) {
        val context = imageView.context
        when {
            !imageUrl.isNullOrBlank() && imageUrl.startsWith("http") -> {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .fit()
                    .centerCrop()
                    .into(imageView)
            }
            !imageName.isNullOrBlank() -> {
                // Normalize imageName: strip extension, lowercase, replace spaces/special chars with _
                val normalizedImageName = imageName.substringBeforeLast(".").lowercase().replace("[^a-z0-9]+".toRegex(), "_").trim('_')
                val resourceId = context.resources.getIdentifier(
                    normalizedImageName,
                    "drawable",
                    context.packageName
                )
                android.util.Log.d("HotelImageLoader", "Trying imageName: $imageName, normalized: $normalizedImageName, resourceId: $resourceId")
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder)
                }
            }
            !hotelName.isNullOrBlank() -> {
                // Normalize hotelName: lowercase, replace spaces/special chars with _
                val normalizedHotelName = hotelName.lowercase().replace("[^a-z0-9]+".toRegex(), "_").trim('_')
                val resourceId = context.resources.getIdentifier(
                    normalizedHotelName,
                    "drawable",
                    context.packageName
                )
                android.util.Log.d("HotelImageLoader", "Trying hotelName: $hotelName, normalized: $normalizedHotelName, resourceId: $resourceId")
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder)
                }
            }
            else -> {
                imageView.setImageResource(R.drawable.ic_image_placeholder)
            }
        }
    }
} 