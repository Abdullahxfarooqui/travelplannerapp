package com.Travelplannerfyp.travelplannerapp.models

data class EnrolledUser(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val seats: Int = 1,
    val bookingTime: Long = 0L
)