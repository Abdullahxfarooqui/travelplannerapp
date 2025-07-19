package com.Travelplannerfyp.travelplannerapp.model

data class ApprovalItem(
    val id: String,
    val type: String, // "trip" or "experience"
    val title: String,
    val organizerId: String,
    var organizerName: String = "",
    val imageUrl: String = "",
    var status: String = "pending"
) 