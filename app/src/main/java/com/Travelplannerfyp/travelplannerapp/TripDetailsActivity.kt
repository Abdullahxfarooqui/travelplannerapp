package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.Travelplannerfyp.travelplannerapp.databinding.ActivityTripDetailsBinding
import com.Travelplannerfyp.travelplannerapp.models.Trip
import com.Travelplannerfyp.travelplannerapp.models.sampleTrips
import com.Travelplannerfyp.travelplannerapp.models.Stop

class TripDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailsBinding
    private lateinit var stopsAdapter: StopsAdapter
    private lateinit var trip: Trip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        // For demo, use first sample trip
        trip = sampleTrips.first()
        binding.tripTitle.text = trip.title
        binding.tripDates.text = "July 15 - July 18, 2024" // Format dates as needed
        binding.tripDescription.text = trip.description

        val stopsList = trip.stops.values.toMutableList()
        stopsAdapter = StopsAdapter(stopsList, onEdit = { _, _ -> }, onRemove = { _, _ -> })
        binding.stopsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.stopsRecyclerView.adapter = stopsAdapter
    }
} 