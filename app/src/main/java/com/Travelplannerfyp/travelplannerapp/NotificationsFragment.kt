package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.NotificationAdapter
import com.Travelplannerfyp.travelplannerapp.model.Notification
import com.google.firebase.database.*

class NotificationsFragment : Fragment() {
    private lateinit var titleEditText: EditText
    private lateinit var bodyEditText: EditText
    private lateinit var audienceSpinner: Spinner
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private val db = FirebaseDatabase.getInstance().reference
    private var notificationsListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        titleEditText = view.findViewById(R.id.notificationTitleEditText)
        bodyEditText = view.findViewById(R.id.notificationBodyEditText)
        audienceSpinner = view.findViewById(R.id.notificationAudienceSpinner)
        sendButton = view.findViewById(R.id.sendNotificationButton)
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        progressBar = view.findViewById(R.id.notificationsProgressBar)
        emptyState = view.findViewById(R.id.notificationsEmptyState)
        adapter = NotificationAdapter(notifications)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        setupAudienceSpinner()
        sendButton.setOnClickListener { sendNotification() }
        fetchNotifications()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationsListener?.let { db.child("notifications").removeEventListener(it) }
    }

    private fun setupAudienceSpinner() {
        val audiences = listOf("All Users", "Only Organizers", "Only Booked Users")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, audiences)
        audienceSpinner.adapter = adapter
    }

    private fun sendNotification() {
        val title = titleEditText.text.toString().trim()
        val body = bodyEditText.text.toString().trim()
        val audience = audienceSpinner.selectedItem.toString()
        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(requireContext(), "Title and message are required", Toast.LENGTH_SHORT).show()
            return
        }
        sendButton.isEnabled = false
        val notification = Notification(
            id = db.child("notifications").push().key ?: System.currentTimeMillis().toString(),
            title = title,
            body = body,
            audience = audience,
            timestamp = System.currentTimeMillis(),
            status = "sent"
        )
        db.child("notifications").child(notification.id).setValue(notification)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Notification sent", Toast.LENGTH_SHORT).show()
                titleEditText.text.clear()
                bodyEditText.text.clear()
                audienceSpinner.setSelection(0)
                sendButton.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send notification", Toast.LENGTH_SHORT).show()
                sendButton.isEnabled = true
            }
        // (Optional) Add FCM send logic here if available
    }

    private fun fetchNotifications() {
        progressBar.visibility = View.VISIBLE
        notifications.clear()
        adapter.updateData(notifications)
        notificationsListener = db.child("notifications").orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                for (child in snapshot.children.reversed()) { // Show newest first
                    val id = child.key ?: continue
                    val title = child.child("title").getValue(String::class.java) ?: "(No Title)"
                    val body = child.child("body").getValue(String::class.java) ?: ""
                    val audience = child.child("audience").getValue(String::class.java) ?: "All Users"
                    val timestamp = try { child.child("timestamp").getValue(Long::class.java) ?: 0L } catch (e: Exception) { 0L }
                    val status = child.child("status").getValue(String::class.java) ?: "sent"
                    notifications.add(Notification(id, title, body, audience, timestamp, status))
                }
                progressBar.visibility = View.GONE
                adapter.updateData(notifications)
                emptyState.text = "No notifications found."
                emptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyState.text = "Failed to load notifications."
                emptyState.visibility = View.VISIBLE
            }
        })
    }
} 