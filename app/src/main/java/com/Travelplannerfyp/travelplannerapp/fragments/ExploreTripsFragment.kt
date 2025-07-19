package com.Travelplannerfyp.travelplannerapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.TripDetailActivity
import com.Travelplannerfyp.travelplannerapp.adapters.EnhancedTripAdapter
import com.Travelplannerfyp.travelplannerapp.models.EnhancedTrip
import com.Travelplannerfyp.travelplannerapp.models.TripVisibility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Log

class ExploreTripsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: EnhancedTripAdapter
    private val trips = mutableListOf<EnhancedTrip>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerViewExploreTrips)
        emptyView = view.findViewById(R.id.emptyViewExploreTrips)
        progressBar = view.findViewById(R.id.progressBarExploreTrips)

        // DEBUG: Update hotel data for a specific trip (remove after running once)
        updateHotelDataForTrip("-OV5NOip3GaR93mk4-Pg")

        setupRecyclerView()
        loadExploreTrips()
    }

    // DEBUG: Function to update hotel data for a trip
    private fun updateHotelDataForTrip(tripId: String) {
        val hotelData = mapOf(
            "name" to "PC Bhurban",
            "pricePerNight" to "11000",
            "imageUrl" to "https://upload.wikimedia.org/wikipedia/commons/2/2e/PC_Bhurban_Hotel.jpg",
            "description" to "A luxury hotel in Murree with panoramic views and excellent amenities."
        )
        // Update under 'hotel'
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("trips")
            .child(tripId)
            .child("hotel")
            .setValue(hotelData)
            .addOnSuccessListener {
                android.util.Log.d("FirebaseUpdate", "Hotel data updated successfully!")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirebaseUpdate", "Failed to update hotel data: ${e.message}")
            }
        // Optionally, also update the first hotel in 'selectedHotels'
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("trips")
            .child(tripId)
            .child("selectedHotels")
            .child("0")
            .setValue(hotelData)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = EnhancedTripAdapter(
            trips = trips,
            currentUserId = currentUserId,
            onTripClick = { trip -> handleTripClick(trip) },
            onJoinTrip = { trip -> handleJoinTrip(trip) }
        )
        recyclerView.adapter = adapter
    }

    private fun loadExploreTrips() {
        showLoading(true)
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trips.clear()
                
                for (tripSnapshot in snapshot.children) {
                    val trip = createEnhancedTripFromSnapshot(tripSnapshot)
                    
                    // Only show public trips that the user hasn't created
                    if (trip.visibility == TripVisibility.PUBLIC && trip.organizerId != currentUserId) {
                        trips.add(trip)
                    }
                }
                
                showLoading(false)
                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showEmptyView("Failed to load trips: ${error.message}")
            }
        })
    }

    private fun createEnhancedTripFromSnapshot(snapshot: DataSnapshot): EnhancedTrip {
        val tripId = snapshot.key ?: ""
        val placeName = snapshot.child("placeName").getValue(String::class.java) ?: ""
        val placeDescription = snapshot.child("placeDescription").getValue(String::class.java) ?: ""
        val tripDescription = snapshot.child("tripDescription").getValue(String::class.java) ?: ""
        val organizerName = snapshot.child("organizerName").getValue(String::class.java) ?: ""
        val organizerPhone = snapshot.child("organizerPhone").getValue(String::class.java) ?: ""
        val organizerId = snapshot.child("organizerId").getValue(String::class.java) ?: ""
        val startDate = snapshot.child("startDate").getValue(String::class.java) ?: ""
        val endDate = snapshot.child("endDate").getValue(String::class.java) ?: ""
        val seatsAvailable = snapshot.child("seatsAvailable").getValue(String::class.java)?.toIntOrNull() ?: 0
        val placeImageUrl = snapshot.child("placeImageUrl").getValue(String::class.java) ?: ""
        val visibility = snapshot.child("visibility").getValue(String::class.java)?.let {
            if (it == "PRIVATE") com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PRIVATE
            else com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PUBLIC
        } ?: com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PUBLIC

        // --- FIX: Robust price parsing, now also supports 'tripPrice' ---
        val pricePerPersonValue = snapshot.child("pricePerPerson").value
        val priceValue = snapshot.child("price").value
        val tripPriceValue = snapshot.child("tripPrice").value
        
        val price = when {
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

        // --- FIX: Robust hotel parsing, now also supports 'selectedHotels' ---
        val hotelSnapshot = snapshot.child("hotel")
        val hotel = if (hotelSnapshot.exists()) {
            try {
                // Robust parsing for hotel name - handle both String and Number types
                val hotelNameValue = hotelSnapshot.child("name").value
                val hotelName = when (hotelNameValue) {
                    is String -> hotelNameValue
                    is Number -> hotelNameValue.toString()
                    else -> ""
                }
                
                // Robust parsing for hotel description - handle both String and Number types
                val hotelDescriptionValue = hotelSnapshot.child("description").value
                val hotelDescription = when (hotelDescriptionValue) {
                    is String -> hotelDescriptionValue
                    is Number -> hotelDescriptionValue.toString()
                    else -> ""
                }
                
                val hotelRating = when (val ratingVal = hotelSnapshot.child("rating").value) {
                    is Number -> ratingVal.toDouble()
                    is String -> ratingVal.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                val pricePerNight = when (val priceVal = hotelSnapshot.child("pricePerNight").value ?: hotelSnapshot.child("price").value) {
                    is Number -> priceVal.toString()
                    is String -> priceVal
                    else -> ""
                }
                
                // Robust parsing for hotel image name - handle both String and Number types
                val hotelImageNameValue = hotelSnapshot.child("imageName").value
                val hotelImageName = when (hotelImageNameValue) {
                    is String -> hotelImageNameValue
                    is Number -> hotelImageNameValue.toString()
                    else -> ""
                }
                
                // Robust parsing for hotel image URL - handle both String and Number types
                val hotelImageUrlValue = hotelSnapshot.child("imageUrl").value
                val hotelImageUrl = when (hotelImageUrlValue) {
                    is String -> hotelImageUrlValue
                    is Number -> hotelImageUrlValue.toString()
                    else -> ""
                }
                
                val amenitiesList = hotelSnapshot.child("amenities").children.mapNotNull { it.getValue(String::class.java) }
                com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = hotelName.ifEmpty { "Hotel data unavailable" },
                    description = hotelDescription.ifEmpty { "Hotel information could not be loaded. Please contact the organizer." },
                    rating = hotelRating,
                    pricePerNight = pricePerNight ?: "",
                    imageName = hotelImageName,
                    imageUrl = hotelImageUrl,
                    amenities = amenitiesList
                )
            } catch (e: Exception) {
                android.util.Log.e("ExploreTripsFragment", "Error parsing hotel data: ", e)
                com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = "Hotel data unavailable",
                    description = "Hotel information could not be loaded. Please contact the organizer.",
                    rating = 0.0,
                    pricePerNight = "",
                    imageName = "",
                    imageUrl = "",
                    amenities = emptyList()
                )
            }
        } else {
            // Try to parse from selectedHotels if hotel is missing
            val selectedHotelsSnap = snapshot.child("selectedHotels")
            Log.d("ExploreTripsFragment", "selectedHotels exists: ${selectedHotelsSnap.exists()}, children count: ${selectedHotelsSnap.childrenCount}")
            
            if (selectedHotelsSnap.exists() && selectedHotelsSnap.childrenCount > 0) {
                val firstHotel = selectedHotelsSnap.children.iterator().next()
                Log.d("ExploreTripsFragment", "First hotel key: ${firstHotel.key}")
                Log.d("ExploreTripsFragment", "First hotel name value: ${firstHotel.child("name").value}")
                Log.d("ExploreTripsFragment", "First hotel price value: ${firstHotel.child("price").value}")
                
                // Robust parsing for hotel name from selectedHotels - handle both String and Number types
                val hotelNameValue = firstHotel.child("name").value
                val hotelName = when (hotelNameValue) {
                    is String -> hotelNameValue
                    is Number -> hotelNameValue.toString()
                    else -> ""
                }
                Log.d("ExploreTripsFragment", "Parsed hotel name: $hotelName")
                
                // Robust parsing for hotel description from selectedHotels - handle both String and Number types
                val hotelDescriptionValue = firstHotel.child("description").value
                val hotelDescription = when (hotelDescriptionValue) {
                    is String -> hotelDescriptionValue
                    is Number -> hotelDescriptionValue.toString()
                    else -> ""
                }
                
                val hotelRating = when (val ratingVal = firstHotel.child("rating").value) {
                    is Number -> ratingVal.toDouble()
                    is String -> ratingVal.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                val pricePerNight = when (val priceVal = firstHotel.child("pricePerNight").value ?: firstHotel.child("price").value) {
                    is Number -> priceVal.toString()
                    is String -> priceVal
                    else -> ""
                }
                
                // Robust parsing for hotel image name from selectedHotels - handle both String and Number types
                val hotelImageNameValue = firstHotel.child("imageName").value
                val hotelImageName = when (hotelImageNameValue) {
                    is String -> hotelImageNameValue
                    is Number -> hotelImageNameValue.toString()
                    else -> ""
                }
                
                // Robust parsing for hotel image URL from selectedHotels - handle both String and Number types
                val hotelImageUrlValue = firstHotel.child("imageUrl").value
                val hotelImageUrl = when (hotelImageUrlValue) {
                    is String -> hotelImageUrlValue
                    is Number -> hotelImageUrlValue.toString()
                    else -> ""
                }
                
                val amenitiesList = firstHotel.child("amenities").children.mapNotNull { it.getValue(String::class.java) }
                val hotel = com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = hotelName.ifEmpty { "Hotel data unavailable" },
                    description = hotelDescription.ifEmpty { "Hotel information could not be loaded. Please contact the organizer." },
                    rating = hotelRating,
                    pricePerNight = pricePerNight ?: "",
                    imageName = hotelImageName,
                    imageUrl = hotelImageUrl,
                    amenities = amenitiesList
                )
                Log.d("ExploreTripsFragment", "Created hotel object: $hotel")
                hotel
            } else null
        }

        // Get joined users
        val joinedUsers = mutableListOf<String>()
        val joinedUsersSnapshot = snapshot.child("joinedUsers")
        for (userSnapshot in joinedUsersSnapshot.children) {
            userSnapshot.key?.let { joinedUsers.add(it) }
        }

        val trip = com.Travelplannerfyp.travelplannerapp.models.EnhancedTrip(
            id = tripId,
            placeName = placeName,
            placeDescription = placeDescription,
            tripDescription = tripDescription,
            organizerName = organizerName,
            organizerPhone = organizerPhone,
            organizerId = organizerId,
            startDate = startDate,
            endDate = endDate,
            seatsAvailable = seatsAvailable,
            placeImageUrl = placeImageUrl,
            visibility = visibility,
            joinedUsers = joinedUsers,
            price = price,
            hotel = hotel
        )
        Log.d("TripCardDebug", "TripId: $tripId, price: $price")
        Log.d("TripDebug", "TripId: $tripId, price: $price, hotel: $hotel")
        Log.d("ExploreTripsFragment", "Created trip with hotel: ${trip.hotel?.name ?: "null"}")
        return trip
    }

    private fun handleTripClick(trip: EnhancedTrip) {
        // Navigate to trip details
        val intent = Intent(context, TripDetailActivity::class.java).apply {
            putExtra("placeName", trip.placeName)
            putExtra("placeDescription", trip.placeDescription)
            putExtra("tripDescription", trip.tripDescription)
            putExtra("organizerName", trip.organizerName)
            putExtra("organizerPhone", trip.organizerPhone)
            putExtra("startDate", trip.startDate)
            putExtra("endDate", trip.endDate)
            putExtra("seatsAvailable", trip.seatsAvailable.toString())
            putExtra("placeImageUrl", trip.placeImageUrl)
            putExtra("tripId", trip.id)
            // Pass coordinates
            putExtra("latitude", trip.latitude ?: 33.6844)
            putExtra("longitude", trip.longitude ?: 73.0479)
        }
        startActivity(intent)
    }

    private fun handleJoinTrip(trip: EnhancedTrip) {
        if (currentUserId.isEmpty()) {
            context?.let {
                Toast.makeText(it, "Please log in to join trips", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Add user to joined users list
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
        val tripRef = tripsRef.child(trip.id)
        
        // Check if user is already joined
        if (trip.joinedUsers.contains(currentUserId)) {
            context?.let {
                Toast.makeText(it, "You have already joined this trip", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Add user to joined users
        tripRef.child("joinedUsers").child(currentUserId).setValue(true)
            .addOnSuccessListener {
                context?.let {
                    Toast.makeText(it, "Successfully joined trip: ${trip.placeName}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                context?.let {
                    Toast.makeText(it, "Failed to join trip: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(message: String) {
        emptyView.text = message
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun updateUI() {
        if (trips.isEmpty()) {
            showEmptyView("No public trips available.\nCheck back later for new trips!")
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }
} 