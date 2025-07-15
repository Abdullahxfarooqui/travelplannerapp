package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UpdateHotelImageNamesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateAllHotelImageNames()
    }

    private fun updateAllHotelImageNames() {
        val db = FirebaseDatabase.getInstance().reference.child("trips")
        db.get().addOnSuccessListener { tripsSnapshot ->
            for (trip in tripsSnapshot.children) {
                val tripId = trip.key ?: continue
                val selectedHotels = trip.child("selectedHotels")
                for (hotel in selectedHotels.children) {
                    val hotelKey = hotel.key ?: continue
                    val hotelName = hotel.child("name").getValue(String::class.java) ?: continue
                    val imageName = when (hotelName) {
                        "PC Bhurban" -> "pc_bhurban"
                        "Fairy Meadows Resort" -> "fairy_meadows_resort"
                        "Pearl Continental Lahore" -> "pearl_continental_lahore"
                        "Ratti Gali Lake Campsite" -> "ratti_gali_lake_campsite"
                        // Add more mappings as needed
                        else -> hotelName.lowercase().replace(" ", "_")
                    }
                    db.child(tripId).child("selectedHotels").child(hotelKey).child("imageName").setValue(imageName)
                    Log.d("UpdateHotelImageNames", "Set imageName for $hotelName in trip $tripId: $imageName")
                }
            }
            Log.d("UpdateHotelImageNames", "Update complete!")
        }.addOnFailureListener { e ->
            Log.e("UpdateHotelImageNames", "Failed to update hotel image names", e)
        }
    }
} 