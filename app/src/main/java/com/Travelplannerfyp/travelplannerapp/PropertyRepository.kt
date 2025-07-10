package com.Travelplannerfyp.travelplannerapp

import android.net.Uri
import android.util.Base64
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.util.*
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage
) {
    private val propertiesRef = database.getReference("properties")
    private val storageRef = storage.reference
    
    suspend fun savePropertyListing(propertyListing: PropertyListing): String {
        return try {
            // Generate a unique ID for the property
            val propertyId = propertiesRef.push().key ?: throw Exception("Failed to generate property ID")
            
            // Create the property with the generated ID
            val propertyWithId = propertyListing.copy(id = propertyId)
            
            // Save to Firebase Realtime Database
            propertiesRef.child(propertyId).setValue(propertyWithId).await()
            
            propertyId
        } catch (e: Exception) {
            throw Exception("Failed to save property listing: ${e.message}")
        }
    }
    
    suspend fun uploadImages(imageUris: List<Uri>): List<String> {
        val imageUrls = mutableListOf<String>()
        
        try {
            for ((index, uri) in imageUris.withIndex()) {
                val imageId = UUID.randomUUID().toString()
                val imageRef = storageRef.child("property_images/$imageId.jpg")
                
                // Upload image to Firebase Storage
                val uploadTask = imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                
                imageUrls.add(downloadUrl.toString())
            }
        } catch (e: Exception) {
            throw Exception("Failed to upload images: ${e.message}")
        }
        
        return imageUrls
    }
    
    suspend fun uploadIdentityDocument(uri: Uri, userId: String): String {
        return try {
            val documentId = UUID.randomUUID().toString()
            val documentRef = storageRef.child("identity_documents/$userId/$documentId.jpg")
            
            // Upload document to Firebase Storage
            val uploadTask = documentRef.putFile(uri).await()
            val downloadUrl = documentRef.downloadUrl.await()
            
            downloadUrl.toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload identity document: ${e.message}")
        }
    }
    
    suspend fun getPropertyListings(): List<PropertyListing> {
        return try {
            val snapshot = propertiesRef.get().await()
            val properties = mutableListOf<PropertyListing>()
            
            for (child in snapshot.children) {
                val property = child.getValue(PropertyListing::class.java)
                property?.let { properties.add(it) }
            }
            
            properties
        } catch (e: Exception) {
            throw Exception("Failed to fetch property listings: ${e.message}")
        }
    }
    
    suspend fun getPropertyById(propertyId: String): PropertyListing? {
        return try {
            val snapshot = propertiesRef.child(propertyId).get().await()
            snapshot.getValue(PropertyListing::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to fetch property: ${e.message}")
        }
    }
    
    suspend fun getPropertiesByOwner(ownerId: String): List<PropertyListing> {
        return try {
            val snapshot = propertiesRef.orderByChild("ownerId").equalTo(ownerId).get().await()
            val properties = mutableListOf<PropertyListing>()
            
            for (child in snapshot.children) {
                val property = child.getValue(PropertyListing::class.java)
                property?.let { properties.add(it) }
            }
            
            properties
        } catch (e: Exception) {
            throw Exception("Failed to fetch owner properties: ${e.message}")
        }
    }
    
    suspend fun updatePropertyAvailability(propertyId: String, isAvailable: Boolean) {
        try {
            propertiesRef.child(propertyId).child("available").setValue(isAvailable).await()
        } catch (e: Exception) {
            throw Exception("Failed to update property availability: ${e.message}")
        }
    }
    
    suspend fun deleteProperty(propertyId: String) {
        try {
            // Get the property first to access image URLs
            val property = getPropertyById(propertyId)
            
            // Delete images from storage
            property?.imageUrls?.forEach { imageUrl ->
                try {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                } catch (e: Exception) {
                    // Continue even if image deletion fails
                }
            }
            
            // Delete identity document if exists
            if (property?.identityVerificationUrl?.isNotEmpty() == true) {
                try {
                    val documentRef = storage.getReferenceFromUrl(property.identityVerificationUrl)
                    documentRef.delete().await()
                } catch (e: Exception) {
                    // Continue even if document deletion fails
                }
            }
            
            // Delete property from database
            propertiesRef.child(propertyId).removeValue().await()
            
        } catch (e: Exception) {
            throw Exception("Failed to delete property: ${e.message}")
        }
    }
    
    suspend fun searchProperties(
        location: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minGuests: Int? = null,
        amenities: List<String>? = null
    ): List<PropertyListing> {
        return try {
            val allProperties = getPropertyListings()
            
            allProperties.filter { property ->
                var matches = true
                
                // Filter by location
                if (location != null && location.isNotEmpty()) {
                    matches = matches && property.location.contains(location, ignoreCase = true)
                }
                
                // Filter by price range
                if (minPrice != null) {
                    matches = matches && property.pricePerNight >= minPrice
                }
                if (maxPrice != null) {
                    matches = matches && property.pricePerNight <= maxPrice
                }
                
                // Filter by minimum guests
                if (minGuests != null) {
                    matches = matches && property.maxGuests >= minGuests
                }
                
                // Filter by amenities
                if (amenities != null && amenities.isNotEmpty()) {
                    matches = matches && amenities.all { amenity ->
                        property.amenities.any { it.equals(amenity, ignoreCase = true) }
                    }
                }
                
                // Only show available properties
                matches = matches && property.available
                
                matches
            }
        } catch (e: Exception) {
            throw Exception("Failed to search properties: ${e.message}")
        }
    }
    
    suspend fun updateProperty(propertyListing: PropertyListing) {
        try {
            propertiesRef.child(propertyListing.id).setValue(propertyListing).await()
        } catch (e: Exception) {
            throw Exception("Failed to update property: ${e.message}")
        }
    }
    
    // Helper function to convert URI to Base64 (alternative approach for image storage)
    private suspend fun uriToBase64(uri: Uri, context: Context): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } else {
                throw Exception("Failed to read image data")
            }
        } catch (e: Exception) {
            throw Exception("Failed to convert image to Base64: ${e.message}")
        }
    }
    
    // Alternative method to upload images as Base64 to Realtime Database
    suspend fun uploadImagesAsBase64(imageUris: List<Uri>, context: Context): List<String> {
        val imageUrls = mutableListOf<String>()
        
        try {
            for ((index, uri) in imageUris.withIndex()) {
                val imageId = UUID.randomUUID().toString()
                val base64Image = uriToBase64(uri, context)
                
                // Store Base64 image in Realtime Database
                val imageRef = database.getReference("property_images").child(imageId)
                val imageData = mapOf(
                    "id" to imageId,
                    "data" to base64Image,
                    "timestamp" to System.currentTimeMillis()
                )
                
                imageRef.setValue(imageData).await()
                
                // Return the database reference URL
                imageUrls.add(imageRef.toString())
            }
        } catch (e: Exception) {
            throw Exception("Failed to upload images as Base64: ${e.message}")
        }
        
        return imageUrls
    }
}