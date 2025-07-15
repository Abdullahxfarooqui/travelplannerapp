package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.Travelplannerfyp.travelplannerapp.fragments.ExploreTripsFragment
import com.Travelplannerfyp.travelplannerapp.fragments.MyTripsFragment
import com.Travelplannerfyp.travelplannerapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PlannedTripsTabbedActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planned_trips_tabbed)

        setupToolbar()
        setupViewPager()
        setupTabLayout()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Planned Trips"
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = PlannedTripsPagerAdapter(this)
        // Disable state restoration to prevent crashes
        viewPager.isSaveEnabled = false
    }

    private fun setupTabLayout() {
        tabLayout = findViewById(R.id.tabLayout)
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Trips"
                1 -> "Explore Trips"
                else -> ""
            }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the adapter to prevent state restoration issues
        viewPager.adapter = null
    }

    private inner class PlannedTripsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when (position) {
                0 -> MyTripsFragment()
                1 -> ExploreTripsFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }

        override fun getItemId(position: Int): Long {
            // Return a unique and stable ID for each tab
            return position.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            // Only two tabs, so valid IDs are 0 and 1
            return itemId == 0L || itemId == 1L
        }
    }
} 