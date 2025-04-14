package com.example.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class DetailActivity : AppCompatActivity() {

    private lateinit var tripImageView: ImageView
    private lateinit var tripNameTextView: TextView
    private lateinit var tripLocationTextView: TextView
    private lateinit var tripDescriptionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Initialize views
        tripImageView = findViewById(R.id.tripImageView)
        tripNameTextView = findViewById(R.id.tripTitleTextView)
        tripLocationTextView = findViewById(R.id.tripLocationTextView)
        tripDescriptionTextView = findViewById(R.id.tripDescriptionTextView)

        // Get data from intent
        val tripName = intent.getStringExtra("trip_name") ?: "Unknown"
        val tripLocation = intent.getStringExtra("trip_location") ?: "Unknown"
        val tripDescription = intent.getStringExtra("trip_description") ?: "No description available"
        val tripImageUrl = intent.getStringExtra("trip_image_url")
        val tripImageResId = intent.getIntExtra("trip_image_res_id", 0)

        // Log for debugging
        Log.d("ImageLoading", "Received trip: $tripName")
        Log.d("ImageLoading", "Image URL: $tripImageUrl, Resource ID: $tripImageResId")

        // Set text views
        tripNameTextView.text = tripName
        tripLocationTextView.text = tripLocation
        tripDescriptionTextView.text = tripDescription

        // Load image with fallback logic
        loadTripImage(tripImageUrl, tripImageResId)
    }

    private fun loadTripImage(imageUrl: String?, imageResId: Int) {
        when {
            !imageUrl.isNullOrEmpty() -> {
                // Try loading from URL
                Log.d("ImageLoading", "Trying to load image from URL: $imageUrl")
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fit()
                    .centerCrop()
                    .into(tripImageView, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            Log.d("ImageLoading", "Successfully loaded image from URL.")
                        }

                        override fun onError(e: Exception) {
                            Log.e("ImageLoading", "Failed to load image from URL: ${e.message}", e)
                            fallbackToResource(imageResId)
                        }
                    })
            }

            imageResId != 0 -> {
                Log.d("ImageLoading", "URL is empty. Loading resource image: $imageResId")
                fallbackToResource(imageResId)
            }

            else -> {
                Log.d("ImageLoading", "No image URL or resource ID. Using placeholder.")
                tripImageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    private fun fallbackToResource(imageResId: Int) {
        try {
            if (imageResId != 0) {
                tripImageView.setImageResource(imageResId)
                Log.d("ImageLoading", "Fallback: Resource image set successfully.")
            } else {
                tripImageView.setImageResource(R.drawable.placeholder_image)
                Log.d("ImageLoading", "Fallback: Invalid resource ID. Using error image.")
            }
        } catch (e: Exception) {
            Log.e("ImageLoading", "Fallback failed: ${e.message}", e)
            tripImageView.setImageResource(R.drawable.placeholder_image)
        }
    }
}
