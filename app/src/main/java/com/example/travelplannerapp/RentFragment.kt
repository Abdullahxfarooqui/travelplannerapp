package com.example.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RentFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var rentalsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var loginPromptText: TextView
    private lateinit var addPropertyFab: FloatingActionButton
    private lateinit var filterChipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var allPropertiesChip: com.google.android.material.chip.Chip
    private lateinit var myPropertiesChip: com.google.android.material.chip.Chip
    
    private var showOnlyMyProperties = false
    private var currentSearchQuery: String? = null
    
    private val propertyList = mutableListOf<PropertyListing>()
    private lateinit var propertyAdapter: PropertyAdapter
    
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        searchView = view.findViewById(R.id.searchView)
        rentalsRecyclerView = view.findViewById(R.id.rentalsRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        loginPromptText = view.findViewById(R.id.loginPromptText)
        addPropertyFab = view.findViewById(R.id.addPropertyFab)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)
        allPropertiesChip = view.findViewById(R.id.allPropertiesChip)
        myPropertiesChip = view.findViewById(R.id.myPropertiesChip)
        
        // Check if user is logged in and update UI accordingly
        updateAuthUI()
        
        // Set up RecyclerView with dividers
        val layoutManager = LinearLayoutManager(requireContext())
        rentalsRecyclerView.layoutManager = layoutManager
        propertyAdapter = PropertyAdapter(requireContext(), propertyList)
        rentalsRecyclerView.adapter = propertyAdapter
        
        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText
                filterProperties()
                return true
            }
        })
        
        // Set up chip group for filtering
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            showOnlyMyProperties = checkedIds.contains(R.id.myPropertiesChip)
            filterProperties()
        }
        
        // Set up FAB click listener
        addPropertyFab.setOnClickListener {
            // Check if user is logged in
            if (auth.currentUser != null) {
                // Navigate to property listing activity
                val intent = Intent(requireContext(), PropertyListingActivity::class.java)
                startActivity(intent)
            } else {
                // Navigate to login activity
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            }
        }
        
        // Set up auth state listener
        auth.addAuthStateListener { firebaseAuth ->
            updateAuthUI()
        }
        
        // Load properties
        loadProperties()
    }
    
    private fun loadProperties() {
        // Clear existing properties
        propertyList.clear()
        
        // Show loading state
        emptyView.text = "Loading properties..."
        emptyView.visibility = View.VISIBLE
        
        // Get current user ID
        val currentUserId = auth.currentUser?.uid
        
        // Query properties from Firebase
        database.child("properties").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                propertyList.clear()
                
                for (propertySnapshot in snapshot.children) {
                    val property = propertySnapshot.getValue(PropertyListing::class.java)
                    property?.let {
                        // If user is logged in, mark their own properties
                        if (currentUserId != null && it.ownerId == currentUserId) {
                            // This is the user's own property
                            it.isOwnProperty = true
                        }
                        propertyList.add(it)
                    }
                }
                
                // Update UI
                propertyAdapter.notifyDataSetChanged()
                updateEmptyView()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("RentFragment", "Error loading properties: ${error.message}")
                emptyView.text = "Error loading properties"
                emptyView.visibility = View.VISIBLE
            }
        })
    }
    
    private fun filterProperties() {
        var filteredList = propertyList.toMutableList()
        
        // Filter by ownership if needed
        if (showOnlyMyProperties) {
            filteredList = filteredList.filter { it.isOwnProperty }.toMutableList()
        }
        
        // Filter by search query if needed
        if (!currentSearchQuery.isNullOrBlank()) {
            filteredList = filteredList.filter {
                it.title.contains(currentSearchQuery!!, ignoreCase = true) ||
                it.location.contains(currentSearchQuery!!, ignoreCase = true) ||
                it.description.contains(currentSearchQuery!!, ignoreCase = true)
            }.toMutableList()
        }
        
        propertyAdapter.updateProperties(filteredList)
        updateEmptyView()
    }
    
    private fun updateEmptyView() {
        if (propertyAdapter.itemCount == 0) {
            if (showOnlyMyProperties && auth.currentUser != null) {
                emptyView.text = "You haven't listed any properties yet"
            } else {
                emptyView.text = "No rentals found"
            }
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }
    
    private fun updateAuthUI() {
        val isLoggedIn = auth.currentUser != null
        
        // Show/hide my properties chip based on login status
        myPropertiesChip.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        
        // If user logs out and was viewing their properties, switch to all properties
        if (!isLoggedIn && showOnlyMyProperties) {
            showOnlyMyProperties = false
            allPropertiesChip.isChecked = true
            filterProperties()
        }
        
        // Show login prompt if needed
        loginPromptText.visibility = if (!isLoggedIn && showOnlyMyProperties) View.VISIBLE else View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        // Reload properties when fragment resumes
        loadProperties()
    }
}