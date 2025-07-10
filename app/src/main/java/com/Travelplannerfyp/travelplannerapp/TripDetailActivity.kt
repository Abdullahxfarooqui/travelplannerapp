package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.models.Hotel
import com.Travelplannerfyp.travelplannerapp.adapters.HotelAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.adapters.EnrolledUserAdapter
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class TripDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_detail)

        // Get trip details from intent
        val placeName = intent.getStringExtra("placeName") ?: ""
        val placeDescription = intent.getStringExtra("placeDescription") ?: ""
        val tripDescription = intent.getStringExtra("tripDescription") ?: ""
        val organizerName = intent.getStringExtra("organizerName") ?: ""
        val organizerPhone = intent.getStringExtra("organizerPhone") ?: ""
        val startDate = intent.getStringExtra("startDate") ?: ""
        val endDate = intent.getStringExtra("endDate") ?: ""
        val seatsAvailable = intent.getStringExtra("seatsAvailable") ?: ""
        val placeImageUrl = intent.getStringExtra("placeImageUrl")
        val placeImageName = intent.getStringExtra("placeImage") ?: extractImageNameFromPlace(placeName)
        val hotelList = intent.getParcelableArrayListExtra<Hotel>("hotels") ?: arrayListOf()

        // Initialize views
        val placeNameTextView = findViewById<TextView>(R.id.placeNameTextView)
        val datesTextView = findViewById<TextView>(R.id.datesTextView)
        val seatsTextView = findViewById<TextView>(R.id.seatsTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
        val organizerTextView = findViewById<TextView>(R.id.organizerTextView)
        val placeImageView = findViewById<ImageView>(R.id.placeImageView)
        val hotelsRecyclerView = findViewById<RecyclerView>(R.id.recycler_view_hotels)

        // Format dates
        val formattedStartDate = formatDate(startDate)
        val formattedEndDate = formatDate(endDate)

        // Set UI data
        placeNameTextView.text = placeName.split(" ").joinToString(" ") { it.capitalize() }

        datesTextView.text = if (startDate == endDate) {
            "1-Day Trip • $formattedStartDate"
        } else {
            "$formattedStartDate - $formattedEndDate"
        }

        seatsTextView.text = "👥 Seats Available: $seatsAvailable"
        descriptionTextView.text = tripDescription.capitalize() +
                if (placeDescription.isNotEmpty()) "\n\n${placeDescription.capitalize()}" else ""

        organizerTextView.text = "Name: $organizerName\nContact: $organizerPhone"

        // Load image
        loadPlaceImage(placeImageUrl, placeImageName, placeImageView)

        // Setup RecyclerView
        hotelsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        hotelsRecyclerView.adapter = HotelAdapter(
            hotelList,
            onItemClick = { hotel ->
                try {
                    val intent = android.content.Intent(this, HotelDetailActivity::class.java).apply {
                        putExtra("name", hotel.name)
                        putExtra("description", hotel.description)
                        putExtra("rating", hotel.rating)
                        putExtra("price", hotel.price)
                        putExtra("imageName", hotel.imageName)
                        putExtra("imageUrl", hotel.imageUrl)
                    }
                    Log.d("TripDetailActivity", "Starting HotelDetailActivity with hotel: ${hotel.name}, imageUrl: ${hotel.imageUrl}")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("TripDetailActivity", "Error starting HotelDetailActivity", e)
                    Toast.makeText(this, "Error opening hotel details", Toast.LENGTH_SHORT).show()
                }
            },
            onAddToCartClick = {}
        )

        // Enrolled Users Section (Organizer Only)
        val enrolledUsersRecycler = findViewById<RecyclerView>(R.id.enrolledUsersRecycler)
        val enrolledUsersTitle = findViewById<TextView>(R.id.enrolledUsersTitle)
        val noEnrolledUsersText = findViewById<TextView>(R.id.noEnrolledUsersText)
        val enrolledUsersList = mutableListOf<EnrolledUser>()
        val enrolledUserAdapter = EnrolledUserAdapter(enrolledUsersList)
        enrolledUsersRecycler.layoutManager = LinearLayoutManager(this)
        enrolledUsersRecycler.adapter = enrolledUserAdapter
        // Fetch enrolled users from Firebase
        val tripId = intent.getStringExtra("tripId") ?: "${placeName}_${organizerName}_${startDate}".replace(" ", "_")
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.orderByChild("itemId").equalTo(tripId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    enrolledUsersList.clear()
                    for (booking in snapshot.children) {
                        val status = booking.child("status").getValue(String::class.java) ?: ""
                        if (status == "Confirmed" || status == "Pending") {
                            val userId = booking.child("userId").getValue(String::class.java) ?: ""
                            val name = booking.child("userName").getValue(String::class.java)
                                ?: booking.child("name").getValue(String::class.java) ?: "Unknown"
                            val email = booking.child("userEmail").getValue(String::class.java) ?: ""
                            val phone = booking.child("userPhone").getValue(String::class.java) ?: ""
                            val seats = booking.child("numberOfGuests").getValue(Int::class.java)
                                ?: booking.child("seats").getValue(Int::class.java) ?: 1
                            val bookingTime = booking.child("createdAt").getValue(Long::class.java) ?: 0L
                            enrolledUsersList.add(
                                EnrolledUser(
                                    userId = userId,
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    seats = seats,
                                    bookingTime = bookingTime
                                )
                            )
                        }
                    }
                    enrolledUsersList.sortBy { it.bookingTime }
                    enrolledUserAdapter.notifyDataSetChanged()
                    if (enrolledUsersList.isEmpty()) {
                        noEnrolledUsersText.visibility = android.view.View.VISIBLE
                        enrolledUsersRecycler.visibility = android.view.View.GONE
                    } else {
                        noEnrolledUsersText.visibility = android.view.View.GONE
                        enrolledUsersRecycler.visibility = android.view.View.VISIBLE
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    noEnrolledUsersText.text = "Failed to load enrolled users"
                    noEnrolledUsersText.visibility = android.view.View.VISIBLE
                    enrolledUsersRecycler.visibility = android.view.View.GONE
                }
            })

        // Setup booking button
        setupBookingButton(placeName, organizerName, organizerPhone, startDate, endDate, seatsAvailable)
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = placeName
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                val monthName = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )[month - 1]
                "$day $monthName, $year"
            } else dateStr
        } catch (e: Exception) {
            Log.e("TripDetailActivity", "Error formatting date: $dateStr", e)
            dateStr
        }
    }

    private fun extractImageNameFromPlace(placeName: String): String {
        // Extract potential image name from place name
        val cleanName = placeName.lowercase().trim()
        return when {
            cleanName.contains("murree") -> "murree"
            cleanName.contains("naran") -> "naran"
            cleanName.contains("hunza") -> "hunza"
            cleanName.contains("skardu") -> "skardu"
            cleanName.contains("chitral") -> "chitral"
            cleanName.contains("gilgit") -> "giglit"
            cleanName.contains("fairy meadows") -> "fairy_meadows"
            cleanName.contains("attabad") -> "attabad"
            cleanName.contains("khunjerab") -> "khunjerab"
            cleanName.contains("neelum") -> "neelumvalley"
            cleanName.contains("ratti gali") -> "rattigali"
            cleanName.contains("balakot") -> "balakot"
            cleanName.contains("ziarat") -> "ziarat"
            cleanName.contains("lahore") -> "lahore"
            cleanName.contains("islamabad") -> "islamabad"
            cleanName.contains("karachi") -> "karachi"
            cleanName.contains("kotli") -> "kotli"
            else -> "placeholder_image"
        }
    }

    private fun loadPlaceImage(imageUrl: String?, imageName: String, imageView: ImageView) {
        Log.d("TripDetailActivity", "Loading image. URL: $imageUrl, Name: $imageName")

        // First try to load from drawable resources
        if (loadImageFromDrawable(imageName, imageView)) {
            Log.d("TripDetailActivity", "Image loaded from drawable: $imageName")
            return
        }

        // If drawable fails and we have a valid URL, try loading from URL
        if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("http")) {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .fit()
                .centerCrop()
                .into(imageView, object : Callback {
                    override fun onSuccess() {
                        Log.d("TripDetailActivity", "Image loaded from URL")
                    }

                    override fun onError(e: Exception?) {
                        Log.e("TripDetailActivity", "Image load failed from URL: ${e?.message}", e)
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                })
        } else {
            Log.d("TripDetailActivity", "No valid URL or drawable found, using placeholder")
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun loadImageFromDrawable(imageName: String, imageView: ImageView): Boolean {
        try {
            val rawImageName = imageName.substringBeforeLast(".").lowercase()
            var resourceId = resources.getIdentifier(rawImageName, "drawable", packageName)

            if (resourceId == 0) {
                val normalized = rawImageName.replace("[^a-z0-9]+".toRegex(), "_").trim('_')
                resourceId = resources.getIdentifier(normalized, "drawable", packageName)
            }

            if (resourceId == 0) {
                val simplified = rawImageName.split("_", " ", "-").first()
                resourceId = resources.getIdentifier(simplified, "drawable", packageName)
            }

            if (resourceId != 0) {
                imageView.setImageResource(resourceId)
                Log.d("TripDetailActivity", "Successfully loaded drawable: $rawImageName")
                return true
            } else {
                Log.d("TripDetailActivity", "No drawable found for: $rawImageName")
                return false
            }
        } catch (e: Exception) {
            Log.e("TripDetailActivity", "Error loading drawable image: ${e.message}", e)
            return false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupBookingButton(placeName: String, organizerName: String, organizerPhone: String, startDate: String, endDate: String, seatsAvailable: String) {
        val bookTripButton = findViewById<MaterialButton>(R.id.bookTripButton)
        
        bookTripButton.setOnClickListener {
            // Check if user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please login to book this trip", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if seats are available
            val availableSeats = seatsAvailable.toIntOrNull() ?: 0
            if (availableSeats <= 0) {
                Toast.makeText(this, "No seats available for this trip", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Create a unique trip ID for booking
            val tripId = "${placeName}_${organizerName}_${startDate}".replace(" ", "_")
            
            // Navigate to booking activity
            val intent = Intent(this, TripBookingActivity::class.java).apply {
                putExtra(TripBookingActivity.EXTRA_TRIP_ID, tripId)
            }
            startActivity(intent)
        }
    }
}
