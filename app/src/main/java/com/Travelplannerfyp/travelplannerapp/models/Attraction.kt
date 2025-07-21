package com.Travelplannerfyp.travelplannerapp.models

data class Attraction(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val distance: String = "",
    val iconUrl: String? = null
)

// Sample dummy data
val sampleAttractions = listOf(
    Attraction(
        id = "attr1",
        name = "Patriata",
        type = "Hill",
        distance = "5km",
        iconUrl = null // or provide a sample URL
    ),
    Attraction(
        id = "attr2",
        name = "Mall Road",
        type = "Market",
        distance = "2km",
        iconUrl = null
    )
) 