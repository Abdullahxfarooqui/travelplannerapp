package com.example.travelplannerapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.squareup.picasso.Picasso

class Hotel_Detail_Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotel_detail)

        // Get hotel data from the intent
        val hotelName = intent.getStringExtra("name")
        val hotelDescription = intent.getStringExtra("description")
        val hotelRating = intent.getDoubleExtra("rating", 0.0)
        val hotelImageUrl = intent.getStringExtra("imageUrl")

        // Populate views with data
        val nameTextView: TextView = findViewById(R.id.detail_hotel_name)
        val descriptionTextView: TextView = findViewById(R.id.detail_hotel_description)
        val ratingBar: RatingBar = findViewById(R.id.detail_hotel_rating)
        val imageView: ImageView = findViewById(R.id.detail_hotel_image)

        nameTextView.text = hotelName
        descriptionTextView.text = hotelDescription
        ratingBar.rating = hotelRating.toFloat()

        // Load image using Picasso or Glide
        Picasso.get().load(hotelImageUrl).into(imageView)
    }
}