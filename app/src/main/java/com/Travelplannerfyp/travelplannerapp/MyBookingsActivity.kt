package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.Travelplannerfyp.travelplannerapp.models.BookingStatus
import com.Travelplannerfyp.travelplannerapp.models.BookingType
import com.Travelplannerfyp.travelplannerapp.repository.BookingRepository
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MyBookingsActivity : AppCompatActivity() {
    
    @Inject
    lateinit var bookingRepository: BookingRepository
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var allBookingsChip: Chip
    private lateinit var activeBookingsChip: Chip
    private lateinit var completedBookingsChip: Chip
    private lateinit var cancelledBookingsChip: Chip
    
    private lateinit var adapter: BookingAdapter
    private val bookingsList = mutableListOf<Booking>()
    private val allBookings = mutableListOf<Booking>()
    private var currentFilter: BookingStatus? = null
    
    companion object {
        private const val TAG = "MyBookingsActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)
        
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        loadBookings()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.bookingsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        allBookingsChip = findViewById(R.id.allBookingsChip)
        activeBookingsChip = findViewById(R.id.activeBookingsChip)
        completedBookingsChip = findViewById(R.id.completedBookingsChip)
        cancelledBookingsChip = findViewById(R.id.cancelledBookingsChip)
    }
    
    private fun setupToolbar() {
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "My Bookings"
        }
    }
    
    private fun setupRecyclerView() {
        adapter = BookingAdapter(bookingsList) { booking ->
            showBookingDetails(booking)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupFilterChips() {
        allBookingsChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = null
                filterBookings()
            }
        }
        
        activeBookingsChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = BookingStatus.CONFIRMED
                filterBookings()
            }
        }
        
        completedBookingsChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = BookingStatus.COMPLETED
                filterBookings()
            }
        }
        
        cancelledBookingsChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = BookingStatus.CANCELLED
                filterBookings()
            }
        }
    }
    
    private fun loadBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view your bookings", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.getUserBookings(currentUser.uid)
                if (result.isSuccess) {
                    val bookings = result.getOrNull() ?: emptyList()
                    allBookings.clear()
                    allBookings.addAll(bookings)
                    filterBookings()
                    updateEmptyView()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(this@MyBookingsActivity, "Failed to load bookings: $error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bookings: ${e.message}")
                Toast.makeText(this@MyBookingsActivity, "Error loading bookings", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun filterBookings() {
        bookingsList.clear()
        
        val filteredBookings = if (currentFilter != null) {
            allBookings.filter { it.status == currentFilter }
        } else {
            allBookings
        }
        
        bookingsList.addAll(filteredBookings.sortedByDescending { it.createdAt })
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }
    
    private fun updateEmptyView() {
        if (bookingsList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            
            val message = when (currentFilter) {
                BookingStatus.CONFIRMED -> "No active bookings found"
                BookingStatus.COMPLETED -> "No completed bookings found"
                BookingStatus.CANCELLED -> "No cancelled bookings found"
                else -> "No bookings found"
            }
            emptyView.text = message
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun showBookingDetails(booking: Booking) {
        // Create custom dialog with professional layout
        val dialog = AlertDialog.Builder(this)
            .create()
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_details, null)
        dialog.setView(dialogView)
        
        // Set dialog properties
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // Populate dialog views
        populateBookingDetailsDialog(dialogView, booking)
        
        // Set up buttons
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            dialog.dismiss()
        }
        
        // Add cancel booking option for active bookings
        if (booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.PENDING) {
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel Booking") { _, _ ->
                dialog.dismiss()
                showCancelBookingDialog(booking)
            }
        }
        
        dialog.show()
    }
    
    private fun populateBookingDetailsDialog(dialogView: View, booking: Booking) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val createdDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        
        // Set booking title and confirmation code
        dialogView.findViewById<TextView>(R.id.bookingTitle).text = booking.itemName
        dialogView.findViewById<TextView>(R.id.bookingConfirmationCode).text = "Code: ${booking.confirmationCode}"
        
        // Set status badge with appropriate color
        val statusBadge = dialogView.findViewById<TextView>(R.id.bookingStatusBadge)
        statusBadge.text = booking.status.name
        val statusColor = when (booking.status) {
            BookingStatus.CONFIRMED -> ContextCompat.getColor(this, R.color.booking_status_confirmed)
            BookingStatus.PENDING -> ContextCompat.getColor(this, R.color.booking_status_pending)
            BookingStatus.CANCELLED -> ContextCompat.getColor(this, R.color.booking_status_cancelled)
            BookingStatus.COMPLETED -> ContextCompat.getColor(this, R.color.booking_status_completed)
            BookingStatus.REFUNDED -> ContextCompat.getColor(this, R.color.booking_status_refunded)
            else -> ContextCompat.getColor(this, R.color.gray)
        }
        statusBadge.setBackgroundColor(statusColor)
        
        // Set booking type
        dialogView.findViewById<TextView>(R.id.bookingTypeText).text = 
            if (booking.bookingType == BookingType.TRIP) "Trip" else "Property"
        
        // Set dates
        dialogView.findViewById<TextView>(R.id.bookingDatesText).text = 
            "${booking.startDate} - ${booking.endDate}"
        
        // Set guests/seats
        val guestText = if (booking.bookingType == BookingType.TRIP) {
            "${booking.numberOfGuests} seats"
        } else {
            "${booking.numberOfGuests} guests"
        }
        dialogView.findViewById<TextView>(R.id.bookingGuestsText).text = guestText
        
        // Set payment status
        dialogView.findViewById<TextView>(R.id.bookingPaymentText).text = booking.paymentStatus.name
        
        // Set pricing details
        dialogView.findViewById<TextView>(R.id.bookingBasePriceText).text = 
            CurrencyUtils.formatAsPKR(booking.basePrice)
        dialogView.findViewById<TextView>(R.id.bookingServiceFeeText).text = 
            CurrencyUtils.formatAsPKR(booking.serviceFee)
        dialogView.findViewById<TextView>(R.id.bookingTotalAmountText).text = 
            CurrencyUtils.formatAsPKR(booking.totalAmount)
        
        // Set host information
        dialogView.findViewById<TextView>(R.id.bookingHostNameText).text = booking.hostName
        dialogView.findViewById<TextView>(R.id.bookingHostContactText).text = booking.hostPhone
        
        // Set special requests if any
        val specialRequestsCard = dialogView.findViewById<View>(R.id.specialRequestsCard)
        val specialRequestsText = dialogView.findViewById<TextView>(R.id.bookingSpecialRequestsText)
        if (booking.specialRequests.isNotEmpty()) {
            specialRequestsCard.visibility = View.VISIBLE
            specialRequestsText.text = booking.specialRequests
        } else {
            specialRequestsCard.visibility = View.GONE
        }
        
        // Set booking creation date
        dialogView.findViewById<TextView>(R.id.bookingCreatedDateText).text = 
            "Booked on: ${createdDateFormat.format(Date(booking.createdAt))}"
        
        // Set booking type icon
        val bookingTypeIcon = dialogView.findViewById<ImageView>(R.id.bookingTypeIcon)
        if (booking.bookingType == BookingType.TRIP) {
            bookingTypeIcon.setImageResource(R.drawable.ic_trips)
        } else {
            bookingTypeIcon.setImageResource(R.drawable.ic_plan)
        }
    }
    
    private fun showCancelBookingDialog(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking? This action cannot be undone.")
            .setPositiveButton("Cancel Booking") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("Keep Booking", null)
            .show()
    }
    
    private fun cancelBooking(booking: Booking) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.cancelBooking(booking.id, "Cancelled by user")
                if (result.isSuccess) {
                    Toast.makeText(this@MyBookingsActivity, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                    loadBookings() // Reload to update the list
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(this@MyBookingsActivity, "Failed to cancel booking: $error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling booking: ${e.message}")
                Toast.makeText(this@MyBookingsActivity, "Error cancelling booking", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Booking Adapter
class BookingAdapter(
    private val bookings: List<Booking>,
    private val onItemClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }
    
    override fun getItemCount(): Int = bookings.size
    
    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val bookingTypeTextView: TextView = itemView.findViewById(R.id.bookingTypeTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val confirmationCodeTextView: TextView = itemView.findViewById(R.id.confirmationCodeTextView)
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(bookings[position])
                }
            }
        }
        
        fun bind(booking: Booking) {
            itemNameTextView.text = booking.itemName
            bookingTypeTextView.text = if (booking.bookingType == BookingType.TRIP) "Trip" else "Property"
            statusTextView.text = booking.status.name
            dateTextView.text = "${booking.startDate} - ${booking.endDate}"
            amountTextView.text = CurrencyUtils.formatAsPKR(booking.totalAmount)
            confirmationCodeTextView.text = "Code: ${booking.confirmationCode}"
            
            // Set status color using color resources for better accessibility
            val statusColor = when (booking.status) {
                BookingStatus.CONFIRMED -> ContextCompat.getColor(itemView.context, R.color.booking_status_confirmed)
                BookingStatus.PENDING -> ContextCompat.getColor(itemView.context, R.color.booking_status_pending)
                BookingStatus.CANCELLED -> ContextCompat.getColor(itemView.context, R.color.booking_status_cancelled)
                BookingStatus.COMPLETED -> ContextCompat.getColor(itemView.context, R.color.booking_status_completed)
                BookingStatus.REFUNDED -> ContextCompat.getColor(itemView.context, R.color.booking_status_refunded)
                else -> ContextCompat.getColor(itemView.context, R.color.gray)
            }
            statusTextView.setTextColor(statusColor)
        }
    }
} 