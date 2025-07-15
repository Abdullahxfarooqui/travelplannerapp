package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.squareup.picasso.Picasso
import java.util.regex.Pattern
import com.Travelplannerfyp.travelplannerapp.R
import android.widget.LinearLayout
import android.widget.Toast
import com.Travelplannerfyp.travelplannerapp.utils.HotelImageLoader

class HotelDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotel_detail)

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up collapsing toolbar
        val collapsingToolbar: CollapsingToolbarLayout = findViewById(R.id.collapsingToolbar)

        // Get hotel data from the intent
        val hotelName = intent.getStringExtra("name") ?: "Hotel Details"
        val hotelDescription = intent.getStringExtra("description") ?: "No description available"
        val hotelRating = intent.getDoubleExtra("rating", 0.0)
        val hotelPrice = intent.getStringExtra("price") ?: "Price not available"
        val hotelImageName = intent.getStringExtra("imageName") // This will be the image name without extension

        // Set the title in the collapsing toolbar
        collapsingToolbar.title = hotelName

        // Populate views with data
        val nameTextView: TextView = findViewById(R.id.detail_hotel_name)
        val descriptionTextView: TextView = findViewById(R.id.detail_hotel_description)
        val priceTextView: TextView = findViewById(R.id.detail_hotel_price)
        val ratingBar: RatingBar = findViewById(R.id.detail_hotel_rating)
        val ratingText: TextView = findViewById(R.id.rating_text)
        val imageView: ImageView = findViewById(R.id.detail_hotel_image)

        nameTextView.text = hotelName
        descriptionTextView.text = hotelDescription
        // Show price as Rs. <amount>/night if available
        if (hotelPrice != "Price not available" && hotelPrice.isNotBlank()) {
            priceTextView.text = "Rs. ${hotelPrice}/night"
        } else {
            priceTextView.text = "Price not set"
        }
        ratingBar.rating = hotelRating.toFloat()
        ratingText.text = String.format("%.1f/5", hotelRating)
        
        // Get hotel image URL from the intent
        val hotelImageUrl = intent.getStringExtra("imageUrl")
        
        // Log all intent extras for debugging
        Log.d("HotelDetailActivity", "Intent extras: name=$hotelName, imageUrl=$hotelImageUrl, imageName=$hotelImageName")
        
        // Try to load image from URL first, then fallback to drawable resource
        HotelImageLoader.loadHotelImage(hotelImageUrl, hotelImageName, hotelName, imageView)

        // Get amenities from intent
        val amenities = intent.getStringArrayListExtra("amenities")
        val amenitiesContainer = findViewById<LinearLayout>(R.id.amenities_container)
        amenitiesContainer?.removeAllViews()
        if (amenities != null && amenities.isNotEmpty()) {
            for (amenity in amenities) {
                val tv = TextView(this)
                tv.text = amenity
                tv.setPadding(16, 8, 16, 8)
                tv.setBackgroundResource(R.drawable.amenity_chip_background)
                tv.setTextColor(resources.getColor(R.color.primary, null))
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(8, 8, 8, 8)
                tv.layoutParams = params
                amenitiesContainer.addView(tv)
            }
        } else {
            val tv = TextView(this)
            tv.text = "No amenities listed"
            tv.setTextColor(resources.getColor(R.color.textSecondary, null))
            amenitiesContainer.addView(tv)
        }

        // Book Now button logic
        val bookHotelButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_book_hotel)
        bookHotelButton.setOnClickListener {
            Toast.makeText(this, "Hotel booking feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}