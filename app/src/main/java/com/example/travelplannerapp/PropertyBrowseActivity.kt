package com.example.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PropertyBrowseActivity : AppCompatActivity() {

    private lateinit var propertiesRecyclerView: RecyclerView
    private lateinit var propertyAdapter: PropertyAdapter
    private lateinit var searchView: SearchView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    
    private val propertyList = mutableListOf<PropertyListing>()
    private val originalPropertyList = mutableListOf<PropertyListing>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_browse)
        
        // Initialize views
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        propertiesRecyclerView = findViewById(R.id.propertiesRecyclerView)
        searchView = findViewById(R.id.searchView)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        
        // Set up toolbar
        setSupportActionBar(toolbar)
        
        // Set up RecyclerView
        propertyAdapter = PropertyAdapter(this, propertyList)
        propertiesRecyclerView.layoutManager = LinearLayoutManager(this)
        propertiesRecyclerView.adapter = propertyAdapter
        
        // Set up bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_rent -> {
                    startActivity(Intent(this, PropertyListingActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProperties(query)
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                filterProperties(newText)
                return true
            }
        })
        
        // Load properties
        loadProperties()
    }
    
    private fun loadProperties() {
        showLoading(true)
        
        val database = FirebaseDatabase.getInstance().getReference("properties")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                propertyList.clear()
                originalPropertyList.clear()
                
                for (propertySnapshot in snapshot.children) {
                    val property = propertySnapshot.getValue(PropertyListing::class.java)
                    property?.let {
                        if (it.isAvailable) {
                            propertyList.add(it)
                            originalPropertyList.add(it)
                        }
                    }
                }
                
                propertyAdapter.notifyDataSetChanged()
                showLoading(false)
                updateEmptyView()
            }
            
            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                Toast.makeText(this@PropertyBrowseActivity, "Failed to load properties: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun filterProperties(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            originalPropertyList
        } else {
            originalPropertyList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        
        propertyAdapter.updateProperties(filteredList)
        updateEmptyView()
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        propertiesRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
    
    private fun updateEmptyView() {
        emptyView.visibility = if (propertyList.isEmpty()) View.VISIBLE else View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        bottomNavigationView.menu.findItem(R.id.nav_home).isChecked = true
    }
}