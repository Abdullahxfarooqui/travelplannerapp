package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.Travelplannerfyp.travelplannerapp.utils.TripImageLoader
import com.Travelplannerfyp.travelplannerapp.R
import com.google.android.material.appbar.CollapsingToolbarLayout

class DetailActivity : AppCompatActivity() {

    private lateinit var tripImageView: ImageView
    private lateinit var tripNameTextView: TextView
    private lateinit var tripLocationTextView: TextView
    private lateinit var tripDescriptionTextView: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbar: CollapsingToolbarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Initialize views
        initializeViews()
        setupToolbar()
        loadTripData()
    }

    private fun initializeViews() {
        tripImageView = findViewById(R.id.tripImageView)
        tripNameTextView = findViewById(R.id.tripTitleTextView)
        tripLocationTextView = findViewById(R.id.tripLocationTextView)
        tripDescriptionTextView = findViewById(R.id.tripDescriptionTextView)
        toolbar = findViewById(R.id.toolbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun loadTripData() {
        val tripName = intent.getStringExtra("trip_name") ?: "Unknown"
        val tripLocation = intent.getStringExtra("trip_location") ?: "Unknown"
        val tripDescription = intent.getStringExtra("trip_description") ?: "No description available"
        val tripImageUrl = intent.getStringExtra("trip_image_url")
        val tripImageName = intent.getStringExtra("trip_image_name") ?: "placeholder_image"

        // Set text views with animations
        tripNameTextView.apply {
            alpha = 0f
            text = tripName
            animate().alpha(1f).setDuration(500).start()
        }

        tripLocationTextView.apply {
            alpha = 0f
            text = tripLocation
            animate().alpha(1f).setDuration(500).setStartDelay(100).start()
        }

        tripDescriptionTextView.apply {
            alpha = 0f
            text = tripDescription
            animate().alpha(1f).setDuration(500).setStartDelay(200).start()
        }

        // Load image using TripImageLoader with improved logic
        TripImageLoader.loadTripImage(
            context = this,
            imageView = tripImageView,
            imageUrl = tripImageUrl,
            imageResId = null,
            tripName = tripName.trim()
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Add exit animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
