package com.Travelplannerfyp.travelplannerapp.model

data class TripAdmin(
    var id: String = "",
    var title: String = "",
    var organizerId: String = "",
    var organizerName: String = "",
    var location: String = "",
    var price: String = "", // Always a String to match Firebase
    var status: String = "pending",
    // Use Any? for fields that might be a map/object in Firebase
    var selectedHotels: Any? = null,
    var hotel: Any? = null,
    var endDate: String = "",
    var itinerary: Any? = null,
    var placeImageUrl: Any? = null,
    var selectedActivities: Any? = null,
    var seatsAvailable: Any? = null,
    var createdAt: Long = 0L,
    var tripDescription: String = "",
    var totalDays: Any? = null,
    var dailyActivities: Any? = null,
    var organizerPhone: String = "",
    var tripPrice: Double = 0.0,
    var placeDescription: String = "",
    var placeName: String = "",
    var startDate: String = ""
) 