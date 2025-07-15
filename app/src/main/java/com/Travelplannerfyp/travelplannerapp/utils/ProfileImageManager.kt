package com.Travelplannerfyp.travelplannerapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

/**
 * Utility class for managing profile images in Firebase Realtime Database
 * Handles saving and loading profile images as Base64 encoded strings
 */
class ProfileImageManager {
    companion object {
        private const val TAG = "ProfileImageManager"
        private const val DB_PREFIX = "db://"
        private const val MAX_IMAGE_SIZE = 500 // Max width/height in pixels
        private const val QUALITY = 80 // JPEG compression quality
        
        /**
         * Save a profile image to Firebase Realtime Database
         * @param context The context
         * @param imageUri The URI of the image to save
         * @param userId The user ID to associate with the image
         * @param onSuccess Callback when image is successfully saved, provides the database reference URL
         * @param onFailure Callback when image saving fails
         */
        fun saveProfileImageToDatabase(
            context: Context,
            imageUri: Uri,
            userId: String,
            onSuccess: (String) -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            try {
                // Show a loading message
                Toast.makeText(context, "Processing image...", Toast.LENGTH_SHORT).show()
                
                // Get input stream from URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    onFailure(Exception("Failed to read the selected image"))
                    return
                }
                
                // Process in background thread
                Thread {
                    try {
                        // Decode the image
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (originalBitmap == null) {
                            onFailure(Exception("Failed to decode image"))
                            return@Thread
                        }
                        
                        // Resize the image to reduce storage size
                        val resizedBitmap = resizeImage(originalBitmap)
                        
                        // Convert to byte array
                        val outputStream = ByteArrayOutputStream()
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream)
                        val imageData = outputStream.toByteArray()
                        
                        // Check size
                        if (imageData.size > 1 * 1024 * 1024) { // 1MB limit for base64 encoded images
                            onFailure(Exception("Image is too large. Please select a smaller image."))
                            return@Thread
                        }
                        
                        // Convert to Base64
                        val base64Image = Base64.encodeToString(imageData, Base64.DEFAULT)
                        
                        // Save to Firebase Realtime Database
                        val database = FirebaseDatabase.getInstance().reference
                        val profileImageRef = database.child("profile_images").child(userId)
                        
                        Log.d(TAG, "Saving profile image to database path: profile_images/$userId")
                        Log.d(TAG, "Image data size: ${base64Image.length} characters")
                        
                        val imageInfo = hashMapOf(
                            "data" to base64Image,
                            "timestamp" to System.currentTimeMillis(),
                            "userId" to userId
                        )
                        
                        profileImageRef.setValue(imageInfo)
                            .addOnSuccessListener {
                                Log.d(TAG, "Profile image saved to database successfully")
                                // Create a database reference URL
                                val dbUrl = "$DB_PREFIX/profile_images/$userId"
                                Log.d(TAG, "Created database URL: $dbUrl")
                                
                                // Update user profile with the reference
                                val userRef = database.child("users").child(userId)
                                val updates = hashMapOf<String, Any>(
                                    "profileImageUrl" to dbUrl,
                                    "lastUpdated" to System.currentTimeMillis()
                                )
                                
                                Log.d(TAG, "Updating user profile with image URL")
                                userRef.updateChildren(updates)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "User profile updated successfully with image URL")
                                        onSuccess(dbUrl)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to update user profile with image reference", e)
                                        onFailure(e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to save image to database", e)
                                onFailure(e)
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image", e)
                        onFailure(e)
                    }
                }.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing image", e)
                onFailure(e)
            }
        }
        
        /**
         * Load a profile image into an ImageView
         * @param imageView The ImageView to load the image into
         * @param imageUrl The image URL or database reference
         */
        fun loadProfileImage(imageView: ImageView, imageUrl: String?) {
            // Use the existing ImageDatabaseLoader for consistency
            ImageDatabaseLoader.loadImage(imageView, imageUrl)
        }
        
        /**
         * Resize an image to reduce storage size
         * @param original The original bitmap
         * @return The resized bitmap
         */
        private fun resizeImage(original: Bitmap): Bitmap {
            val width = original.width
            val height = original.height
            
            // If image is already small enough, return it as is
            if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
                return original
            }
            
            // Calculate new dimensions while maintaining aspect ratio
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            
            if (width > height) {
                newWidth = MAX_IMAGE_SIZE
                newHeight = (newWidth / ratio).toInt()
            } else {
                newHeight = MAX_IMAGE_SIZE
                newWidth = (newHeight * ratio).toInt()
            }
            
            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
        }
    }
}