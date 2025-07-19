package com.Travelplannerfyp.travelplannerapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminPanelPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 7
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> DashboardFragment()
        1 -> UsersFragment()
        2 -> OrganizersFragment()
        3 -> TripsFragment()
        4 -> ApprovalsFragment()
        5 -> NotificationsFragment()
        6 -> ReportsFragment()
        else -> Fragment()
    }
} 