package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.TripSummaryAdapter
import com.Travelplannerfyp.travelplannerapp.models.TripSummary
import com.google.firebase.database.FirebaseDatabase
import com.Travelplannerfyp.travelplannerapp.R

class trips_planned : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripSummaryAdapter
    private val tripList = mutableListOf<TripSummary>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trips_planned)

        recyclerView = findViewById(R.id.recycler_view_trips)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TripSummaryAdapter(tripList) { trip ->
            // Navigate to the trip details screen
            val intent = Intent(this, trips_planned_activity::class.java).apply {
                putExtra("TRIP_ID", trip.placeName) // Use real trip ID if you have one
                putExtra("TRIP_IMAGE", trip.placeImageUrl)
                putExtra("TRIP_NAME", trip.placeName)
                putExtra("TRIP_DATES", "${trip.startDate} - ${trip.endDate}")
                putExtra("TRIP_DESCRIPTION", "Trip to ${trip.placeName}") // Update if you have a real description
            }
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        fetchTripsFromFirebase()
    }

    private fun fetchTripsFromFirebase() {
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")

        tripsRef.get().addOnSuccessListener { snapshot ->
            tripList.clear()
            for (tripSnapshot in snapshot.children) {
                val placeName = tripSnapshot.child("placeName").getValue(String::class.java) ?: ""
                val startDate = tripSnapshot.child("startDate").getValue(String::class.java) ?: ""
                val endDate = tripSnapshot.child("endDate").getValue(String::class.java) ?: ""
                val imageName = tripSnapshot.child("placeImageUrl").getValue(String::class.java) ?: ""

                val trip = TripSummary(placeName, startDate, endDate, imageName)
                tripList.add(trip)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load trips: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}