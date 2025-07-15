package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.fragments.OrganizerTripsFragment
import com.Travelplannerfyp.travelplannerapp.R

class OrganizerTripsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organizer_trips)

        setupToolbar()
        setupFragment()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Trips Planned - Enrolled Users"
    }

    private fun setupFragment() {
        val fragment = OrganizerTripsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 