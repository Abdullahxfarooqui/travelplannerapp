package com.example.travelplannerapp

import android.annotation.SuppressLint

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.Adapter.HotelAdapter
import com.example.travelplannerapp.models.Destination
import com.example.travelplannerapp.models.Hotel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.IOException


class PlaceDetailActivity : AppCompatActivity() {
    private lateinit var placeName: TextView
    private lateinit var placeDescription: TextView
    private lateinit var placeImage: ImageView
    private lateinit var addToTripButton: Button
    private lateinit var recyclerView: RecyclerView

    private var hotelList: List<Hotel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        placeName = findViewById(R.id.detail_place_name)
        placeDescription = findViewById(R.id.detail_place_description)
        placeImage = findViewById(R.id.detail_place_image)
        addToTripButton = findViewById(R.id.btn_add_to_trip)
        recyclerView = findViewById(R.id.recycler_view_hotels)

        recyclerView.layoutManager = LinearLayoutManager(this)

        addToTripButton.setOnClickListener {
            Toast.makeText(this, "Added to trip!", Toast.LENGTH_SHORT).show()
        }

        val name = intent.getStringExtra("PLACE_NAME")
        val description = intent.getStringExtra("PLACE_DESCRIPTION")
        val imageUrl = intent.getStringExtra("PLACE_IMAGE_URL")

        placeName.text = name ?: "Unknown Place"
        placeDescription.text = description ?: "No description available"

        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get().load(imageUrl).into(placeImage)
        } else {
            placeImage.setImageResource(R.drawable.placeholder)
        }

        name?.let { loadHotelsFromJson(it) }
    }

    private fun loadHotelsFromJson(place: String) {
        try {
            val jsonString = assets.open("hotels.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val trimmedPlace = place.trim()
            Log.d("DEBUG", "PLACE_NAME received: '$trimmedPlace'")

            if (!jsonObject.has(trimmedPlace)) {
                Toast.makeText(this, "No hotel data found for $trimmedPlace", Toast.LENGTH_SHORT).show()
                Log.d("DEBUG", "No key '$trimmedPlace' found in JSON")
                return
            }

            val placeObject = jsonObject.getJSONObject(trimmedPlace)
            val hotelsArray = placeObject.getJSONArray("hotels")
            Log.d("DEBUG", "Found ${hotelsArray.length()} hotels for '$trimmedPlace'")

            val gson = Gson()
            val hotelListType = object : TypeToken<List<Hotel>>() {}.type
            hotelList = gson.fromJson(hotelsArray.toString(), hotelListType)

            Log.d("DEBUG", "Parsed ${hotelList.size} hotels")

            setupHotelAdapter()

        } catch (e: IOException) {
            Log.e("PlaceDetailActivity", "Error reading JSON", e)
        } catch (e: Exception) {
            Log.e("PlaceDetailActivity", "Error parsing JSON", e)
        }
    }
    private fun setupHotelAdapter() {
        val adapter = HotelAdapter(hotelList) { hotel ->
            Toast.makeText(this, "Selected: ${hotel.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
    }
}