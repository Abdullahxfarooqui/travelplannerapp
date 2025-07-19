package com.Travelplannerfyp.travelplannerapp.models

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 