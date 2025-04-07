package com.example.travelplannerapp

import android.os.Parcel
import android.os.Parcelable

data class Trip(
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String? = null,  // URL for online images
    val imageResId: Int? = null    // Nullable image resource ID for local images
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),  // Read nullable string (imageUrl)
        parcel.readValue(Int::class.java.classLoader) as? Int  // Read nullable Int (imageResId)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(location)
        parcel.writeString(description)
        parcel.writeString(imageUrl) // Save image URL
        parcel.writeValue(imageResId) // Save nullable Int
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Trip> {
        override fun createFromParcel(parcel: Parcel): Trip {
            return Trip(parcel)
        }

        override fun newArray(size: Int): Array<Trip?> {
            return arrayOfNulls(size)
        }
    }
}
