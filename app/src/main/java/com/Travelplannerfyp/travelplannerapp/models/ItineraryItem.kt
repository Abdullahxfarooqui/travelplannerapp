package com.Travelplannerfyp.travelplannerapp.models

data class ItineraryItem(
    val time: String = "", // e.g., "09:00 AM"
    val title: String = "", // e.g., "Breakfast at hotel"
    val description: String = "" // e.g., "Enjoy a buffet breakfast at the main restaurant."
) 