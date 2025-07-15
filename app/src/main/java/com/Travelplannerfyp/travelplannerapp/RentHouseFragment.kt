package com.Travelplannerfyp.travelplannerapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.Travelplannerfyp.travelplannerapp.databinding.FragmentRentHouseExtendedBinding
import com.google.android.material.snackbar.Snackbar
import com.Travelplannerfyp.travelplannerapp.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RentHouseFragment : Fragment() {

    private var _binding: FragmentRentHouseExtendedBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RentHouseViewModel by viewModels()
    private lateinit var imageAdapter: ImageAdapter
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    private var checkInTime: String = ""
    private var checkOutTime: String = ""
    private var availabilityStartDate: String = ""
    private var availabilityEndDate: String = ""
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageAdapter.addImage(it)
        }
    }
    
    // Identity document picker launcher
    private val identityPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setIdentityDocumentUri(it)
            binding.identityStatusText.text = "Document selected"
            binding.identityStatusText.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRentHouseExtendedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Setup property type spinner
        val propertyTypes = arrayOf("Apartment", "House", "Shared Room", "Villa")
        val propertyTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, propertyTypes)
        propertyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPropertyType.adapter = propertyTypeAdapter
        
        // Setup cancellation policy spinner
        val cancellationPolicies = arrayOf("Flexible", "Moderate", "Strict")
        val cancellationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cancellationPolicies)
        cancellationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCancellationPolicy.adapter = cancellationAdapter
        
        // Setup RecyclerView for images
        imageAdapter = ImageAdapter { position ->
            // Remove image callback
            imageAdapter.removeImage(position)
        }
        binding.photosRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.submitButton.isEnabled = !isLoading
        })
        
        viewModel.submitResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is RentHouseViewModel.SubmitResult.Success -> {
                    Snackbar.make(binding.root, "Property listed successfully!", Snackbar.LENGTH_LONG).show()
                    clearForm()
                }
                is RentHouseViewModel.SubmitResult.Error -> {
                    Snackbar.make(binding.root, "Error: ${result.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        })
        
        viewModel.validationErrors.observe(viewLifecycleOwner, Observer { errors ->
            clearErrors()
            errors.forEach { error ->
                when (error.field) {
                    "title" -> binding.titleEditText.error = error.message
                    "location" -> binding.locationEditText.error = error.message
                    "description" -> binding.descriptionEditText.error = error.message
                    "price" -> binding.priceEditText.error = error.message
                    "guests" -> binding.guestsEditText.error = error.message
                    "bedrooms" -> binding.bedroomsEditText.error = error.message
                    "bathrooms" -> binding.bathroomsEditText.error = error.message
                    "beds" -> binding.bedsEditText.error = error.message
                    "minimumStay" -> binding.minimumStayEditText.error = error.message
                    "hostPhone" -> binding.hostPhoneEditText.error = error.message
                    "emergencyContact" -> binding.emergencyContactEditText.error = error.message
                    "cleaningFee" -> binding.cleaningFeeEditText.error = error.message
                    "securityDeposit" -> binding.securityDepositEditText.error = error.message
                    else -> {
                        Snackbar.make(binding.root, error.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.addPhotoButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        binding.uploadIdentityButton.setOnClickListener {
            identityPickerLauncher.launch("image/*")
        }
        
        binding.checkInTimeButton.setOnClickListener {
            showTimePicker { time ->
                checkInTime = time
                binding.checkInTimeButton.text = "Check-in: $time"
            }
        }
        
        binding.checkOutTimeButton.setOnClickListener {
            showTimePicker { time ->
                checkOutTime = time
                binding.checkOutTimeButton.text = "Check-out: $time"
            }
        }
        
        binding.availabilityStartButton.setOnClickListener {
            showDatePicker { date ->
                availabilityStartDate = date
                binding.availabilityStartButton.text = "Start: $date"
            }
        }
        
        binding.availabilityEndButton.setOnClickListener {
            showDatePicker { date ->
                availabilityEndDate = date
                binding.availabilityEndButton.text = "End: $date"
            }
        }
        
        binding.submitButton.setOnClickListener {
            submitForm()
        }
    }
    
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(time)
        }, hour, minute, true).show()
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val dateString = dateFormat.format(selectedDate.time)
            onDateSelected(dateString)
        }, year, month, day).show()
    }
    
    private fun submitForm() {
        val propertyListing = PropertyListing(
            id = "", // Will be generated by Firebase
            title = binding.titleEditText.text.toString().trim(),
            description = binding.descriptionEditText.text.toString().trim(),
            location = binding.locationEditText.text.toString().trim(),
            pricePerNight = binding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0,
            maxGuests = binding.guestsEditText.text.toString().toIntOrNull() ?: 0,
            bedrooms = binding.bedroomsEditText.text.toString().toIntOrNull() ?: 0,
            bathrooms = binding.bathroomsEditText.text.toString().toIntOrNull() ?: 0,
            beds = binding.bedsEditText.text.toString().toIntOrNull() ?: 0,
            amenities = getSelectedAmenities(),
            houseRules = getHouseRules(),
            customRules = binding.customRulesEditText.text.toString().trim(),
            cleaningFee = binding.cleaningFeeEditText.text.toString().toDoubleOrNull() ?: 0.0,
            securityDeposit = binding.securityDepositEditText.text.toString().toDoubleOrNull() ?: 0.0,
            availabilityStart = availabilityStartDate,
            availabilityEnd = availabilityEndDate,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            imageUrls = emptyList(), // Will be populated after image upload
            hostId = "", // Will be set by ViewModel
            hostName = "", // Will be set by ViewModel
            hostPhoneNumber = binding.hostPhoneEditText.text.toString().trim(),
            emergencyContact = binding.emergencyContactEditText.text.toString().trim(),
            identityVerificationUrl = "", // Will be set after upload
            available = true
        )
        
        viewModel.submitPropertyListing(propertyListing, imageAdapter.getImageUris())
    }
    
    private fun getSelectedAmenities(): List<String> {
        val amenities = mutableListOf<String>()
        
        if (binding.checkboxWifi.isChecked) amenities.add("Wi-Fi")
        if (binding.checkboxAC.isChecked) amenities.add("AC")
        if (binding.checkboxTV.isChecked) amenities.add("TV")
        if (binding.checkboxKitchen.isChecked) amenities.add("Kitchen")
        if (binding.checkboxParking.isChecked) amenities.add("Parking")
        if (binding.checkboxWashingMachine.isChecked) amenities.add("Washing Machine")
        if (binding.checkboxPool.isChecked) amenities.add("Pool")
        if (binding.checkboxPetFriendly.isChecked) amenities.add("Pet-Friendly")
        
        return amenities
    }
    
    private fun getHouseRules(): List<String> {
        val rules = mutableListOf<String>()
        
        if (binding.switchNoSmoking.isChecked) rules.add("No Smoking")
        if (binding.switchNoParties.isChecked) rules.add("No Parties")
        if (binding.switchPetsAllowed.isChecked) rules.add("Pets Allowed")
        
        return rules
    }
    
    private fun clearErrors() {
        binding.titleEditText.error = null
        binding.locationEditText.error = null
        binding.descriptionEditText.error = null
        binding.priceEditText.error = null
        binding.guestsEditText.error = null
        binding.bedroomsEditText.error = null
        binding.bathroomsEditText.error = null
        binding.bedsEditText.error = null
        binding.minimumStayEditText.error = null
        binding.hostPhoneEditText.error = null
        binding.emergencyContactEditText.error = null
        binding.cleaningFeeEditText.error = null
        binding.securityDepositEditText.error = null
    }
    
    private fun clearForm() {
        binding.titleEditText.text?.clear()
        binding.locationEditText.text?.clear()
        binding.descriptionEditText.text?.clear()
        binding.priceEditText.text?.clear()
        binding.guestsEditText.text?.clear()
        binding.bedroomsEditText.text?.clear()
        binding.bathroomsEditText.text?.clear()
        binding.bedsEditText.text?.clear()
        binding.minimumStayEditText.text?.clear()
        binding.maximumStayEditText.text?.clear()
        binding.hostPhoneEditText.text?.clear()
        binding.emergencyContactEditText.text?.clear()
        binding.cleaningFeeEditText.text?.clear()
        binding.securityDepositEditText.text?.clear()
        binding.customRulesEditText.text?.clear()
        
        // Reset spinners
        binding.spinnerPropertyType.setSelection(0)
        binding.spinnerCancellationPolicy.setSelection(0)
        
        // Reset checkboxes and switches
        binding.checkboxWifi.isChecked = false
        binding.checkboxAC.isChecked = false
        binding.checkboxTV.isChecked = false
        binding.checkboxKitchen.isChecked = false
        binding.checkboxParking.isChecked = false
        binding.checkboxWashingMachine.isChecked = false
        binding.checkboxPool.isChecked = false
        binding.checkboxPetFriendly.isChecked = false
        
        binding.switchNoSmoking.isChecked = false
        binding.switchNoParties.isChecked = false
        binding.switchPetsAllowed.isChecked = false
        
        // Reset time and date buttons
        binding.checkInTimeButton.text = "Check-in Time"
        binding.checkOutTimeButton.text = "Check-out Time"
        binding.availabilityStartButton.text = "Start Date"
        binding.availabilityEndButton.text = "End Date"
        
        checkInTime = ""
        checkOutTime = ""
        availabilityStartDate = ""
        availabilityEndDate = ""
        
        // Clear images
        imageAdapter.clearImages()
        
        // Reset identity verification
        binding.identityStatusText.visibility = View.GONE
        viewModel.clearIdentityDocument()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}