package com.example.travelplannerapp

import android.os.Parcel
import android.os.Parcelable

data class Trip(
    var id: String = "",  // Firebase unique identifier
    var name: String = "",
    var location: String = "",
    var description: String = "",
    var imageUrl: String? = null,  // URL for online images
    val imageResId: Int? = null    // Nullable image resource ID for local images
) : Parcelable {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", null, null)

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),  // Read nullable string (imageUrl)
        parcel.readValue(Int::class.java.classLoader) as? Int  // Read nullable Int (imageResId)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
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
