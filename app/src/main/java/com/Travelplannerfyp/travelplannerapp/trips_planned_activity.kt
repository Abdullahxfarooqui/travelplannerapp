package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.EnrolledUserAdapter
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.Travelplannerfyp.travelplannerapp.R


class trips_planned_activity : AppCompatActivity() {
    private lateinit var tripImageView: ImageView
    private lateinit var tripNameTextView: TextView
    private lateinit var tripDateTextView: TextView
    private lateinit var tripDescriptionTextView: TextView
    private lateinit var enrolledUsersRecyclerView: RecyclerView

    private lateinit var tripId: String
    private lateinit var enrolledUsersAdapter: EnrolledUserAdapter
    private val enrolledUsersList = mutableListOf<EnrolledUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trips_planned2)

        // Initialize views
        tripImageView = findViewById(R.id.trip_image)
        tripNameTextView = findViewById(R.id.trip_name)
        tripDateTextView = findViewById(R.id.trip_dates)
        tripDescriptionTextView = findViewById(R.id.trip_description)
        enrolledUsersRecyclerView = findViewById(R.id.recycler_view_enrolled_users)

        // Get data from intent
        tripId = intent.getStringExtra("TRIP_ID") ?: ""
        val imageName = intent.getStringExtra("TRIP_IMAGE") ?: ""
        val tripName = intent.getStringExtra("TRIP_NAME") ?: "Unknown Trip"
        val tripDates = intent.getStringExtra("TRIP_DATES") ?: "No dates"
        val tripDescription = intent.getStringExtra("TRIP_DESCRIPTION") ?: "No description"

        // Set UI values
        tripNameTextView.text = tripName
        tripDateTextView.text = tripDates
        tripDescriptionTextView.text = tripDescription

        val resId = resources.getIdentifier(imageName.lowercase(), "drawable", packageName)
        if (resId != 0) {
            tripImageView.setImageResource(resId)
        } else {
            tripImageView.setImageResource(R.drawable.ic_placeholder)
        }

        // Setup RecyclerView
        enrolledUsersRecyclerView.layoutManager = LinearLayoutManager(this)
        enrolledUsersAdapter = EnrolledUserAdapter(enrolledUsersList)
        enrolledUsersRecyclerView.adapter = enrolledUsersAdapter

        // Fetch enrolled users from Firebase
        fetchEnrolledUsers()
    }

    private fun fetchEnrolledUsers() {
        val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
        bookingsRef.orderByChild("itemId").equalTo(tripId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    enrolledUsersList.clear()
                    for (child in snapshot.children) {
                        val status = child.child("status").getValue(String::class.java) ?: ""
                        if (status == "Confirmed" || status == "Pending") {
                            val userId = child.child("userId").getValue(String::class.java) ?: ""
                            val name = child.child("userName").getValue(String::class.java)
                                ?: child.child("name").getValue(String::class.java) ?: "Unknown"
                            val email = child.child("userEmail").getValue(String::class.java)
                                ?: child.child("email").getValue(String::class.java) ?: "N/A"
                            val phone = child.child("userPhone").getValue(String::class.java)
                                ?: child.child("phone").getValue(String::class.java) ?: "N/A"
                            val seats = child.child("numberOfGuests").getValue(Int::class.java)
                                ?: child.child("seats").getValue(Int::class.java) ?: 1
                            val bookingTime = child.child("createdAt").getValue(Long::class.java) ?: 0L
                            enrolledUsersList.add(
                                com.Travelplannerfyp.travelplannerapp.models.EnrolledUser(
                                    userId = userId,
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    seats = seats,
                                    bookingTime = bookingTime
                                )
                            )
                        }
                    }
                    // Sort by booking time
                    enrolledUsersList.sortBy { it.bookingTime }
                    enrolledUsersAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@trips_planned_activity, "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            })
    }
}