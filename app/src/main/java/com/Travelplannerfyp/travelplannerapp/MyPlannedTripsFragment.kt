package com.Travelplannerfyp.travelplannerapp

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
import com.Travelplannerfyp.travelplannerapp.adapters.EnhancedTripAdapter
import com.Travelplannerfyp.travelplannerapp.models.EnhancedTrip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyPlannedTripsFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_my_planned_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.browseRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
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
        val price = snapshot.child("pricePerPerson").getValue(String::class.java)
            ?: snapshot.child("price").getValue(String::class.java) ?: ""
        val visibility = snapshot.child("visibility").getValue(String::class.java)?.let { 
            if (it == "PRIVATE") com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PRIVATE 
            else com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PUBLIC 
        } ?: com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PUBLIC

        // Get joined users
        val joinedUsers = mutableListOf<String>()
        val joinedUsersSnapshot = snapshot.child("joinedUsers")
        for (userSnapshot in joinedUsersSnapshot.children) {
            userSnapshot.key?.let { joinedUsers.add(it) }
        }

        // Get hotel information
        val hotelSnapshot = snapshot.child("hotel")
        val pricePerNightDouble = hotelSnapshot.child("pricePerNight").getValue(Double::class.java)
            ?: hotelSnapshot.child("pricePerNight").getValue(Long::class.java)?.toDouble()
            ?: hotelSnapshot.child("price").getValue(Double::class.java)
            ?: hotelSnapshot.child("price").getValue(Long::class.java)?.toDouble()
        val pricePerNight = pricePerNightDouble?.toString() ?: ""
        android.util.Log.d("MyPlannedTripsFragment", "Hotel snapshot for trip $tripId: name=${hotelSnapshot.child("name").getValue(String::class.java)}, pricePerNight=$pricePerNight, imageUrl=${hotelSnapshot.child("imageUrl").getValue(String::class.java)}")
        val hotel = if (hotelSnapshot.exists()) {
            val hotelName = hotelSnapshot.child("name").getValue(String::class.java) ?: ""
            val hotelDescription = hotelSnapshot.child("description").getValue(String::class.java) ?: ""
            val hotelRating = hotelSnapshot.child("rating").getValue(Double::class.java) ?: 0.0
            val hotelImageName = hotelSnapshot.child("imageName").getValue(String::class.java) ?: ""
            val hotelImageUrl = hotelSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
            com.Travelplannerfyp.travelplannerapp.models.Hotel(
                name = hotelName,
                description = hotelDescription,
                rating = hotelRating,
                pricePerNight = pricePerNight,
                imageName = hotelImageName,
                imageUrl = hotelImageUrl
            )
        } else null

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
            hotel = hotel
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
            putExtra("price", trip.price)
            // No need to pass hotel as parcelable since TripDetailActivity will load it directly from Firebase
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
