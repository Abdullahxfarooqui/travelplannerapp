package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.model.User
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class UserAdapter(
    private var users: List<User>,
    private val onEdit: (User) -> Unit,
    private val onBlock: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.email.text = user.email
        holder.userId.text = "UID: ${user.uid}"
        val date = if (user.createdAt > 0) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(user.createdAt)) else "Unknown"
        holder.signupDate.text = "Signed up: $date"
        holder.editButton.setOnClickListener { onEdit(user) }
        holder.blockButton.setOnClickListener { onBlock(user) }
        holder.deleteButton.setOnClickListener { onDelete(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.userNameTextView)
        val email: TextView = itemView.findViewById(R.id.userEmailTextView)
        val signupDate: TextView = itemView.findViewById(R.id.userSignupDateTextView)
        val userId: TextView = itemView.findViewById(R.id.userIdTextView)
        val editButton: MaterialButton = itemView.findViewById(R.id.editUserButton)
        val blockButton: MaterialButton = itemView.findViewById(R.id.blockUserButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteUserButton)
    }
} 