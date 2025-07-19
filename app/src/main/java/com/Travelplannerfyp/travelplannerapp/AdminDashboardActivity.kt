package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.fragments.AdminDashboardFragment

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_dashboard_container, AdminDashboardFragment())
                .commit()
        }
    }
} 