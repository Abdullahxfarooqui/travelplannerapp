package com.example.travelplannerapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.example.travelplannerapp.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

/**
 * Utility class for loading images from Firebase Realtime Database
 * Handles loading Base64 encoded images stored in the database
 */
class ImageDatabaseLoader {
    companion object {
        private const val TAG = "ImageDatabaseLoader"
        private const val DB_PREFIX = "db://"
        
        /**
         * Load an image from a URL or Firebase Realtime Database
         * @param imageView The ImageView to load the image into
         * @param imageUrl The image URL or database reference (format: "db://path/to/image")
         */
        fun loadImage(imageView: ImageView, imageUrl: String?) {
            if (imageUrl.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.placeholder_image)
                return
            }
            
            // Check if this is a database reference
            if (imageUrl.startsWith(DB_PREFIX)) {
                // Extract the database path from the URL
                val dbPath = imageUrl.substring(DB_PREFIX.length)
                loadImageFromDatabase(imageView, dbPath)
            } else {
                // Regular URL, use Picasso
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageView)
            }
        }
        
        /**
         * Load an image from Firebase Realtime Database
         * @param imageView The ImageView to load the image into
         * @param dbPath The path to the image in the database
         */
        private fun loadImageFromDatabase(imageView: ImageView, dbPath: String) {
            val database = FirebaseDatabase.getInstance().reference
            val imageRef = database.child(dbPath)
            
            imageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Get the Base64 encoded image data
                        val imageData = snapshot.child("data").getValue(String::class.java)
                        if (imageData != null) {
                            // Decode the Base64 string to a bitmap
                            val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            
                            // Set the bitmap to the ImageView
                            imageView.setImageBitmap(bitmap)
                        } else {
                            Log.e(TAG, "Image data not found at $dbPath")
                            imageView.setImageResource(R.drawable.placeholder_image)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading image from database: ${e.message}")
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            })
        }
    }
}