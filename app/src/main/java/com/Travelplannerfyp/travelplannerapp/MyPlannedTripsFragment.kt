package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.models.Hotel
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import android.widget.ImageView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser
import android.widget.LinearLayout

class MyPlannedTripsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlannedTripsAdapter
    private val plannedTrips = mutableListOf<PlannedTrip>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_planned_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.browseRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PlannedTripsAdapter(plannedTrips)
        recyclerView.adapter = adapter
        // Add a TextView for empty state feedback
        val emptyView = view.findViewById<TextView?>(R.id.emptyView)
        fetchPlannedTrips(emptyView)
    }

    private fun fetchPlannedTrips(emptyView: TextView? = null) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")
        val bookingsRef = database.getReference("bookings")
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            emptyView?.let {
                it.visibility = View.VISIBLE
                it.text = "Please login to view your planned trips."
            }
            return
        }
        
        // Add ValueEventListener to continuously listen for changes
        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                plannedTrips.clear()
                for (tripSnapshot in snapshot.children) {
                    try {
                        val organizerId =
                            tripSnapshot.child("organizerId").getValue(String::class.java)

                        // Only add trips created by the current user
                        if (organizerId == currentUser.uid) {
                            val tripKey = tripSnapshot.key ?: continue
                            val placeName =
                                tripSnapshot.child("placeName").getValue(String::class.java) ?: ""
                            val placeDescription =
                                tripSnapshot.child("placeDescription").getValue(String::class.java)
                                    ?: ""
                            val tripDescription =
                                tripSnapshot.child("tripDescription").getValue(String::class.java)
                                    ?: ""
                            val organizerName =
                                tripSnapshot.child("organizerName").getValue(String::class.java)
                                    ?: ""
                            val organizerPhone =
                                tripSnapshot.child("organizerPhone").getValue(String::class.java)
                                    ?: ""
                            val startDate =
                                tripSnapshot.child("startDate").getValue(String::class.java) ?: ""
                            val endDate =
                                tripSnapshot.child("endDate").getValue(String::class.java) ?: ""
                            val seatsAvailable =
                                tripSnapshot.child("seatsAvailable").getValue(String::class.java)
                                    ?: ""
                            val placeImageUrl =
                                tripSnapshot.child("placeImageUrl").getValue(String::class.java)

                            // Parse hotels
                            val hotelsList = mutableListOf<Hotel>()
                            val hotelsSnapshot = tripSnapshot.child("selectedHotels")
                            if (hotelsSnapshot.exists()) {
                                for (hotelSnapshot in hotelsSnapshot.children) {
                                    try {
                                        val hotel = hotelSnapshot.getValue(Hotel::class.java)
                                        if (hotel != null) {
                                            hotelsList.add(hotel)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "MyPlannedTripsFragment",
                                            "Error parsing hotel: ${e.message}"
                                        )
                                    }
                                }
                            }

                            // Create and add the planned trip
                            val plannedTrip = PlannedTrip(
                                placeName,
                                placeDescription,
                                tripDescription,
                                organizerName,
                                organizerPhone,
                                startDate,
                                endDate,
                                seatsAvailable,
                                hotelsList,
                                placeImageUrl,
                                mutableListOf()
                            )
                            plannedTrips.add(plannedTrip)
                            // Fetch enrolled users for this trip
                            bookingsRef.orderByChild("itemId").equalTo(tripKey)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(bookingSnapshot: DataSnapshot) {
                                        plannedTrip.enrolledUsers.clear()
                                        for (booking in bookingSnapshot.children) {
                                            val status = booking.child("status").getValue(String::class.java) ?: ""
                                            if (status == "Confirmed" || status == "Pending") {
                                                val userId = booking.child("userId").getValue(String::class.java) ?: ""
                                                val name = booking.child("userName").getValue(String::class.java)
                                                    ?: booking.child("name").getValue(String::class.java) ?: "Unknown"
                                                val seats = booking.child("numberOfGuests").getValue(Int::class.java)
                                                    ?: booking.child("seats").getValue(Int::class.java) ?: 1
                                                val bookingTime = booking.child("createdAt").getValue(Long::class.java) ?: 0L
                                                plannedTrip.enrolledUsers.add(
                                                    EnrolledUser(
                                                        userId = userId,
                                                        name = name,
                                                        seats = seats,
                                                        bookingTime = bookingTime
                                                    )
                                                )
                                            }
                                        }
                                        plannedTrip.enrolledUsers.sortBy { it.bookingTime }
                                        adapter.notifyDataSetChanged()
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "MyPlannedTripsFragment",
                            "Error parsing trip: ${e.message}"
                        )
                    }
                }
                
                // Update UI
                adapter.notifyDataSetChanged()
                android.util.Log.d("MyPlannedTripsFragment", "Planned trips loaded: ${plannedTrips.size}")
                
                // Update empty view
                emptyView?.let {
                    if (plannedTrips.isEmpty()) {
                        it.visibility = View.VISIBLE
                        it.text = "No planned trips found."
                    } else {
                        it.visibility = View.GONE
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load planned trips", Toast.LENGTH_SHORT).show()
                emptyView?.let {
                    it.visibility = View.VISIBLE
                    it.text = "Failed to load planned trips."
                }
            }
        })
    }
}

