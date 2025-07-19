package com.Travelplannerfyp.travelplannerapp.model

data class Notification(
    val id: String = "",
    var title: String = "",
    var body: String = "",
    var audience: String = "All Users",
    var timestamp: Long = 0L,
    var status: String = "sent"
) 