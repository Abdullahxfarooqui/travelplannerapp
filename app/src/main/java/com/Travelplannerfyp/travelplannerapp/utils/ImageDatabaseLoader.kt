package com.Travelplannerfyp.travelplannerapp.utils

import android.util.Log
import android.widget.ImageView
import com.Travelplannerfyp.travelplannerapp.R
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class ImageDatabaseLoader {
    companion object {
        private const val TAG = "ImageDatabaseLoader"

        fun loadImage(imageView: ImageView, imageUrl: String?) {
            Log.d(TAG, "Loading image with URL: $imageUrl")

            if (imageUrl.isNullOrEmpty()) {
                Log.d(TAG, "Image URL is null or empty, using placeholder")
                imageView.setImageResource(R.drawable.placeholder_image)
                return
            }

            // Try to load from local resources first if URL contains a recognizable place name
            val placeName = extractPlaceNameFromUrl(imageUrl)
            if (placeName != null) {
                val normalizedName = normalizePlaceName(placeName)
                Log.d(TAG, "Found local resource for $placeName (normalized: $normalizedName)")
                val resourceId = getLocalResourceId(normalizedName)
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                    return
                }
            }

            // If no local resource found, try loading from URL
            Log.d(TAG, "Loading regular URL with Picasso: $imageUrl")
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(imageView, object : Callback {
                    override fun onSuccess() {
                        Log.d(TAG, "Successfully loaded image from URL: $imageUrl")
                    }

                    override fun onError(e: Exception?) {
                        Log.e(TAG, "Failed to load image from URL: $imageUrl, Error: ${e?.message}")
                        
                        // Try local fallback
                        tryLocalImageFallback(imageView, imageUrl)
                    }
                })
        }

        private fun tryLocalImageFallback(imageView: ImageView, failedUrl: String) {
            val placeName = extractPlaceNameFromUrl(failedUrl)
            Log.d(TAG, "Extracted place name from URL: $placeName")

            if (placeName != null) {
                val normalizedName = normalizePlaceName(placeName)
                Log.d(TAG, "Loading local image for place: $placeName (normalized: $normalizedName)")
                
                // Try local drawable resource
                val resourceId = getLocalResourceId(normalizedName)
                Log.d(TAG, "Loading local image for place: $placeName, resourceId: $resourceId")
                
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                } else {
                    // If no local resource, try Firebase Storage path
                    val sanitizedPath = sanitizeDatabasePath(placeName)
                    loadImageFromDatabase(imageView, sanitizedPath)
                }
            } else {
                Log.d(TAG, "No place name found in URL, using placeholder")
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }

        private fun sanitizeDatabasePath(path: String): String {
            // Replace invalid characters with underscores
            return path.replace(Regex("[.#$\\[\\]]"), "_")
        }

        private fun loadImageFromDatabase(imageView: ImageView, placePath: String) {
            val sanitizedPath = "places/${sanitizeDatabasePath(placePath)}/image_url"
            Log.d(TAG, "Attempting to load from database path: $sanitizedPath")
            
            FirebaseDatabase.getInstance().reference
                .child(sanitizedPath)
                .get()
                .addOnSuccessListener { snapshot ->
                    val imageUrl = snapshot.getValue(String::class.java)
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d(TAG, "Found image URL in database: $imageUrl")
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .into(imageView)
                    } else {
                        Log.d(TAG, "No image URL found in database, using placeholder")
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load image from database: ${e.message}")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
        }

        private fun extractPlaceNameFromUrl(url: String): String? {
            // Extract filename without extension
            val regex = ".*/([^/]+)\\.[^.]+$".toRegex()
            return regex.find(url)?.groupValues?.get(1)
        }

        private fun normalizePlaceName(name: String): String {
            return name.lowercase().replace(Regex("[^a-z0-9]+"), "_")
        }

        private fun getLocalResourceId(normalizedName: String): Int {
            return when (normalizedName) {
                "hunza_valley", "hunza" -> R.drawable.hunza
                "naran_kaghan", "naran" -> R.drawable.naran
                "swat_valley", "swat" -> R.drawable.swat_valley_view
                "murree", "murree_hills" -> R.drawable.murree
                "skardu" -> R.drawable.skardu
                "fairy_meadows" -> R.drawable.fairy_meadows
                "lahore" -> R.drawable.lahore
                "karimabad" -> R.drawable.karimabad
                "ratti_gali_lake" -> R.drawable.rattigali
                "ziarat" -> R.drawable.ziarat
                "islamabad" -> R.drawable.islamabad
                "balakot" -> R.drawable.balakot
                "attabad_lake" -> R.drawable.attabad
                "chitral" -> R.drawable.chitral
                "khunjerab_pass" -> R.drawable.khunjerab
                "gilgit" -> R.drawable.gilgit
                "kotli" -> R.drawable.kotli
                "neelum_valley" -> R.drawable.neelumvalley
                else -> 0
            }
        }
    }
}