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

        setupRecyclerView()
        loadExploreTrips()
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
            if (it == "PRIVATE") TripVisibility.PRIVATE 
            else TripVisibility.PUBLIC 
        } ?: TripVisibility.PUBLIC

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
            joinedUsers = joinedUsers
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
        }
        startActivity(intent)
    }

    private fun handleJoinTrip(trip: EnhancedTrip) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(context, "Please log in to join trips", Toast.LENGTH_SHORT).show()
            return
        }

        // Add user to joined users list
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
        val tripRef = tripsRef.child(trip.id)
        
        // Check if user is already joined
        if (trip.joinedUsers.contains(currentUserId)) {
            Toast.makeText(context, "You have already joined this trip", Toast.LENGTH_SHORT).show()
            return
        }

        // Add user to joined users
        tripRef.child("joinedUsers").child(currentUserId).setValue(true)
            .addOnSuccessListener {
                Toast.makeText(context, "Successfully joined trip: ${trip.placeName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to join trip: ${e.message}", Toast.LENGTH_SHORT).show()
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