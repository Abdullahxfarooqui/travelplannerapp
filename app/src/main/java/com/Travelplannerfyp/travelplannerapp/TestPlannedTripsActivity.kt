package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.Travelplannerfyp.travelplannerapp.R

class TestPlannedTripsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_planned_trips)
        
        findViewById<Button>(R.id.btn_test_planned_trips).setOnClickListener {
            val intent = Intent(this, PlannedTripsTabbedActivity::class.java)
            startActivity(intent)
        }
    }
} 