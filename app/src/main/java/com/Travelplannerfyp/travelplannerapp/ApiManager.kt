package com.Travelplannerfyp.travelplannerapp

import android.os.Handler
import android.os.Looper
import com.Travelplannerfyp.travelplannerapp.models.Attraction
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

object ApiManager {
    private const val LOCATIONIQ_TOKEN = "pk.1ef4faca32216233742e487f2372c924"
    private val client = OkHttpClient()

    fun fetchNearbyAttractions(lat: Double, lon: Double, onResult: (List<Attraction>) -> Unit) {
        val coordinates = "$lon,$lat"
        val url = "https://us1.locationiq.com/v1/nearest/driving/$coordinates?key=$LOCATIONIQ_TOKEN&number=10"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(emptyList()) }
            }
            override fun onResponse(call: Call, response: Response) {
                val attractions = mutableListOf<Attraction>()
                response.body?.string()?.let { body ->
                    try {
                        val obj = JSONObject(body)
                        val waypoints = obj.optJSONArray("waypoints")
                        if (waypoints != null) {
                            for (i in 0 until waypoints.length()) {
                                val wp = waypoints.getJSONObject(i)
                                val name = wp.optString("name", "Waypoint")
                                val id = wp.optString("waypoint_index", i.toString())
                                val type = wp.optString("type", "Unknown")
                                val dist = wp.optDouble("distance", 0.0).toString()
                                attractions.add(
                                    Attraction(
                                        id = id,
                                        name = name,
                                        type = type,
                                        distance = dist,
                                        iconUrl = null
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }
                Handler(Looper.getMainLooper()).post { onResult(attractions) }
            }
        })
    }

    fun fetchCoordinatesForLocation(location: String, onResult: (Double?, Double?, String?) -> Unit) {
        val url = "https://us1.locationiq.com/v1/search.php?key=$LOCATIONIQ_TOKEN&q=${location.trim()}&format=json&limit=1"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onResult(null, null, "Network error: ${e.localizedMessage}") }
            }
            override fun onResponse(call: Call, response: Response) {
                var lat: Double? = null
                var lon: Double? = null
                var error: String? = null
                response.body?.string()?.let { body ->
                    try {
                        val arr = JSONArray(body)
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            lat = obj.getDouble("lat")
                            lon = obj.getDouble("lon")
                        } else {
                            error = "No results found for location."
                        }
                    } catch (e: Exception) {
                        error = "Error parsing geocoding response."
                    }
                }
                Handler(Looper.getMainLooper()).post { onResult(lat, lon, error) }
            }
        })
    }
} 