package com.Travelplannerfyp.travelplannerapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
  import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.TripAdminAdapter
import com.Travelplannerfyp.travelplannerapp.adapters.UserAdapter
import com.Travelplannerfyp.travelplannerapp.adapters.BookingAdapter
import com.Travelplannerfyp.travelplannerapp.adapters.FeedbackAdapter
import com.Travelplannerfyp.travelplannerapp.adapters.AnnouncementAdapter
import com.Travelplannerfyp.travelplannerapp.model.TripAdmin
import com.Travelplannerfyp.travelplannerapp.model.User
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.Travelplannerfyp.travelplannerapp.models.Feedback
import com.Travelplannerfyp.travelplannerapp.models.Announcement
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import com.Travelplannerfyp.travelplannerapp.LoginActivity
import android.util.Log
import android.widget.Button
import com.google.android.material.button.MaterialButton

class AdminDashboardFragment : Fragment() {

    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalBookings: TextView
    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalTrips: TextView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var emptyLayout: LinearLayout
    private lateinit var recyclerTrips: RecyclerView
    private lateinit var btnAddTrip: View
    private lateinit var tripAdapter: TripAdminAdapter
    private val tripsList = mutableListOf<TripAdmin>()

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val usersList = mutableListOf<User>()

    private lateinit var recyclerBookings: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val bookingsList = mutableListOf<Booking>()

    private lateinit var recyclerFeedback: RecyclerView
    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackList = mutableListOf<Feedback>()

    private lateinit var recyclerAnnouncements: RecyclerView
    private lateinit var btnAddAnnouncement: View
    private lateinit var announcementAdapter: AnnouncementAdapter
    private val announcementsList = mutableListOf<Announcement>()

    private lateinit var btnExportBookings: View
    private lateinit var btnExportRevenue: View
    private lateinit var btnLogout: View

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val bookingsRef = database.getReference("bookings")
    private val tripsRef = database.getReference("trips")

    private var totalUsers = 0
    private var totalBookings = 0
    private var totalRevenue = 0.0
    private var totalTrips = 0

    private val bookingsData = mutableMapOf<String, Int>()
    private val revenueData = mutableMapOf<String, Double>()

    // Store sorted date keys for chart axis
    private var sortedBookingDateKeys: List<String> = emptyList()
    private var sortedRevenueDateKeys: List<String> = emptyList()

    // Add a button to open the bulk edit dialog (call this in onViewCreated)
    private lateinit var btnBulkEditTitles: Button

