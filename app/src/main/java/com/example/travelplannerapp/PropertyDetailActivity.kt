package com.example.travelplannerapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var property: PropertyListing
    private lateinit var imageViewPager: ViewPager2
    private lateinit var titleTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var priceTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var guestsTextView: TextView
    private lateinit var amenitiesChipGroup: ChipGroup
    private lateinit var bookButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_detail)
        
        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Initialize views
        imageViewPager = findViewById(R.id.imageViewPager)
        titleTextView = findViewById(R.id.propertyTitleTextView)
        locationTextView = findViewById(R.id.propertyLocationTextView)
        priceTextView = findViewById(R.id.propertyPriceTextView)
        descriptionTextView = findViewById(R.id.propertyDescriptionTextView)
        guestsTextView = findViewById(R.id.propertyGuestsTextView)
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup)
        bookButton = findViewById(R.id.bookButton)
        
        // Get property from intent
        property = intent.getParcelableExtra("PROPERTY") ?: return
        
        // Set up property details
        displayPropertyDetails()
        
        // Set up image slider
        setupImageSlider()
        
        // Set up book button
        bookButton.setOnClickListener {
            // In a real app, this would navigate to a booking screen
            Toast.makeText(this, "Booking functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayPropertyDetails() {
        titleTextView.text = property.title
        locationTextView.text = property.location
        priceTextView.text = "$${property.pricePerNight} / night"
        descriptionTextView.text = property.description
        guestsTextView.text = "${property.maxGuests} guests maximum"
        
        // Add amenities chips
        amenitiesChipGroup.removeAllViews()
        property.amenities.forEach { amenity ->
            val chip = Chip(this)
            chip.text = amenity
            chip.isClickable = false
            amenitiesChipGroup.addView(chip)
        }
    }
    
    private fun setupImageSlider() {
        val adapter = PropertyImageAdapter(property.imageUrls)
        imageViewPager.adapter = adapter
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

// Adapter for the image slider
class PropertyImageAdapter(private val imageUrls: List<String>) : 
    RecyclerView.Adapter<PropertyImageAdapter.ImageViewHolder>() {
    
    class ImageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val imageView: com.google.android.material.imageview.ShapeableImageView = 
            itemView.findViewById(R.id.propertySlideImageView)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ImageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_slide, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        
        // Check if this is a database reference URL
        if (imageUrl.startsWith("db://")) {
            // Use our custom database image loader
            com.example.travelplannerapp.utils.ImageDatabaseLoader.loadImage(holder.imageView, imageUrl)
        } else {
            // Regular URL, use Picasso
            com.squareup.picasso.Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .fit()
                .centerCrop()
                .into(holder.imageView)
        }
    }
    
    override fun getItemCount(): Int = imageUrls.size
}