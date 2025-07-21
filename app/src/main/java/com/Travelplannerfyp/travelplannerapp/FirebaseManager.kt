package com.Travelplannerfyp.travelplannerapp

import com.google.firebase.database.FirebaseDatabase
import com.Travelplannerfyp.travelplannerapp.models.Stop

object FirebaseManager {
    private val db = FirebaseDatabase.getInstance().reference

    fun saveTrip(tripName: String, organizerId: String, stops: List<Stop>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val trip = mapOf(
            "tripName" to tripName,
            "organizerId" to organizerId,
            "stops" to stops.map {
                mapOf(
                    "stopName" to it.stopName,
                    "arrivalTime" to it.arrivalTime,
                    "durationMinutes" to it.durationMinutes,
                    "attractions" to it.attractions
                )
            }
        )
        db.child("trips").push().setValue(trip)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
} 