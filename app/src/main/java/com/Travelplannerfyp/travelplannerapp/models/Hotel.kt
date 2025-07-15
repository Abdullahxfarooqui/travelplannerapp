package com.Travelplannerfyp.travelplannerapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hotel(
    val name: String = "",
    val description: String = "",
    val rating: Double = 0.0,
    val pricePerNight: String = "", // Changed from price to pricePerNight for clarity
    val imageName: String = "",
    val imageUrl: String = "",
    val amenities: List<String> = emptyList() // Added amenities field
) : Parcelable {
    // For backward compatibility
    val price: String
        get() = pricePerNight
        
    constructor() : this("", "", 0.0, "", "", "", emptyList())
}