// Data class for displaying planned trips with hotels
data class PlannedTrip(
    val placeName: String,
    val placeDescription: String,
    val tripDescription: String,
    val organizerName: String,
    val organizerPhone: String,
    val startDate: String,
    val endDate: String,
    val seatsAvailable: String,
    val hotels: List<Hotel>,
    val placeImageUrl: String? = null,
    var enrolledUsers: MutableList<EnrolledUser> = mutableListOf()
)

// Adapter for planned trips (organizer-only: enrolled users must not be shown in user-facing screens)
class PlannedTripsAdapter(private val trips: List<PlannedTrip>) : RecyclerView.Adapter<PlannedTripsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.bind(trip)
    }
    
    override fun getItemCount(): Int = trips.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripName: TextView = itemView.findViewById(R.id.tripTitleTextView)
        private val tripLocation: TextView = itemView.findViewById(R.id.tripLocationTextView)
        private val tripDescription: TextView = itemView.findViewById(R.id.tripDescriptionTextView)
        private val tripImageView: ImageView = itemView.findViewById(R.id.tripImageView)
        // --- Enrolled Users Section ---
        // Removed all enrolled users UI references from trip card ViewHolder
        private var expanded = false
        init {
            // Only keep the trip card click listener
            itemView.setOnClickListener {
                val context = itemView.context
                val trip = trips[adapterPosition]
                // Find the tripKey for this trip (matching order in plannedTrips)
                val tripKey = (context as? androidx.fragment.app.FragmentActivity)
                    ?.supportFragmentManager
                    ?.fragments
                    ?.filterIsInstance<MyPlannedTripsFragment>()
                    ?.firstOrNull()
                    ?.let { fragment ->
                        fragment.view?.findViewById<RecyclerView>(R.id.browseRecyclerView)
                            ?.adapter
                            ?.let { adapter ->
                                if (adapter is PlannedTripsAdapter) {
                                    val index = adapterPosition
                                    // Use the same index as in plannedTrips
                                    // plannedTrips and trips should be in sync
                                    fragment.arguments?.getStringArrayList("tripKeys")?.getOrNull(index)
                                } else null
                            }
                    } ?: null
                val intent = android.content.Intent(context, TripDetailActivity::class.java).apply {
                    putExtra("placeName", trip.placeName)
                    putExtra("placeDescription", trip.placeDescription)
                    putExtra("tripDescription", trip.tripDescription)
                    putExtra("organizerName", trip.organizerName)
                    putExtra("organizerPhone", trip.organizerPhone)
                    putExtra("startDate", trip.startDate)
                    putExtra("endDate", trip.endDate)
                    putExtra("seatsAvailable", trip.seatsAvailable)
                    putExtra("placeImageUrl", trip.placeImageUrl)
                    putExtra("hotels", ArrayList(trip.hotels))
                    if (tripKey != null) putExtra("tripId", tripKey)
                }
                context.startActivity(intent)
            }
        }
        fun bind(trip: PlannedTrip) {
            tripName.text = trip.placeName.takeIf { it.isNotBlank() } ?: "Unnamed Trip"
            tripLocation.text = buildString {
                append(trip.startDate)
                if (trip.endDate.isNotBlank()) {
                    append(" - ${trip.endDate}")
                }
                if (trip.seatsAvailable.isNotBlank()) {
                    append("  |  Seats: ${trip.seatsAvailable}")
                }
            }
            val hotelNames = if (trip.hotels.isNotEmpty()) {
                trip.hotels.joinToString(", ") { it.name }
            } else "No hotels selected"
            val desc = buildString {
                if (trip.tripDescription.isNotBlank()) {
                    append(trip.tripDescription)
                    append("\n\n")
                }
                append("Hotels: $hotelNames")
                if (trip.placeDescription.isNotBlank()) {
                    append("\n\n")
                    append(trip.placeDescription)
                }
                append("\n\nOrganizer: ${trip.organizerName}")
                if (trip.organizerPhone.isNotBlank()) {
                    append(" (${trip.organizerPhone})")
                }
            }
            tripDescription.text = desc.trim()
            val imageUrl = when {
                !trip.placeImageUrl.isNullOrEmpty() -> trip.placeImageUrl
                trip.hotels.isNotEmpty() && !trip.hotels.first().imageUrl.isNullOrEmpty() -> trip.hotels.first().imageUrl
                else -> null
            }
            if (!imageUrl.isNullOrEmpty()) {
                try {
                    com.Travelplannerfyp.travelplannerapp.utils.ImageDatabaseLoader.loadImage(tripImageView, imageUrl)
                } catch (_: Exception) {
                    loadPlaceImageFromDrawable(trip.placeName, tripImageView)
                }
            } else {
                loadPlaceImageFromDrawable(trip.placeName, tripImageView)
            }
        }
    }
}

private fun loadPlaceImageFromDrawable(placeName: String, imageView: ImageView) {
    val context = imageView.context
    // Always use the placeholder image, as specific place images do not exist
    val resourceId = R.drawable.placeholder_image
    imageView.setImageResource(resourceId)
}
