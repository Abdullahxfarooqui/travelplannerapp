package com.Travelplannerfyp.travelplannerapp.models

data class Place(
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val type: String = "",
    val distance: Double = 0.0, // in meters
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)