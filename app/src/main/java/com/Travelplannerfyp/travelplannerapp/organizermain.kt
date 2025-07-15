package com.Travelplannerfyp.travelplannerapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import com.Travelplannerfyp.travelplannerapp.R
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.placeadapterAQ
import com.Travelplannerfyp.travelplannerapp.models.places
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener



class organizermain :AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var notificationIcon: ImageView
    private lateinit var notificationBadge: TextView
    private lateinit var searchContainer: CardView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var filterIcon: ImageView
    private lateinit var seeAllText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: placeadapterAQ
    private lateinit var progressDialog: ProgressDialog
    private val placeList = mutableListOf<places>()
    private val originalPlaceList = mutableListOf<places>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organizermain)

        // Initialize Views
        initializeViews()
        setupToolbar()
        setupDrawerLayout()
        setupRecyclerView()
        setupClickListeners()
        setupSearchFunctionality()

        // Fetch data from Firebase/JSON
        fetchPlacesFromJSON()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        notificationIcon = findViewById(R.id.notification)
        notificationBadge = findViewById(R.id.notification_badge)
        searchContainer = findViewById(R.id.search_container)
        searchEditText = findViewById(R.id.search_edittext)
        searchIcon = findViewById(R.id.search_icon)
        filterIcon = findViewById(R.id.filter_icon)
        seeAllText = findViewById(R.id.see_all_text)
        recyclerView = findViewById(R.id.recommendations_recycler)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading places...")
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupDrawerLayout() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = placeadapterAQ(placeList)
        recyclerView.adapter = adapter
    }

    private fun setupSearchFunctionality() {
        // Real-time search as user types
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                searchPlaces(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle search action when user presses search button on keyboard
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val query = searchEditText.text.toString().trim()
                searchPlaces(query)
                // Hide keyboard
                searchEditText.clearFocus()
                true
            } else {
                false
            }
        }

        // Search icon click - focus on EditText
        searchIcon.setOnClickListener {
            searchEditText.requestFocus()
            // Show keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupClickListeners() {
        // Notification icon click
        notificationIcon.setOnClickListener {
            showNotificationPopup(it)
            updateNotificationBadge()
        }

        // Filter icon click
        filterIcon.setOnClickListener {
            showFilterOptions()
        }

        // See all text click
        seeAllText.setOnClickListener {
            showAllPlaces()
        }
    }

    private fun showFilterOptions() {
        val filterOptions = arrayOf("All Places", "Popular", "Recently Added", "Alphabetical")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Filter Places")
        builder.setItems(filterOptions) { _, which ->
            when (which) {
                0 -> showAllPlaces()
                1 -> filterPopularPlaces()
                2 -> filterRecentPlaces()
                3 -> sortAlphabetically()
            }
        }
        builder.show()
    }

    private fun filterPopularPlaces() {
        adapter.updatePlaceList(originalPlaceList)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Showing popular places", Toast.LENGTH_SHORT).show()
    }

    private fun filterRecentPlaces() {
        adapter.updatePlaceList(originalPlaceList)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Showing recent places", Toast.LENGTH_SHORT).show()
    }

    private fun sortAlphabetically() {
        val sortedList = originalPlaceList.sortedBy { it.name }
        adapter.updatePlaceList(sortedList)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Sorted alphabetically", Toast.LENGTH_SHORT).show()
        // Clear search text when applying filter
        searchEditText.setText("")
    }

    private fun showAllPlaces() {
        adapter.updatePlaceList(originalPlaceList)
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Showing all places", Toast.LENGTH_SHORT).show()
        // Clear search text when showing all
        searchEditText.setText("")
    }

    private fun updateNotificationBadge() {
        notificationBadge.visibility = View.GONE
    }

    private fun searchPlaces(query: String) {
        if (query.isEmpty()) {
            // Show all places when search is empty
            adapter.updatePlaceList(originalPlaceList)
        } else {
            // Filter places based on search query
            val filteredList = originalPlaceList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
            adapter.updatePlaceList(filteredList)

            // Show toast with search results count
            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No places found for '$query'", Toast.LENGTH_SHORT).show()
            } else if (query.length > 2) { // Only show count for meaningful searches
                Toast.makeText(this, "Found ${filteredList.size} place(s)", Toast.LENGTH_SHORT).show()
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchPlacesFromJSON() {
        progressDialog.show()

        Log.d("organizermain", "Starting to fetch places from Firebase")

        val databaseReference = FirebaseDatabase.getInstance().reference.child("places")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val newPlaceList = mutableListOf<places>()
                    Log.d("organizermain", "Received data snapshot with ${snapshot.childrenCount} places")

                    for (placeSnapshot in snapshot.children) {
                        val placeKey = placeSnapshot.key ?: continue
                        val name = placeSnapshot.child("name").getValue(String::class.java) ?: ""
                        val description = placeSnapshot.child("description").getValue(String::class.java) ?: ""

                        var imageUrl: String? = null

                        val directUrl = placeSnapshot.child("imageUrl").getValue(String::class.java)
                        if (!directUrl.isNullOrEmpty()) {
                            imageUrl = directUrl
                            Log.d("organizermain", "Found direct imageUrl: $imageUrl")
                        }
                        else if (placeSnapshot.hasChild("image")) {
                            imageUrl = "db://places/$placeKey/image"
                            Log.d("organizermain", "Using database reference to image field: $imageUrl")
                        }
                        else if (placeSnapshot.hasChild("image/data")) {
                            imageUrl = "db://places/$placeKey/image/data"
                            Log.d("organizermain", "Using database reference to image/data field: $imageUrl")
                        }
                        else {
                            imageUrl = "db://places/$placeKey"
                            Log.d("organizermain", "Using generic database reference: $imageUrl")
                        }

                        if (imageUrl.isNullOrEmpty()) {
                            Log.w("organizermain", "No image URL found for place: $name, using placeholder")
                            imageUrl = ""
                        }

                        newPlaceList.add(places(name, description, imageUrl))
                        Log.d("organizermain", "Added place: $name with image URL: $imageUrl")
                    }

                    if (newPlaceList.isEmpty()) {
                        Log.d("organizermain", "No places found in database, falling back to local JSON")
                        loadPlacesFromLocalJSON()
                    } else {
                        Log.d("organizermain", "Updating adapter with ${newPlaceList.size} places from database")
                        placeList.clear()
                        placeList.addAll(newPlaceList)
                        originalPlaceList.clear()
                        originalPlaceList.addAll(newPlaceList)
                        adapter.updatePlaceList(newPlaceList)
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("DatabaseError", "Error loading places from database: ${e.message}")
                    e.printStackTrace()
                    loadPlacesFromLocalJSON()
                }
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", "Database operation cancelled: ${error.message}")
                loadPlacesFromLocalJSON()
                progressDialog.dismiss()
            }
        })
    }

    private fun loadPlacesFromLocalJSON() {
        try {
            val json = loadJSONFromAsset()
            val placesJSON = JSONObject(json)
            val newPlaceList = mutableListOf<places>()

            placesJSON.keys().forEach { key ->
                val placeObject = placesJSON.getJSONObject(key)
                val name = placeObject.getString("name")
                val description = placeObject.getString("description")

                val originalImageUrl = placeObject.getString("imageUrl")
                val imageUrl = if (originalImageUrl.startsWith("http")) {
                    originalImageUrl
                } else {
                    "db://places/$key/image"
                }

                newPlaceList.add(places(name, description, imageUrl))
                Log.d("organizermain", "Added place from JSON: $name with image URL: $imageUrl")
            }

            placeList.clear()
            placeList.addAll(newPlaceList)
            originalPlaceList.clear()
            originalPlaceList.addAll(newPlaceList)
            adapter.updatePlaceList(newPlaceList)
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("JSONError", "Error loading JSON: ${e.message}")
        }
    }

    private fun normalizePlaceName(placeName: String): String {
        return placeName.toLowerCase()
            .replace("valley", "")
            .replace("tour", "")
            .replace("kaghan", "")
            .replace("meadows", "meadows")
            .replace(" ", "")
            .trim()
    }

    private fun loadJSONFromAsset(): String {
        return try {
            val inputStream: InputStream = assets.open("places.json")
            inputStream.bufferedReader().use { it.readText() }
        } catch (ex: IOException) {
            ex.printStackTrace()
            ""
        }
    }

    private fun showNotificationPopup(anchorView: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.notification_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(anchorView, -30, 10)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_profile -> startActivity(Intent(this, profile::class.java))
                R.id.nav_trips_planned -> startActivity(Intent(this, OrganizerTripsActivity::class.java))
                R.id.nav_plan_trip -> startActivity(Intent(this, PlanTripActivity::class.java))
                R.id.nav_logout -> logoutUser()
                R.id.nav_switch_user -> switchToUser()
            }
        } catch (e: Exception) {
            Log.e("NavigationError", "Error in navigation: ${e.message}")
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().remove("SelectedRole").apply()
        progressDialog.show()

        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            progressDialog.dismiss()
            finish()
        }, 2000)
    }

    private fun switchToUser() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().remove("SelectedRole").apply()

        Toast.makeText(this@organizermain, "Switching to user...", Toast.LENGTH_SHORT).show()

        progressDialog.setMessage("Switching role...")
        progressDialog.show()

        Handler().postDelayed({
            progressDialog.dismiss()

            val intent = Intent(this@organizermain, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 2000)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}