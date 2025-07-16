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
import com.Travelplannerfyp.travelplannerapp.utils.HotelImageLoader
import com.Travelplannerfyp.travelplannerapp.adapters.ItineraryAdapter
import com.Travelplannerfyp.travelplannerapp.models.ItineraryItem
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import com.google.android.material.textfield.TextInputEditText
import android.widget.Spinner
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import com.Travelplannerfyp.travelplannerapp.adapters.NearbyAttractionAdapter
import com.Travelplannerfyp.travelplannerapp.models.Place

class TripDetailActivity : AppCompatActivity() {
    private var loadedTripImageUrl: String? = null
    private val currencyApiKey = "a433ae5ec51a375c02cb1ccb"
    private val currencyApiUrl = "https://v6.exchangerate-api.com/v6/$currencyApiKey/latest/PKR"
    private var latestRates: Map<String, Double>? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var tripLatitude: Double? = null
    private var tripLongitude: Double? = null
    private var mapWebView: WebView? = null
    private var mapLat: Double = 33.6844
    private var mapLon: Double = 73.0479
    private lateinit var nearbyAttractionAdapter: NearbyAttractionAdapter
    private var nearbyAttractions: List<Place> = emptyList()

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

        // Get trip lat/lon from intent or fallback
        tripLatitude = intent.getDoubleExtra("latitude", 33.6844)
        tripLongitude = intent.getDoubleExtra("longitude", 73.0479)

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

