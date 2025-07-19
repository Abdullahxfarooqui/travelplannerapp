package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.Travelplannerfyp.travelplannerapp.adapters.ReportAdapter
import com.Travelplannerfyp.travelplannerapp.model.Report
import com.Travelplannerfyp.travelplannerapp.model.User

class ReportsFragment : Fragment() {
    private lateinit var reportsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var reportAdapter: ReportAdapter
    private lateinit var reportsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private val reportsList = mutableListOf<Report>()
    private val usersMap = mutableMapOf<String, User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.emptyView)
        filterSpinner = view.findViewById(R.id.filterSpinner)

        reportsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        reportAdapter = ReportAdapter(reportsList, usersMap, ::onResolveClicked, ::onDeleteItemClicked, ::onBlockOrganizerClicked)
        reportsRecyclerView.adapter = reportAdapter

        reportsRef = FirebaseDatabase.getInstance().getReference("reports")
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        setupFilterSpinner()
        fetchUsersAndReports()
        return view
    }

    private fun setupFilterSpinner() {
        val options = listOf("All", "Unresolved Only")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterReports()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchUsersAndReports() {
        progressBar.visibility = View.VISIBLE
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersMap.clear()
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    user?.let { usersMap[user.uid ?: userSnap.key ?: ""] = it }
                }
                fetchReports()
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyView.text = "Failed to load users."
                emptyView.visibility = View.VISIBLE
            }
        })
    }

    private fun fetchReports() {
        reportsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reportsList.clear()
                for (reportSnap in snapshot.children) {
                    val report = try {
                        val r = reportSnap.getValue(Report::class.java)
                        if (r != null && !r.reportId.isNullOrBlank()) r else null
                    } catch (e: Exception) { null }
                    report?.let { reportsList.add(it) }
                }
                filterReports()
                progressBar.visibility = View.GONE
                if (reportsList.isEmpty()) {
                    emptyView.text = "No reports found."
                    emptyView.visibility = View.VISIBLE
                    showToast("No reports found.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyView.text = "Failed to load reports."
                emptyView.visibility = View.VISIBLE
                showToast("Failed to load reports.")
            }
        })
    }

    private fun filterReports() {
        val showUnresolvedOnly = filterSpinner.selectedItemPosition == 1
        val filtered = if (showUnresolvedOnly) reportsList.filter { it.status != "resolved" } else reportsList
        reportAdapter.submitList(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onResolveClicked(report: Report) {
        reportsRef.child(report.reportId ?: return).child("status").setValue("resolved")
            .addOnSuccessListener { showToast("Report resolved.") }
            .addOnFailureListener { showToast("Failed to resolve report.") }
    }

    private fun onDeleteItemClicked(report: Report) {
        val itemType = report.reportedItemType
        val itemId = report.reportedItemId
        if (itemType == "trip") {
            FirebaseDatabase.getInstance().getReference("trips").child(itemId ?: return).removeValue()
                .addOnSuccessListener { showToast("Trip deleted.") }
                .addOnFailureListener { showToast("Failed to delete trip.") }
        } else if (itemType == "organizer") {
            FirebaseDatabase.getInstance().getReference("organizers").child(itemId ?: return).removeValue()
                .addOnSuccessListener { showToast("Organizer deleted.") }
                .addOnFailureListener { showToast("Failed to delete organizer.") }
        }
    }

    private fun onBlockOrganizerClicked(report: Report) {
        if (report.reportedItemType == "organizer") {
            FirebaseDatabase.getInstance().getReference("organizers").child(report.reportedItemId ?: return).child("status").setValue("blocked")
                .addOnSuccessListener { showToast("Organizer blocked.") }
                .addOnFailureListener { showToast("Failed to block organizer.") }
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }
} 