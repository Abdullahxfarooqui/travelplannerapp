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
        priceTextView.text = hotelPrice
        ratingBar.rating = hotelRating.toFloat()
        ratingText.text = String.format("%.1f/5", hotelRating)
        
        // Get hotel image URL from the intent
        val hotelImageUrl = intent.getStringExtra("imageUrl")
        
        // Log all intent extras for debugging
        Log.d("HotelDetailActivity", "Intent extras: name=$hotelName, imageUrl=$hotelImageUrl, imageName=$hotelImageName")
        
        // Try to load image from URL first, then fallback to drawable resource
        if (!hotelImageUrl.isNullOrEmpty()) {
            // Check if the URL is valid using regex
            val urlPattern = Pattern.compile(
                "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                Pattern.CASE_INSENSITIVE
            )
            
            val validUrl = if (!hotelImageUrl.startsWith("http://") && !hotelImageUrl.startsWith("https://")) {
                "https://$hotelImageUrl"
            } else {
                hotelImageUrl
            }
            
            // Check if the URL is valid
            val isValidUrl = urlPattern.matcher(validUrl).matches()
            
            // Log the URL to debug
            Log.d("HotelDetailActivity", "Loading hotel image from URL: $validUrl (valid URL: $isValidUrl)")
            
            if (isValidUrl) {
                // Use a more robust approach to load the image
                try {
                    Picasso.get()
                        .load(validUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .fit()
                        .centerCrop()
                        .into(imageView, object : com.squareup.picasso.Callback {
                            override fun onSuccess() {
                                Log.d("HotelDetailActivity", "Hotel image loaded successfully from URL")
                            }
                            
                            override fun onError(e: Exception?) {
                                Log.e("HotelDetailActivity", "Error loading hotel image from URL: ${e?.message}, trying drawable", e)
                                if (!hotelImageName.isNullOrEmpty()) {
                                    loadImageFromDrawable(hotelImageName, imageView)
                                } else {
                                    imageView.setImageResource(R.drawable.placeholder_image)
                                }
                            }
                        })
                } catch (e: Exception) {
                    Log.e("HotelDetailActivity", "Exception when loading image from URL: ${e.message}", e)
                    if (!hotelImageName.isNullOrEmpty()) {
                        loadImageFromDrawable(hotelImageName, imageView)
                    } else {
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                }
            } else {
                Log.d("HotelDetailActivity", "Invalid URL format, trying to load from drawable")
                if (!hotelImageName.isNullOrEmpty()) {
                    loadImageFromDrawable(hotelImageName, imageView)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            }
        } else if (!hotelImageName.isNullOrEmpty()) {
            Log.d("HotelDetailActivity", "No URL available, trying to load from drawable: $hotelImageName")
            loadImageFromDrawable(hotelImageName, imageView)
        } else {
            Log.d("HotelDetailActivity", "No image URL or name available, using placeholder")
            imageView.setImageResource(R.drawable.placeholder_image) // Default placeholder
        }
    }
    
    private fun loadImageFromDrawable(imageName: String?, imageView: ImageView) {
        if (imageName.isNullOrEmpty()) {
            Log.d("HotelDetailActivity", "Image name is null or empty, using placeholder")
            imageView.setImageResource(R.drawable.placeholder_image)
            return
        }
        
        try {
            // Try different variations of the image name to find a match
            val rawImageName = imageName.substringBeforeLast(".").lowercase()
            Log.d("HotelDetailActivity", "Trying to load drawable with raw name: $rawImageName")
            
            // Try the raw name first
            var resourceId = resources.getIdentifier(rawImageName, "drawable", packageName)
            
            // If not found, try with normalized name (replace special chars with underscore)
            if (resourceId == 0) {
                val normalizedName = rawImageName.replace("[^a-z0-9]+".toRegex(), "_").trim('_')
                Log.d("HotelDetailActivity", "Raw name not found, trying normalized name: $normalizedName")
                resourceId = resources.getIdentifier(normalizedName, "drawable", packageName)
            }
            
            // If still not found, try with just the first part of the name (before any underscore or space)
            if (resourceId == 0) {
                val simplifiedName = rawImageName.split("_", " ", "-").first().lowercase()
                Log.d("HotelDetailActivity", "Normalized name not found, trying simplified name: $simplifiedName")
                resourceId = resources.getIdentifier(simplifiedName, "drawable", packageName)
            }
            
            // If a resource was found, use it
            if (resourceId != 0) {
                Log.d("HotelDetailActivity", "Loading hotel image from drawable, resourceId: $resourceId")
                imageView.setImageResource(resourceId)
            } else {
                // Last resort - try some common drawable names
                val commonDrawables = arrayOf("placeholder_image", "ic_placeholder", "placeholder")
                for (commonName in commonDrawables) {
                    resourceId = resources.getIdentifier(commonName, "drawable", packageName)
                    if (resourceId != 0) {
                        Log.d("HotelDetailActivity", "Using common drawable: $commonName")
                        imageView.setImageResource(resourceId)
                        return
                    }
                }
                
                // If all else fails, use placeholder
                Log.d("HotelDetailActivity", "No drawable found for: $rawImageName, using placeholder")
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            Log.e("HotelDetailActivity", "Error loading image from drawable: ${e.message}", e)
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}