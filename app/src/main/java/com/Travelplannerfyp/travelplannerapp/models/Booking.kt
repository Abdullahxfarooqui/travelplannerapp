package com.Travelplannerfyp.travelplannerapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Booking(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val bookingType: BookingType = BookingType.TRIP,
    val itemId: String = "", // Trip ID or Property ID
    val itemName: String = "",
    val itemImageUrl: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val numberOfGuests: Int = 1,
    val numberOfNights: Int = 1,
    val totalAmount: Double = 0.0,
    val basePrice: Double = 0.0,
    val cleaningFee: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val serviceFee: Double = 0.0,
    val status: BookingStatus = BookingStatus.PENDING,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val specialRequests: String = "",
    val cancellationPolicy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hostId: String = "",
    val hostName: String = "",
    val hostPhone: String = "",
    val confirmationCode: String = "",
    val notes: String = "",
    val cancelled: Boolean = false, // Firebase field name
    val cancellationReason: String = "",
    val refundAmount: Double = 0.0
) : Parcelable

enum class BookingType {
    TRIP,
    PROPERTY
}

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    REFUNDED
}

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
} 