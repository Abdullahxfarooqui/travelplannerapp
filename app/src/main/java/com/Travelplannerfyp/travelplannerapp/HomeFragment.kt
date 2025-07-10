package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.Travelplannerfyp.travelplannerapp.R

class HomeFragment : Fragment() {    
    private lateinit var recommendedTripsRecyclerView: RecyclerView
    private lateinit var availableTripsRecyclerView: RecyclerView
    private lateinit var recommendedAdapter: TripAdapter
    private lateinit var availableAdapter: AvailableAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var searchView: SearchView
    private val originalRecommendedTrips = mutableListOf<Trip>()
    private val originalAvailableTrips = mutableListOf<Trip>()
    private var availableTripsListener: ValueEventListener? = null

    private fun getLocalImageResource(tripName: String): Int {
        return when (tripName.lowercase()) {
            "neelum valley" -> R.drawable.neelumvalley
            "islamabad tour" -> R.drawable.islamabad
            "hunza valley" -> R.drawable.hunza
            "fairy meadows" -> R.drawable.fairy_meadows
            "naran kaghan" -> R.drawable.naran
            "skardu" -> R.drawable.skardu
            "swat valley" -> R.drawable.swat_valley_tour
            "lahore" -> R.drawable.lahore
            "karachi" -> R.drawable.karachi
            else -> R.drawable.placeholder_image
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("HomeFragment", "onViewCreated called")
        
        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // Initialize SearchView
        searchView = view.findViewById(R.id.searchView)
        setupSearchView()

        // Initialize RecyclerViews
        recommendedTripsRecyclerView = view.findViewById(R.id.recommendedTripsRecyclerView)
        availableTripsRecyclerView = view.findViewById(R.id.availableTripsRecyclerView)

        // Set up RecyclerViews
        recommendedTripsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        availableTripsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Initialize adapters with empty lists
        recommendedAdapter = TripAdapter(requireContext(), emptyList())
        availableAdapter = AvailableAdapter(requireContext(), emptyList())

        // Set adapters to RecyclerViews
        recommendedTripsRecyclerView.adapter = recommendedAdapter
        availableTripsRecyclerView.adapter = availableAdapter
        
        Log.d("HomeFragment", "Adapters initialized and set to RecyclerViews")

        // Load trips from Firebase - load available trips first to prioritize it
        loadAvailableTrips()
        loadRecommendedTrips()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterTrips(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTrips(newText)
                return true
            }
        })
    }

    private fun filterTrips(query: String?) {
        val filteredRecommendedTrips = if (query.isNullOrBlank()) {
            originalRecommendedTrips
        } else {
            originalRecommendedTrips.filter {
                it.name?.contains(query, ignoreCase = true) == true ||
                it.location?.contains(query, ignoreCase = true) == true ||
                it.description?.contains(query, ignoreCase = true) == true
            }
        }

        val filteredAvailableTrips = if (query.isNullOrBlank()) {
            originalAvailableTrips
        } else {
            originalAvailableTrips.filter {
                it.name?.contains(query, ignoreCase = true) == true ||
                it.location?.contains(query, ignoreCase = true) == true ||
                it.description?.contains(query, ignoreCase = true) == true
            }
        }

        recommendedAdapter.updateTrips(filteredRecommendedTrips)
        availableAdapter.updateTrips(filteredAvailableTrips)
    }
    
    private fun loadRecommendedTrips() {
        Log.d("HomeFragment", "Starting to load recommended trips")
        databaseReference.child("recommended_trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                Log.d("HomeFragment", "Received data snapshot for recommended trips: ${snapshot.childrenCount} items")
                for (tripSnapshot in snapshot.children) {
                    try {
                        val name = tripSnapshot.child("name").getValue(String::class.java) ?: ""
                        val location = tripSnapshot.child("location").getValue(String::class.java) ?: ""
                        val description = tripSnapshot.child("description").getValue(String::class.java) ?: ""
                        val imageUrl = tripSnapshot.child("imageUrl").getValue(String::class.java)
                        
                        // First try to get imageResId from Firebase
                        val imageResIdStr = tripSnapshot.child("imageResId").getValue(String::class.java)
                        
                        // Get local image resource ID based on Firebase value or trip name
                        val imageResId = if (!imageResIdStr.isNullOrEmpty()) {
                            // Try to get resource ID from the string value in Firebase
                            resources.getIdentifier(imageResIdStr, "drawable", requireContext().packageName)
                        } else {
                            // Fallback to name-based lookup
                            getLocalImageResource(name)
                        }

                        val trip = Trip(
                            name = name,
                            location = location,
                            description = description,
                            imageUrl = imageUrl,
                            imageResId = imageResId
                        )
                        trips.add(trip)
                        Log.d("HomeFragment", "Loaded recommended trip: ${trip.name} with imageUrl: ${trip.imageUrl}, imageResId: ${trip.imageResId}")
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error parsing trip data: ${e.message}")
                    }
                }

                if (trips.isEmpty()) {
                    Log.w("HomeFragment", "No recommended trips found in the database, adding fallback trips")
                    addFallbackRecommendedTrips(trips)
                }

                originalRecommendedTrips.clear()
                originalRecommendedTrips.addAll(trips)
                recommendedAdapter.updateTrips(trips)
                Log.d("HomeFragment", "Updating adapter with ${trips.size} recommended trips")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error loading recommended trips: ${error.message}")
                Toast.makeText(context, "Failed to load recommended trips", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAvailableTrips() {
        Log.d("HomeFragment", "Starting to load available trips")
        availableTripsListener?.let {
            databaseReference.child("available_trips").removeEventListener(it)
            Log.d("HomeFragment", "Removed existing listener")
        }

        availableTripsListener = databaseReference.child("available_trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                Log.d("HomeFragment", "Received data snapshot for available trips: ${snapshot.childrenCount} items")

                for (tripSnapshot in snapshot.children) {
                    try {
                        val name = tripSnapshot.child("name").getValue(String::class.java) ?: ""
                        val location = tripSnapshot.child("location").getValue(String::class.java) ?: ""
                        val description = tripSnapshot.child("description").getValue(String::class.java) ?: ""
                        val imageUrl = tripSnapshot.child("imageUrl").getValue(String::class.java)
                        
                        // First try to get imageResId from Firebase
                        val imageResIdStr = tripSnapshot.child("imageResId").getValue(String::class.java)
                        
                        // Get local image resource ID based on Firebase value or trip name
                        val imageResId = if (!imageResIdStr.isNullOrEmpty()) {
                            // Try to get resource ID from the string value in Firebase
                            resources.getIdentifier(imageResIdStr, "drawable", requireContext().packageName)
                        } else {
                            // Fallback to name-based lookup
                            getLocalImageResource(name)
                        }

                        val trip = Trip(
                            name = name,
                            location = location,
                            description = description,
                            imageUrl = imageUrl,
                            imageResId = imageResId
                        )
                        trips.add(trip)
                        Log.d("HomeFragment", "Loaded available trip: ${trip.name} with imageUrl: ${trip.imageUrl}, imageResId: ${trip.imageResId}")
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error parsing trip data: ${e.message}")
                    }
                }

                if (trips.isEmpty()) {
                    Log.w("HomeFragment", "No available trips found in the database, adding fallback trips")
                    addFallbackAvailableTrips(trips)
                }

                originalAvailableTrips.clear()
                originalAvailableTrips.addAll(trips)
                availableAdapter.updateTrips(trips)
                Log.d("HomeFragment", "Updating adapter with ${trips.size} available trips")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error loading available trips: ${error.message}")
                Toast.makeText(context, "Failed to load available trips", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun addFallbackRecommendedTrips(trips: MutableList<Trip>) {
        // Add some hardcoded trips as fallback
        trips.add(Trip(
            id = "fallback1",
            name = "Neelum Valley",
            location = "Azad Kashmir, Pakistan",
            description = "Experience the breathtaking beauty of Neelum Valley, with its lush green meadows, pristine rivers, and snow-capped mountains.",
            imageUrl = null,
            imageResId = R.drawable.neelumvalley
        ))
        
        trips.add(Trip(
            id = "fallback2",
            name = "Hunza Valley",
            location = "Gilgit-Baltistan, Pakistan",
            description = "Discover the majestic Hunza Valley, known for its stunning landscapes, ancient forts, and rich cultural heritage.",
            imageUrl = null,
            imageResId = R.drawable.hunza
        ))
        
        trips.add(Trip(
            id = "fallback3",
            name = "Fairy Meadows",
            location = "Gilgit-Baltistan, Pakistan",
            description = "Visit the enchanting Fairy Meadows, offering spectacular views of Nanga Parbat and pristine alpine landscapes.",
            imageUrl = null,
            imageResId = R.drawable.fairy_meadows
        ))
        
        Log.d("HomeFragment", "Added ${trips.size} fallback recommended trips")
    }

    private fun addFallbackAvailableTrips(trips: MutableList<Trip>) {
        // Add some hardcoded trips as fallback
        trips.add(Trip(
            id = "available1",
            name = "Naran Kaghan",
            location = "Khyber Pakhtunkhwa, Pakistan",
            description = "Explore the stunning valleys of Naran and Kaghan, with their crystal-clear lakes, lush forests, and majestic mountains.",
            imageUrl = null,
            imageResId = R.drawable.naran
        ))
        
        trips.add(Trip(
            id = "available2",
            name = "Skardu",
            location = "Gilgit-Baltistan, Pakistan",
            description = "Visit the breathtaking Skardu valley, home to some of the world's highest peaks, pristine lakes, and ancient forts.",
            imageUrl = null,
            imageResId = R.drawable.skardu
        ))
        
        trips.add(Trip(
            id = "available3",
            name = "Swat Valley",
            location = "Khyber Pakhtunkhwa, Pakistan",
            description = "Discover the 'Switzerland of Pakistan' with its lush green meadows, clear rivers, and snow-capped mountains.",
            imageUrl = null,
            imageResId = R.drawable.swat_valley_tour
        ))
        
        Log.d("HomeFragment", "Added ${trips.size} fallback available trips")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Clean up Firebase listeners to prevent memory leaks
        availableTripsListener?.let {
            databaseReference.child("available_trips").removeEventListener(it)
            availableTripsListener = null
            Log.d("HomeFragment", "Removed available trips listener in onDestroyView")
        }
    }
}