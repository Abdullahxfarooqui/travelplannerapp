package com.example.travelplannerapp


import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class trips_planned : AppCompatActivity(){

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trips_planned)

        // Find the TextViews by their IDs
        val titleTextView: TextView = findViewById(R.id.historyTitle)
        val trip1TextView: TextView = findViewById(R.id.trip1)
        val trip2TextView: TextView = findViewById(R.id.trip2)
        val trip3TextView: TextView = findViewById(R.id.trip3)

        // Set the text values for each TextView
        titleTextView.text = "History of Trips"
        trip1TextView.text = "Trip to Murree - April 10, 2025"
        trip2TextView.text = "Hunza Valley - March 20, 2025"
        trip3TextView.text = "Swat - February 15, 2025"
    }
}