    private var tripsListener: ValueEventListener? = null
    private var usersListener: ValueEventListener? = null
    private var bookingsListener: ValueEventListener? = null
    private var feedbackListener: ValueEventListener? = null
    private var announcementsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove all Firebase listeners to prevent crashes
        tripsListener?.let { FirebaseDatabase.getInstance().getReference("trips").removeEventListener(it) }
        usersListener?.let { FirebaseDatabase.getInstance().getReference("users").removeEventListener(it) }
        bookingsListener?.let { FirebaseDatabase.getInstance().getReference("bookings").removeEventListener(it) }
        feedbackListener?.let { FirebaseDatabase.getInstance().getReference("feedback").removeEventListener(it) }
        announcementsListener?.let { FirebaseDatabase.getInstance().getReference("announcements").removeEventListener(it) }
    }

    // One-time migration: Fix existing trips with missing/invalid title or price
    private fun fixTripsDataIfNeeded() {
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
        tripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tripSnap in snapshot.children) {
                    val id = tripSnap.key ?: continue
                    val title = tripSnap.child("title").getValue(String::class.java) ?: ""
                    val price = tripSnap.child("price").getValue(String::class.java) ?: ""
                    var needsUpdate = false
                    val updates = mutableMapOf<String, Any>()
                    if (title.isEmpty()) {
                        updates["title"] = "Untitled Trip"
                        needsUpdate = true
                    }
                    if (price.isEmpty() || price == "0") {
                        updates["price"] = "10000" // Set a sensible default
                        needsUpdate = true
                    }
                    if (needsUpdate) {
                        tripsRef.child(id).updateChildren(updates)
                        Log.d("TripMigration", "Fixed trip $id: $updates")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("TripMigration", "Migration failed: ${error.message}")
            }
        })
    }

    // One-time migration: Copy placeName or name to title if title is missing
    private fun migrateTripTitlesIfNeeded() {
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
        tripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tripSnap in snapshot.children) {
                    val id = tripSnap.key ?: continue
                    val title = tripSnap.child("title").getValue(String::class.java) ?: ""
                    if (title.isEmpty() || title == "Untitled Trip") {
                        val placeName = tripSnap.child("placeName").getValue(String::class.java)
                        val name = tripSnap.child("name").getValue(String::class.java)
                        val newTitle = placeName ?: name
                        if (!newTitle.isNullOrEmpty()) {
                            tripsRef.child(id).child("title").setValue(newTitle)
                            Log.d("TripMigration", "Migrated trip $id: set title='$newTitle'")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("TripMigration", "Migration failed: ${error.message}")
            }
        })
    }

    private fun ensureAdminUserExists() {
        val adminUid = "QSYFRzkmQEa4vqy30CC6CaA8ACq1"
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(adminUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val adminData = mapOf(
                        "name" to "Admin",
                        "email" to "admin@email.com",
                        "createdAt" to System.currentTimeMillis(),
                        "status" to "active",
                        "admin" to true
                    )
                    usersRef.child(adminUid).setValue(adminData)
                        .addOnSuccessListener { Log.d("AdminUser", "Admin user created.") }
                        .addOnFailureListener { e -> Log.e("AdminUser", "Failed to create admin user: ${e.message}") }
                } else {
                    Log.d("AdminUser", "Admin user already exists.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminUser", "Failed to check admin user: ${error.message}")
            }
        })
    }

    private fun addSampleUsers() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val sampleUsers = mapOf(
            "user1" to mapOf(
                "name" to "John Doe",
                "email" to "john@example.com",
                "createdAt" to System.currentTimeMillis() - 86400000, // 1 day ago
                "status" to "active"
            ),
            "user2" to mapOf(
                "name" to "Jane Smith",
                "email" to "jane@example.com",
                "createdAt" to System.currentTimeMillis() - 172800000, // 2 days ago
                "status" to "active"
            ),
            "user3" to mapOf(
                "name" to "Bob Johnson",
                "email" to "bob@example.com",
                "createdAt" to System.currentTimeMillis() - 259200000, // 3 days ago
                "status" to "active"
            )
        )
        
        sampleUsers.forEach { (uid, userData) ->
            usersRef.child(uid).setValue(userData)
                .addOnSuccessListener { Log.d("SampleUsers", "Added user: $uid") }
                .addOnFailureListener { e -> Log.e("SampleUsers", "Failed to add user $uid: ${e.message}") }
        }
        
        Toast.makeText(requireContext(), "Sample users added. Refreshing...", Toast.LENGTH_SHORT).show()
        fetchUsers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupCharts()
        loadDashboardData()
        recyclerTrips = view.findViewById(R.id.recyclerTrips)
        btnAddTrip = view.findViewById(R.id.btnAddTrip)
        tripAdapter = TripAdminAdapter(tripsList,
            onDeleteClick = { trip -> deleteTrip(trip) },
            onFeatureClick = { /* Optional: implement feature logic */ })
        recyclerTrips.layoutManager = LinearLayoutManager(requireContext())
        recyclerTrips.adapter = tripAdapter
        fetchTrips()
        btnAddTrip.setOnClickListener { showAddTripDialog() }

        recyclerUsers = view.findViewById(R.id.recyclerUsers)
        userAdapter = UserAdapter(usersList,
            onEdit = { user -> /* Optional: implement edit logic */ },
            onBlock = { user -> blockUser(user) },
            onDelete = { user -> deleteUser(user) })
        recyclerUsers.layoutManager = LinearLayoutManager(requireContext())
        recyclerUsers.adapter = userAdapter
        fetchUsers()

        recyclerBookings = view.findViewById(R.id.recyclerBookings)
        bookingAdapter = BookingAdapter(bookingsList) { bookingId, newStatus ->
            val ref = FirebaseDatabase.getInstance().getReference("bookings")
            ref.child(bookingId).child("status").setValue(newStatus)
        }
        recyclerBookings.layoutManager = LinearLayoutManager(requireContext())
        recyclerBookings.adapter = bookingAdapter
        fetchBookings()

        recyclerFeedback = view.findViewById(R.id.recyclerFeedback)
        feedbackAdapter = FeedbackAdapter(feedbackList, onDelete = { feedback -> deleteFeedback(feedback) })
        recyclerFeedback.layoutManager = LinearLayoutManager(requireContext())
        recyclerFeedback.adapter = feedbackAdapter
        fetchFeedback()

        // Find the export CSV button in the new layout
        val exportCsvButton = view.findViewById<MaterialButton>(R.id.exportCsvButton)
        exportCsvButton?.setOnClickListener { exportBookingsCsv() }

        // Find the add announcement button in the new layout
        val addAnnouncementButton = view.findViewById<MaterialButton>(R.id.addAnnouncementButton)
        addAnnouncementButton?.setOnClickListener { showAddAnnouncementDialog() }

        enforceAdminSecurity()
        tripAdapter.onEditTripListener = object : TripAdminAdapter.OnEditTripListener {
            override fun onEditTrip(trip: TripAdmin) {
                showEditTripTitleDialog(trip)
            }
        }
        
        // Add sample users button
        val btnAddSampleUsers = Button(requireContext()).apply {
            text = "Add Sample Users"
            setOnClickListener { addSampleUsers() }
        }
        
        // Find the main container and add the sample users button
        val mainContainer = view.findViewById<LinearLayout>(R.id.admin_dashboard_linear_layout)
        mainContainer?.addView(btnAddSampleUsers, 0)
        
        ensureAdminUserExists()
        migrateTripTitlesIfNeeded()
        fixTripsDataIfNeeded()
    }

    private fun initializeViews(view: View) {
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers)
        tvTotalBookings = view.findViewById(R.id.tvTotalBookings)
        tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue)
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips)
        
        // Initialize charts if they exist in the layout
        // Charts removed from new Material 3 layout
    }

    private fun setupCharts() {
        // Charts removed from new Material 3 layout
        // This method is kept for compatibility but does nothing
    }

    private fun updateCharts() {
        // Charts removed from new Material 3 layout
        // This method is kept for compatibility but does nothing
    }

    private fun formatDateForChart(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date)
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        emptyLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
        emptyLayout.visibility = View.GONE
    }

    private fun showEmpty() {
        loadingLayout.visibility = View.GONE
        emptyLayout.visibility = View.VISIBLE
    }

    private fun fetchTrips() {
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
        tripsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripsList.clear()
                for (tripSnap in snapshot.children) {
                    val id = tripSnap.key ?: ""
                    val title = tripSnap.child("title").getValue(String::class.java) ?: ""
                    Log.d("TripDebug", "Fetched trip: id=$id, title='$title'")
                    val organizerId = tripSnap.child("organizerId").getValue(String::class.java) ?: ""
                    val organizerName = tripSnap.child("organizerName").getValue(String::class.java) ?: ""
                    val location = tripSnap.child("location").getValue(String::class.java) ?: ""
                    val status = tripSnap.child("status").getValue(String::class.java) ?: "pending"
                    val priceValue = tripSnap.child("price").value
                    val price = when (priceValue) {
                        is Number -> priceValue.toString()
                        is String -> priceValue
                        else -> ""
                    }
                    val seatsAvailableValue = tripSnap.child("seatsAvailable").value
                    val seatsAvailable = when (seatsAvailableValue) {
                        is Number -> (seatsAvailableValue as Number).toInt()
                        is String -> (seatsAvailableValue as String).toIntOrNull() ?: 0
                        else -> 0
                    }
                    tripsList.add(
                        TripAdmin(
                            id = id,
                            title = title,
                            organizerId = organizerId,
                            organizerName = organizerName,
                            location = location,
                            price = price,
                            status = status,
                            seatsAvailable = seatsAvailable
                        )
                    )
                }
                tripAdapter.updateData(tripsList)
                Log.d("AdminDashboard", "Loaded trips: ${tripsList.size}")
                if (tripsList.isEmpty()) {
                    context?.let { Toast.makeText(it, "No trips found in the database.", Toast.LENGTH_SHORT).show() }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                context?.let { Toast.makeText(it, "Failed to load trips: ${error.message}", Toast.LENGTH_LONG).show() }
                Log.e("AdminDashboard", "Failed to load trips: ${error.message}")
            }
        }
        tripsRef.addValueEventListener(tripsListener!!)
    }

    private fun showAddTripDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_trip, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputTripTitle)
        val locationInput = dialogView.findViewById<EditText>(R.id.inputTripLocation)
        val priceInput = dialogView.findViewById<EditText>(R.id.inputTripPrice)
        AlertDialog.Builder(requireContext())
            .setTitle("Add Trip")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val titleText = titleInput.text.toString().trim()
                val priceText = priceInput.text.toString().trim()
                val priceValue = priceText.toDoubleOrNull()
                if (titleText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a trip title.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (priceValue == null || priceValue <= 0.0) {
                    Toast.makeText(requireContext(), "Please enter a valid price greater than 0.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val trip = TripAdmin(
                    id = FirebaseDatabase.getInstance().getReference("trips").push().key ?: "",
                    title = titleText,
                    location = locationInput.text.toString(),
                    price = priceValue.toString(),
                    organizerId = "",
                    organizerName = "",
                    status = "active"
                )
                FirebaseDatabase.getInstance().getReference("trips").child(trip.id).setValue(trip)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip(trip: TripAdmin) {
        if (trip.id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Trip ID is missing. Cannot delete.", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseDatabase.getInstance().getReference("trips").child(trip.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Trip deleted successfully.", Toast.LENGTH_SHORT).show()
                fetchTrips()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete trip: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchUsers() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    val name = userSnap.child("name").getValue(String::class.java) ?: "(No Name)"
                    val email = userSnap.child("email").getValue(String::class.java) ?: "(No Email)"
                    val createdAt = try { 
                        userSnap.child("createdAt").getValue(Long::class.java) ?: 0L 
                    } catch (e: Exception) { 
                        0L 
                    }
                    val status = userSnap.child("status").getValue(String::class.java) ?: "active"
                    
                    val user = User(uid, name, email, createdAt, status)
                    usersList.add(user)
                    Log.d("UserDebug", "Added user: $name ($email)")
                }
                userAdapter.updateData(usersList)
                Log.d("AdminDashboard", "Loaded users: ${usersList.size}")
                if (usersList.isEmpty()) {
                    context?.let { Toast.makeText(it, "No users found in the database.", Toast.LENGTH_SHORT).show() }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                context?.let { Toast.makeText(it, "Failed to load users: ${error.message}", Toast.LENGTH_LONG).show() }
                Log.e("AdminDashboard", "Failed to load users: ${error.message}")
            }
        }
        usersRef.addValueEventListener(usersListener!!)
    }

    private fun blockUser(user: User) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(user.uid).child("status").setValue("blocked")
    }

    private fun deleteUser(user: User) {
        if (user.uid.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User ID is missing. Cannot delete.", Toast.LENGTH_SHORT).show()
            return
        }
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(user.uid).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User deleted successfully.", Toast.LENGTH_SHORT).show()
                fetchUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchBookings() {
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookingsList.clear()
                Log.d("AdminDashboard", "Fetching bookings from Firebase...")
                
                for (bookingSnap in snapshot.children) {
                    Log.d("AdminDashboard", "Processing booking: ${bookingSnap.key}")
                    
                    if (bookingSnap.value is Map<*, *>) {
                        val booking = bookingSnap.getValue(Booking::class.java)
                        if (booking != null) {
                            bookingsList.add(booking)
                            Log.d("AdminDashboard", "Added booking: ID=${booking.id}, Type=${booking.bookingType}, Amount=${booking.totalAmount}, Status=${booking.status}")
                        } else {
                            Log.e("AdminDashboard", "Failed to parse booking: ${bookingSnap.key}")
                        }
                    } else {
                        Log.e("BookingError", "Invalid booking data: ${bookingSnap.value}")
                    }
                }
                
                bookingAdapter.updateData(bookingsList)
                Log.d("AdminDashboard", "Loaded bookings: ${bookingsList.size}")
                if (bookingsList.isEmpty()) {
                    context?.let { Toast.makeText(it, "No bookings found in the database.", Toast.LENGTH_SHORT).show() }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                context?.let { Toast.makeText(it, "Failed to load bookings: ${error.message}", Toast.LENGTH_LONG).show() }
                Log.e("AdminDashboard", "Failed to load bookings: ${error.message}")
            }
        }
        bookingsRef.addValueEventListener(bookingsListener!!)
    }

    private fun updateBookingStatus(booking: Booking, status: String) {
        if (booking.id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Booking ID is missing. Cannot update status.", Toast.LENGTH_SHORT).show()
            return
        }
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.child(booking.id).child("status").setValue(status)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Booking status updated to $status.", Toast.LENGTH_SHORT).show()
                // Update local list for instant UI feedback
                val index = bookingsList.indexOfFirst { it.id == booking.id }
                if (index != -1) {
                    try {
                        bookingsList[index] = bookingsList[index].copy(status = status)
                        bookingAdapter.notifyItemChanged(index)
                    } catch (e: Exception) {
                        // Fallback: just refresh the list
                        bookingAdapter.notifyDataSetChanged()
                    }
                }
                fetchBookings() // Still fetch to stay in sync with Firebase
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchFeedback() {
        val feedbackRef = FirebaseDatabase.getInstance().getReference("feedback")
        feedbackListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                feedbackList.clear()
                for (feedbackSnap in snapshot.children) {
                    val feedback = feedbackSnap.getValue(Feedback::class.java)
                    if (feedback != null) feedbackList.add(feedback)
                }
                feedbackAdapter.updateData(feedbackList)
                Log.d("AdminDashboard", "Loaded feedback: ${feedbackList.size}")
                if (feedbackList.isEmpty()) {
                    context?.let { Toast.makeText(it, "No feedback found in the database.", Toast.LENGTH_SHORT).show() }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                context?.let { Toast.makeText(it, "Failed to load feedback: ${error.message}", Toast.LENGTH_LONG).show() }
                Log.e("AdminDashboard", "Failed to load feedback: ${error.message}")
            }
        }
        feedbackRef.addValueEventListener(feedbackListener!!)
    }

    private fun deleteFeedback(feedback: Feedback) {
        val feedbackRef = FirebaseDatabase.getInstance().getReference("feedback")
        feedbackRef.child(feedback.id).removeValue()
    }

    private fun fetchAnnouncements() {
        val announcementsRef = FirebaseDatabase.getInstance().getReference("announcements")
        announcementsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                announcementsList.clear()
                for (announcementSnap in snapshot.children) {
                    val announcement = announcementSnap.getValue(Announcement::class.java)
                    if (announcement != null) announcementsList.add(announcement)
                }
                announcementAdapter.updateData(announcementsList)
                Log.d("AdminDashboard", "Loaded announcements: ${announcementsList.size}")
                if (announcementsList.isEmpty()) {
                    context?.let { Toast.makeText(it, "No announcements found in the database.", Toast.LENGTH_SHORT).show() }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                context?.let { Toast.makeText(it, "Failed to load announcements: ${error.message}", Toast.LENGTH_LONG).show() }
                Log.e("AdminDashboard", "Failed to load announcements: ${error.message}")
            }
        }
        announcementsRef.addValueEventListener(announcementsListener!!)
    }

    private fun showAddAnnouncementDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_announcement, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.inputAnnouncementTitle)
        val messageInput = dialogView.findViewById<EditText>(R.id.inputAnnouncementMessage)
        AlertDialog.Builder(requireContext())
            .setTitle("Post Announcement")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val announcement = Announcement(
                    id = FirebaseDatabase.getInstance().getReference("announcements").push().key ?: "",
                    title = titleInput.text.toString(),
                    message = messageInput.text.toString()
                )
                FirebaseDatabase.getInstance().getReference("announcements").child(announcement.id).setValue(announcement)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAnnouncement(announcement: Announcement) {
        FirebaseDatabase.getInstance().getReference("announcements").child(announcement.id).removeValue()
    }

    private fun exportBookingsCsv() {
        try {
            if (bookingsList.isEmpty()) return
            val csvHeader = "Booking ID,Trip Name,User Name,Booking Date,Status,Amount\n"
            val csvBody = bookingsList.joinToString("\n") { b ->
                "${b.id},${if (b.tripName.isNotEmpty()) b.tripName else b.itemName},${b.userName},${b.startDate},${b.status},${b.totalAmount}"
            }
            val csv = csvHeader + csvBody
            shareCsvFile(csv, "bookings_export.csv")
        } catch (e: Exception) {
            Log.e("ExportCSV", "Export bookings failed: ${e.message}")
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportRevenueCsv() {
        try {
            if (bookingsList.isEmpty()) return
            val csvHeader = "Booking ID,Trip Name,Amount,Date\n"
            val csvBody = bookingsList.joinToString("\n") { b ->
                "${b.id},${if (b.tripName.isNotEmpty()) b.tripName else b.itemName},${b.totalAmount},${b.startDate}"
            }
            val csv = csvHeader + csvBody
            shareCsvFile(csv, "revenue_export.csv")
        } catch (e: Exception) {
            Log.e("ExportCSV", "Export revenue failed: ${e.message}")
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCsvFile(csv: String, filename: String) {
        val context = requireContext()
        val file = File(context.cacheDir, filename)
        FileWriter(file).use { it.write(csv) }
        val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export CSV"))
    }

    // Update DateAxisFormatter to accept date keys
    private class DateAxisFormatter(private val dateKeys: List<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index in dateKeys.indices) dateKeys[index] else ""
        }
    }

    private fun enforceAdminSecurity() {
        val adminEmail = "abdullahxfarooquii@gmail.com"
        val adminUid = "QSYFRzkmQEa4vqy30CC6CaA8ACq1"
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Unauthorized access. Logging out...", Toast.LENGTH_LONG).show()
            logout()
            return
        }
        
        // Check if user is admin by email, UID, or admin field
        val isAdminByEmail = currentUser.email?.equals(adminEmail, ignoreCase = true) == true
        val isAdminByUid = currentUser.uid == adminUid
        
        if (!isAdminByEmail && !isAdminByUid) {
            // Check Firebase database for admin field
            FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isAdminByField = snapshot.child("admin").getValue(Boolean::class.java) == true ||
                                           snapshot.child("isAdmin").getValue(Boolean::class.java) == true
                        
                        if (!isAdminByField) {
                            Toast.makeText(requireContext(), "Unauthorized access. Logging out...", Toast.LENGTH_LONG).show()
                            logout()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Failed to verify admin status. Logging out...", Toast.LENGTH_LONG).show()
                        logout()
                    }
                })
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showEditTripTitleDialog(trip: TripAdmin) {
        val editText = EditText(requireContext())
        editText.setText(trip.title)
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Trip Title")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isEmpty()) {
                    Toast.makeText(requireContext(), "Title cannot be empty.", Toast.LENGTH_SHORT).show()
                } else {
                    val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
                    tripsRef.child(trip.id).child("title").setValue(newTitle)
                        .addOnSuccessListener {
                            Log.d("TripDebug", "Successfully updated title for trip id=${trip.id} to '$newTitle'")
                            Toast.makeText(requireContext(), "Trip title updated.", Toast.LENGTH_SHORT).show()
                            fetchTrips()
                        }
                        .addOnFailureListener { e ->
                            Log.e("TripDebug", "Failed to update title for trip id=${trip.id}: ${e.message}")
                            Toast.makeText(requireContext(), "Failed to update title: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBulkEditTitlesDialog() {
        val untitledTrips = tripsList.filter { it.title.isEmpty() || it.title == "Untitled Trip" }
        if (untitledTrips.isEmpty()) {
            Toast.makeText(requireContext(), "No untitled trips to edit.", Toast.LENGTH_SHORT).show()
            return
        }
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val editFields = mutableListOf<Pair<TripAdmin, EditText>>()
        untitledTrips.forEach { trip ->
            val label = TextView(requireContext()).apply { text = "Trip ID: ${trip.id}" }
            val edit = EditText(requireContext()).apply {
                hint = "Enter title"
                setText("")
            }
            layout.addView(label)
            layout.addView(edit)
            editFields.add(trip to edit)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Bulk Edit Trip Titles")
            .setView(layout)
            .setPositiveButton("Save All") { dialog, _ ->
                val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
                editFields.forEach { (trip, edit) ->
                    val newTitle = edit.text.toString().trim()
                    if (newTitle.isNotEmpty()) {
                        tripsRef.child(trip.id).child("title").setValue(newTitle)
                        Log.d("TripBulkEdit", "Updated trip ${trip.id} to title '$newTitle'")
                    }
                }
                Toast.makeText(requireContext(), "Titles updated. Refreshing...", Toast.LENGTH_SHORT).show()
                fetchTrips()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDashboardData() {
        // Load users count
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalUsers = snapshot.childrenCount.toInt()
                updateUI()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to load users: ${error.message}")
            }
        })

        // Load trips count
        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalTrips = snapshot.childrenCount.toInt()
                updateUI()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to load trips: ${error.message}")
            }
        })

        // Load bookings and revenue data
        bookingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalBookings = 0
                totalRevenue = 0.0

                for (bookingSnapshot in snapshot.children) {
                    // Only try to map if the node is a Map/object
                    if (bookingSnapshot.value is Map<*, *>) {
                        val booking = bookingSnapshot.getValue(com.Travelplannerfyp.travelplannerapp.models.Booking::class.java)
                        booking?.let {
                            totalBookings++
                            // Parse price
                            val price = it.totalAmount
                            totalRevenue += price
                        }
                    }
                }

                updateUI()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to load bookings: ${error.message}")
            }
        })
    }

    private fun updateUI() {
        tvTotalUsers?.text = totalUsers.toString()
        tvTotalBookings?.text = totalBookings.toString()
        tvTotalRevenue?.text = CurrencyUtils.formatPrice(totalRevenue)
        tvTotalTrips?.text = totalTrips.toString()
    }
} 