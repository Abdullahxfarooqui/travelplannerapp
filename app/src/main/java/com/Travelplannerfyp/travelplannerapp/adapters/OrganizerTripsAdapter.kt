package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.fragments.OrganizerTripsFragment.OrganizerTripData
import com.Travelplannerfyp.travelplannerapp.fragments.OrganizerTripsFragment.HouseBooking
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser
import com.Travelplannerfyp.travelplannerapp.adapters.EnrolledUserAdapter
import java.text.SimpleDateFormat
import java.util.*

class OrganizerTripsAdapter(
    private val organizerTrips: List<OrganizerTripData>,
    private val houseBookings: List<HouseBooking>,
    private val onTripClick: (OrganizerTripData) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TRIP = 0
        private const val VIEW_TYPE_HOUSE_BOOKING = 1
        private const val VIEW_TYPE_SECTION_HEADER = 2
    }

    private val items = mutableListOf<Any>()

    init {
        updateItems()
    }

    private fun updateItems() {
        items.clear()
        
        // Add trips section
        if (organizerTrips.isNotEmpty()) {
            items.add("Enrolled Users in Trips")
            items.addAll(organizerTrips)
        }
        
        // Add house bookings section
        if (houseBookings.isNotEmpty()) {
            items.add("House Bookings")
            items.addAll(houseBookings)
        }
    }

    inner class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.section_header_text)

        fun bind(header: String) {
            headerText.text = header
        }
    }

    inner class OrganizerTripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tripImage: ImageView = itemView.findViewById(R.id.trip_image)
        val tripName: TextView = itemView.findViewById(R.id.trip_name)
        val tripDates: TextView = itemView.findViewById(R.id.trip_dates)
        val tripLocation: TextView = itemView.findViewById(R.id.trip_location)
        val enrolledUsersCount: TextView = itemView.findViewById(R.id.enrolled_users_count)
        val enrolledUsersRecyclerView: RecyclerView = itemView.findViewById(R.id.enrolled_users_recycler_view)
        val noEnrolledUsersText: TextView = itemView.findViewById(R.id.no_enrolled_users_text)

        fun bind(tripData: OrganizerTripData) {
            val trip = tripData.trip
            val enrolledUsers = tripData.enrolledUsers

            // Set trip information
            tripName.text = trip.placeName
            tripDates.text = "${trip.startDate} - ${trip.endDate}"
            tripLocation.text = trip.placeDescription
            enrolledUsersCount.text = "${enrolledUsers.size} enrolled user${if (enrolledUsers.size != 1) "s" else ""}"

            // Set trip image
            val resId = itemView.context.resources.getIdentifier(
                trip.placeImageUrl.lowercase(), "drawable", itemView.context.packageName
            )
            if (resId != 0) {
                tripImage.setImageResource(resId)
            } else {
                tripImage.setImageResource(R.drawable.ic_placeholder)
            }

            // Setup enrolled users RecyclerView
            if (enrolledUsers.isNotEmpty()) {
                noEnrolledUsersText.visibility = View.GONE
                enrolledUsersRecyclerView.visibility = View.VISIBLE
                
                enrolledUsersRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                enrolledUsersRecyclerView.adapter = EnrolledUserAdapter(enrolledUsers)
            } else {
                noEnrolledUsersText.visibility = View.VISIBLE
                enrolledUsersRecyclerView.visibility = View.GONE
                noEnrolledUsersText.text = "No users enrolled yet"
            }

            // Set click listener
            itemView.setOnClickListener {
                onTripClick(tripData)
            }
        }
    }

    inner class HouseBookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val houseName: TextView = itemView.findViewById(R.id.house_name)
        private val houseLocation: TextView = itemView.findViewById(R.id.house_location)
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val userEmail: TextView = itemView.findViewById(R.id.user_email)
        private val userPhone: TextView = itemView.findViewById(R.id.user_phone)
        private val bookingDates: TextView = itemView.findViewById(R.id.booking_dates)
        private val numberOfNights: TextView = itemView.findViewById(R.id.number_of_nights)
        private val totalPrice: TextView = itemView.findViewById(R.id.total_price)
        private val bookingStatus: TextView = itemView.findViewById(R.id.booking_status)
        private val bookingDate: TextView = itemView.findViewById(R.id.booking_date)

        fun bind(booking: HouseBooking) {
            houseName.text = booking.houseName
            houseLocation.text = booking.houseLocation
            userName.text = booking.userName
            userEmail.text = booking.userEmail
            userPhone.text = "Phone: ${booking.userPhone}"
            bookingDates.text = "${booking.checkInDate} - ${booking.checkOutDate}"
            numberOfNights.text = "${booking.numberOfNights} night${if (booking.numberOfNights != 1) "s" else ""}"
            totalPrice.text = "Price: ${booking.totalPrice}"
            bookingStatus.text = booking.status
            bookingDate.text = "Booked: ${formatDate(booking.bookingDate)}"
        }

        private fun formatDate(timestamp: Long): String {
            if (timestamp == 0L) return "-"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> VIEW_TYPE_SECTION_HEADER
            is OrganizerTripData -> VIEW_TYPE_TRIP
            is HouseBooking -> VIEW_TYPE_HOUSE_BOOKING
            else -> VIEW_TYPE_TRIP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            VIEW_TYPE_TRIP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_organizer_trip, parent, false)
                OrganizerTripViewHolder(view)
            }
            VIEW_TYPE_HOUSE_BOOKING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_house_booking, parent, false)
                HouseBookingViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionHeaderViewHolder -> {
                val header = items[position] as String
                holder.bind(header)
            }
            is OrganizerTripViewHolder -> {
                val tripData = items[position] as OrganizerTripData
                holder.bind(tripData)
            }
            is HouseBookingViewHolder -> {
                val booking = items[position] as HouseBooking
                holder.bind(booking)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newTrips: List<OrganizerTripData>, newBookings: List<HouseBooking>) {
        // Update the data and refresh the items list
        // This would be called from the fragment when data changes
        updateItems()
        notifyDataSetChanged()
    }
} 