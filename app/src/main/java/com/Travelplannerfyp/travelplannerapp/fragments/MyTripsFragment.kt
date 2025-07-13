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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyTripsFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_my_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerViewMyTrips)
        emptyView = view.findViewById(R.id.emptyViewMyTrips)
        progressBar = view.findViewById(R.id.progressBarMyTrips)

        setupRecyclerView()
        loadMyTrips()
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

    private fun loadMyTrips() {
        if (currentUserId.isEmpty()) {
            showEmptyView("Please log in to view your trips")
            return
        }

        showLoading(true)
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trips.clear()
                
                for (tripSnapshot in snapshot.children) {
                    val trip = createEnhancedTripFromSnapshot(tripSnapshot)
                    
                    // Include trips that the user created or joined
                    if (trip.organizerId == currentUserId || trip.joinedUsers.contains(currentUserId)) {
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

    // Helper to robustly get a Double from a DataSnapshot (handles Number, String, etc. without throwing)
    private fun getDoubleFromSnapshot(snapshot: DataSnapshot): Double? {
        val value = snapshot.value
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
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

        // NEW FIELDS FOR MISSION-CRITICAL FIXES
        val price = snapshot.child("pricePerPerson").getValue(String::class.java)
            ?: snapshot.child("price").getValue(String::class.java) ?: ""
        val reservation = snapshot.child("reservation").getValue(Boolean::class.java) ?: false
        
        // Load hotel information
        val hotelSnapshot = snapshot.child("hotel")
        val hotel = if (hotelSnapshot.exists()) {
            try {
                val hotelName = hotelSnapshot.child("name").getValue(String::class.java) ?: ""
                val hotelDescription = hotelSnapshot.child("description").getValue(String::class.java) ?: ""
                val hotelRating = getDoubleFromSnapshot(hotelSnapshot.child("rating")) ?: 0.0
                val pricePerNight = getDoubleFromSnapshot(hotelSnapshot.child("pricePerNight"))
                    ?: getDoubleFromSnapshot(hotelSnapshot.child("price"))
                val pricePerNightStr = pricePerNight?.toString() ?: ""
                val hotelImageName = hotelSnapshot.child("imageName").getValue(String::class.java) ?: ""
                val hotelImageUrl = hotelSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = hotelName.ifEmpty { "Hotel data unavailable" },
                    description = hotelDescription.ifEmpty { "Hotel information could not be loaded. Please contact the organizer." },
                    rating = hotelRating,
                    pricePerNight = pricePerNightStr,
                    imageName = hotelImageName,
                    imageUrl = hotelImageUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("MyTripsFragment", "Error parsing hotel data: ${e.message}", e)
                com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = "Hotel data unavailable",
                    description = "Hotel information could not be loaded. Please contact the organizer.",
                    rating = 0.0,
                    pricePerNight = "",
                    imageName = "",
                    imageUrl = ""
                )
            }
        } else null

        // Load itinerary information
        val itinerarySnapshot = snapshot.child("itinerary")
        val itinerary = mutableMapOf<String, List<String>>()
        for (daySnapshot in itinerarySnapshot.children) {
            val dayKey = daySnapshot.key ?: continue
            val activities = daySnapshot.children.mapNotNull { it.getValue(String::class.java) }
            if (activities.isNotEmpty()) {
                itinerary[dayKey] = activities
            }
        }

        // Get joined users
        val joinedUsers = mutableListOf<String>()
        val joinedUsersSnapshot = snapshot.child("joinedUsers")
        for (userSnapshot in joinedUsersSnapshot.children) {
            userSnapshot.key?.let { joinedUsers.add(it) }
        }

        return EnhancedTrip(
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
            hotel = hotel,
            itinerary = itinerary,
            reservation = reservation
        )
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
            putExtra("organizerId", trip.organizerId)
            // Pass hotel info for booking
            trip.hotel?.let { hotel ->
                putExtra("hotelPricePerNight", hotel.pricePerNight)
                putExtra("hotelImageUrl", hotel.imageUrl)
            }
        }
        startActivity(intent)
    }

    private fun handleJoinTrip(trip: EnhancedTrip) {
        // This shouldn't be called in My Trips tab, but handle it gracefully
        Toast.makeText(context, "Already joined this trip", Toast.LENGTH_SHORT).show()
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
            showEmptyView("No trips found.\nCreate a new trip or join an existing one!")
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }
} 