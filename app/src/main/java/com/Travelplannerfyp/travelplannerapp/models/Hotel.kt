package com.Travelplannerfyp.travelplannerapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Hotel(
    val name: String = "",
    val description: String = "",
    val rating: Double = 0.0,
    val price: String = "",
    val imageName: String = "",
    val imageUrl: String = ""
) : Parcelable {
    constructor() : this("", "", 0.0, "", "", "")
}