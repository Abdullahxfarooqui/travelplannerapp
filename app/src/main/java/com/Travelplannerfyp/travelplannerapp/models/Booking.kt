package com.Travelplannerfyp.travelplannerapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
// Full Booking model with all fields used in the app, using String for status
// You can remove fields you are 100% sure are not used anywhere
// but keep all that are referenced in your codebase

data class Booking(
    var id: String = "", // Firebase key or bookingId
    var bookingId: String = "", // for compatibility, can be mapped to id
    var tripName: String = "",
    var itemName: String = "",
    var userName: String = "",
    var userId: String = "",
    var userEmail: String = "",
    var userPhone: String = "",
    var bookingType: String = "", // e.g., "TRIP" or "PROPERTY"
    var itemId: String = "",
    var itemImageUrl: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var checkInTime: String = "",
    var checkOutTime: String = "",
    var numberOfGuests: Int = 1,
    var numberOfNights: Int = 1,
    var totalAmount: Double = 0.0,
    var basePrice: Double = 0.0,
    var cleaningFee: Double = 0.0,
    var securityDeposit: Double = 0.0,
    var serviceFee: Double = 0.0,
    var status: String = "pending", // "pending", "approved", "rejected"
    var paymentStatus: String = "pending",
    var specialRequests: String = "",
    var cancellationPolicy: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var hostId: String = "",
    var hostName: String = "",
    var hostPhone: String = "",
    var confirmationCode: String = "",
    var notes: String = "",
    var cancelled: Boolean = false,
    var cancellationReason: String = "",
    var refundAmount: Double = 0.0,
    var timestamp: Long? = null // Add this field to match Firebase data
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