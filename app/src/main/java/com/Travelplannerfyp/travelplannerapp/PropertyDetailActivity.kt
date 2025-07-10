package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.Travelplannerfyp.travelplannerapp.PropertyListing
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.Travelplannerfyp.travelplannerapp.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.material.button.MaterialButton
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var property: PropertyListing
    private lateinit var imageViewPager: ViewPager2
    private lateinit var propertyTitleTextView: TextView
    private lateinit var propertyLocationTextView: TextView
    private lateinit var propertyPriceTextView: TextView
    private lateinit var propertyGuestsTextView: TextView
    private lateinit var bedroomsTextView: TextView
    private lateinit var bathroomsTextView: TextView
    private lateinit var bedsTextView: TextView
    private lateinit var availabilityStartTextView: TextView
    private lateinit var availabilityEndTextView: TextView
    private lateinit var checkInTimeTextView: TextView
    private lateinit var checkOutTimeTextView: TextView
    private lateinit var propertyDescriptionTextView: TextView
    private lateinit var amenitiesChipGroup: ChipGroup
    private lateinit var houseRulesChipGroup: ChipGroup
    private lateinit var customRulesTextView: TextView
    private lateinit var cleaningFeeTextView: TextView
    private lateinit var securityDepositTextView: TextView
    private lateinit var hostPhoneTextView: TextView
    private lateinit var emergencyContactTextView: TextView
    private lateinit var bookButton: MaterialButton
    private lateinit var appBarLayout: AppBarLayout
    
    private var propertyId: String? = null
    private var propertyListing: PropertyListing? = null
    private var propertyListener: ValueEventListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_detail)
        
        // Initialize views
        initializeViews()

        // Get property ID from intent
        propertyId = intent.getStringExtra("property_id")
        if (propertyId == null) {
            Toast.makeText(this, "Error: Property ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up toolbar
        setupToolbar()

        // Set up book button
        setupBookButton()

        // Load property details
        loadPropertyDetails()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener when the activity is destroyed
        propertyListener?.let {
            FirebaseDatabase.getInstance().getReference("properties")
                .child(propertyId!!)
                .removeEventListener(it)
        }
    }

    private fun initializeViews() {
        imageViewPager = findViewById(R.id.imageViewPager)
        propertyTitleTextView = findViewById(R.id.propertyTitleTextView)
        propertyLocationTextView = findViewById(R.id.propertyLocationTextView)
        propertyPriceTextView = findViewById(R.id.propertyPriceTextView)
        propertyGuestsTextView = findViewById(R.id.propertyGuestsTextView)
        bedroomsTextView = findViewById(R.id.bedroomsTextView)
        bathroomsTextView = findViewById(R.id.bathroomsTextView)
        bedsTextView = findViewById(R.id.bedsTextView)
        availabilityStartTextView = findViewById(R.id.availabilityStartTextView)
        availabilityEndTextView = findViewById(R.id.availabilityEndTextView)
        checkInTimeTextView = findViewById(R.id.checkInTimeTextView)
        checkOutTimeTextView = findViewById(R.id.checkOutTimeTextView)
        propertyDescriptionTextView = findViewById(R.id.propertyDescriptionTextView)
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup)
        houseRulesChipGroup = findViewById(R.id.houseRulesChipGroup)
        customRulesTextView = findViewById(R.id.customRulesTextView)
        cleaningFeeTextView = findViewById(R.id.cleaningFeeTextView)
        securityDepositTextView = findViewById(R.id.securityDepositTextView)
        hostPhoneTextView = findViewById(R.id.hostPhoneTextView)
        emergencyContactTextView = findViewById(R.id.emergencyContactTextView)
        bookButton = findViewById(R.id.bookButton)
        appBarLayout = findViewById(R.id.appBarLayout)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun loadPropertyDetails() {
        val database = FirebaseDatabase.getInstance()
        val propertyRef = database.getReference("properties").child(propertyId!!)

        propertyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                propertyListing = snapshot.getValue(PropertyListing::class.java)
                propertyListing?.let { updateUI(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PropertyDetailActivity, "Error loading property details", Toast.LENGTH_SHORT).show()
            }
        }

        propertyListener?.let { propertyRef.addValueEventListener(it) }
    }
    
    private fun updateUI(property: PropertyListing) {
        this.property = property
        displayPropertyDetails()
    }

    private fun displayPropertyDetails() {
        // Animate content appearance
        val contentViews = listOf(propertyTitleTextView, propertyLocationTextView, propertyPriceTextView, propertyDescriptionTextView, propertyGuestsTextView, bedroomsTextView, bathroomsTextView, bedsTextView, availabilityStartTextView, availabilityEndTextView, checkInTimeTextView, checkOutTimeTextView)
        contentViews.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(index * 100L)
                .start()
        }

        propertyTitleTextView.text = property.title
        propertyLocationTextView.text = property.location
        propertyPriceTextView.text = CurrencyUtils.formatAsPKR(property.pricePerNight) + " / night"
        propertyDescriptionTextView.text = property.description
        propertyGuestsTextView.text = "${property.maxGuests} guests maximum"
        bedroomsTextView.text = property.bedrooms.toString()
        bathroomsTextView.text = property.bathrooms.toString()
        bedsTextView.text = property.beds.toString()
        availabilityStartTextView.text = property.availabilityStart
        availabilityEndTextView.text = property.availabilityEnd
        checkInTimeTextView.text = property.checkInTime
        checkOutTimeTextView.text = property.checkOutTime
        
        setupAmenities()
        setupHouseRules()
        setupAdditionalFees()
        setupHostInformation()

        // Set up image view pager
        setupImagePager(property.imageUrls)
    }

    private fun setupAmenities() {
        amenitiesChipGroup.removeAllViews()
        property.amenities.forEachIndexed { index, amenity ->
            val chip = Chip(this).apply {
                text = amenity
                isCheckable = false
            }
            amenitiesChipGroup.addView(chip)
        }
    }

    private fun setupHouseRules() {
        houseRulesChipGroup.removeAllViews()
        property.houseRules.forEachIndexed { index, rule ->
            val chip = Chip(this).apply {
                text = rule
                isCheckable = false
            }
            houseRulesChipGroup.addView(chip)
        }
        customRulesTextView.text = property.customRules
    }

    private fun setupAdditionalFees() {
        cleaningFeeTextView.text = CurrencyUtils.formatAsPKR(property.cleaningFee)
        securityDepositTextView.text = CurrencyUtils.formatAsPKR(property.securityDeposit)
    }

    private fun setupHostInformation() {
        hostPhoneTextView.text = property.hostPhoneNumber
        emergencyContactTextView.text = property.emergencyContact
    }
    
    private fun setupImagePager(imageUrls: List<String>) {
        if (imageUrls.isNotEmpty()) {
            val adapter = PropertyImageAdapter(imageUrls)
            imageViewPager.adapter = adapter
        }
    }

    private fun setupBookButton() {
        bookButton.setOnClickListener {
            // Check if user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please login to book this property", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if property is available
            if (!property.available) {
                Toast.makeText(this, "This property is not available for booking", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Navigate to booking activity
            val intent = Intent(this, PropertyBookingActivity::class.java).apply {
                putExtra(PropertyBookingActivity.EXTRA_PROPERTY_ID, propertyId)
            }
            startActivity(intent)
        }
    }

    private fun setupAppBarScrollListener() {
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrollProgress = Math.abs(verticalOffset) / appBarLayout.totalScrollRange.toFloat()
            bookButton.alpha = 1 - scrollProgress
            bookButton.translationY = 100 * scrollProgress
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}