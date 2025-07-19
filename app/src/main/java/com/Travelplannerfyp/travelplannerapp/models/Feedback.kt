package com.Travelplannerfyp.travelplannerapp.models

data class Feedback(
    val id: String = "",
    val tripId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 