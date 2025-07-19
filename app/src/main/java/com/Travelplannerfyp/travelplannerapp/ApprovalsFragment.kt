package com.Travelplannerfyp.travelplannerapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.ApprovalAdapter
import com.Travelplannerfyp.travelplannerapp.model.ApprovalItem
import com.google.firebase.database.*

class ApprovalsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var adapter: ApprovalAdapter
    private val approvalItems = mutableListOf<ApprovalItem>()
    private val db = FirebaseDatabase.getInstance().reference
    private var tripsListener: ValueEventListener? = null
    private var experiencesListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_approvals, container, false)
        recyclerView = view.findViewById(R.id.approvalsRecyclerView)
        progressBar = view.findViewById(R.id.approvalsProgressBar)
        emptyState = view.findViewById(R.id.approvalsEmptyState)
        adapter = ApprovalAdapter(approvalItems, ::onApprove, ::onReject)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fetchApprovals()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners
        tripsListener?.let { db.child("trips").removeEventListener(it) }
        experiencesListener?.let { db.child("experiences").removeEventListener(it) }
    }

    private fun fetchApprovals() {
        progressBar.visibility = View.VISIBLE
        approvalItems.clear()
        adapter.updateData(approvalItems)
        emptyState.visibility = View.GONE
        // Listen for pending trips
        tripsListener = db.child("trips").orderByChild("status").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    approvalItems.removeAll { it.type == "trip" }
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val title = child.child("placeName").getValue(String::class.java) ?: child.child("title").getValue(String::class.java) ?: "(No Title)"
                        val organizerId = child.child("organizerId").getValue(String::class.java) ?: ""
                        val imageUrl = child.child("placeImageUrl").getValue(String::class.java)
                            ?: child.child("imageUrl").getValue(String::class.java) ?: ""
                        if (id.isNotBlank() && organizerId.isNotBlank()) {
                            val item = ApprovalItem(id, "trip", title, organizerId, imageUrl = imageUrl)
                            approvalItems.add(item)
                        }
                    }
                    fetchOrganizerNamesAndUpdate()
                    if (approvalItems.none { it.type == "trip" }) showToast("No pending trips found.")
                }
                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    emptyState.text = "Failed to load trips"
                    emptyState.visibility = View.VISIBLE
                    showToast("Failed to load trips")
                }
            })
        // Listen for pending experiences
        experiencesListener = db.child("experiences").orderByChild("status").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    approvalItems.removeAll { it.type == "experience" }
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val title = child.child("title").getValue(String::class.java) ?: "(No Title)"
                        val organizerId = child.child("organizerId").getValue(String::class.java) ?: ""
                        val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                        if (id.isNotBlank() && organizerId.isNotBlank()) {
                            val item = ApprovalItem(id, "experience", title, organizerId, imageUrl = imageUrl)
                            approvalItems.add(item)
                        }
                    }
                    fetchOrganizerNamesAndUpdate()
                    if (approvalItems.none { it.type == "experience" }) showToast("No pending experiences found.")
                }
                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    emptyState.text = "Failed to load experiences"
                    emptyState.visibility = View.VISIBLE
                    showToast("Failed to load experiences")
                }
            })
    }

    private fun fetchOrganizerNamesAndUpdate() {
        if (approvalItems.isEmpty()) {
            progressBar.visibility = View.GONE
            adapter.updateData(approvalItems)
            emptyState.text = "No pending approvals."
            emptyState.visibility = View.VISIBLE
            return
        }
        emptyState.visibility = View.GONE
        // Fetch organizer names for all unique organizerIds
        val ids = approvalItems.map { it.organizerId }.filter { it.isNotEmpty() }.toSet()
        if (ids.isEmpty()) {
            progressBar.visibility = View.GONE
            adapter.updateData(approvalItems)
            return
        }
        val namesMap = mutableMapOf<String, String>()
        var fetched = 0
        for (id in ids) {
            db.child("organizers").child(id).child("name").get().addOnSuccessListener { snap ->
                namesMap[id] = snap.getValue(String::class.java) ?: "(Unknown Organizer)"
            }.addOnCompleteListener {
                fetched++
                if (fetched == ids.size) {
                    // Update items with names
                    approvalItems.forEach { itCopy ->
                        if (namesMap.containsKey(itCopy.organizerId)) {
                            itCopy.organizerName = namesMap[itCopy.organizerId] ?: "(Unknown Organizer)"
                        }
                    }
                    progressBar.visibility = View.GONE
                    adapter.updateData(approvalItems)
                    emptyState.visibility = if (approvalItems.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun onApprove(item: ApprovalItem) {
        val ref = db.child(if (item.type == "trip") "trips" else "experiences").child(item.id)
        // Disable buttons during action
        progressBar.visibility = View.VISIBLE
        ref.child("status").setValue("approved").addOnSuccessListener {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Approved!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Failed to approve", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onReject(item: ApprovalItem) {
        val input = EditText(requireContext())
        input.hint = "Reason (optional)"
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Listing")
            .setMessage("Enter a reason for rejection (optional):")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString()
                val ref = db.child(if (item.type == "trip") "trips" else "experiences").child(item.id)
                progressBar.visibility = View.VISIBLE
                ref.child("status").setValue("rejected").addOnCompleteListener {
                    progressBar.visibility = View.GONE
                }
                if (reason.isNotBlank()) {
                    ref.child("rejectionReason").setValue(reason)
                }
                Toast.makeText(requireContext(), "Rejected!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }
} 