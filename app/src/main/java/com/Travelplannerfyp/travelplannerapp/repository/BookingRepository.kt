package com.Travelplannerfyp.travelplannerapp.repository

import android.util.Log
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.Travelplannerfyp.travelplannerapp.models.PaymentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val bookingsRef = database.getReference("bookings")
    private val tripsRef = database.getReference("trips")
    private val propertiesRef = database.getReference("properties")
    private val usersRef = database.getReference("users")

    companion object {
        private const val TAG = "BookingRepository"
    }

    // Create a new booking
    suspend fun createBooking(booking: Booking): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Generate booking ID
            val bookingId = bookingsRef.push().key ?: throw Exception("Failed to generate booking ID")
            
            // Generate confirmation code
            val confirmationCode = generateConfirmationCode()
            
            // Create booking with ID and confirmation code
            val bookingWithId = booking.copy(
                id = bookingId,
                userId = currentUser.uid,
                confirmationCode = confirmationCode,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Validate booking data
            val validationResult = validateBooking(bookingWithId)
            if (!validationResult.isSuccess) {
                return Result.failure(validationResult.exceptionOrNull() ?: Exception("Validation failed"))
            }

            // Check availability and update seats atomically
            val availabilityResult = when (bookingWithId.bookingType) {
                "TRIP" -> checkAndUpdateTripSeats(bookingWithId)
                "PROPERTY" -> checkAvailability(bookingWithId)
                else -> Result.failure(Exception("Invalid booking type"))
            }
            
            if (!availabilityResult.isSuccess) {
                return Result.failure(availabilityResult.exceptionOrNull() ?: Exception("Not available"))
            }

            // Save booking to Firebase
            Log.d(TAG, "Starting Firebase write operation for booking: $bookingId")
            bookingsRef.child(bookingId).setValue(bookingWithId).await()
            Log.d(TAG, "Firebase write operation completed successfully for booking: $bookingId")

            Log.d(TAG, "Booking created successfully: $bookingId")
            Result.success(bookingId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking: ${e.message}")
            Result.failure(e)
        }
    }

    // Get user's bookings
    suspend fun getUserBookings(userId: String): Result<List<Booking>> {
        return try {
            val bookings = mutableListOf<Booking>()
            try {
                val snapshot = bookingsRef.orderByChild("userId").equalTo(userId).get().await()
                for (bookingSnapshot in snapshot.children) {
                    val booking = bookingSnapshot.getValue(Booking::class.java)
                    booking?.let { bookings.add(it) }
                }
            } catch (e: Exception) {
                val allBookings = bookingsRef.get().await()
                for (bookingSnapshot in allBookings.children) {
                    val booking = bookingSnapshot.getValue(Booking::class.java)
                    if (booking?.userId == userId) {
                        bookings.add(booking)
                    }
                }
            }
            Result.success(bookings.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get booking by ID
    suspend fun getBookingById(bookingId: String): Result<Booking?> {
        return try {
            val snapshot = bookingsRef.child(bookingId).get().await()
            val booking = snapshot.getValue(Booking::class.java)
            Result.success(booking)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching booking: ${e.message}")
            Result.failure(e)
        }
    }

    // Update booking status
    suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            
            bookingsRef.child(bookingId).updateChildren(updates).await()
            Log.d(TAG, "Booking status updated: $bookingId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating booking status: ${e.message}")
            Result.failure(e)
        }
    }

    // Update payment status
    suspend fun updatePaymentStatus(bookingId: String, paymentStatus: PaymentStatus): Result<Unit> {
        return try {
            val updates = mapOf(
                "paymentStatus" to paymentStatus.name,
                "updatedAt" to System.currentTimeMillis()
            )
            
            bookingsRef.child(bookingId).updateChildren(updates).await()
            Log.d(TAG, "Payment status updated: $bookingId -> $paymentStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: ${e.message}")
            Result.failure(e)
        }
    }

    // Cancel booking
    suspend fun cancelBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "CANCELLED",
                "cancelled" to true,
                "cancellationReason" to reason,
                "updatedAt" to System.currentTimeMillis()
            )
            
            bookingsRef.child(bookingId).updateChildren(updates).await()
            
            // Restore item availability
            val booking = getBookingById(bookingId).getOrNull()
            booking?.let { restoreItemAvailability(it) }
            
            Log.d(TAG, "Booking cancelled: $bookingId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling booking: ${e.message}")
            Result.failure(e)
        }
    }

    // Get bookings for host (property owner or trip organizer)
    suspend fun getHostBookings(hostId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsRef.orderByChild("hostId").equalTo(hostId).get().await()
            val bookings = mutableListOf<Booking>()
            
            for (bookingSnapshot in snapshot.children) {
                val booking = bookingSnapshot.getValue(Booking::class.java)
                booking?.let { bookings.add(it) }
            }
            
            Result.success(bookings.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching host bookings: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Get real-time seat availability for a trip
    suspend fun getTripSeatAvailability(tripId: String): Result<Int> {
        return try {
            val snapshot = tripsRef.child(tripId).get().await()
            val tripData = snapshot.getValue(Map::class.java)
            val availableSeats = (tripData?.get("seatsAvailable") as? String)?.toIntOrNull() ?: 0
            Log.d(TAG, "Current seat availability for trip $tripId: $availableSeats")
            Result.success(availableSeats)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trip seat availability: ${e.message}")
            Result.failure(e)
        }
    }

    // Check if item is available for booking
    suspend fun checkItemAvailability(itemId: String, startDate: String, endDate: String, bookingType: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Checking availability for itemId: $itemId, startDate: $startDate, endDate: $endDate, type: $bookingType")
            
            // First try with indexed query
            val conflictingBookings = try {
                bookingsRef
                    .orderByChild("itemId")
                    .equalTo(itemId)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Indexed query failed, falling back to manual filtering: ${e.message}")
                // Fallback: get all bookings and filter manually
                bookingsRef.get().await()
            }

            val conflicts = mutableListOf<Booking>()
            
            for (bookingSnapshot in conflictingBookings.children) {
                val booking = bookingSnapshot.getValue(Booking::class.java)
                if (booking != null && 
                    booking.itemId == itemId &&
                    booking.bookingType == bookingType &&
                    booking.status != "CANCELLED" &&
                    hasDateConflict(booking.startDate, booking.endDate, startDate, endDate)) {
                    Log.d(TAG, "Found conflicting booking: ${booking.id} - ${booking.startDate} to ${booking.endDate}")
                    conflicts.add(booking)
                }
            }
            
            if (conflicts.isNotEmpty()) {
                Log.w(TAG, "Found ${conflicts.size} conflicting bookings")
                return Result.success(false)
            }
            
            Log.d(TAG, "Item is available for booking")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking availability: ${e.message}")
            Result.failure(e)
        }
    }

    // Private helper methods
    private suspend fun validateBooking(booking: Booking): Result<Unit> {
        return try {
            Log.d(TAG, "Validating booking - Start: ${booking.startDate}, End: ${booking.endDate}, Type: ${booking.bookingType}")
            
            when {
                booking.userId.isEmpty() -> throw Exception("User ID is required")
                booking.itemId.isEmpty() -> throw Exception("Item ID is required")
                booking.startDate.isEmpty() -> throw Exception("Start date is required")
                booking.endDate.isEmpty() -> throw Exception("End date is required")
                booking.numberOfGuests <= 0 -> throw Exception("Number of guests must be greater than 0")
                booking.totalAmount <= 0 -> throw Exception("Total amount must be greater than 0")
                booking.startDate > booking.endDate -> throw Exception("Start date must be before or equal to end date")
            }
            
            Log.d(TAG, "Booking validation passed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Booking validation failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun checkAndUpdateTripSeats(booking: Booking): Result<Unit> {
        return try {
            Log.d(TAG, "Checking and updating trip seats for booking: ${booking.id}")
            val tripRef = tripsRef.child(booking.itemId)
            val transactionResult = kotlinx.coroutines.suspendCancellableCoroutine<Result<Unit>> { cont ->
                tripRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val trip = currentData.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})?.toMutableMap()
                        if (trip != null) {
                            val currentSeats = (trip["seatsAvailable"] as? String)?.toIntOrNull() ?: 0
                            Log.d(TAG, "Current seats: $currentSeats, Requested: ${booking.numberOfGuests}")
                            if (currentSeats >= booking.numberOfGuests) {
                                val newSeats = currentSeats - booking.numberOfGuests
                                trip["seatsAvailable"] = newSeats.toString()
                                currentData.value = trip
                                Log.d(TAG, "Seats updated: $currentSeats -> $newSeats")
                                return Transaction.success(currentData)
                            } else {
                                Log.w(TAG, "Insufficient seats: $currentSeats available, ${booking.numberOfGuests} requested")
                                return Transaction.abort()
                            }
                        } else {
                            Log.e(TAG, "Trip data not found")
                            return Transaction.abort()
                        }
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (error != null) {
                            Log.e(TAG, "Transaction failed: ${error.message}")
                            cont.resume(Result.failure(Exception(error.message))) {}
                        } else if (committed) {
                            Log.d(TAG, "Transaction committed successfully")
                            cont.resume(Result.success(Unit)) {}
                        } else {
                            Log.w(TAG, "Transaction aborted")
                            cont.resume(Result.failure(Exception("Only ${booking.numberOfGuests} seats are available. Please reduce the number of people or choose another trip."))) {}
                        }
                    }
                })
            }
            transactionResult
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndUpdateTripSeats: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun getCurrentTripSeats(tripId: String): Int {
        return try {
            val snapshot = tripsRef.child(tripId).get().await()
            val tripData = snapshot.getValue(Map::class.java)
            (tripData?.get("seatsAvailable") as? String)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current trip seats: ${e.message}")
            0
        }
    }
    
    private suspend fun checkAvailability(booking: Booking): Result<Unit> {
        return try {
            val isAvailable = checkItemAvailability(
                booking.itemId,
                booking.startDate,
                booking.endDate,
                booking.bookingType
            ).getOrNull() ?: false

            if (!isAvailable) {
                throw Exception("The selected dates (${booking.startDate} to ${booking.endDate}) are already booked. Please choose different dates.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hasDateConflict(
        existingStart: String,
        existingEnd: String,
        newStart: String,
        newEnd: String
    ): Boolean {
        return (newStart < existingEnd && newEnd > existingStart)
    }

    private fun generateConfirmationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }



    private suspend fun restoreItemAvailability(booking: Booking) {
        try {
            when (booking.bookingType) {
                "PROPERTY" -> {
                    Log.d(TAG, "Property availability restored for booking: ${booking.id}")
                }
                "TRIP" -> {
                    val tripRef = tripsRef.child(booking.itemId)
                    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                        tripRef.runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                val trip = currentData.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})?.toMutableMap()
                                if (trip != null) {
                                    val currentSeats = (trip["seatsAvailable"] as? String)?.toIntOrNull() ?: 0
                                    val newSeats = currentSeats + booking.numberOfGuests
                                    trip["seatsAvailable"] = newSeats.toString()
                                    currentData.value = trip
                                    Log.d(TAG, "Restored seats: $currentSeats -> $newSeats")
                                }
                                return Transaction.success(currentData)
                            }
                            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                if (error != null) {
                                    Log.e(TAG, "Error restoring trip seats: ${error.message}")
                                } else {
                                    Log.d(TAG, "Trip seats restored successfully")
                                }
                                cont.resume(Unit) {}
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring item availability: ${e.message}")
        }
    }
} 