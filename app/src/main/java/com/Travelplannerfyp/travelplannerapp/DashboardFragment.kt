package com.Travelplannerfyp.travelplannerapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {
    private lateinit var bookingsText: TextView
    private lateinit var revenueText: TextView
    private lateinit var newUsersText: TextView
    private lateinit var bookingsChart: LineChart
    private lateinit var revenueChart: BarChart
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        bookingsText = view.findViewById(R.id.totalBookingsText)
        revenueText = view.findViewById(R.id.totalRevenueText)
        newUsersText = view.findViewById(R.id.newUsersText)
        bookingsChart = view.findViewById(R.id.bookingsChart)
        revenueChart = view.findViewById(R.id.revenueChart)
        progressBar = view.findViewById(R.id.dashboardProgressBar)
        fetchAnalytics()
        return view
    }

    private fun fetchAnalytics() {
        progressBar.visibility = View.VISIBLE
        fetchBookingsAndRevenue()
        fetchNewUsers()
    }

    private fun fetchBookingsAndRevenue() {
        progressBar.visibility = View.VISIBLE
        db.child("bookings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalBookings = 0
                var totalRevenue = 0.0
                val bookingsPerDay = mutableMapOf<String, Int>()
                val revenuePerDay = mutableMapOf<String, Double>()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val bookings = snapshot.children.toList()
                if (bookings.isEmpty()) {
                    bookingsText.text = "0"
                    revenueText.text = "Rs. 0"
                    setBookingsChart(emptyMap())
                    setRevenueChart(emptyMap())
                    progressBar.visibility = View.GONE
                    showToast("No booking data available")
                    return
                }
                var processed = 0
                for (child in bookings) {
                    totalBookings++
                    val priceValue = child.child("price").value
                    val basePriceValue = child.child("basePrice").value
                    val tripId = child.child("tripId").getValue(String::class.java)
                        ?: child.child("itemId").getValue(String::class.java)
                    val price = when {
                        priceValue is Number -> priceValue.toDouble()
                        priceValue is String -> priceValue.toDoubleOrNull() ?: 0.0
                        basePriceValue is Number -> basePriceValue.toDouble()
                        basePriceValue is String -> basePriceValue.toDoubleOrNull() ?: 0.0
                        else -> null
                    }
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val date = if (timestamp > 0) dateFormat.format(java.util.Date(timestamp)) else null

                    fun addToCharts(priceToAdd: Double) {
                        if (date != null) {
                            bookingsPerDay[date] = bookingsPerDay.getOrDefault(date, 0) + 1
                            revenuePerDay[date] = revenuePerDay.getOrDefault(date, 0.0) + priceToAdd
                        }
                    }

                    if (price != null && price > 0) {
                        totalRevenue += price
                        addToCharts(price)
                        processed++
                        if (processed == bookings.size) {
                            updateDashboard(totalBookings, totalRevenue)
                            setBookingsChart(bookingsPerDay)
                            setRevenueChart(revenuePerDay)
                        }
                    } else if (!tripId.isNullOrEmpty()) {
                        db.child("trips").child(tripId).child("price").get().addOnSuccessListener { tripSnap ->
                            val tripPrice = when (val tripPriceVal = tripSnap.value) {
                                is Number -> tripPriceVal.toDouble()
                                is String -> tripPriceVal.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                            totalRevenue += tripPrice
                            addToCharts(tripPrice)
                            processed++
                            if (processed == bookings.size) {
                                updateDashboard(totalBookings, totalRevenue)
                                setBookingsChart(bookingsPerDay)
                                setRevenueChart(revenuePerDay)
                            }
                        }.addOnFailureListener {
                            processed++
                            if (processed == bookings.size) {
                                updateDashboard(totalBookings, totalRevenue)
                                setBookingsChart(bookingsPerDay)
                                setRevenueChart(revenuePerDay)
                            }
                        }
                    } else {
                        processed++
                        if (processed == bookings.size) {
                            updateDashboard(totalBookings, totalRevenue)
                            setBookingsChart(bookingsPerDay)
                            setRevenueChart(revenuePerDay)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                bookingsText.text = "-"
                revenueText.text = "-"
                showToast("Failed to load bookings/revenue")
            }
        })
    }

    private fun updateDashboard(totalBookings: Int, totalRevenue: Double) {
        bookingsText.text = totalBookings.toString()
        revenueText.text = "Rs. %,.0f".format(totalRevenue)
        // Optionally update charts here
        progressBar.visibility = View.GONE
    }

    private fun fetchNewUsers() {
        db.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
                var newUsers = 0
                for (child in snapshot.children) {
                    val created = child.child("createdAt").getValue(Long::class.java)
                    if (created != null && created >= weekAgo) newUsers++
                }
                newUsersText.text = newUsers.toString()
                if (snapshot.children.count() == 0) {
                    showToast("No user data available")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                newUsersText.text = "-"
                showToast("Failed to load users")
            }
        })
    }

    private fun setBookingsChart(bookingsPerDay: Map<String, Int>) {
        if (bookingsPerDay.isEmpty()) {
            bookingsChart.clear()
            bookingsChart.setNoDataText("No bookings data available.")
            return
        }
        val entries = bookingsPerDay.toSortedMap().entries.mapIndexed { idx, entry: Map.Entry<String, Int> ->
            Entry(idx.toFloat(), entry.value.toFloat())
        }
        val dataSet = LineDataSet(entries, "Bookings").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
            setCircleColor(Color.BLUE)
            circleRadius = 4f
        }
        bookingsChart.data = LineData(dataSet)
        bookingsChart.description = Description().apply { text = "Bookings per Day" }
        bookingsChart.invalidate()
    }

    private fun setRevenueChart(revenuePerDay: Map<String, Double>) {
        if (revenuePerDay.isEmpty()) {
            revenueChart.clear()
            revenueChart.setNoDataText("No revenue data available.")
            return
        }
        val entries = revenuePerDay.toSortedMap().entries.mapIndexed { idx, entry: Map.Entry<String, Double> ->
            BarEntry(idx.toFloat(), entry.value.toFloat())
        }
        val dataSet = BarDataSet(entries, "Revenue").apply {
            color = Color.GREEN
            valueTextColor = Color.BLACK
        }
        revenueChart.data = BarData(dataSet)
        revenueChart.description = Description().apply { text = "Revenue per Day" }
        revenueChart.invalidate()
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }
} 