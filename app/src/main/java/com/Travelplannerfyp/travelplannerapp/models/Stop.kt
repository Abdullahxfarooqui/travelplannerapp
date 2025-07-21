package com.Travelplannerfyp.travelplannerapp.models

data class Stop(
    val stopName: String = "",
    val arrivalTime: String = "",
    val durationMinutes: Int = 0,
    val attractions: List<String> = emptyList(),
    val imageUrl: String? = null // URL or local path for stop image
) 