package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.Travelplannerfyp.travelplannerapp.adapters.OrganizerAdapter
import com.Travelplannerfyp.travelplannerapp.model.Organizer

class OrganizersFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var adapter: OrganizerAdapter
    private val organizers = mutableListOf<Organizer>()
    private val db = FirebaseDatabase.getInstance().getReference("organizers")
    private var usersListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_organizers, container, false)
        recyclerView = view.findViewById(R.id.organizersRecyclerView)
        progressBar = view.findViewById(R.id.organizersProgressBar)
        emptyState = view.findViewById(R.id.organizersEmptyState)
        adapter = OrganizerAdapter(organizers, onBlockClick = { organizer -> blockOrganizer(organizer) }, onApproveClick = { organizer -> approveOrganizer(organizer) }, onDeleteClick = { organizer -> deleteOrganizer(organizer) }, onEditClick = { organizer -> editOrganizer(organizer) })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fetchOrganizers()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.let { db.removeEventListener(it) }
    }

    private fun fetchOrganizers() {
        progressBar.visibility = View.VISIBLE
        organizers.clear()
        adapter.updateData(organizers)
        usersListener = db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                organizers.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val email = child.child("email").getValue(String::class.java) ?: ""
                    val organization = child.child("organization").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "active"
                    organizers.add(Organizer(id, name, email, organization, status))
                }
                progressBar.visibility = View.GONE
                adapter.updateData(organizers)
                emptyState.text = "No organizers found."
                emptyState.visibility = if (organizers.isEmpty()) View.VISIBLE else View.GONE
                if (organizers.isEmpty()) showToast("No organizers found.")
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyState.text = "Failed to load organizers."
                emptyState.visibility = View.VISIBLE
                showToast("Failed to load organizers.")
            }
        })
    }

    private fun blockOrganizer(organizer: Organizer) {
        db.child(organizer.id).child("status").setValue("blocked")
            .addOnSuccessListener { showToast("Organizer blocked") }
            .addOnFailureListener { showToast("Failed to block organizer") }
    }

    private fun approveOrganizer(organizer: Organizer) {
        db.child(organizer.id).child("status").setValue("approved")
            .addOnSuccessListener { showToast("Organizer approved") }
            .addOnFailureListener { showToast("Failed to approve organizer") }
    }

    private fun deleteOrganizer(organizer: Organizer) {
        db.child(organizer.id).removeValue()
            .addOnSuccessListener { showToast("Organizer deleted") }
            .addOnFailureListener { showToast("Failed to delete organizer") }
    }

    private fun editOrganizer(organizer: Organizer) {
        val input = android.widget.EditText(requireContext())
        input.setText(organizer.name)
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Organizer Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    db.child(organizer.id).child("name").setValue(newName)
                        .addOnSuccessListener { showToast("Name updated") }
                        .addOnFailureListener { showToast("Failed to update name") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }
} 