        // Load trip price and hotel data robustly from Firebase
        if (tripId.isNotEmpty()) {
            val tripRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("trips").child(tripId)
            tripRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    Log.d("TripDetailDebug", "pricePerPerson: " + snapshot.child("pricePerPerson").getValue(String::class.java))
                    Log.d("TripDetailDebug", "price: " + snapshot.child("price").getValue(String::class.java))
                    Log.d("TripDetailDebug", "tripPrice: " + snapshot.child("tripPrice").getValue(String::class.java))
                    Log.d("TripDetailDebug", "hotel: " + snapshot.child("hotel").value)
                    Log.d("TripDetailDebug", "selectedHotels: " + snapshot.child("selectedHotels").value)
                    // --- Price ---
                    val pricePerPersonValue = snapshot.child("pricePerPerson").value
                    val priceValue = snapshot.child("price").value
                    val tripPriceValue = snapshot.child("tripPrice").value
                    
                    val priceString = when {
                        pricePerPersonValue != null -> when (pricePerPersonValue) {
                            is String -> pricePerPersonValue
                            is Number -> pricePerPersonValue.toString()
                            else -> ""
                        }
                        priceValue != null -> when (priceValue) {
                            is String -> priceValue
                            is Number -> priceValue.toString()
                            else -> ""
                        }
                        tripPriceValue != null -> when (tripPriceValue) {
                            is String -> tripPriceValue
                            is Number -> tripPriceValue.toString()
                            else -> ""
                        }
                        else -> ""
                    }
                    val priceDouble = priceString.replace(",", "").toDoubleOrNull() ?: 0.0
                    val tripPriceTextView = findViewById<TextView?>(R.id.tripPriceTextView)
                    if (priceDouble > 0.0) {
                        tripPriceTextView?.text = com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils.formatAsPKR(priceDouble)
                        tripPriceTextView?.visibility = View.VISIBLE
                    } else {
                        tripPriceTextView?.text = "Price not set"
                        tripPriceTextView?.visibility = View.VISIBLE
                    }
                    // --- Hotel ---
                    val hotelSnapshot = snapshot.child("hotel")
                    val hotelCard = findViewById<androidx.cardview.widget.CardView?>(R.id.hotelCard)
                    val hotelNameTextView = findViewById<TextView?>(R.id.hotelNameTextView)
                    val hotelPriceTextView = findViewById<TextView?>(R.id.hotelPriceTextView)
                    val hotelAmenitiesTextView = findViewById<TextView?>(R.id.hotelAmenitiesTextView)
                    val hotelImageView = findViewById<ImageView?>(R.id.hotelImageView)
                    if (hotelSnapshot.exists()) {
                        val hasHotelData = hotelSnapshot.child("name").value != null || hotelSnapshot.child("pricePerNight").value != null || hotelSnapshot.child("price").value != null
                        if (hasHotelData) {
                            hotelCard?.visibility = View.VISIBLE
                            Log.d("TripDetailDebug", "Hotel card set to VISIBLE (hotel)")
                            val hotelNameValue = hotelSnapshot.child("name").value
                            val hotelName = when (hotelNameValue) {
                                is String -> hotelNameValue
                                is Number -> hotelNameValue.toString()
                                else -> "Hotel not set"
                            }
                            val pricePerNightValue = hotelSnapshot.child("pricePerNight").value ?: hotelSnapshot.child("price").value
                            val pricePerNightString = when (pricePerNightValue) {
                                is String -> pricePerNightValue
                                is Number -> pricePerNightValue.toString()
                                else -> ""
                            }
                            val pricePerNightDouble = pricePerNightString.replace(",", "").toDoubleOrNull() ?: 0.0
                            val hotelImageUrlValue = hotelSnapshot.child("imageUrl").value
                            val hotelImageUrl = when (hotelImageUrlValue) {
                                is String -> hotelImageUrlValue
                                is Number -> hotelImageUrlValue.toString()
                                else -> ""
                            }
                            val hotelImageNameValue = hotelSnapshot.child("imageName").value
                            val hotelImageName = when (hotelImageNameValue) {
                                is String -> hotelImageNameValue
                                is Number -> hotelImageNameValue.toString()
                                else -> ""
                            }
                            val hotelDescriptionValue = hotelSnapshot.child("description").value
                            val hotelDescription = when (hotelDescriptionValue) {
                                is String -> hotelDescriptionValue
                                is Number -> hotelDescriptionValue.toString()
                                else -> "Amenities: Not listed"
                            }
                            Log.d("TripDetailDebug", "Hotel object: name=$hotelName, pricePerNight=$pricePerNightDouble, imageUrl=$hotelImageUrl, imageName=$hotelImageName, description=$hotelDescription")
                            hotelNameTextView?.text = hotelName
                            hotelPriceTextView?.text = if (pricePerNightDouble > 0.0) "Price per night: "+com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils.formatAsPKR(pricePerNightDouble) else "Price per night: Not set"
                            hotelPriceTextView?.visibility = View.VISIBLE
                            hotelAmenitiesTextView?.text = hotelDescription
                            hotelAmenitiesTextView?.visibility = View.VISIBLE
                            if (hotelImageUrl.isNotEmpty()) {
                                Log.d("TripDetailDebug", "Loading hotel image from URL: $hotelImageUrl")
                                try {
                                    com.squareup.picasso.Picasso.get()
                                        .load(hotelImageUrl)
                                        .placeholder(R.drawable.ic_image_placeholder)
                                        .error(R.drawable.ic_image_placeholder)
                                        .fit()
                                        .centerCrop()
                                        .into(hotelImageView)
                                } catch (e: Exception) {
                                    Log.e("TripDetailDebug", "Failed to load hotel image from URL, using placeholder", e)
                                    hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                                }
                            } else if (hotelImageName.isNotEmpty() && hotelImageView != null) {
                                Log.d("TripDetailDebug", "Trying to load hotel image from drawable: $hotelImageName")
                                val loaded = loadImageFromDrawable(hotelImageName, hotelImageView)
                                Log.d("TripDetailDebug", "Tried loading drawable: $hotelImageName, success: $loaded")
                                if (!loaded) hotelImageView.setImageResource(R.drawable.ic_image_placeholder)
                            } else {
                                Log.d("TripDetailDebug", "No imageUrl or imageName, using placeholder")
                                hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                            }
                        } else {
                            hotelCard?.visibility = View.GONE
                            Log.d("TripDetailDebug", "Hotel card set to GONE (hotel)")
                        }
                    } else {
                        // Try to use selectedHotels
                        val selectedHotelsSnap = snapshot.child("selectedHotels")
                        Log.d("TripDetailDebug", "selectedHotelsSnap.exists(): ${selectedHotelsSnap.exists()}, childrenCount: ${selectedHotelsSnap.childrenCount}")
                        for (child in selectedHotelsSnap.children) {
                            Log.d("TripDetailDebug", "selectedHotels child key: ${child.key}, value: ${child.value}")
                        }
                        if (selectedHotelsSnap.exists() && selectedHotelsSnap.childrenCount > 0) {
                            var hotelFound = false
                            for (hotelSnap in selectedHotelsSnap.children) {
                                val hotelName = hotelSnap.child("name").getValue(String::class.java) ?: "Hotel not set"
                                val price = hotelSnap.child("price").getValue(String::class.java) ?: hotelSnap.child("pricePerNight").getValue(String::class.java) ?: ""
                                val rating = hotelSnap.child("rating").getValue(Double::class.java) ?: 0.0
                                val imageUrl = hotelSnap.child("imageUrl").getValue(String::class.java) ?: ""
                                val imageName = hotelSnap.child("imageName").getValue(String::class.java) ?: ""
                                val description = hotelSnap.child("description").getValue(String::class.java) ?: "Amenities: Not listed"
                                Log.d("TripDetailDebug", "Iterating hotel: name=$hotelName, price=$price, imageUrl=$imageUrl, imageName=$imageName, rating=$rating, description=$description")
                                // Show the first valid hotel and break
                                hotelNameTextView?.text = hotelName
                                hotelPriceTextView?.text = if (price.isNotEmpty()) "Price per night: ${com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils.formatAsPKR(price.toDoubleOrNull() ?: 0.0)}" else "Price not set"
                                hotelPriceTextView?.visibility = View.VISIBLE
                                hotelAmenitiesTextView?.text = description
                                hotelAmenitiesTextView?.visibility = View.VISIBLE
                                hotelCard?.visibility = View.VISIBLE
                                HotelImageLoader.loadHotelImage(imageUrl, imageName, hotelName, hotelImageView!!)
                                // Optionally set rating if you have a RatingBar
                                // hotelRatingBar?.rating = rating.toFloat()
                                hotelFound = true
                                break
                            }
                            if (!hotelFound) {
                                hotelCard?.visibility = View.GONE
                                Log.d("TripDetailDebug", "No valid hotel found in selectedHotels, hiding hotel card")
                            }
                        } else {
                            hotelCard?.visibility = View.GONE
                            Log.d("TripDetailDebug", "Hotel card set to GONE (no hotel data)")
                        }
                    }
                    // --- Itinerary ---
                    val itinerarySnap = snapshot.child("itinerary")
                    Log.d("TripDetailDebug", "itinerarySnap: ${itinerarySnap.value}")
                    val itineraryMap = mutableMapOf<String, List<ItineraryItem>>()
                    for (daySnap in itinerarySnap.children) {
                        val dayKey = daySnap.key ?: continue
                        val activities = daySnap.children.mapNotNull { it.getValue(String::class.java) }
                        // If activities are just strings, convert to ItineraryItem with title only
                        val items = activities.map { activityStr ->
                            // Try to parse as JSON if possible, else fallback to title only
                            try {
                                val obj = org.json.JSONObject(activityStr)
                                ItineraryItem(
                                    time = obj.optString("time", ""),
                                    title = obj.optString("title", activityStr),
                                    description = obj.optString("description", "")
                                )
                            } catch (e: Exception) {
                                ItineraryItem(title = activityStr)
                            }
                        }
                        if (items.isNotEmpty()) {
                            itineraryMap[dayKey] = items
                        }
                    }
                    val itinerarySectionTitle = findViewById<TextView>(R.id.itinerarySectionTitle)
                    val itineraryRecyclerView = findViewById<RecyclerView>(R.id.itineraryRecyclerView)
                    val itineraryAdapter = ItineraryAdapter()
                    itineraryRecyclerView.layoutManager = LinearLayoutManager(this@TripDetailActivity)
                    itineraryRecyclerView.adapter = itineraryAdapter
                    if (itineraryMap.isNotEmpty()) {
                        Log.d("TripDetailDebug", "Parsed itineraryMap: $itineraryMap")
                        itinerarySectionTitle.visibility = View.VISIBLE
                        itineraryRecyclerView.visibility = View.VISIBLE
                        itineraryAdapter.setItinerary(itineraryMap)
                        // Auto-expand all days
                        try {
                            val field = itineraryAdapter.javaClass.getDeclaredField("expandedDays")
                            field.isAccessible = true
                            field.set(itineraryAdapter, itineraryMap.keys.toMutableSet())
                            itineraryAdapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                            Log.e("TripDetailDebug", "Failed to auto-expand days: ${e.message}")
                        }
                    } else {
                        itinerarySectionTitle.visibility = View.VISIBLE
                        itineraryRecyclerView.visibility = View.VISIBLE
                        itineraryAdapter.setItinerary(emptyMap()) // triggers empty state
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    val tripPriceTextView = findViewById<TextView?>(R.id.tripPriceTextView)
                    tripPriceTextView?.text = "Price not set"
                    tripPriceTextView?.visibility = View.VISIBLE
                    val hotelCard = findViewById<androidx.cardview.widget.CardView?>(R.id.hotelCard)
                    val hotelNameTextView = findViewById<TextView?>(R.id.hotelNameTextView)
                    val hotelPriceTextView = findViewById<TextView?>(R.id.hotelPriceTextView)
                    val hotelAmenitiesTextView = findViewById<TextView?>(R.id.hotelAmenitiesTextView)
                    val hotelImageView = findViewById<ImageView?>(R.id.hotelImageView)
                    hotelCard?.visibility = View.VISIBLE
                    hotelNameTextView?.text = "Hotel not set"
                    hotelPriceTextView?.text = "Price per night: Not set"
                    hotelPriceTextView?.visibility = View.VISIBLE
                    hotelAmenitiesTextView?.text = "Amenities: Not listed"
                    hotelAmenitiesTextView?.visibility = View.VISIBLE
                    hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                }
            })
        }

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

        // Setup booking button
        setupBookingButton(placeName, organizerName, organizerPhone, startDate, endDate, seatsAvailable)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = placeName

        // --- Map Section with LocationIQ ---
        // Remove all code that references directionsButton and mapImageView, including findViewById, click listeners, and related logic.

        // Setup Nearby Attractions Map WebView
        mapWebView = findViewById(R.id.nearbyMapWebView)
        // Geocode place name for accurate map location
        geocodePlaceAndLoadMap(placeName)

        // Setup Get Directions button below the map
        val getDirectionsButton = findViewById<MaterialButton>(R.id.getDirectionsButton)
        getDirectionsButton.setOnClickListener {
            val lat = mapLat
            val lon = mapLon
            val gmmIntentUri = android.net.Uri.parse("google.navigation:q=$lat,$lon")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to generic maps app
                val fallbackUri = android.net.Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "No maps app available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Setup Nearby Attractions RecyclerView
        val nearbyRecyclerView = findViewById<RecyclerView>(R.id.nearbyAttractionsRecyclerView)
        nearbyAttractionAdapter = NearbyAttractionAdapter(emptyList(),
            onItemClick = { place, idx ->
                // Only highlight marker on map
                mapWebView?.evaluateJavascript("mapHighlightMarker($idx);", null)
            },
            onDirectionsClick = { place, idx ->
                // Open directions to the attraction
                if (place.latitude != null && place.longitude != null && place.latitude != 0.0 && place.longitude != 0.0) {
                    val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${place.latitude},${place.longitude}(${place.name})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        // Fallback to generic maps app
                        val fallbackUri = android.net.Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${place.name})")
                        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                        if (fallbackIntent.resolveActivity(packageManager) != null) {
                            startActivity(fallbackIntent)
                        } else {
                            Toast.makeText(this, "No maps app available", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Location not available for this place", Toast.LENGTH_SHORT).show()
                }
            }
        )
        nearbyRecyclerView.layoutManager = LinearLayoutManager(this)
        nearbyRecyclerView.adapter = nearbyAttractionAdapter
        nearbyRecyclerView.isNestedScrollingEnabled = true

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

        // Budget Estimator Button
        val budgetEstimatorButton = findViewById<MaterialButton>(R.id.budgetEstimatorButton)
        budgetEstimatorButton.setOnClickListener {
            showBudgetEstimatorDialog()
        }
        fetchCurrencyRates()
    }

    private fun loadTripPrice(tripId: String, tripPriceTextView: TextView?) {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        tripRef.child("pricePerPerson").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val priceValue = snapshot.value
                val priceString = when (priceValue) {
                    is String -> priceValue
                    is Number -> priceValue.toString()
                    else -> null
                }
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
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        tripRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // --- Try hotel field first ---
                val hotelSnapshot = snapshot.child("hotel")
                val hotelNameValue = hotelSnapshot.child("name").value
                val hotelName = when (hotelNameValue) {
                    is String -> hotelNameValue
                    is Number -> hotelNameValue.toString()
                    else -> null
                }
                val pricePerNightValue = hotelSnapshot.child("pricePerNight").value
                val pricePerNight = when (pricePerNightValue) {
                    is Number -> pricePerNightValue.toDouble().toString()
                    is String -> pricePerNightValue
                    else -> ""
                }
                val imageUrlValue = hotelSnapshot.child("imageUrl").value
                val imageUrl = when (imageUrlValue) {
                    is String -> imageUrlValue
                    is Number -> imageUrlValue.toString()
                    else -> null
                }
                val imageNameValue = hotelSnapshot.child("imageName").value
                val imageName = when (imageNameValue) {
                    is String -> imageNameValue
                    is Number -> imageNameValue.toString()
                    else -> null
                }
                val descriptionValue = hotelSnapshot.child("description").value
                val description = when (descriptionValue) {
                    is String -> descriptionValue
                    is Number -> descriptionValue.toString()
                    else -> null
                }
                val amenitiesList = hotelSnapshot.child("amenities").children.mapNotNull { it.getValue(String::class.java) }
                val reserved = hotelSnapshot.child("reserved").getValue(Boolean::class.java) ?: false
                val hasHotelData = !hotelName.isNullOrBlank() || !pricePerNight.isNullOrBlank() || !imageUrl.isNullOrBlank() || amenitiesList.isNotEmpty()

                if (hasHotelData) {
                    hotelCard?.visibility = View.VISIBLE
                    hotelNameTextView?.text = hotelName ?: "Hotel"
                    hotelNameTextView?.visibility = View.VISIBLE
                    if (!pricePerNight.isNullOrBlank()) {
                        hotelPriceTextView?.text = "Price per night: ${CurrencyUtils.formatAsPKR(pricePerNight)}"
                    } else {
                        hotelPriceTextView?.text = "Price not set"
                    }
                    hotelPriceTextView?.visibility = View.VISIBLE
                    if (amenitiesList.isNotEmpty()) {
                        hotelAmenitiesTextView?.text = "Amenities: ${amenitiesList.joinToString(", ")}"
                    } else if (!description.isNullOrBlank()) {
                        hotelAmenitiesTextView?.text = description
                    } else {
                        hotelAmenitiesTextView?.text = "Amenities: Not listed"
                    }
                    hotelAmenitiesTextView?.visibility = View.VISIBLE
                    if (!imageUrl.isNullOrBlank()) {
                        HotelImageLoader.loadHotelImage(imageUrl, imageName, hotelName, hotelImageView!!)
                    } else if (!imageName.isNullOrBlank()) {
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
                            hotelSnapshot.ref.child("reserved").setValue(true).addOnSuccessListener {
                                reservedBadge?.visibility = View.VISIBLE
                                reserveHotelButton.visibility = View.GONE
                                Toast.makeText(this@TripDetailActivity, "Hotel reserved successfully!", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this@TripDetailActivity, "Failed to reserve hotel.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    return
                }

                // --- Fallback: Try selectedHotels ---
                val selectedHotelsSnap = snapshot.child("selectedHotels")
                if (selectedHotelsSnap.exists() && selectedHotelsSnap.childrenCount > 0) {
                    var hotelFound = false
                    for (hotelSnap in selectedHotelsSnap.children) {
                        val sHotelName = hotelSnap.child("name").getValue(String::class.java) ?: "Hotel not set"
                        val sPrice = hotelSnap.child("price").getValue(String::class.java) ?: hotelSnap.child("pricePerNight").getValue(String::class.java) ?: ""
                        val sRating = hotelSnap.child("rating").getValue(Double::class.java) ?: 0.0
                        val sImageUrl = hotelSnap.child("imageUrl").getValue(String::class.java) ?: ""
                        val sImageName = hotelSnap.child("imageName").getValue(String::class.java) ?: ""
                        val sDescription = hotelSnap.child("description").getValue(String::class.java) ?: "Amenities: Not listed"
                        val sAmenitiesList = hotelSnap.child("amenities").children.mapNotNull { it.getValue(String::class.java) }
                        // Show the first valid hotel and break
                        hotelNameTextView?.text = sHotelName
                        hotelPriceTextView?.text = if (sPrice.isNotEmpty()) "Price per night: ${CurrencyUtils.formatAsPKR(sPrice.toDoubleOrNull() ?: 0.0)}" else "Price not set"
                        hotelPriceTextView?.visibility = View.VISIBLE
                        if (sAmenitiesList.isNotEmpty()) {
                            hotelAmenitiesTextView?.text = "Amenities: ${sAmenitiesList.joinToString(", ")}"
                        } else if (!sDescription.isNullOrBlank()) {
                            hotelAmenitiesTextView?.text = sDescription
                        } else {
                            hotelAmenitiesTextView?.text = "Amenities: Not listed"
                        }
                        hotelAmenitiesTextView?.visibility = View.VISIBLE
                        hotelCard?.visibility = View.VISIBLE
                        HotelImageLoader.loadHotelImage(sImageUrl, sImageName, sHotelName, hotelImageView!!)
                        hotelFound = true
                        break
                    }
                    if (!hotelFound) {
                        hotelCard?.visibility = View.VISIBLE
                        hotelNameTextView?.text = "No hotel selected for this trip"
                        hotelPriceTextView?.visibility = View.GONE
                        hotelAmenitiesTextView?.visibility = View.GONE
                        hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                        reserveHotelButton?.visibility = View.GONE
                        reservedBadge?.visibility = View.GONE
                    }
                } else {
                    hotelCard?.visibility = View.VISIBLE
                    hotelNameTextView?.text = "No hotel selected for this trip"
                    hotelPriceTextView?.visibility = View.GONE
                    hotelAmenitiesTextView?.visibility = View.GONE
                    hotelImageView?.setImageResource(R.drawable.ic_image_placeholder)
                    reserveHotelButton?.visibility = View.GONE
                    reservedBadge?.visibility = View.GONE
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

    private fun showBudgetEstimatorDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget_estimator, null)
        val travelersInput = dialogView.findViewById<TextInputEditText>(R.id.inputTravelers)
        val daysInput = dialogView.findViewById<TextInputEditText>(R.id.inputDays)
        val chipGroupTripType = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupTripType)
        val chipBudget = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipBudget)
        val chipModerate = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipModerate)
        val chipLuxury = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipLuxury)
        val tripTypeDescription = dialogView.findViewById<TextView>(R.id.tripTypeDescription)
        val currencySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCurrency)
        val resultText = dialogView.findViewById<TextView>(R.id.estimatedBudgetResult)
        val btnEstimate = dialogView.findViewById<MaterialButton>(R.id.btnEstimate)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)
        val breakdownCard = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.budgetBreakdownCard)
        val breakdownDetails = dialogView.findViewById<TextView>(R.id.breakdownDetails)

        // Trip type info
        val tripTypeInfo = mapOf(
            chipBudget.id to "Budget: Basic hotels, public transport, local meals.",
            chipModerate.id to "Moderate: 3-star hotels, some private transport, mix of local and restaurant meals.",
            chipLuxury.id to "Luxury: 4-5 star hotels, private transport, fine dining, premium experiences."
        )
        chipGroupTripType.setOnCheckedChangeListener { group, checkedId ->
            tripTypeDescription.text = tripTypeInfo[checkedId] ?: "Select a trip type to see details."
        }
        chipBudget.isChecked = true
        tripTypeDescription.text = tripTypeInfo[chipBudget.id]

        // Currency options
        val currencies = listOf("PKR", "USD", "EUR")
        currencySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnEstimate.setOnClickListener {
            val travelers = travelersInput.text.toString().toIntOrNull() ?: 1
            val days = daysInput.text.toString().toIntOrNull() ?: 1
            val tripType = when (chipGroupTripType.checkedChipId) {
                chipBudget.id -> "Budget"
                chipModerate.id -> "Moderate"
                chipLuxury.id -> "Luxury"
                else -> "Budget"
            }
            val currency = currencySpinner.selectedItem.toString()
            val hotelPrice = getSelectedHotelPrice()
            val (meals, transport, experience) = getAddOns(tripType, days, travelers)
            val totalPKR = (hotelPrice * days * travelers) + meals + transport + experience
            // Show breakdown
            val breakdown = "Hotel: ${CurrencyUtils.formatAsPKR(hotelPrice * days * travelers)}\n" +
                    "Meals: ${CurrencyUtils.formatAsPKR(meals)}\n" +
                    "Transport: ${CurrencyUtils.formatAsPKR(transport)}\n" +
                    "Experiences: ${CurrencyUtils.formatAsPKR(experience)}"
            breakdownDetails.text = breakdown
            breakdownCard.visibility = View.VISIBLE
            convertAndDisplayBudget(totalPKR, travelers, days, currency, resultText)
        }
        dialog.show()
    }

    private fun getBaseTripPrice(): Double {
        // Try to get from loaded trip price, fallback to 5000
        val tripPriceTextView = findViewById<TextView?>(R.id.tripPriceTextView)
        val priceText = tripPriceTextView?.text?.toString()?.replace("Rs.", "")?.replace(",", "")?.trim() ?: "5000"
        return priceText.toDoubleOrNull() ?: 5000.0
    }

    private fun getSelectedHotelPrice(): Double {
        // Try to get from hotel price text view, fallback to 5000
        val hotelPriceTextView = findViewById<TextView?>(R.id.hotelPriceTextView)
        val priceText = hotelPriceTextView?.text?.toString()
        // Expecting format: "Price per night: Rs. 5,000" or "Price per night: Not set"
        val regex = Regex("[\\d,]+(?:\\.\\d+)?")
        val match = priceText?.let { regex.find(it.replace(",", "")) }
        val price = match?.value?.replace(",", "")?.toDoubleOrNull()
        return price ?: 5000.0
    }

    private fun getAddOns(tripType: String, days: Int, travelers: Int): Triple<Double, Double, Double> {
        // Example values, adjust as needed
        val mealPerDay = when (tripType) {
            "Budget" -> 600.0
            "Moderate" -> 1200.0
            "Luxury" -> 2500.0
            else -> 1000.0
        }
        val transportPerDay = when (tripType) {
            "Budget" -> 800.0
            "Moderate" -> 2000.0
            "Luxury" -> 4000.0
            else -> 1500.0
        }
        val experiencePerTrip = when (tripType) {
            "Budget" -> 1000.0
            "Moderate" -> 3000.0
            "Luxury" -> 8000.0
            else -> 2000.0
        }
        val meals = mealPerDay * days * travelers
        val transport = transportPerDay * days
        val experience = experiencePerTrip * travelers
        return Triple(meals, transport, experience)
    }

    private fun fetchCurrencyRates() {
        executor.execute {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(currencyApiUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val rates = json.getJSONObject("conversion_rates")
                    latestRates = mapOf(
                        "PKR" to 1.0,
                        "USD" to rates.optDouble("USD", 0.0),
                        "EUR" to rates.optDouble("EUR", 0.0)
                    )
                }
            } catch (e: Exception) {
                Log.e("BudgetEstimator", "Failed to fetch currency rates: ${e.message}")
            }
        }
    }

    private fun convertAndDisplayBudget(totalPKR: Double, travelers: Int, days: Int, currency: String, resultText: TextView) {
        val rates = latestRates ?: mapOf("PKR" to 1.0, "USD" to 0.0036, "EUR" to 0.0033)
        val rate = rates[currency] ?: 1.0
        val converted = totalPKR * rate
        val formatted = CurrencyUtils.format(converted, currency)
        runOnUiThread {
            resultText.text = "Estimated Cost: $formatted (for $travelers travelers, $days days)"
        }
    }

    private fun geocodePlaceAndLoadMap(placeName: String) {
        val locationIqToken = "pk.1ef4faca32216233742e487f2372c924"
        val encodedLocation = java.net.URLEncoder.encode(placeName, "UTF-8")
        val geocodeUrl = "https://us1.locationiq.com/v1/search.php?key=$locationIqToken&q=$encodedLocation&format=json"
        val client = OkHttpClient()
        val request = Request.Builder().url(geocodeUrl).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread { loadMapWebView(mapLat, mapLon) }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    try {
                        val jsonArray = JSONArray(body)
                        if (jsonArray.length() > 0) {
                            val obj = jsonArray.getJSONObject(0)
                            mapLat = obj.getString("lat").toDoubleOrNull() ?: mapLat
                            mapLon = obj.getString("lon").toDoubleOrNull() ?: mapLon
                        }
                    } catch (_: Exception) {}
                }
                runOnUiThread { loadMapWebView(mapLat, mapLon) }
            }
        })
    }

    private fun loadMapWebView(lat: Double, lon: Double) {
        mapWebView?.settings?.javaScriptEnabled = true
        mapWebView?.settings?.domStorageEnabled = true
        mapWebView?.settings?.setSupportZoom(true)
        mapWebView?.settings?.builtInZoomControls = true
        mapWebView?.settings?.displayZoomControls = false
        mapWebView?.settings?.useWideViewPort = true
        mapWebView?.settings?.loadWithOverviewMode = true
        mapWebView?.webViewClient = WebViewClient()
        mapWebView?.addJavascriptInterface(object {
            @JavascriptInterface
            fun getTripLatLng(): String {
                return "{" + "\"lat\":" + lat + ",\"lon\":" + lon + "}"
            }
            @JavascriptInterface
            fun setNearbyAttractions(json: String) {
                try {
                    val arr = JSONArray(json)
                    val list = mutableListOf<Place>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            Place(
                                name = obj.optString("name"),
                                description = obj.optString("description"),
                                imageUrl = obj.optString("imageUrl"),
                                type = obj.optString("type"),
                                distance = obj.optDouble("distance", 0.0),
                                address = obj.optString("address"),
                                latitude = obj.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                                longitude = obj.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                            )
                        )
                    }
                    runOnUiThread {
                        nearbyAttractions = list
                        nearbyAttractionAdapter.updateData(list)
                    }
                } catch (e: Exception) {
                    Log.e("TripDetailActivity", "Failed to parse nearby attractions: ${e.message}")
                }
            }
        }, "AndroidBridge")
        mapWebView?.loadUrl("file:///android_asset/nearby_map.html")
    }
}
