package com.example.travelplannerapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class HotelDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotel_detail)

        // Get hotel data from the intent
        val hotelName = intent.getStringExtra("name")
        val hotelDescription = intent.getStringExtra("description")
        val hotelRating = intent.getDoubleExtra("rating", 0.0)
        val hotelPrice = intent.getStringExtra("price")
        val hotelImageUrl = intent.getStringExtra("imageUrl")

        // Populate views with data
        val nameTextView: TextView = findViewById(R.id.detail_hotel_name)
        val descriptionTextView: TextView = findViewById(R.id.detail_hotel_description)
        val priceTextView: TextView = findViewById(R.id.detail_hotel_price)
        val ratingBar: RatingBar = findViewById(R.id.detail_hotel_rating)
        val imageView: ImageView = findViewById(R.id.detail_hotel_image)

        nameTextView.text = hotelName
        descriptionTextView.text = hotelDescription
        priceTextView.text = hotelPrice
        ratingBar.rating = hotelRating.toFloat()

        // Load image using Picasso
        if (!hotelImageUrl.isNullOrEmpty()) {
            Picasso.get().load(hotelImageUrl).into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_placeholder)
        }
    }
}