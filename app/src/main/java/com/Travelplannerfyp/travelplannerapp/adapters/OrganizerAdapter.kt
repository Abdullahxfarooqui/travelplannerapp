package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.model.Organizer

class OrganizerAdapter(
    private var organizers: List<Organizer>,
    private val onBlockClick: (Organizer) -> Unit,
    private val onApproveClick: (Organizer) -> Unit,
    private val onDeleteClick: (Organizer) -> Unit,
    private val onEditClick: (Organizer) -> Unit
) : RecyclerView.Adapter<OrganizerAdapter.OrganizerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_organizer, parent, false)
        return OrganizerViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrganizerViewHolder, position: Int) {
        val organizer = organizers[position]
        holder.name.text = organizer.name
        holder.email.text = "Email: ${organizer.email}"
        holder.organization.text = "Organization: ${organizer.organization}"
        holder.status.text = "Status: ${organizer.status}"
        holder.btnBlock.setOnClickListener { onBlockClick(organizer) }
        holder.btnApprove.setOnClickListener { onApproveClick(organizer) }
        holder.btnDelete.setOnClickListener { onDeleteClick(organizer) }
        holder.btnEdit.setOnClickListener { onEditClick(organizer) }
    }

    override fun getItemCount(): Int = organizers.size

    fun updateData(newOrganizers: List<Organizer>) {
        organizers = newOrganizers
        notifyDataSetChanged()
    }

    class OrganizerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.organizerName)
        val email: TextView = itemView.findViewById(R.id.organizerEmail)
        val organization: TextView = itemView.findViewById(R.id.organizerOrganization)
        val status: TextView = itemView.findViewById(R.id.organizerStatus)
        val btnBlock: View = itemView.findViewById(R.id.btnBlockOrganizer)
        val btnApprove: View = itemView.findViewById(R.id.btnApproveOrganizer)
        val btnDelete: View = itemView.findViewById(R.id.btnDeleteOrganizer)
        val btnEdit: View = itemView.findViewById(R.id.btnEditOrganizer)
    }
} 