package com.Travelplannerfyp.travelplannerapp.models

data class Trip(
    val id: String = "",
    val title: String = "",
    val organizerId: String = "",
    val description: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val stops: Map<String, Stop> = emptyMap()
)

// Sample dummy data
val sampleTrips = listOf(
    Trip(
        id = "trip1",
        title = "Northern Explorer",
        organizerId = "admin1",
        description = "A scenic journey through the north.",
        startDate = 1721000000000,
        endDate = 1721200000000,
        stops = emptyMap() // or use sampleStops.associateBy { it.id }
    )
) 