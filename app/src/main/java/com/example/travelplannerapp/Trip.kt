package com.example.travelplannerapp

import android.os.Parcel
import android.os.Parcelable

data class Trip(
    var id: String = "",  // Firebase unique identifier
    var name: String = "",
    var location: String = "",
    var description: String = "",
    var imageUrl: String? = null,  // URL for online images
    val imageResId: Int? = null,    // Nullable image resource ID for local images
    var organizerName: String = "",
    var contactNumber: String = "",
    var seatsAvailable: Int = 0,
    var startDate: String = "",
    var endDate: String = "",
    var departureLocation: String = "",
    var pricePerPerson: Double = 0.0,
    var rating: Float = 0.0f,        // Trip rating (0-5 stars)
    var category: String = "",      // e.g., Adventure, Cultural, Relaxation
    var difficulty: String = "",    // e.g., Easy, Moderate, Difficult
    var weather: String = "",       // Weather conditions during trip
    var recommendedSeason: String = ""  // Best season for this trip
) : Parcelable {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", null, null, "", "", 0, "", "", "", 0.0, 0.0f, "", "", "", "")

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),  // Read nullable string (imageUrl)
        parcel.readValue(Int::class.java.classLoader) as? Int,  // Read nullable Int (imageResId)
        parcel.readString() ?: "", // organizerName
        parcel.readString() ?: "", // contactNumber
        parcel.readInt(), // seatsAvailable
        parcel.readString() ?: "", // startDate
        parcel.readString() ?: "", // endDate
        parcel.readString() ?: "", // departureLocation
        parcel.readDouble(), // pricePerPerson
        parcel.readFloat(), // rating
        parcel.readString() ?: "", // category
        parcel.readString() ?: "", // difficulty
        parcel.readString() ?: "", // weather
        parcel.readString() ?: "" // recommendedSeason
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(location)
        parcel.writeString(description)
        parcel.writeString(imageUrl) // Save image URL
        parcel.writeValue(imageResId) // Save nullable Int
        parcel.writeString(organizerName)
        parcel.writeString(contactNumber)
        parcel.writeInt(seatsAvailable)
        parcel.writeString(startDate)
        parcel.writeString(endDate)
        parcel.writeString(departureLocation)
        parcel.writeDouble(pricePerPerson)
        parcel.writeFloat(rating)
        parcel.writeString(category)
        parcel.writeString(difficulty)
        parcel.writeString(weather)
        parcel.writeString(recommendedSeason)
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
