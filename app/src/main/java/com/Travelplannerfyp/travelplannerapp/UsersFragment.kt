package com.Travelplannerfyp.travelplannerapp

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
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
import com.Travelplannerfyp.travelplannerapp.adapters.UserAdapter
import com.Travelplannerfyp.travelplannerapp.model.User
import com.google.firebase.database.*

class UsersFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var adapter: UserAdapter
    private val users = mutableListOf<User>()
    private val db = FirebaseDatabase.getInstance().reference
    private var usersListener: ValueEventListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_users, container, false)
        recyclerView = view.findViewById(R.id.usersRecyclerView)
        progressBar = view.findViewById(R.id.usersProgressBar)
        emptyState = view.findViewById(R.id.usersEmptyState)
        adapter = UserAdapter(users, ::onEdit, ::onBlock, ::onDelete)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fetchUsers()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.let { db.child("users").removeEventListener(it) }
    }

    private fun fetchUsers() {
        progressBar.visibility = View.VISIBLE
        users.clear()
        adapter.updateData(users)
        usersListener = db.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: "(No Name)"
                    val email = child.child("email").getValue(String::class.java) ?: "(No Email)"
                    val createdAt = try { child.child("createdAt").getValue(Long::class.java) ?: 0L } catch (e: Exception) { 0L }
                    val status = child.child("status").getValue(String::class.java) ?: "active"
                    users.add(User(uid, name, email, createdAt, status))
                }
                progressBar.visibility = View.GONE
                adapter.updateData(users)
                emptyState.text = "No users found."
                emptyState.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyState.text = "Failed to load users."
                emptyState.visibility = View.VISIBLE
            }
        })
    }

    private fun onEdit(user: User) {
        val input = EditText(requireContext())
        input.setText(user.name)
        input.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
            .setTitle("Edit User Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    db.child("users").child(user.uid).child("name").setValue(newName)
                        .addOnSuccessListener { Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener { Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onBlock(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Block User")
            .setMessage("Are you sure you want to block this user?")
            .setPositiveButton("Block") { _, _ ->
                db.child("users").child(user.uid).child("status").setValue("blocked")
                    .addOnSuccessListener { Toast.makeText(requireContext(), "User blocked", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireContext(), "Failed to block user", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onDelete(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.child("users").child(user.uid).removeValue()
                    .addOnSuccessListener { Toast.makeText(requireContext(), "User deleted", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireContext(), "Failed to delete user", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 