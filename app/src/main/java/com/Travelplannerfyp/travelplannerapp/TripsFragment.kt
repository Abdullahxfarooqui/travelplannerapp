package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.Travelplannerfyp.travelplannerapp.adapters.TripAdminAdapter
import com.Travelplannerfyp.travelplannerapp.model.TripAdmin
import android.util.Log

class TripsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var adapter: TripAdminAdapter
    private val trips = mutableListOf<TripAdmin>()
    private val db = FirebaseDatabase.getInstance().getReference("trips")
    private var tripsListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_trips, container, false)
        recyclerView = view.findViewById(R.id.tripsRecyclerView)
        progressBar = view.findViewById(R.id.tripsProgressBar)
        emptyState = view.findViewById(R.id.tripsEmptyState)
        adapter = TripAdminAdapter(trips, onDeleteClick = { trip -> deleteTrip(trip) }, onFeatureClick = { trip -> featureTrip(trip) })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fetchTrips()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tripsListener?.let { db.removeEventListener(it) }
    }

    private fun fetchTrips() {
        progressBar.visibility = View.VISIBLE
        emptyState.text = "No trips found."
        trips.clear()
        adapter.updateData(trips)
        tripsListener = db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trips.clear()
                for (child in snapshot.children) {
                    if (child.exists() && child.child("title").exists()) {
                        val id = child.key ?: ""
                        val title = child.child("title").getValue(String::class.java) ?: ""
                        val organizerId = child.child("organizerId").getValue(String::class.java) ?: ""
                        val organizerName = child.child("organizerName").getValue(String::class.java) ?: ""
                        val location = child.child("location").getValue(String::class.java) ?: ""
                        val status = child.child("status").getValue(String::class.java) ?: "pending"
                        // Robust price parsing
                        val priceValue = child.child("price").value
                        val price = when (priceValue) {
                            is Number -> priceValue.toString()
                            is String -> priceValue
                            else -> ""
                        }
                        trips.add(TripAdmin(id, title, organizerId, organizerName, location,
                            price, status))
                    } else {
                        Log.e("TripError", "Invalid trip data: ${child.value}")
                    }
                }
                progressBar.visibility = View.GONE
                adapter.updateData(trips)
                emptyState.text = "No trips found."
                emptyState.visibility = if (trips.isEmpty()) View.VISIBLE else View.GONE
                if (trips.isEmpty()) showToast("No trips found.")
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyState.text = "Failed to load trips."
                emptyState.visibility = View.VISIBLE
                showToast("Failed to load trips.")
            }
        })
    }

    private fun deleteTrip(trip: TripAdmin) {
        Log.d("TripID", "Deleting trip with ID: ${trip.id}")
        db.child(trip.id).removeValue()
    }

    private fun featureTrip(trip: TripAdmin) {
        db.child(trip.id).child("featured").setValue(true)
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }
} 