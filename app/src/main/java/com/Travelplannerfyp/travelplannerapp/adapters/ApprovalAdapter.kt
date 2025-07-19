package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.model.ApprovalItem
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso

class ApprovalAdapter(
    private var items: List<ApprovalItem>,
    private val onApprove: (ApprovalItem) -> Unit,
    private val onReject: (ApprovalItem) -> Unit
) : RecyclerView.Adapter<ApprovalAdapter.ApprovalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_approval, parent, false)
        return ApprovalViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApprovalViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.organizer.text = item.organizerName
        if (item.imageUrl.isNotEmpty()) {
            Picasso.get().load(item.imageUrl).placeholder(R.drawable.ic_placeholder).into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_placeholder)
        }
        holder.approveButton.setOnClickListener { onApprove(item) }
        holder.rejectButton.setOnClickListener { onReject(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ApprovalItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ApprovalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.approvalImageView)
        val title: TextView = itemView.findViewById(R.id.approvalTitleTextView)
        val organizer: TextView = itemView.findViewById(R.id.approvalOrganizerTextView)
        val approveButton: MaterialButton = itemView.findViewById(R.id.approveButton)
        val rejectButton: MaterialButton = itemView.findViewById(R.id.rejectButton)
    }
} 