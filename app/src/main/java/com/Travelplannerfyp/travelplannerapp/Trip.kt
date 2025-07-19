package com.Travelplannerfyp.travelplannerapp

import android.os.Parcel
import android.os.Parcelable

data class Trip(
    var id: String = "",
    var name: String? = null,
    var location: String? = null,
    var description: String? = null,
    var imageUrl: String? = null,
    var imageResId: Int? = null,
    var imageName: String? = null,
    var organizerName: String? = null,
    var contactNumber: String? = null,
    var seatsAvailable: Int = 0,
    var startDate: String? = null,
    var endDate: String? = null,
    var departureLocation: String? = null,
    var pricePerPerson: String = "",
    var rating: Float = 0f,
    var category: String? = null,
    var difficulty: String? = null,
    var weather: String? = null,
    var recommendedSeason: String? = null,
    var hotels: List<String>? = null,
    var latitude: Double? = null,
    var longitude: Double? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString(),
        location = parcel.readString(),
        description = parcel.readString(),
        imageUrl = parcel.readString(),
        imageResId = parcel.readValue(Int::class.java.classLoader) as? Int,
        imageName = parcel.readString(),
        organizerName = parcel.readString(),
        contactNumber = parcel.readString(),
        seatsAvailable = parcel.readInt(),
        startDate = parcel.readString(),
        endDate = parcel.readString(),
        departureLocation = parcel.readString(),
        pricePerPerson = parcel.readString() ?: "",
        rating = parcel.readFloat(),
        category = parcel.readString(),
        difficulty = parcel.readString(),
        weather = parcel.readString(),
        recommendedSeason = parcel.readString(),
        hotels = parcel.createStringArrayList(),
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(location)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeValue(imageResId)
        parcel.writeString(imageName)
        parcel.writeString(organizerName)
        parcel.writeString(contactNumber)
        parcel.writeInt(seatsAvailable)
        parcel.writeString(startDate)
        parcel.writeString(endDate)
        parcel.writeString(departureLocation)
        parcel.writeString(pricePerPerson)
        parcel.writeFloat(rating)
        parcel.writeString(category)
        parcel.writeString(difficulty)
        parcel.writeString(weather)
        parcel.writeString(recommendedSeason)
        parcel.writeStringList(hotels)
        parcel.writeDouble(latitude ?: 0.0)
        parcel.writeDouble(longitude ?: 0.0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Trip> {
        override fun createFromParcel(parcel: Parcel): Trip = Trip(parcel)
        override fun newArray(size: Int): Array<Trip?> = arrayOfNulls(size)
    }
}
