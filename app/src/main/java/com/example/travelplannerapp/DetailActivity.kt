package com.example.travelplannerapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
        val tripImageResId = intent.getIntExtra("trip_image", 0) // Default to 0 if missing

        // Set trip details
        tripNameTextView.text = tripName
        tripLocationTextView.text = tripLocation
        tripDescriptionTextView.text = tripDescription

        // Set trip image
        if (tripImageResId != 0) {
            tripImageView.setImageResource(tripImageResId)
        } else {
            tripImageView.setImageResource(R.drawable.placeholder_image) // Default if missing
        }
    }
}
