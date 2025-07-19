package com.Travelplannerfyp.travelplannerapp.model

data class User(
    val uid: String = "",
    var name: String = "",
    var email: String = "",
    var createdAt: Long = 0L,
    var status: String = "active"
) 