package com.Travelplannerfyp.travelplannerapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.adapters.OrganizerTripsAdapter
import com.Travelplannerfyp.travelplannerapp.models.EnhancedTrip
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Log

class OrganizerTripsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: OrganizerTripsAdapter
    private val organizerTrips = mutableListOf<OrganizerTripData>()
    private val houseBookings = mutableListOf<HouseBooking>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    data class OrganizerTripData(
        val trip: EnhancedTrip,
        val enrolledUsers: List<EnrolledUser>
    )

    data class HouseBooking(
        val bookingId: String = "",
        val houseId: String = "",
        val houseName: String = "",
        val houseLocation: String = "",
        val userId: String = "",
        val userName: String = "",
        val userEmail: String = "",
        val userPhone: String = "",
        val checkInDate: String = "",
        val checkOutDate: String = "",
        val numberOfNights: Int = 0,
        val bookingDate: Long = 0L,
        val totalPrice: String = "",
        val status: String = ""
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_organizer_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerViewOrganizerTrips)
        emptyView = view.findViewById(R.id.emptyViewOrganizerTrips)
        progressBar = view.findViewById(R.id.progressBarOrganizerTrips)

        setupRecyclerView()
        loadOrganizerData()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = OrganizerTripsAdapter(organizerTrips, houseBookings) { tripData ->
            // Handle trip click - could show detailed enrolled users view
            context?.let {
                Toast.makeText(it, "Trip: ${tripData.trip.placeName}", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadOrganizerData() {
        if (currentUserId.isEmpty()) {
            showEmptyView("Please log in to view your data")
            return
        }

        showLoading(true)
        
        // Load both trips and house bookings
        loadOrganizerTrips()
        loadHouseBookings()
    }

    private fun loadOrganizerTrips() {
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                organizerTrips.clear()
                
                for (tripSnapshot in snapshot.children) {
                    val trip = createEnhancedTripFromSnapshot(tripSnapshot)
                    
                    // Only include trips that the current user created (organizer)
                    if (trip.organizerId == currentUserId) {
                        // Load enrolled users for this trip
                        loadEnrolledUsersForTrip(trip) { enrolledUsers ->
                            val tripData = OrganizerTripData(trip, enrolledUsers)
                            organizerTrips.add(tripData)
                            updateUI()
                        }
                    }
                }
                
                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showEmptyView("Failed to load trips: ${error.message}")
            }
        })
    }

    private fun loadHouseBookings() {
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        val TAG = "OrganizerTripsDebug"
        bookingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                houseBookings.clear()
                Log.d(TAG, "Checking all house bookings for organizer $currentUserId")
                for (bookingSnapshot in snapshot.children) {
                    val bookingType = bookingSnapshot.child("bookingType").getValue(String::class.java) ?: ""
                    val houseId = bookingSnapshot.child("itemId").getValue(String::class.java) ?: ""
                    val status = bookingSnapshot.child("status").getValue(String::class.java) ?: ""
                    Log.d(TAG, "Booking: type=$bookingType, houseId=$houseId, status=$status, bookingId=${bookingSnapshot.key}")
                    // Only process house/property bookings
                    if (bookingType == "house" || bookingType == "property") {
                        // Fetch house/property to check organizer
                        val housesRef = FirebaseDatabase.getInstance().getReference("houses")
                        housesRef.child(houseId).get().addOnSuccessListener { houseSnap ->
                            val organizerId = houseSnap.child("organizerId").getValue(String::class.java) ?: houseSnap.child("ownerId").getValue(String::class.java) ?: ""
                            Log.d(TAG, "House $houseId organizerId=$organizerId")
                            if (organizerId == currentUserId) {
                                val booking = createHouseBookingFromSnapshot(bookingSnapshot)
                                houseBookings.add(booking)
                                Log.d(TAG, "Added house booking for house $houseId by user ${booking.userId}")
                                updateUI()
                            }
                        }.addOnFailureListener {
                            Log.e(TAG, "Failed to fetch house $houseId for booking ${bookingSnapshot.key}")
                        }
                    }
                }
                // Sort by booking date (newest first)
                houseBookings.sortByDescending { it.bookingDate }
                updateUI()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading house bookings: ${error.message}")
                showLoading(false)
                showEmptyView("Failed to load house bookings: ${error.message}")
            }
        })
    }

    private fun createHouseBookingFromSnapshot(snapshot: DataSnapshot): HouseBooking {
        val bookingId = snapshot.key ?: ""
        val houseId = snapshot.child("itemId").getValue(String::class.java) ?: ""
        val houseName = snapshot.child("itemName").getValue(String::class.java) ?: ""
        val houseLocation = snapshot.child("location").getValue(String::class.java) ?: ""
        val userId = snapshot.child("userId").getValue(String::class.java) ?: ""
        val userName = snapshot.child("userName").getValue(String::class.java) ?: "Unknown"
        val userEmail = snapshot.child("userEmail").getValue(String::class.java) ?: "N/A"
        val userPhone = snapshot.child("userPhone").getValue(String::class.java) ?: "N/A"
        val checkInDate = snapshot.child("checkInDate").getValue(String::class.java) ?: ""
        val checkOutDate = snapshot.child("checkOutDate").getValue(String::class.java) ?: ""
        val numberOfNights = snapshot.child("numberOfNights").getValue(Int::class.java) ?: 1
        val bookingDate = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        val totalPrice = snapshot.child("totalPrice").getValue(String::class.java) ?: ""
        val status = snapshot.child("status").getValue(String::class.java) ?: "Pending"

        return HouseBooking(
            bookingId = bookingId,
            houseId = houseId,
            houseName = houseName,
            houseLocation = houseLocation,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            userPhone = userPhone,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            numberOfNights = numberOfNights,
            bookingDate = bookingDate,
            totalPrice = totalPrice,
            status = status
        )
    }

    private fun loadEnrolledUsersForTrip(trip: EnhancedTrip, onComplete: (List<EnrolledUser>) -> Unit) {
        val enrolledUsers = mutableListOf<EnrolledUser>()
        val TAG = "OrganizerTripsDebug"
        Log.d(TAG, "Fetching enrolled users for trip: ${trip.id} (${trip.placeName})")
        // First, try to get users from bookings collection
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.orderByChild("itemId").equalTo(trip.id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    enrolledUsers.clear()
                    Log.d(TAG, "Bookings found for trip ${trip.id}: ${snapshot.childrenCount}")
                    for (bookingSnapshot in snapshot.children) {
                        val bookingType = bookingSnapshot.child("bookingType").getValue(String::class.java) ?: ""
                        val status = bookingSnapshot.child("status").getValue(String::class.java) ?: ""
                        Log.d(TAG, "Booking: type=$bookingType, status=$status, bookingId=${bookingSnapshot.key}")
                        // Only process trip bookings
                        if (bookingType.equals("trip", ignoreCase = true) && (status.equals("Confirmed", ignoreCase = true) || status.equals("Pending", ignoreCase = true))) {
                            Log.d(TAG, "Booking raw data: ${bookingSnapshot.value}")
                            val userId = bookingSnapshot.child("userId").getValue(String::class.java)
                                ?: bookingSnapshot.child("uid").getValue(String::class.java)
                                ?: bookingSnapshot.child("user_id").getValue(String::class.java)
                                ?: ""
                            var name = bookingSnapshot.child("userName").getValue(String::class.java)
                                ?: bookingSnapshot.child("name").getValue(String::class.java)
                                ?: bookingSnapshot.child("user_name").getValue(String::class.java)
                                ?: ""
                            var email = bookingSnapshot.child("userEmail").getValue(String::class.java)
                                ?: bookingSnapshot.child("email").getValue(String::class.java)
                                ?: bookingSnapshot.child("user_email").getValue(String::class.java)
                                ?: ""
                            var phone = bookingSnapshot.child("userPhone").getValue(String::class.java)
                                ?: bookingSnapshot.child("phone").getValue(String::class.java)
                                ?: bookingSnapshot.child("user_phone").getValue(String::class.java)
                                ?: ""
                            val seats = bookingSnapshot.child("numberOfGuests").getValue(Int::class.java)
                                ?: bookingSnapshot.child("seats").getValue(Int::class.java)
                                ?: 1
                            val bookingTime = bookingSnapshot.child("createdAt").getValue(Long::class.java)
                                ?: bookingSnapshot.child("bookingTime").getValue(Long::class.java)
                                ?: 0L
                            if (name.isNullOrBlank() || email.isNullOrBlank() || phone.isNullOrBlank()) {
                                // Fetch from users node
                                val usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                                usersRef.get().addOnSuccessListener { userSnap ->
                                    if (name.isNullOrBlank()) name = userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                                    if (email.isNullOrBlank()) email = userSnap.child("email").getValue(String::class.java) ?: "N/A"
                                    if (phone.isNullOrBlank()) phone = userSnap.child("phone").getValue(String::class.java) ?: "N/A"
                                    Log.d(TAG, "Fetched user info from users node: $userId, $name, $email, $phone")
                                    enrolledUsers.add(EnrolledUser(
                                        userId = userId,
                                        name = name,
                                        email = email,
                                        phone = phone,
                                        seats = seats,
                                        bookingTime = bookingTime
                                    ))
                                    Log.d(TAG, "Enrolled user: $userId, $name, $email, $phone, seats=$seats, bookingTime=$bookingTime (from users node)")
                                    onComplete(enrolledUsers)
                                }.addOnFailureListener {
                                    Log.d(TAG, "Failed to fetch user info for $userId from users node")
                                    enrolledUsers.add(EnrolledUser(
                                        userId = userId,
                                        name = if (name.isNullOrBlank()) "Unknown" else name,
                                        email = if (email.isNullOrBlank()) "N/A" else email,
                                        phone = if (phone.isNullOrBlank()) "N/A" else phone,
                                        seats = seats,
                                        bookingTime = bookingTime
                                    ))
                                    onComplete(enrolledUsers)
                                }
                            } else {
                                Log.d(TAG, "Enrolled user: $userId, $name, $email, $phone, seats=$seats, bookingTime=$bookingTime")
                                enrolledUsers.add(EnrolledUser(
                                    userId = userId,
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    seats = seats,
                                    bookingTime = bookingTime
                                ))
                            }
                        }
                    }
                    Log.d(TAG, "Total enrolled users for trip ${trip.id}: ${enrolledUsers.size}")
                    // If no bookings found, try to get from joinedUsers in trip
                    if (enrolledUsers.isEmpty() && trip.joinedUsers.isNotEmpty()) {
                        Log.d(TAG, "No bookings found, checking joinedUsers for trip ${trip.id}")
                        loadUsersFromJoinedUsers(trip.joinedUsers) { users ->
                            enrolledUsers.addAll(users)
                            Log.d(TAG, "Enrolled users from joinedUsers: ${users.size}")
                            onComplete(enrolledUsers)
                        }
                    } else {
                        onComplete(enrolledUsers)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading bookings for trip ${trip.id}: ${error.message}")
                    // Fallback to joinedUsers if bookings fail
                    if (trip.joinedUsers.isNotEmpty()) {
                        loadUsersFromJoinedUsers(trip.joinedUsers) { users ->
                            enrolledUsers.addAll(users)
                            onComplete(enrolledUsers)
                        }
                    } else {
                        onComplete(enrolledUsers)
                    }
                }
            })
    }

    private fun loadUsersFromJoinedUsers(joinedUserIds: List<String>, onComplete: (List<EnrolledUser>) -> Unit) {
        val users = mutableListOf<EnrolledUser>()
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        
        var completedCount = 0
        val totalUsers = joinedUserIds.size
        
        if (totalUsers == 0) {
            onComplete(users)
            return
        }
        
        for (userId in joinedUserIds) {
            usersRef.child(userId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val email = snapshot.child("email").getValue(String::class.java) ?: "N/A"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "N/A"
                    
                    users.add(EnrolledUser(
                        userId = userId,
                        name = name,
                        email = email,
                        phone = phone,
                        seats = 1,
                        bookingTime = System.currentTimeMillis()
                    ))
                }
                
                completedCount++
                if (completedCount == totalUsers) {
                    onComplete(users)
                }
            }.addOnFailureListener {
                completedCount++
                if (completedCount == totalUsers) {
                    onComplete(users)
                }
            }
        }
    }

    private fun createEnhancedTripFromSnapshot(snapshot: DataSnapshot): EnhancedTrip {
        val tripId = snapshot.key ?: ""
        val placeName = snapshot.child("placeName").getValue(String::class.java) ?: ""
        val placeDescription = snapshot.child("placeDescription").getValue(String::class.java) ?: ""
        val tripDescription = snapshot.child("tripDescription").getValue(String::class.java) ?: ""
        val organizerName = snapshot.child("organizerName").getValue(String::class.java) ?: ""
        val organizerPhoneValue = snapshot.child("organizerPhone").value
        val organizerPhone = when (organizerPhoneValue) {
            is String -> organizerPhoneValue
            is Number -> organizerPhoneValue.toString()
            else -> ""
        }
        val organizerId = snapshot.child("organizerId").getValue(String::class.java) ?: ""
        val startDateValue = snapshot.child("startDate").value
        val startDate = when (startDateValue) {
            is String -> startDateValue
            is Number -> startDateValue.toString()
            else -> ""
        }
        val endDateValue = snapshot.child("endDate").value
        val endDate = when (endDateValue) {
            is String -> endDateValue
            is Number -> endDateValue.toString()
            else -> ""
        }
        val seatsAvailableValue = snapshot.child("seatsAvailable").value
        val seatsAvailable = when (seatsAvailableValue) {
            is String -> seatsAvailableValue.toIntOrNull() ?: 0
            is Number -> seatsAvailableValue.toInt()
            else -> 0
        }
        val placeImageUrl = snapshot.child("placeImageUrl").getValue(String::class.java) ?: ""
        val visibility = snapshot.child("visibility").getValue(String::class.java)?.let { 
            if (it == "PRIVATE") com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PRIVATE 
            else com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PUBLIC 
        } ?: com.Travelplannerfyp.travelplannerapp.models.TripVisibility.PUBLIC

        val pricePerPersonValue = snapshot.child("pricePerPerson").value
        val priceValue = snapshot.child("price").value
        val tripPriceValue = snapshot.child("tripPrice").value
        
        val price = when {
            pricePerPersonValue != null -> when (pricePerPersonValue) {
                is String -> pricePerPersonValue
                is Number -> pricePerPersonValue.toString()
                else -> ""
            }
            priceValue != null -> when (priceValue) {
                is String -> priceValue
                is Number -> priceValue.toString()
                else -> ""
            }
            tripPriceValue != null -> when (tripPriceValue) {
                is String -> tripPriceValue
                is Number -> tripPriceValue.toString()
                else -> ""
            }
            else -> ""
        }
        val reservation = snapshot.child("reservation").getValue(Boolean::class.java) ?: false
        
        // Load hotel information
        val hotelSnapshot = snapshot.child("hotel")
        val hotel = if (hotelSnapshot.exists()) {
            try {
                // Robust parsing for hotel name - handle both String and Number types
                val hotelNameValue = hotelSnapshot.child("name").value
                val hotelName = when (hotelNameValue) {
                    is String -> hotelNameValue
                    is Number -> hotelNameValue.toString()
                    else -> ""
                }
                
                // Robust parsing for hotel description - handle both String and Number types
                val hotelDescriptionValue = hotelSnapshot.child("description").value
                val hotelDescription = when (hotelDescriptionValue) {
                    is String -> hotelDescriptionValue
                    is Number -> hotelDescriptionValue.toString()
                    else -> ""
                }
                
                val hotelRating = getDoubleFromSnapshot(hotelSnapshot.child("rating")) ?: 0.0
                val pricePerNight = getDoubleFromSnapshot(hotelSnapshot.child("pricePerNight"))
                    ?: getDoubleFromSnapshot(hotelSnapshot.child("price"))
                val pricePerNightStr = pricePerNight?.toString() 
                    ?: hotelSnapshot.child("pricePerNight").getValue(String::class.java)
                    ?: hotelSnapshot.child("price").getValue(String::class.java)
                    ?: hotelSnapshot.child("pricePerNight").getValue(Long::class.java)?.toString()
                    ?: hotelSnapshot.child("price").getValue(Long::class.java)?.toString()
                    ?: ""
                
                // Robust parsing for hotel image name - handle both String and Number types
                val hotelImageNameValue = hotelSnapshot.child("imageName").value
                val hotelImageName = when (hotelImageNameValue) {
                    is String -> hotelImageNameValue
                    is Number -> hotelImageNameValue.toString()
                    else -> ""
                }
                
                // Robust parsing for hotel image URL - handle both String and Number types
                val hotelImageUrlValue = hotelSnapshot.child("imageUrl").value
                val hotelImageUrl = when (hotelImageUrlValue) {
                    is String -> hotelImageUrlValue
                    is Number -> hotelImageUrlValue.toString()
                    else -> ""
                }
                com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = hotelName.ifEmpty { "Hotel data unavailable" },
                    description = hotelDescription.ifEmpty { "Hotel information could not be loaded. Please contact the organizer." },
                    rating = hotelRating,
                    pricePerNight = pricePerNightStr,
                    imageName = hotelImageName,
                    imageUrl = hotelImageUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("OrganizerTripsFragment", "Error parsing hotel data: ${e.message}", e)
                com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = "Hotel data unavailable",
                    description = "Hotel information could not be loaded. Please contact the organizer.",
                    rating = 0.0,
                    pricePerNight = "",
                    imageName = "",
                    imageUrl = ""
                )
            }
        } else {
            // Try to parse from selectedHotels if hotel is missing
            val selectedHotelsSnap = snapshot.child("selectedHotels")
            Log.d("OrganizerTripsFragment", "selectedHotels exists: ${selectedHotelsSnap.exists()}, children count: ${selectedHotelsSnap.childrenCount}")
            
            if (selectedHotelsSnap.exists() && selectedHotelsSnap.childrenCount > 0) {
                val firstHotel = selectedHotelsSnap.children.iterator().next()
                Log.d("OrganizerTripsFragment", "First hotel key: ${firstHotel.key}")
                Log.d("OrganizerTripsFragment", "First hotel name value: ${firstHotel.child("name").value}")
                Log.d("OrganizerTripsFragment", "First hotel price value: ${firstHotel.child("price").value}")
                
                // Robust parsing for hotel name from selectedHotels - handle both String and Number types
                val hotelNameValue = firstHotel.child("name").value
                val hotelName = when (hotelNameValue) {
                    is String -> hotelNameValue
                    is Number -> hotelNameValue.toString()
                    else -> ""
                }
                Log.d("OrganizerTripsFragment", "Parsed hotel name: $hotelName")
                
                // Robust parsing for hotel description from selectedHotels - handle both String and Number types
                val hotelDescriptionValue = firstHotel.child("description").value
                val hotelDescription = when (hotelDescriptionValue) {
                    is String -> hotelDescriptionValue
                    is Number -> hotelDescriptionValue.toString()
                    else -> ""
                }
                
                val hotelRating = when (val ratingVal = firstHotel.child("rating").value) {
                    is Number -> ratingVal.toDouble()
                    is String -> ratingVal.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                val pricePerNight = when (val priceVal = firstHotel.child("pricePerNight").value ?: firstHotel.child("price").value) {
                    is Number -> priceVal.toString()
                    is String -> priceVal
                    else -> ""
                }
                
                // Robust parsing for hotel image name from selectedHotels - handle both String and Number types
                val hotelImageNameValue = firstHotel.child("imageName").value
                val hotelImageName = when (hotelImageNameValue) {
                    is String -> hotelImageNameValue
                    is Number -> hotelImageNameValue.toString()
                    else -> ""
                }
                
                // Robust parsing for hotel image URL from selectedHotels - handle both String and Number types
                val hotelImageUrlValue = firstHotel.child("imageUrl").value
                val hotelImageUrl = when (hotelImageUrlValue) {
                    is String -> hotelImageUrlValue
                    is Number -> hotelImageUrlValue.toString()
                    else -> ""
                }
                
                val amenitiesList = firstHotel.child("amenities").children.mapNotNull { it.getValue(String::class.java) }
                val hotel = com.Travelplannerfyp.travelplannerapp.models.Hotel(
                    name = hotelName.ifEmpty { "Hotel data unavailable" },
                    description = hotelDescription.ifEmpty { "Hotel information could not be loaded. Please contact the organizer." },
                    rating = hotelRating,
                    pricePerNight = pricePerNight ?: "",
                    imageName = hotelImageName,
                    imageUrl = hotelImageUrl,
                    amenities = amenitiesList
                )
                Log.d("OrganizerTripsFragment", "Created hotel object: $hotel")
                hotel
            } else null
        }

        // Load itinerary information
        val itinerarySnapshot = snapshot.child("itinerary")
        val itinerary = mutableMapOf<String, List<String>>()
        for (daySnapshot in itinerarySnapshot.children) {
            val dayKey = daySnapshot.key ?: continue
            val activities = daySnapshot.children.mapNotNull { it.getValue(String::class.java) }
            if (activities.isNotEmpty()) {
                itinerary[dayKey] = activities
            }
        }

        // Get joined users
        val joinedUsers = mutableListOf<String>()
        val joinedUsersSnapshot = snapshot.child("joinedUsers")
        for (userSnapshot in joinedUsersSnapshot.children) {
            userSnapshot.key?.let { joinedUsers.add(it) }
        }

        val trip = EnhancedTrip(
            id = tripId,
            placeName = placeName,
            placeDescription = placeDescription,
            tripDescription = tripDescription,
            organizerName = organizerName,
            organizerPhone = organizerPhone,
            organizerId = organizerId,
            startDate = startDate,
            endDate = endDate,
            seatsAvailable = seatsAvailable,
            placeImageUrl = placeImageUrl,
            visibility = visibility,
            joinedUsers = joinedUsers,
            price = price,
            hotel = hotel,
            itinerary = itinerary,
            reservation = reservation
        )
        Log.d("OrganizerTripsFragment", "Created trip with hotel: ${trip.hotel?.name ?: "null"}")
        return trip
    }

    private fun getDoubleFromSnapshot(snapshot: DataSnapshot): Double? {
        val value = snapshot.value
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(message: String) {
        emptyView.text = message
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun updateUI() {
        if (organizerTrips.isEmpty() && houseBookings.isEmpty()) {
            showEmptyView("No trips planned or house bookings found.\nCreate a new trip or list a property to see enrolled users!")
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            // Update adapter with new data
            adapter.updateData(organizerTrips, houseBookings)
        }
    }
} 