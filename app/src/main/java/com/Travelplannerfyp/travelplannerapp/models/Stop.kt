package com.Travelplannerfyp.travelplannerapp.models

data class Stop(
    val stopName: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val imageUrl: String? = null,
    val attractions: List<Place> = emptyList()
) 