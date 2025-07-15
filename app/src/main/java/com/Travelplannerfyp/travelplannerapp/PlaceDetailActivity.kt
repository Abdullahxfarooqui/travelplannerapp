package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.HotelAdapter
import com.Travelplannerfyp.travelplannerapp.models.Hotel
import com.Travelplannerfyp.travelplannerapp.utils.TripImageLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.IOException
import com.Travelplannerfyp.travelplannerapp.R

class PlaceDetailActivity : AppCompatActivity() {
    private lateinit var placeName: TextView
    private lateinit var placeDescription: TextView
    private lateinit var placeImage: ImageView
    private lateinit var addToTripButton: Button
    private lateinit var recyclerView: RecyclerView

    private var hotelList: List<Hotel> = emptyList()
    private val selectedHotels = mutableListOf<Hotel>()

    private var currentPlaceName: String = ""
    private var currentPlaceDescription: String = ""
    private var currentPlaceImageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_detail)

        // Initialize views
        placeName = findViewById(R.id.detail_place_name)
        placeDescription = findViewById(R.id.detail_place_description)
        placeImage = findViewById(R.id.detail_place_image)
        addToTripButton = findViewById(R.id.btn_add_to_trip)
        recyclerView = findViewById(R.id.recycler_view_hotels)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get data from intent
        currentPlaceName = intent.getStringExtra("PLACE_NAME") ?: "Unknown Place"
        currentPlaceDescription = intent.getStringExtra("PLACE_DESCRIPTION") ?: "No description available"
        currentPlaceImageName = intent.getStringExtra("PLACE_IMAGE_URL") ?: ""

        // Set place info
        placeName.text = currentPlaceName
        placeDescription.text = currentPlaceDescription

        // Load image using TripImageLoader with improved logic
        TripImageLoader.loadTripImage(
            context = this,
            imageView = placeImage,
            imageUrl = currentPlaceImageName,
            imageResId = null,
            tripName = currentPlaceName.trim()
        )

        // Add to trip button logic
        addToTripButton.setOnClickListener {
            if (selectedHotels.isEmpty()) {
                Toast.makeText(this, "Please add a hotel to your trip first!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, PlanTripActivity::class.java).apply {
                    putExtra("SELECTED_HOTELS", Gson().toJson(selectedHotels))
                    putExtra("PLACE_NAME", currentPlaceName)
                    putExtra("PLACE_DESCRIPTION", currentPlaceDescription)
                    putExtra("PLACE_IMAGE_URL", currentPlaceImageName)
                }
                startActivity(intent)
            }
        }

        // Load hotels
        loadHotelsFromJson(currentPlaceName)
    }

    private fun loadHotelsFromJson(place: String) {
        try {
            val jsonString = assets.open("hotel.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val possibleKeys = listOf(
                place,
                place.trim(),
                place.replace(" ", "-"),
                place.replace("-", " ")
            )

            val matchingKey = possibleKeys.find { jsonObject.has(it) } ?: run {
                Toast.makeText(this, "No hotel data found for $place", Toast.LENGTH_SHORT).show()
                return
            }

            val placeObject = jsonObject.getJSONObject(matchingKey)
            if (!placeObject.has("hotels")) return

            val hotelsArray = placeObject.getJSONArray("hotels")
            val hotelListType = object : TypeToken<List<Hotel>>() {}.type
            hotelList = Gson().fromJson(hotelsArray.toString(), hotelListType)

            setupHotelAdapter()

        } catch (e: IOException) {
            Toast.makeText(this, "Error loading hotel data: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error parsing hotel data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHotelAdapter() {
        val adapter = HotelAdapter(
            hotelList,
            onItemClick = { hotel ->
                val intent = Intent(this, HotelDetailActivity::class.java).apply {
                    putExtra("name", hotel.name)
                    putExtra("description", hotel.description)
                    putExtra("rating", hotel.rating)
                    putExtra("price", hotel.price)
                    putExtra("imageName", hotel.imageName)
                }
                startActivity(intent)
            },
            onAddToCartClick = { hotel ->
                if (!selectedHotels.contains(hotel)) {
                    selectedHotels.add(hotel)
                    Toast.makeText(this, "${hotel.name} added to trip", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${hotel.name} is already in your trip", Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerView.adapter = adapter
    }
}
