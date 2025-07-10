package com.Travelplannerfyp.travelplannerapp.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.Travelplannerfyp.travelplannerapp.R
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class TripImageLoader {
    companion object {
        private const val TAG = "TripImageLoader"

        fun loadTripImage(
            context: Context,
            imageView: ImageView,
            imageUrl: String?,
            imageResId: Int?,
            tripName: String
        ) {
            Log.d(TAG, "Starting to load image for $tripName - URL: $imageUrl, ResID: $imageResId")
            
            // First try to load from drawable resources
            val resourceId = imageResId ?: findDrawableResource(context, tripName)
            if (resourceId != R.drawable.placeholder_image) {
                Log.d(TAG, "Found drawable resource for $tripName: $resourceId")
                try {
                    imageView.setImageResource(resourceId)
                    Log.d(TAG, "Successfully loaded drawable resource for $tripName")
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load drawable resource for $tripName: ${e.message}")
                }
            }

            // If drawable resource not found or failed to load, try URL
            when {
                !imageUrl.isNullOrEmpty() && imageUrl.trim().isNotEmpty() -> {
                    Log.d(TAG, "Loading URL image for $tripName: $imageUrl")
                    try {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .into(imageView, object : Callback {
                                override fun onSuccess() {
                                    Log.d(TAG, "Successfully loaded image from URL for $tripName")
                                }

                                override fun onError(e: Exception) {
                                    Log.e(TAG, "Failed to load image from URL for $tripName: ${e.message}")
                                    imageView.setImageResource(R.drawable.placeholder_image)
                                }
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during Picasso setup for $tripName: ${e.message}")
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                }
                else -> {
                    Log.d(TAG, "No valid URL for $tripName, using placeholder image")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            }
        }

        private fun findDrawableResource(context: Context, tripName: String): Int {
            val normalizedName = tripName.trim().toLowerCase()
            
            // Direct matches
            val directMatch = when (normalizedName) {
                "neelum valley" -> R.drawable.neelumvalley
                "islamabad tour", "islamabad" -> R.drawable.islamabad
                "hunza valley", "hunza" -> R.drawable.hunza
                "fairy meadows" -> R.drawable.fairy_meadows
                "naran kaghan", "naran", "kaghan" -> R.drawable.naran
                "skardu" -> R.drawable.skardu
                "swat valley", "swat" -> R.drawable.swat_valley_tour
                "lahore" -> R.drawable.lahore
                "karachi" -> R.drawable.karachi
                else -> null
            }
            if (directMatch != null) return directMatch

            // Try different name variations
            val variations = listOf(
                normalizedName,
                normalizedName.replace(" ", ""),
                normalizedName.replace(" ", "_"),
                normalizedName.replace(" ", "-"),
                if (normalizedName.contains("valley")) normalizedName.replace("valley", "").trim() else null,
                if (normalizedName.contains("tour")) normalizedName.replace("tour", "").trim() else null
            ).filterNotNull()

            // Try to find resource ID for each variation
            for (variation in variations) {
                try {
                    val resId = context.resources.getIdentifier(
                        variation,
                        "drawable",
                        context.packageName
                    )
                    if (resId != 0) {
                        Log.d(TAG, "Found resource for variation '$variation' of '$tripName'")
                        return resId
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error finding resource for variation '$variation': ${e.message}")
                }
            }

            Log.d(TAG, "No drawable resource found for $tripName, using placeholder")
            return R.drawable.placeholder_image
        }
    }
}