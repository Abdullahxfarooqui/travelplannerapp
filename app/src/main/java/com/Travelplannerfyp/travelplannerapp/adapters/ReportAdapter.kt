package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.model.Report
import com.Travelplannerfyp.travelplannerapp.model.User

class ReportAdapter(
    private val allReports: List<Report>,
    private val usersMap: Map<String, User>,
    private val onResolve: (Report) -> Unit,
    private val onDeleteItem: (Report) -> Unit,
    private val onBlockOrganizer: (Report) -> Unit
) : ListAdapter<Report, ReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = getItem(position)
        holder.bind(report, usersMap, onResolve, onDeleteItem, onBlockOrganizer)
    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val reporterName: TextView = itemView.findViewById(R.id.reporterName)
        private val reportedItem: TextView = itemView.findViewById(R.id.reportedItem)
        private val reportReason: TextView = itemView.findViewById(R.id.reportReason)
        private val reportTimestamp: TextView = itemView.findViewById(R.id.reportTimestamp)
        private val resolveBtn: Button = itemView.findViewById(R.id.resolveBtn)
        private val deleteBtn: Button = itemView.findViewById(R.id.deleteBtn)
        private val blockBtn: Button = itemView.findViewById(R.id.blockBtn)

        fun bind(
            report: Report,
            usersMap: Map<String, User>,
            onResolve: (Report) -> Unit,
            onDeleteItem: (Report) -> Unit,
            onBlockOrganizer: (Report) -> Unit
        ) {
            val reporter = usersMap[report.reporterId ?: ""]
            reporterName.text = "Reporter: ${reporter?.name ?: report.reporterId ?: "Unknown"}"
            reportedItem.text = "Reported: ${report.reportedItemType} (${report.reportedItemId})"
            reportReason.text = "Reason: ${report.reason ?: "No reason provided"}"
            reportTimestamp.text = report.timestamp ?: ""

            resolveBtn.setOnClickListener { onResolve(report) }
            deleteBtn.setOnClickListener { onDeleteItem(report) }
            if (report.reportedItemType == "organizer") {
                blockBtn.visibility = View.VISIBLE
                blockBtn.setOnClickListener { onBlockOrganizer(report) }
            } else {
                blockBtn.visibility = View.GONE
            }

            resolveBtn.isEnabled = report.status != "resolved"
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.reportId == newItem.reportId
        }
        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
} 