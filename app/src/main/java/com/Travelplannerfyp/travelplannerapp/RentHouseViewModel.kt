package com.Travelplannerfyp.travelplannerapp

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RentHouseViewModel @Inject constructor(
    private val repository: PropertyRepository
) : ViewModel() {
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _submitResult = MutableLiveData<SubmitResult>()
    val submitResult: LiveData<SubmitResult> = _submitResult
    
    private val _validationErrors = MutableLiveData<List<ValidationError>>()
    val validationErrors: LiveData<List<ValidationError>> = _validationErrors
    
    private var identityDocumentUri: Uri? = null
    
    sealed class SubmitResult {
        object Success : SubmitResult()
        data class Error(val message: String) : SubmitResult()
    }
    
    data class ValidationError(
        val field: String,
        val message: String
    )
    
    fun setIdentityDocumentUri(uri: Uri) {
        identityDocumentUri = uri
    }
    
    fun clearIdentityDocument() {
        identityDocumentUri = null
    }
    
    fun submitPropertyListing(propertyListing: PropertyListing, imageUris: List<Uri>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Validate the form
                val errors = validatePropertyListing(propertyListing, imageUris)
                if (errors.isNotEmpty()) {
                    _validationErrors.value = errors
                    _isLoading.value = false
                    return@launch
                }
                
                // Get current user ID
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _submitResult.value = SubmitResult.Error("User not authenticated")
                    _isLoading.value = false
                    return@launch
                }
                
                // Set owner ID
                val updatedListing = propertyListing.copy(hostId = currentUser.uid)
                
                // Upload images first
                val imageUrls = if (imageUris.isNotEmpty()) {
                    repository.uploadImages(imageUris)
                } else {
                    emptyList()
                }
                
                // Upload identity document if provided
                val identityUrl = identityDocumentUri?.let { uri ->
                    repository.uploadIdentityDocument(uri, currentUser.uid)
                } ?: ""
                
                // Update listing with image URLs and identity verification URL
                val finalListing = updatedListing.copy(
                    imageUrls = imageUrls,
                    identityVerificationUrl = identityUrl
                )
                
                // Save to database
                repository.savePropertyListing(finalListing)
                
                _submitResult.value = SubmitResult.Success
                
            } catch (e: Exception) {
                _submitResult.value = SubmitResult.Error(e.message ?: "Unknown error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun validatePropertyListing(propertyListing: PropertyListing, imageUris: List<Uri>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        // Required fields validation
        if (propertyListing.title.isBlank()) {
            errors.add(ValidationError("title", "Property title is required"))
        } else if (propertyListing.title.length < 5) {
            errors.add(ValidationError("title", "Property title must be at least 5 characters"))
        }
        
        if (propertyListing.location.isBlank()) {
            errors.add(ValidationError("location", "Location is required"))
        }
        
        if (propertyListing.description.isBlank()) {
            errors.add(ValidationError("description", "Description is required"))
        } else if (propertyListing.description.length < 20) {
            errors.add(ValidationError("description", "Description must be at least 20 characters"))
        }
        
        if (propertyListing.pricePerNight <= 0) {
            errors.add(ValidationError("price", "Price per night must be greater than 0"))
        }
        
        if (propertyListing.maxGuests <= 0) {
            errors.add(ValidationError("guests", "Number of guests must be greater than 0"))
        }
        
        if (propertyListing.bedrooms < 0) {
            errors.add(ValidationError("bedrooms", "Number of bedrooms cannot be negative"))
        }
        
        if (propertyListing.bathrooms < 0) {
            errors.add(ValidationError("bathrooms", "Number of bathrooms cannot be negative"))
        }
        
        if (propertyListing.beds <= 0) {
            errors.add(ValidationError("beds", "Number of beds must be greater than 0"))
        }
        
        if (propertyListing.hostPhoneNumber.isBlank()) {
            errors.add(ValidationError("hostPhone", "Host phone number is required"))
        } else if (!isValidPhoneNumber(propertyListing.hostPhoneNumber)) {
            errors.add(ValidationError("hostPhone", "Please enter a valid phone number"))
        }
        
        if (propertyListing.emergencyContact.isBlank()) {
            errors.add(ValidationError("emergencyContact", "Emergency contact is required"))
        } else if (!isValidPhoneNumber(propertyListing.emergencyContact)) {
            errors.add(ValidationError("emergencyContact", "Please enter a valid emergency contact number"))
        }
        
        if (propertyListing.cleaningFee < 0) {
            errors.add(ValidationError("cleaningFee", "Cleaning fee cannot be negative"))
        }
        
        if (propertyListing.securityDeposit < 0) {
            errors.add(ValidationError("securityDeposit", "Security deposit cannot be negative"))
        }
        
        // Time validation
        if (propertyListing.checkInTime.isBlank()) {
            errors.add(ValidationError("checkInTime", "Check-in time is required"))
        }
        
        if (propertyListing.checkOutTime.isBlank()) {
            errors.add(ValidationError("checkOutTime", "Check-out time is required"))
        }
        
        // Date validation
        if (propertyListing.availabilityStart.isBlank()) {
            errors.add(ValidationError("availabilityStart", "Availability start date is required"))
        }
        
        if (propertyListing.availabilityEnd.isBlank()) {
            errors.add(ValidationError("availabilityEnd", "Availability end date is required"))
        }
        
        // Image validation
        if (imageUris.isEmpty()) {
            errors.add(ValidationError("images", "At least one property photo is required"))
        } else if (imageUris.size > 10) {
            errors.add(ValidationError("images", "Maximum 10 photos allowed"))
        }
        
        // Business logic validation
        if (propertyListing.beds > propertyListing.maxGuests * 2) {
            errors.add(ValidationError("beds", "Number of beds seems too high for the number of guests"))
        }
        
        return errors
    }
    
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Basic phone number validation - adjust regex based on your requirements
        val phoneRegex = "^[+]?[0-9]{10,15}$".toRegex()
        return phoneRegex.matches(phoneNumber.replace("\\s".toRegex(), ""))
    }
    
    private fun isValidDate(dateString: String): Boolean {
        return try {
            val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            format.isLenient = false
            format.parse(dateString)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isValidTime(timeString: String): Boolean {
        return try {
            val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            format.isLenient = false
            format.parse(timeString)
            true
        } catch (e: Exception) {
            false
        }
    }
}