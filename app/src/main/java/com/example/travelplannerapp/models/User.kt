package com.example.travelplannerapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = 0L,
    val lastLogin: Long = 0L,
    val isEmailVerified: Boolean = false,
    val role: String = "" // Can be "User" or "Organizer"
) : Parcelable