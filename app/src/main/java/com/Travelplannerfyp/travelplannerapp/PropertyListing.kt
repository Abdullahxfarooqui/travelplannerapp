package com.Travelplannerfyp.travelplannerapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PropertyListing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val pricePerNight: Double = 0.0,
    val maxGuests: Int = 0,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val beds: Int = 0,
    val amenities: List<String> = listOf(),
    val houseRules: List<String> = listOf(),
    val customRules: String = "",
    val cleaningFee: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val availabilityStart: String = "",
    val availabilityEnd: String = "",
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val imageUrls: List<String> = listOf(),
    val hostId: String = "",
    val hostName: String = "",
    val hostPhoneNumber: String = "",
    val emergencyContact: String = "",
    val identityVerificationUrl: String = "",
    @get:JvmName("isAvailable")
    val available: Boolean = true,
    var isOwnProperty: Boolean = false
) : Parcelable