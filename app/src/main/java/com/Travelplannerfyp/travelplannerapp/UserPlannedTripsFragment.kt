package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.Travelplannerfyp.travelplannerapp.fragments.ExploreTripsFragment
import com.Travelplannerfyp.travelplannerapp.fragments.MyTripsFragment
import com.Travelplannerfyp.travelplannerapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class UserPlannedTripsFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_planned_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupViewPager()
        setupTabLayout()
    }

    private fun setupViewPager() {
        viewPager.adapter = UserPlannedTripsPagerAdapter(this)
        // Disable state restoration to prevent crashes
        viewPager.isSaveEnabled = false
    }

    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "My Trips"
                1 -> "Explore Trips"
                else -> ""
            }
        }.attach()
    }

    private inner class UserPlannedTripsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter to prevent state restoration issues
        viewPager.adapter = null
    }
} 