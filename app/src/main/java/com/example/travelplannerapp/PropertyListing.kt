package com.example.travelplannerapp

import android.os.Parcel
import android.os.Parcelable

data class PropertyListing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val pricePerNight: Double = 0.0,
    val maxGuests: Int = 0,
    val imageUrls: List<String> = listOf(),
    val ownerId: String = "",
    val isAvailable: Boolean = true,
    val amenities: List<String> = listOf()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readInt(),
        parcel.createStringArrayList() ?: listOf(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.createStringArrayList() ?: listOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(location)
        parcel.writeDouble(pricePerNight)
        parcel.writeInt(maxGuests)
        parcel.writeStringList(imageUrls)
        parcel.writeString(ownerId)
        parcel.writeByte(if (isAvailable) 1 else 0)
        parcel.writeStringList(amenities)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PropertyListing> {
        override fun createFromParcel(parcel: Parcel): PropertyListing {
            return PropertyListing(parcel)
        }

        override fun newArray(size: Int): Array<PropertyListing?> {
            return arrayOfNulls(size)
        }
    }
}