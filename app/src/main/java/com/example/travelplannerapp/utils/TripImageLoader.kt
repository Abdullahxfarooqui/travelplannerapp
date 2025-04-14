package com.example.travelplannerapp.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.example.travelplannerapp.R
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
            
            when {
                // Only try to load from URL if it's not null or empty
                !imageUrl.isNullOrEmpty() && imageUrl.trim().isNotEmpty() -> {
                    Log.d(TAG, "Loading URL image for $tripName: $imageUrl")
                    try {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .into(imageView, object : Callback {
                                override fun onSuccess() {
                                    Log.d(TAG, "Successfully loaded image for $tripName")
                                }

                                override fun onError(e: Exception) {
                                    Log.e(TAG, "Failed to load image for $tripName: ${e.message}")
                                    loadLocalImage(imageView, imageResId, tripName)
                                }
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during Picasso setup for $tripName: ${e.message}")
                        loadLocalImage(imageView, imageResId, tripName)
                    }
                }
                // If URL is null, empty, or just whitespace, go straight to local image
                else -> {
                    Log.d(TAG, "No valid URL for $tripName, using local image")
                    loadLocalImage(imageView, imageResId, tripName)
                }
            }
        }

        private fun loadLocalImage(imageView: ImageView, imageResId: Int?, tripName: String) {
            try {
                if (imageResId != null && imageResId != 0) {
                    Log.d(TAG, "Loading local resource image for $tripName: $imageResId")
                    imageView.setImageResource(imageResId)
                } else {
                    Log.d(TAG, "Using placeholder image for $tripName")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load local image for $tripName: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }

        fun getLocalImageResource(tripName: String): Int {
            return when (tripName.toLowerCase()) {
                "neelum valley" -> R.drawable.neelumvalley
                "islamabad tour" -> R.drawable.islamabad
                "hunza valley" -> R.drawable.hunza
                "fairy meadows" -> R.drawable.fairy_meadows
                "naran kaghan" -> R.drawable.naran
                "skardu" -> R.drawable.skardu
                "swat valley" -> R.drawable.swat_valley_tour
                "lahore" -> R.drawable.lahore
                "karachi" -> R.drawable.karachi
                else -> R.drawable.placeholder_image
            }
        }
    }
}