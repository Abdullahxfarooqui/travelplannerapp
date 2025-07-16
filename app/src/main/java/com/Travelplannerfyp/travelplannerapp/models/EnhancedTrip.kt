package com.Travelplannerfyp.travelplannerapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EnhancedTrip(
    val id: String = "",
    val placeName: String = "",
    val placeDescription: String = "",
    val tripDescription: String = "",
    val organizerName: String = "",
    val organizerPhone: String = "",
    val organizerId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val seatsAvailable: Int = 0,
    val placeImageUrl: String = "",
    val visibility: TripVisibility = TripVisibility.PUBLIC,
    val joinedUsers: MutableList<String> = mutableListOf(), // List of user UIDs who joined
    val hotels: List<Hotel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // NEW FIELDS FOR MISSION-CRITICAL FIXES
    val price: String = "", // Trip price in PKR
    val hotel: Hotel? = null, // Hotel information
    val itinerary: Map<String, List<String>> = emptyMap(), // Day-wise itinerary
    val reservation: Boolean = false, // Hotel reservation status
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable

enum class TripVisibility {
    PUBLIC,
    PRIVATE
}

enum class TripJoinStatus {
    NOT_JOINED,
    JOINED,
    OWNED
} 