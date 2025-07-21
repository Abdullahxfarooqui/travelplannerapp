package com.Travelplannerfyp.travelplannerapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.Travelplannerfyp.travelplannerapp.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

/**
 * Utility class for loading images from Firebase Realtime Database
 * Handles loading images from various sources: URLs, Firebase paths, and Base64 encoded data
 */
class ImageDatabaseLoader {
    companion object {
        private const val TAG = "ImageDatabaseLoader"
        private const val DB_PREFIX = "db://"

        /**
         * Upload a local drawable image to Firebase Database
         * @param imageView The ImageView to display the image in after upload
         * @param placeName The name of the place to get the local resource
         * @param dbPath The database path to store the image
         */
        private fun uploadLocalImageToDatabase(imageView: ImageView, placeName: String, dbPath: String) {
            try {
                val resourceId = getResourceIdForPlace(normalizePlaceName(placeName))
                if (resourceId == R.drawable.placeholder_image) {
                    Log.e(TAG, "No local image found for place: $placeName")
                    imageView.setImageResource(resourceId)
                    return
                }

                // Load the drawable as a bitmap
                val bitmap = BitmapFactory.decodeResource(imageView.context.resources, resourceId)
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode local image for place: $placeName")
                    imageView.setImageResource(R.drawable.placeholder_image)
                    return
                }

                // Convert bitmap to Base64 with optimized quality
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Image = "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                // Upload to Firebase
                val database = FirebaseDatabase.getInstance().reference
                database.child(dbPath).setValue(base64Image)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully uploaded image to database for place: $placeName")
                        // Set the image to the ImageView
                        imageView.setImageBitmap(bitmap)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to upload image to database: ${e.message}")
                        imageView.setImageBitmap(bitmap)
                    }

                bitmap.recycle()
                outputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading local image to database: ${e.message}")
                loadLocalImageByName(imageView, placeName)
            }
        }

        /**
         * Load an image from a URL or Firebase Realtime Database
         * @param imageView The ImageView to load the image into
         * @param imageUrl The image URL or database reference (format: "db://path/to/image")
         */
        fun loadImage(imageView: ImageView, imageUrl: String?) {
            Log.d(TAG, "Loading image with URL: $imageUrl")

            if (imageUrl.isNullOrEmpty()) {
                Log.d(TAG, "Image URL is null or empty, using placeholder")
                imageView.setImageResource(R.drawable.placeholder_image)
                return
            }

            // Set placeholder immediately while loading
            imageView.setImageResource(R.drawable.placeholder_image)

            // Try to load from local resources first if URL contains a recognizable place name
            val placeName = extractPlaceNameFromUrl(imageUrl)
            if (placeName != null) {
                val normalizedName = normalizePlaceName(placeName)
                val resourceId = imageView.context.resources.getIdentifier(
                    normalizedName,
                    "drawable",
                    imageView.context.packageName
                )
                if (resourceId != 0) {
                    Log.d(TAG, "Found local resource for $placeName (normalized: $normalizedName)")
                    imageView.setImageResource(resourceId)
                    return
                }
            }

            when {
                // Check if this is a database reference
                imageUrl.startsWith(DB_PREFIX) -> {
                    val dbPath = imageUrl.substring(DB_PREFIX.length)
                    Log.d(TAG, "Detected database URL, extracted path: $dbPath")
                    loadImageFromDatabase(imageView, dbPath)
                }

                // Handle direct Base64 data
                imageUrl.startsWith("data:image") -> {
                    try {
                        val base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            Log.e(TAG, "Failed to decode Base64 image data")
                            tryLocalImageFallback(imageView, imageUrl)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing Base64 image data: ${e.message}")
                        tryLocalImageFallback(imageView, imageUrl)
                    }
                }

                // Regular URL - validate it's a proper HTTP/HTTPS URL before using Picasso
                imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                    Log.d(TAG, "Loading regular URL with Picasso: $imageUrl")
                    try {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .into(imageView, object : Callback {
                                override fun onSuccess() {
                                    Log.d(TAG, "Successfully loaded image from URL: $imageUrl")
                                }

                                override fun onError(e: Exception) {
                                    Log.e(TAG, "Failed to load image from URL: $imageUrl, Error: ${e.message}")
                                    tryLocalImageFallback(imageView, imageUrl)
                                }
                            })
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during Picasso setup: ${e.message}")
                        tryLocalImageFallback(imageView, imageUrl)
                    }
                }

                // Unknown URL format - try to handle as database path or fallback
                else -> {
                    Log.w(TAG, "Unknown URL format: $imageUrl, attempting fallback handling")
                    tryLocalImageFallback(imageView, imageUrl)
                }
            }
        }

        private fun handleImageLoadError(imageView: ImageView, imageUrl: String) {
            // Try to extract a place name from the URL for fallback
            val placeName = extractPlaceNameFromUrl(imageUrl)
            if (placeName != null) {
                Log.d(TAG, "Extracted place name from URL: $placeName")
                // Normalize the place name to match drawable resource naming
                val normalizedPlaceName = normalizePlaceName(placeName)
                loadLocalImageByName(imageView, normalizedPlaceName)
                return
            }

            // Try to load from database as fallback
            if (imageUrl.contains("/")) {
                val possibleDbPath = "places/${imageUrl.substringAfterLast('/')}/image"
                Log.d(TAG, "Attempting fallback to database path: $possibleDbPath")
                loadImageFromDatabase(imageView, possibleDbPath)
                return
            }

            // Final fallback to placeholder
            imageView.setImageResource(R.drawable.placeholder_image)
        }

        /**
         * Extract a place name from a URL
         * @param url The URL to extract from
         * @return The extracted place name or null if not found
         */
        private fun extractPlaceNameFromUrl(url: String): String? {
            // Extract the filename without extension
            val fileName = url.substringAfterLast('/').substringBeforeLast('.')
            // Remove common prefixes/suffixes and dimensions
            return fileName.replace(Regex("^(\\d+px-)|(-\\d+x\\d+)$"), "")
        }

        /**
         * Load an image from Firebase Realtime Database
         * @param imageView The ImageView to load the image into
         * @param dbPath The path to the image in the database
         */
        private fun loadImageFromDatabase(imageView: ImageView, dbPath: String) {
            val database = FirebaseDatabase.getInstance().reference
            val imageRef = database.child(dbPath)

            Log.d(TAG, "Loading image from database path: $dbPath")

            // Extract place name and type from the database path
            val pathInfo = when {
                dbPath.contains("places/") -> {
                    val parts = dbPath.split("/")
                    Pair(if (parts.size >= 2) parts[1] else null, "place")
                }
                dbPath.contains("property_images/") -> {
                    val parts = dbPath.split("/")
                    Pair(if (parts.size >= 2) parts[1] else null, "property")
                }
                dbPath.contains("profile_images/") -> {
                    val parts = dbPath.split("/")
                    Pair(if (parts.size >= 2) parts[1] else null, "profile")
                }
                dbPath.contains("users/") -> {
                    val parts = dbPath.split("/")
                    Pair(if (parts.size >= 2) parts[1] else null, "profile")
                }
                else -> Pair(null, "unknown")
            }

            val (id, type) = pathInfo

            imageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d(TAG, "Database snapshot received for path: $dbPath")

                        var imageData: String? = null
                        
                        // For profile images, always check for 'data' child node first
                        if (type == "profile" && snapshot.hasChild("data")) {
                            Log.d(TAG, "Profile image: using 'data' child node")
                            imageData = snapshot.child("data").getValue(String::class.java)
                        }
                        // For property_images, always check for 'data' child node first
                        else if (type == "property" && snapshot.hasChild("data")) {
                            Log.d(TAG, "Property image: using 'data' child node")
                            imageData = snapshot.child("data").getValue(String::class.java)
                        }
                        // If not found, try direct value
                        if (imageData == null) {
                            imageData = snapshot.getValue(String::class.java)
                            Log.d(TAG, "Direct value retrieval result: ${imageData != null}")
                        }
                        // If still not found, try to get it from a child node named "data"
                        if (imageData == null && snapshot.hasChild("data")) {
                            Log.d(TAG, "Trying to get image from 'data' child node (fallback)")
                            imageData = snapshot.child("data").getValue(String::class.java)
                        }
                        // If still not found, try to get it from a child node named "image"
                        if (imageData == null && snapshot.hasChild("image")) {
                            Log.d(TAG, "Trying to get image from 'image' child node")
                            imageData = snapshot.child("image").getValue(String::class.java)
                        }
                        // If still not found, try to get it from various possible child nodes
                        val possibleNodes = listOf("imageUrl", "photoUrl", "profileImage", "image_url", "url", "profileImageUrl", "avatar", "photo")
                        for (nodeName in possibleNodes) {
                            if (imageData == null && snapshot.hasChild(nodeName)) {
                                Log.d(TAG, "Trying to get image from '$nodeName' child node")
                                val urlData = snapshot.child(nodeName).getValue(String::class.java)
                                if (!urlData.isNullOrEmpty()) {
                                    // If it's a URL, load it with Picasso
                                    Log.d(TAG, "Found URL in database, loading with Picasso: $urlData")
                                    Picasso.get()
                                        .load(urlData)
                                        .placeholder(R.drawable.placeholder_image)
                                        .error(R.drawable.placeholder_image)
                                        .into(imageView)
                                    return
                                }
                            }
                        }
                        if (imageData != null) {
                            try {
                                when {
                                    // Handle URLs
                                    imageData.startsWith("http") -> {
                                        Log.d(TAG, "Found URL in database value, loading with Picasso: $imageData")
                                        Picasso.get()
                                            .load(imageData)
                                            .placeholder(when (type) {
                                                "property" -> R.drawable.placeholder_property
                                                "profile" -> R.drawable.ic_profile_placeholder
                                                else -> R.drawable.placeholder_image
                                            })
                                            .error(when (type) {
                                                "property" -> R.drawable.placeholder_property
                                                "profile" -> R.drawable.ic_profile_placeholder
                                                else -> R.drawable.placeholder_image
                                            })
                                            .fit()
                                            .centerCrop()
                                            .into(imageView)
                                    }
                                    // Handle Base64 data
                                    else -> {
                                        Log.d(TAG, "Attempting to decode Base64 data for $type image, length: ${imageData.length}")
                                        try {
                                            // Handle both raw Base64 and data URL formats
                                            val base64Data = if (imageData.startsWith("data:image")) {
                                                imageData.substring(imageData.indexOf(",") + 1)
                                            } else {
                                                imageData
                                            }
                                            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                            val options = BitmapFactory.Options().apply {
                                                inSampleSize = 1
                                            }
                                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)

                                            if (bitmap != null) {
                                                // Set the bitmap to the ImageView
                                                imageView.setImageBitmap(bitmap)
                                                Log.d(TAG, "Successfully loaded $type image from database at $dbPath")
                                            } else {
                                                Log.e(TAG, "Failed to decode bitmap from Base64 data for $type")
                                                setFallbackImage(imageView, type, id)
                                            }
                                        } catch (e: IllegalArgumentException) {
                                            Log.e(TAG, "Error decoding Base64 data for $type: ${e.message}")
                                            setFallbackImage(imageView, type, id)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing image data: ${e.message}")
                                setFallbackImage(imageView, type, id)
                            }
                        } else {
                            Log.e(TAG, "Image data not found at $dbPath")
                            // If we have a place name and the image doesn't exist in database, upload it
                            if (type == "place" && id != null) {
                                uploadLocalImageToDatabase(imageView, id, dbPath)
                            } else {
                                setFallbackImage(imageView, type, id)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading image from database: ${e.message}")
                        tryLocalImageFallback(imageView, dbPath)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                    tryLocalImageFallback(imageView, dbPath)
                }
            })
        }

        /**
         * Try to load a local image resource based on the database path
         * @param imageView The ImageView to load the image into
         * @param dbPath The database path that failed to load
         */
        private fun setFallbackImage(imageView: ImageView, type: String, id: String?) {
            when (type) {
                "property" -> imageView.setImageResource(R.drawable.placeholder_property)
                "profile" -> imageView.setImageResource(R.drawable.ic_profile_placeholder)
                "place" -> {
                    if (id != null) {
                        loadLocalImageByName(imageView, id)
                    } else {
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                }
                else -> imageView.setImageResource(R.drawable.placeholder_image)
            }
        }

        private fun tryLocalImageFallback(imageView: ImageView, imageUrl: String) {
            // Try to extract a place name from the URL for fallback
            val placeName = extractPlaceNameFromUrl(imageUrl)
            if (placeName != null) {
                Log.d(TAG, "Extracted place name from URL: $placeName")
                // Normalize the place name to match drawable resource naming
                val normalizedPlaceName = normalizePlaceName(placeName)
                val resourceId = imageView.context.resources.getIdentifier(
                    normalizedPlaceName,
                    "drawable",
                    imageView.context.packageName
                )
                Log.d(TAG, "Loading local image for place: $placeName (normalized: $normalizedPlaceName), resourceId: $resourceId")
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                    return
                }
            }

            // Try to load from database as fallback
            if (imageUrl.contains("/")) {
                val possibleDbPath = "places/${imageUrl.substringAfterLast('/')}/image"
                Log.d(TAG, "Attempting fallback to database path: $possibleDbPath")
                loadImageFromDatabase(imageView, possibleDbPath)
                return
            }

            // Final fallback to placeholder
            imageView.setImageResource(R.drawable.placeholder_image)
        }

        /**
         * Load a local image resource based on the place name
         * @param imageView The ImageView to load the image into
         * @param placeName The name of the place to load an image for
         */
        fun loadLocalImageByName(imageView: ImageView, placeName: String) {
            try {
                val normalizedName = normalizePlaceName(placeName)
                val resourceId = getResourceIdForPlace(normalizedName)

                Log.d(TAG, "Loading local image for place: $placeName (normalized: $normalizedName), resourceId: $resourceId")
                imageView.setImageResource(resourceId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading local image by name: ${e.message}")
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }

        /**
         * Normalize a place name for resource matching
         * @param placeName The place name to normalize
         * @return The normalized place name
         */
        private fun normalizePlaceName(name: String): String {
            return name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
        }

        /**
         * Get the resource ID for a place name
         * @param normalizedName The normalized place name
         * @return The resource ID for the place
         */
        fun getResourceIdForPlace(normalizedName: String): Int {
            val name = normalizedName.trim()
            return when {
                name.contains("neelum") -> R.drawable.neelumvalley
                name.contains("islamabad") -> R.drawable.islamabad
                name.contains("hunza") -> R.drawable.hunza
                name.contains("fairy") || name.contains("meadows") -> R.drawable.fairy_meadows
                name.contains("naran") || name.contains("kaghan") -> R.drawable.naran
                name.contains("skardu") -> R.drawable.skardu
                name.contains("swat") -> R.drawable.swat_valley_tour
                name.contains("lahore") -> R.drawable.lahore
                name.contains("karachi") -> R.drawable.karachi
                name.contains("murree") -> R.drawable.murree
                name.contains("ziarat") -> R.drawable.ziarat
                name.contains("chitral") -> R.drawable.chitral
                name.contains("khunjerab") -> R.drawable.khunjerab
                name.contains("gilgit") -> R.drawable.giglit
                name.contains("kotli") -> R.drawable.kotli
                name.contains("rattigali") || name.contains("ratti") -> R.drawable.rattigali
                name.contains("attabad") -> R.drawable.attabad
                name.contains("balakot") -> R.drawable.balakot
                name.contains("property") || name.contains("house") || name.contains("rental") -> R.drawable.placeholder_image
                else -> R.drawable.placeholder_image
            }
        }
    }
}