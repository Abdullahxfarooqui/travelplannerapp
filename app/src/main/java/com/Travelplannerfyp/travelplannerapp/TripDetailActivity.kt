package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
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
// Removed unused imports: ItineraryAdapter, ReservationAdapter, ItineraryItem
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import android.view.View

class TripDetailActivity : AppCompatActivity() {
    private var loadedTripImageUrl: String? = null
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
        val tripId = intent.getStringExtra("tripId") ?: ""
        val organizerId = intent.getStringExtra("organizerId") ?: ""

        // Initialize views
        val placeNameTextView = findViewById<TextView>(R.id.placeNameTextView)
        val datesTextView = findViewById<TextView>(R.id.datesTextView)
        val seatsTextView = findViewById<TextView>(R.id.seatsTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
        val organizerTextView = findViewById<TextView>(R.id.organizerTextView)
        val placeImageView = findViewById<ImageView>(R.id.placeImageView)
        val tripPriceTextView = findViewById<TextView?>(R.id.tripPriceTextView)
        // Hotel section views
        val hotelCard = findViewById<androidx.cardview.widget.CardView?>(R.id.hotelCard)
        val hotelNameTextView = findViewById<TextView?>(R.id.hotelNameTextView)
        val hotelPriceTextView = findViewById<TextView?>(R.id.hotelPriceTextView)
        val hotelAmenitiesTextView = findViewById<TextView?>(R.id.hotelAmenitiesTextView)
        val hotelImageView = findViewById<ImageView?>(R.id.hotelImageView)
        val reserveHotelButton = findViewById<MaterialButton?>(R.id.reserveHotelButton)
        val reservedBadge = findViewById<TextView?>(R.id.reservedBadge)

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
        
        // Load trip price
        loadTripPrice(tripId, tripPriceTextView)

        // --- Hotel Section ---
        loadHotelData(tripId, hotelCard, hotelNameTextView, hotelPriceTextView, hotelAmenitiesTextView, hotelImageView, reserveHotelButton, reservedBadge)

        // --- Remove Itinerary and Reservations Sections ---
        // Itinerary section completely removed as per requirements
        // Reservation section completely removed as per requirements
        // Removed reservationsCard reference as per requirements

        // --- Action Buttons ---
        val shareButton = findViewById<MaterialButton>(R.id.shareButton)
        val addToCalendarButton = findViewById<MaterialButton>(R.id.addToCalendarButton)
        val openDirectionsButton = findViewById<MaterialButton>(R.id.openDirectionsButton)
        val deleteTripButton = findViewById<MaterialButton>(R.id.deleteTripButton)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        // Only show delete button if current user is the organizer
        if (currentUserId == organizerId) {
            deleteTripButton.visibility = View.VISIBLE
        } else {
            deleteTripButton.visibility = View.GONE
        }
        deleteTripButton.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
                    tripRef.removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Trip deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to delete trip: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this trip!")
                putExtra(Intent.EXTRA_TEXT, "Trip: $placeName\n$tripDescription")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Trip"))
        }

        addToCalendarButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(android.provider.CalendarContract.Events.TITLE, placeName)
                putExtra(android.provider.CalendarContract.Events.DESCRIPTION, tripDescription)
                // Parse start and end dates if possible
            }
            startActivity(intent)
        }

        openDirectionsButton.setOnClickListener {
            try {
                // First try to get coordinates from the geocoded location
                val locationIqToken = "pk.1ef4faca32216233742e487f2372c924"
                val encodedLocation = java.net.URLEncoder.encode(placeName, "UTF-8")
                val geocodeUrl = "https://us1.locationiq.com/v1/search.php?key=$locationIqToken&q=$encodedLocation&format=json"
                
                val client = OkHttpClient()
                val request = Request.Builder().url(geocodeUrl).build()
                
                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        runOnUiThread {
                            // Fallback to place name search
                            openDirectionsWithPlaceName(placeName)
                        }
                    }
                    
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            try {
                                val jsonArray = JSONArray(body)
                                if (jsonArray.length() > 0) {
                                    val obj = jsonArray.getJSONObject(0)
                                    val lat = obj.getString("lat")
                                    val lon = obj.getString("lon")
                                    runOnUiThread {
                                        openDirectionsWithCoordinates(lat, lon, placeName)
                                    }
                                    return
                                }
                            } catch (e: Exception) {
                                // Fallback to place name
                            }
                        }
                        runOnUiThread {
                            openDirectionsWithPlaceName(placeName)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("TripDetailActivity", "Error opening directions", e)
                openDirectionsWithPlaceName(placeName)
            }
        }
        
        // Setup directions button in map section
        val directionsButton = findViewById<MaterialButton>(R.id.directionsButton)
        directionsButton.setOnClickListener {
            try {
                // Use the same logic as the main directions button
                val locationIqToken = "pk.1ef4faca32216233742e487f2372c924"
                val encodedLocation = java.net.URLEncoder.encode(placeName, "UTF-8")
                val geocodeUrl = "https://us1.locationiq.com/v1/search.php?key=$locationIqToken&q=$encodedLocation&format=json"
                
                val client = OkHttpClient()
                val request = Request.Builder().url(geocodeUrl).build()
                
                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        runOnUiThread {
                            openDirectionsWithPlaceName(placeName)
                        }
                    }
                    
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            try {
                                val jsonArray = JSONArray(body)
                                if (jsonArray.length() > 0) {
                                    val obj = jsonArray.getJSONObject(0)
                                    val lat = obj.getString("lat")
                                    val lon = obj.getString("lon")
                                    runOnUiThread {
                                        openDirectionsWithCoordinates(lat, lon, placeName)
                                    }
                                    return
                                }
                            } catch (e: Exception) {
                                // Fallback to place name
                            }
                        }
                        runOnUiThread {
                            openDirectionsWithPlaceName(placeName)
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("TripDetailActivity", "Error opening directions", e)
                openDirectionsWithPlaceName(placeName)
            }
        }

        // Setup booking button
        setupBookingButton(placeName, organizerName, organizerPhone, startDate, endDate, seatsAvailable)
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = placeName

        // --- Map Section with LocationIQ ---
        val mapImageView = findViewById<ImageView>(R.id.mapImageView)
        val location = placeName // or use a more precise address if available
        val locationIqToken = "pk.1ef4faca32216233742e487f2372c924"
        val encodedLocation = java.net.URLEncoder.encode(location, "UTF-8")

        // Apply professional styling to map
        mapImageView.apply {
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 24f)
                }
            }
            background = resources.getDrawable(R.drawable.map_rounded_background, null)
        }

        fun loadStaticMap(lat: String?, lon: String?) {
            val center = if (lat != null && lon != null) "$lat,$lon" else encodedLocation
            val marker = if (lat != null && lon != null) "$lat,$lon" else encodedLocation
            val mapUrl = "https://maps.locationiq.com/v3/staticmap?key=$locationIqToken&center=$center&zoom=13&size=800x400&markers=icon:large-red-cutout|$marker&format=png"
            
            Picasso.get()
                .load(mapUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(mapImageView, object : Callback {
                    override fun onSuccess() {
                        Log.d("TripDetailActivity", "Map loaded successfully")
                    }
                    
                    override fun onError(e: Exception?) {
                        Log.e("TripDetailActivity", "Map load failed: ${e?.message}", e)
                        mapImageView.setImageResource(R.drawable.ic_placeholder)
                    }
                })
        }

        // Try to geocode the place name to get lat/lon
        val client = OkHttpClient()
        val geocodeUrl = "https://us1.locationiq.com/v1/search.php?key=$locationIqToken&q=$encodedLocation&format=json"
        val request = Request.Builder().url(geocodeUrl).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // Fallback to place name if geocoding fails
                runOnUiThread { loadStaticMap(null, null) }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    try {
                        val jsonArray = JSONArray(body)
                        if (jsonArray.length() > 0) {
                            val obj = jsonArray.getJSONObject(0)
                            val lat = obj.getString("lat")
                            val lon = obj.getString("lon")
                            runOnUiThread { loadStaticMap(lat, lon) }
                            return
                        }
                    } catch (e: Exception) {
                        // Ignore and fallback
                    }
                }
                runOnUiThread { loadStaticMap(null, null) }
            }
        })

        // Remove booking summary card references (no longer in layout)
        // val bookingSummaryCard = findViewById<androidx.cardview.widget.CardView>(R.id.bookingSummaryCard)
        // val bookingTripPrice = findViewById<TextView>(R.id.bookingTripPrice)
        // val bookingHotelPrice = findViewById<TextView>(R.id.bookingHotelPrice)
        // val bookingNights = findViewById<TextView>(R.id.bookingNights)
        // val bookingTotalPrice = findViewById<TextView>(R.id.bookingTotalPrice)

        // Remove all logic that sets these views
        // Calculate number of nights from startDate and endDate
        // val nights = try {
        //     val sdf = java.text.SimpleDateFormat("dd/MM/yyyy")
        //     val start = sdf.parse(startDate)
        //     val end = sdf.parse(endDate)
        //     val diff = (end.time - start.time) / (1000 * 60 * 60 * 24)
        //     (if (diff > 0) diff else 1).toInt()
        // } catch (e: Exception) { 1 }
        // bookingNights.text = nights.toString()

        // Remove Firebase logic for summary card
        // val tripRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        // tripRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
        //     override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
        //         val tripPriceValue = snapshot.child("pricePerPerson").value ?: snapshot.child("price").value
        //         val tripPriceStr = when (tripPriceValue) {
        //             is Number -> tripPriceValue.toDouble().toString()
        //             is String -> tripPriceValue
        //             else -> ""
        //         }
        //         val hotelPriceValue = snapshot.child("hotel").child("pricePerNight").value
        //         val hotelPriceStr = when (hotelPriceValue) {
        //             is Number -> hotelPriceValue.toDouble().toString()
        //             is String -> hotelPriceValue
        //             else -> ""
        //         }
        //         val tripPrice = tripPriceStr.replace(",", "").toDoubleOrNull() ?: 0.0
        //         val hotelPrice = hotelPriceStr.replace(",", "").toDoubleOrNull() ?: 0.0
        //         bookingTripPrice.text = CurrencyUtils.formatAsPKR(tripPrice)
        //         bookingHotelPrice.text = if (hotelPrice > 0) CurrencyUtils.formatAsPKR(hotelPrice) + "/night" else "-"
        //         val total = tripPrice + (hotelPrice * nights)
        //         bookingTotalPrice.text = CurrencyUtils.formatAsPKR(total)
        //         // Hide card if both prices are zero
        //         bookingSummaryCard.visibility = if (tripPrice == 0.0 && hotelPrice == 0.0) View.GONE else View.VISIBLE
        //     }
        //     override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
        //         bookingSummaryCard.visibility = View.GONE
        //     }
        // })
    }

    private fun loadTripPrice(tripId: String, tripPriceTextView: TextView?) {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        tripRef.child("pricePerPerson").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val priceString = snapshot.getValue(String::class.java)
                val price = priceString?.toDoubleOrNull()
                if (price != null) {
                    tripPriceTextView?.text = CurrencyUtils.formatAsPKR(price.toString())
                    tripPriceTextView?.visibility = View.VISIBLE
                } else {
                    tripPriceTextView?.visibility = View.GONE
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                tripPriceTextView?.visibility = View.GONE
            }
        })
    }

    private fun loadHotelData(tripId: String, hotelCard: androidx.cardview.widget.CardView?, hotelNameTextView: TextView?, hotelPriceTextView: TextView?, hotelAmenitiesTextView: TextView?, hotelImageView: ImageView?, reserveHotelButton: MaterialButton?, reservedBadge: TextView?) {
        val hotelRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("hotel")
        hotelRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hotelName = snapshot.child("name").getValue(String::class.java)
                val pricePerNightValue = snapshot.child("pricePerNight").value
                val pricePerNight = when (pricePerNightValue) {
                    is Number -> pricePerNightValue.toDouble().toString()
                    is String -> pricePerNightValue
                    else -> ""
                }
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                val imageName = snapshot.child("imageName").getValue(String::class.java)
                val description = snapshot.child("description").getValue(String::class.java)
                val amenitiesList = snapshot.child("amenities").children.mapNotNull { it.getValue(String::class.java) }
                val reserved = snapshot.child("reserved").getValue(Boolean::class.java) ?: false
                val hasHotelData = !hotelName.isNullOrBlank() || !pricePerNight.isNullOrBlank() || !imageUrl.isNullOrBlank() || amenitiesList.isNotEmpty()

                if (!hasHotelData) {
                    hotelCard?.visibility = View.VISIBLE
                    hotelNameTextView?.text = "No hotel selected for this trip"
                    hotelPriceTextView?.visibility = View.GONE
                    hotelAmenitiesTextView?.visibility = View.GONE
                    hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                    reserveHotelButton?.visibility = View.GONE
                    reservedBadge?.visibility = View.GONE
                    return
                }

                hotelCard?.visibility = View.VISIBLE
                hotelNameTextView?.text = hotelName ?: "Hotel"
                hotelNameTextView?.visibility = View.VISIBLE

                // Format price with proper PKR formatting
                if (!pricePerNight.isNullOrBlank()) {
                    hotelPriceTextView?.text = "Price per night: ${CurrencyUtils.formatAsPKR(pricePerNight)}"
                } else {
                    hotelPriceTextView?.text = "Price not set"
                }
                hotelPriceTextView?.visibility = View.VISIBLE

                // Show amenities if available, else show description, else fallback
                if (amenitiesList.isNotEmpty()) {
                    hotelAmenitiesTextView?.text = "Amenities: ${amenitiesList.joinToString(", ")}"
                } else if (!description.isNullOrBlank()) {
                    hotelAmenitiesTextView?.text = description
                } else {
                    hotelAmenitiesTextView?.text = "Amenities: Not listed"
                }
                hotelAmenitiesTextView?.visibility = View.VISIBLE

                // Load hotel image
                if (!imageUrl.isNullOrBlank()) {
                    Picasso.get().load(imageUrl).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_image_placeholder).into(hotelImageView)
                } else if (!imageName.isNullOrBlank()) {
                    // Try to load from drawable
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
                            hotelImageView?.setImageResource(resourceId)
                        } else {
                            hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    } catch (e: Exception) {
                        hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                }

                if (reserved) {
                    reservedBadge?.visibility = View.VISIBLE
                    reserveHotelButton?.visibility = View.GONE
                } else {
                    reservedBadge?.visibility = View.GONE
                    reserveHotelButton?.visibility = View.VISIBLE
                    reserveHotelButton?.setOnClickListener {
                        hotelRef.child("reserved").setValue(true).addOnSuccessListener {
                            reservedBadge?.visibility = View.VISIBLE
                            reserveHotelButton.visibility = View.GONE
                            Toast.makeText(this@TripDetailActivity, "Hotel reserved successfully!", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this@TripDetailActivity, "Failed to reserve hotel.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                hotelCard?.visibility = View.VISIBLE
                hotelNameTextView?.text = "No hotel selected for this trip"
                hotelPriceTextView?.visibility = View.GONE
                hotelAmenitiesTextView?.visibility = View.GONE
                hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                reserveHotelButton?.visibility = View.GONE
                reservedBadge?.visibility = View.GONE
            }
        })
    }

    private fun formatDate(dateString: String): String {
        return try {
            val parts = dateString.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                val monthName = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )[month - 1]
                "$day $monthName, $year"
            } else dateString
        } catch (e: Exception) {
            Log.e("TripDetailActivity", "Error formatting date: $dateString", e)
            dateString
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

    private fun setupBookingButton(placeName: String, organizerName: String, organizerPhone: String, startDate: String, endDate: String, seatsAvailable: String) {
        val bookTripButton = findViewById<MaterialButton>(R.id.bookTripButton)
        val tripId = intent.getStringExtra("tripId") ?: ""
        val placeImageName = intent.getStringExtra("placeImage") ?: extractImageNameFromPlace(placeName)
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
            // Always use the real Firebase trip key and pass the loaded image URL and imageName
            Log.d("TripDetailActivity", "Passing image URL to booking: " + (loadedTripImageUrl ?: "null") + ", imageName: $placeImageName")
            val intent = Intent(this, TripBookingActivity::class.java).apply {
                putExtra(TripBookingActivity.EXTRA_TRIP_ID, tripId)
                putExtra("tripImageUrl", loadedTripImageUrl ?: "")
                putExtra("tripImageName", placeImageName)
            }
            startActivity(intent)
        }
    }
    
    private fun setupHotelReservation(tripId: String) {
        // Check if hotel is already reserved
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        tripsRef.child("reservation").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isReserved = snapshot.getValue(Boolean::class.java) ?: false
                updateReservationUI(isReserved)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("TripDetailActivity", "Error checking reservation status: ${error.message}")
            }
        })
    }
    
    private fun updateReservationUI(isReserved: Boolean) {
        // Find or create reservation button
        val hotelCard = findViewById<androidx.cardview.widget.CardView>(R.id.hotelCard)
        val existingButton = hotelCard.findViewById<MaterialButton>(R.id.reserveHotelButton)
        
        if (existingButton != null) {
            if (isReserved) {
                existingButton.text = "Reserved ✅"
                existingButton.isEnabled = false
                existingButton.setBackgroundColor(resources.getColor(R.color.primary, null))
            } else {
                existingButton.text = "Reserve Hotel"
                existingButton.isEnabled = true
                existingButton.setOnClickListener {
                    reserveHotel()
                }
            }
        } else {
            // Create reservation button if it doesn't exist
            val reserveButton = MaterialButton(this).apply {
                id = R.id.reserveHotelButton
                text = if (isReserved) "Reserved ✅" else "Reserve Hotel"
                isEnabled = !isReserved
                setBackgroundColor(resources.getColor(if (isReserved) R.color.primary else R.color.accent, null))
                setTextColor(resources.getColor(android.R.color.white, null))
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                
                if (!isReserved) {
                    setOnClickListener {
                        reserveHotel()
                    }
                }
            }
            
            // Add button to hotels card
            val hotelsCardLayout = hotelCard.getChildAt(0) as? LinearLayout
            hotelsCardLayout?.addView(reserveButton)
        }
    }
    
    private fun reserveHotel() {
        val tripId = intent.getStringExtra("tripId") ?: return
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        
        tripsRef.child("reservation").setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Hotel reserved successfully!", Toast.LENGTH_SHORT).show()
                updateReservationUI(true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reserve hotel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun openDirectionsWithCoordinates(lat: String, lon: String, placeName: String) {
        try {
            val gmmIntentUri = android.net.Uri.parse("google.navigation:q=$lat,$lon")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to generic maps app
                val fallbackUri = android.net.Uri.parse("geo:$lat,$lon?q=$placeName")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "No maps app available", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("TripDetailActivity", "Error opening directions with coordinates", e)
            openDirectionsWithPlaceName(placeName)
        }
    }
    
    private fun openDirectionsWithPlaceName(placeName: String) {
        try {
            val encodedPlaceName = java.net.URLEncoder.encode(placeName, "UTF-8")
            val gmmIntentUri = android.net.Uri.parse("google.navigation:q=$encodedPlaceName")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to generic maps app
                val fallbackUri = android.net.Uri.parse("geo:0,0?q=$encodedPlaceName")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "No maps app available", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("TripDetailActivity", "Error opening directions with place name", e)
            Toast.makeText(this, "Unable to open directions", Toast.LENGTH_SHORT).show()
        }
    }
}
