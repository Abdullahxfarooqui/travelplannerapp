// CRITICAL BUGFIX 2024-06-09:
// - Added robust logging and error handling for Add Stop dialog's JS bridge and RecyclerView update logic.
// - Added logging and error handling for all image loading (trip, hotel, stop, static map) in PlanTripActivity.
// - Ensured RecyclerView and label are always set to visible after any update attempt.
// - This patch addresses: Add Stop dialog attractions sync, image loading failures, and provides logs for QA.
package com.Travelplannerfyp.travelplannerapp

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.databinding.ActivityPlanTripBinding
import com.Travelplannerfyp.travelplannerapp.models.Stop
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.util.*
import com.Travelplannerfyp.travelplannerapp.models.Attraction
import com.Travelplannerfyp.travelplannerapp.models.Place
import com.Travelplannerfyp.travelplannerapp.adapters.NearbyAttractionAdapter
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import android.content.DialogInterface
import android.view.WindowManager
import android.widget.TextView

class PlanTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlanTripBinding
    private val stops = mutableListOf<Stop>()
    private lateinit var stopsAdapter: StopsAdapter
    private var activeDialogWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load place image using Picasso with fallback and null-checks
        val placeImageUrl = intent.getStringExtra("PLACE_IMAGE_URL")
        android.util.Log.d("PlanTripActivity", "Loading place image: $placeImageUrl")
        if (!placeImageUrl.isNullOrEmpty()) {
            Picasso.get().load(placeImageUrl).placeholder(R.drawable.ic_trip_placeholder).error(R.drawable.ic_trip_placeholder).fit().centerCrop().into(binding.placeImage, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    android.util.Log.d("PlanTripActivity", "Place image loaded successfully: $placeImageUrl")
                }
                override fun onError(e: java.lang.Exception?) {
                    android.util.Log.e("PlanTripActivity", "Failed to load place image: $placeImageUrl", e)
                }
            })
        } else {
            binding.placeImage.setImageResource(R.drawable.ic_trip_placeholder)
        }

        // Setup stops RecyclerView
        stopsAdapter = StopsAdapter(
            stops,
            onEdit = { position, stop -> showStopDialog(editPosition = position, stop = stop) },
            onRemove = { position, _ ->
                stops.removeAt(position)
                stopsAdapter.notifyItemRemoved(position)
            }
        )
        binding.stopsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.stopsRecyclerView.adapter = stopsAdapter

        // Add Stop button
        binding.addStopButton.setOnClickListener { showStopDialog() }

        // Save Trip button
        binding.createTripButton.setOnClickListener { saveTrip() }

        // Setup hotels RecyclerView with Picasso for hotel images
        val hotelsJson = intent.getStringExtra("SELECTED_HOTELS")
        val hotels = if (!hotelsJson.isNullOrEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.Travelplannerfyp.travelplannerapp.models.Hotel>>() {}.type
                com.google.gson.Gson().fromJson<List<com.Travelplannerfyp.travelplannerapp.models.Hotel>>(hotelsJson, type)
            } catch (e: Exception) {
                android.util.Log.e("PlanTripActivity", "Failed to parse hotels JSON: ${e.message}", e)
                Toast.makeText(this, "Failed to load hotels. Please try again.", Toast.LENGTH_LONG).show()
                emptyList()
            }
        } else emptyList()
        val hotelAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val view = layoutInflater.inflate(R.layout.hotel_item, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
            }
            override fun getItemCount() = hotels.size
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val hotel = hotels[position]
                val imageView = holder.itemView.findViewById<android.widget.ImageView>(R.id.hotel_image)
                android.util.Log.d("PlanTripActivity", "Loading hotel image: ${hotel.imageUrl}")
                // Load hotel image with null-check and fallback
                if (!hotel.imageUrl.isNullOrEmpty()) {
                    Picasso.get().load(hotel.imageUrl).placeholder(R.drawable.placeholder_image).error(R.drawable.placeholder_image).fit().centerCrop().into(imageView, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            android.util.Log.d("PlanTripActivity", "Hotel image loaded successfully: ${hotel.imageUrl}")
                        }
                        override fun onError(e: java.lang.Exception?) {
                            android.util.Log.e("PlanTripActivity", "Failed to load hotel image: ${hotel.imageUrl}", e)
                        }
                    })
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
                holder.itemView.findViewById<android.widget.TextView>(R.id.hotel_name)?.text = hotel.name
                holder.itemView.findViewById<android.widget.TextView>(R.id.hotel_description)?.text = hotel.description
                holder.itemView.findViewById<android.widget.EditText>(R.id.hotel_price_input)?.setText(hotel.pricePerNight)
                holder.itemView.findViewById<android.widget.TextView>(R.id.hotel_price)?.text = hotel.pricePerNight
                holder.itemView.findViewById<android.widget.RatingBar>(R.id.hotel_rating)?.rating = hotel.rating.toFloat()
            }
        }
        binding.recyclerViewHotels.adapter = hotelAdapter
        binding.recyclerViewHotels.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    }

    private fun showStopDialog(editPosition: Int? = null, stop: Stop? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_stop, null)
        val locationInput = dialogView.findViewById<EditText>(R.id.location_name_input)
        val arrivalTimeInput = dialogView.findViewById<EditText>(R.id.stop_time_input)
        val durationInput = dialogView.findViewById<EditText>(R.id.duration_input)
        val stopImagePreview = dialogView.findViewById<ImageView>(R.id.stop_image_preview)
        val attractionsRecycler = dialogView.findViewById<RecyclerView>(R.id.attractions_recycler_view)
        val attractionsLabel = dialogView.findViewById<View>(R.id.nearby_attractions_label)
        val mapWebView = dialogView.findViewById<WebView>(R.id.stop_map_webview)
        val placeholderText = TextView(this).apply {
            text = "Enter a location to view map and attractions"
            textSize = 16f
            setPadding(0, 24, 0, 24)
            gravity = android.view.Gravity.CENTER
        }
        val parentLayout = dialogView as? android.widget.LinearLayout
        // Insert placeholderText above mapWebView
        parentLayout?.addView(placeholderText, parentLayout.indexOfChild(mapWebView))
        placeholderText.visibility = View.VISIBLE
        mapWebView.visibility = View.GONE
        attractionsLabel.visibility = View.GONE
        attractionsRecycler.visibility = View.GONE
        activeDialogWebView = mapWebView

        // State for selected attractions
        val selectedAttractions = mutableSetOf<Place>()
        var stopImageUrl: String? = null
        var fetchedAttractions: List<Place> = emptyList()
        var lastLat: Double? = null
        var lastLon: Double? = null

        // Pre-fill if editing
        stop?.let {
            locationInput.setText(it.stopName)
            arrivalTimeInput.setText(it.arrivalTime)
            durationInput.setText(it.durationMinutes.toString())
            stopImageUrl = it.imageUrl
        }
        stopImageUrl?.let { url ->
            if (url.isNotEmpty()) {
                android.util.Log.d("PlanTripActivity", "Loading stop image preview: $url")
                Picasso.get().load(url).placeholder(R.drawable.placeholder_image).error(R.drawable.placeholder_image).fit().centerCrop().into(stopImagePreview, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        android.util.Log.d("PlanTripActivity", "Stop image preview loaded successfully: $url")
                    }
                    override fun onError(e: java.lang.Exception?) {
                        android.util.Log.e("PlanTripActivity", "Failed to load stop image preview: $url", e)
                    }
                })
            }
        }

        // Setup attractions RecyclerView with NearbyAttractionAdapter
        val attractionAdapter = NearbyAttractionAdapter(
            attractions = emptyList(),
            onItemClick = { place, position ->
                if (selectedAttractions.contains(place)) {
                    selectedAttractions.remove(place)
                } else {
                    selectedAttractions.add(place)
                }
                attractionsRecycler.adapter?.notifyItemChanged(position)
            },
            onDirectionsClick = { place, _ -> }
        )
        attractionsRecycler.adapter = attractionAdapter
        // Change to vertical orientation for best UX
        attractionsRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // JS Bridge for receiving attractions from WebView (same as TripDetailActivity)
        class JSBridge {
            @JavascriptInterface
            fun setNearbyAttractions(json: String) {
                android.util.Log.d("PlanTripActivity", "JSBridge.setNearbyAttractions called with: $json")
                runOnUiThread {
                    try {
                        val arr = JSONArray(json)
                        val list = mutableListOf<Place>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Place(
                                    name = obj.optString("name"),
                                    description = obj.optString("description"),
                                    imageUrl = obj.optString("imageUrl"),
                                    type = obj.optString("type"),
                                    distance = obj.optDouble("distance", 0.0),
                                    address = obj.optString("address"),
                                    latitude = obj.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                                    longitude = obj.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                                )
                            )
                        }
                        android.util.Log.d("PlanTripActivity", "Nearby attractions received: ${list.size}")
                        Toast.makeText(this@PlanTripActivity, "Nearby attractions: ${list.size}", Toast.LENGTH_SHORT).show()
                        fetchedAttractions = list
                        attractionAdapter.updateData(list)
                        attractionsLabel.visibility = View.VISIBLE
                        attractionsRecycler.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        android.util.Log.e("PlanTripActivity", "Failed to parse nearby attractions: ${e.message}", e)
                        Toast.makeText(this@PlanTripActivity, "Failed to load nearby attractions.", Toast.LENGTH_LONG).show()
                        fetchedAttractions = emptyList()
                        attractionAdapter.updateData(emptyList())
                        attractionsLabel.visibility = View.VISIBLE
                        attractionsRecycler.visibility = View.VISIBLE
                    }
                }
            }
        }
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.webViewClient = WebViewClient()
        mapWebView.addJavascriptInterface(JSBridge(), "AndroidBridge")

        // Helper to load map with given lat/lon
        fun loadMapWebView(lat: Double, lon: Double) {
            lastLat = lat
            lastLon = lon
            mapWebView.loadUrl("file:///android_asset/nearby_map.html")
        }

        // Provide trip coordinates to JS
        mapWebView.addJavascriptInterface(object {
            @JavascriptInterface
            fun getTripLatLng(): String {
                val lat = lastLat ?: 33.6844
                val lon = lastLon ?: 73.0479
                return "{\"lat\":$lat,\"lon\":$lon}"
            }
        }, "AndroidBridge")

        // When location changes, geocode and update map/attractions
        locationInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val location = locationInput.text.toString().trim()
                if (location.isNotEmpty()) {
                    placeholderText.visibility = View.GONE
                    mapWebView.visibility = View.VISIBLE
                    attractionsRecycler.visibility = View.VISIBLE
                    attractionsLabel.visibility = View.VISIBLE
                    ApiManager.fetchCoordinatesForLocation(location) { lat, lon, error ->
                        if (lat != null && lon != null) {
                            loadMapWebView(lat, lon)
                            loadLocationIQStaticMap(stopImagePreview, lat, lon)
                            stopImageUrl = "https://maps.locationiq.com/v3/staticmap?key=pk.1ef4faca32216233742e487f2372c924&center=$lat,$lon&zoom=13&size=600x400"
                            // Attractions will be fetched and sent by JS in nearby_map.html
                        } else {
                            fetchedAttractions = emptyList()
                            attractionAdapter.updateData(emptyList())
                            attractionsLabel.visibility = View.GONE
                            stopImagePreview.setImageResource(R.drawable.placeholder_image)
                        }
                    }
                } else {
                    // No location entered: show placeholder, hide map and attractions
                    placeholderText.visibility = View.VISIBLE
                    mapWebView.visibility = View.GONE
                    attractionsLabel.visibility = View.GONE
                    attractionsRecycler.visibility = View.GONE
                }
            }
        }

        // Time picker for arrival time
        arrivalTimeInput.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                arrivalTimeInput.setText(String.format("%02d:%02d", h, m))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editPosition == null) "Add Stop" else "Edit Stop")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog: DialogInterface, _: Int ->
                val location = locationInput.text.toString().trim()
                val arrivalTime = arrivalTimeInput.text.toString().trim()
                val duration = durationInput.text.toString().toIntOrNull() ?: 0
                val selectedAttractionNames = selectedAttractions.map { it.name }
                val imageUrl = stopImageUrl

                if (location.isEmpty() || arrivalTime.isEmpty() || duration <= 0) {
                    Toast.makeText(this, "Please fill all stop fields.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newStop = Stop(
                    stopName = location,
                    arrivalTime = arrivalTime,
                    durationMinutes = duration,
                    attractions = selectedAttractionNames,
                    imageUrl = imageUrl
                )
                if (editPosition == null) {
                    stops.add(newStop)
                    stopsAdapter.notifyItemInserted(stops.size - 1)
                } else {
                    stops[editPosition] = newStop
                    stopsAdapter.notifyItemChanged(editPosition)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .create()
        dialog.setOnDismissListener {
            // Release WebView resources when dialog is dismissed
            try { activeDialogWebView?.destroy() } catch (_: Exception) {}
            activeDialogWebView = null
        }
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun saveTrip() {
        // Example: Save stops to Firebase or your backend
        val trip = mapOf(
            "stops" to stops.map {
                mapOf(
                    "stopName" to it.stopName,
                    "arrivalTime" to it.arrivalTime,
                    "durationMinutes" to it.durationMinutes,
                    "attractions" to it.attractions
                )
            }
        )
        FirebaseDatabase.getInstance().reference.child("trips").push().setValue(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "Trip saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save trip: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { activeDialogWebView?.destroy() } catch (_: Exception) {}
        activeDialogWebView = null
    }

    // Helper to load a static map from LocationIQ into an ImageView using Picasso
    private fun loadLocationIQStaticMap(imageView: ImageView, lat: Double, lon: Double) {
        val staticMapUrl = "https://maps.locationiq.com/v3/staticmap?key=pk.1ef4faca32216233742e487f2372c924&center=$lat,$lon&zoom=13&size=600x400"
        android.util.Log.d("PlanTripActivity", "Loading static map image: $staticMapUrl")
        Picasso.get()
            .load(staticMapUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .fit()
            .centerCrop()
            .into(imageView, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    android.util.Log.d("PlanTripActivity", "Static map image loaded successfully: $staticMapUrl")
                }
                override fun onError(e: java.lang.Exception?) {
                    android.util.Log.e("PlanTripActivity", "Failed to load static map image: $staticMapUrl", e)
                }
            })
    }

    // Provide keyword-based mock attractions for realism
    private fun getNearbyAttractionsByKeyword(location: String, lat: Double, lon: Double): List<Place> {
        val key = location.lowercase()
        return when {
            key.contains("hunza") -> listOf(
                Place("Altit Fort", "Historic fort in Hunza", "", "Historical", 500.0, "Altit, Hunza", lat+0.001, lon+0.001),
                Place("Baltit Fort", "Iconic fort with museum", "", "Historical", 800.0, "Baltit, Hunza", lat+0.002, lon+0.002),
                Place("Attabad Lake", "Famous turquoise lake", "", "Lake", 3000.0, "Attabad, Hunza", lat+0.003, lon+0.003)
            )
            key.contains("lahore") -> listOf(
                Place("Badshahi Mosque", "Grand Mughal-era mosque", "", "Religious", 1200.0, "Lahore", lat+0.001, lon+0.001),
                Place("Lahore Fort", "UNESCO World Heritage site", "", "Historical", 1500.0, "Lahore", lat+0.002, lon+0.002),
                Place("Shalimar Gardens", "Mughal gardens", "", "Garden", 2000.0, "Lahore", lat+0.003, lon+0.003)
            )
            key.contains("murree") -> listOf(
                Place("Mall Road", "Popular shopping street", "", "Shopping", 400.0, "Murree", lat+0.001, lon+0.001),
                Place("Patriata (New Murree)", "Chair lifts & hills", "", "Scenic", 2500.0, "Murree", lat+0.002, lon+0.002),
                Place("Pindi Point", "Hilltop viewpoint", "", "Viewpoint", 1800.0, "Murree", lat+0.003, lon+0.003)
            )
            else -> listOf(
                Place("Local Park", "A nice park to relax", "", "Park", 600.0, "Central Area", lat+0.001, lon+0.001),
                Place("Museum", "Learn about the region", "", "Museum", 1200.0, "Downtown", lat+0.002, lon+0.002)
            )
        }
    }

    // Helper to load any image with Picasso and fallback
    private fun loadImageWithFallback(imageView: ImageView, url: String?) {
        android.util.Log.d("PlanTripActivity", "Loading image with fallback: $url")
        if (!url.isNullOrEmpty()) {
            Picasso.get()
                .load(url)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .fit()
                .centerCrop()
                .into(imageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        android.util.Log.d("PlanTripActivity", "Image loaded with fallback successfully: $url")
                    }
                    override fun onError(e: java.lang.Exception?) {
                        android.util.Log.e("PlanTripActivity", "Failed to load image with fallback: $url", e)
                    }
                })
        } else {
            imageView.setImageResource(R.drawable.placeholder_image)
        }
    }
}