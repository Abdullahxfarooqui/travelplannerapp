package com.Travelplannerfyp.travelplannerapp.adapters



import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.PlaceDetailActivity
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.places
import com.Travelplannerfyp.travelplannerapp.utils.ImageDatabaseLoader

class placeadapterAQ(private var placeList: MutableList<places>) :
    RecyclerView.Adapter<placeadapterAQ.PlaceViewHolder>() {

    private var filteredPlaceList: MutableList<places> = placeList // Maintain a filtered list
    private var selectedPosition: Int = -1

    // Method to update the place list
    fun updatePlaceList(newPlaceList: List<places>) {
        placeList.clear()
        placeList.addAll(newPlaceList)
        filteredPlaceList = placeList.toMutableList() // Reset to full list when updating
        notifyDataSetChanged()
    }

    // Method to filter the places based on a search query
    fun filter(query: String) {
        filteredPlaceList = if (query.isEmpty()) {
            placeList // If query is empty, show all places
        } else {
            placeList.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }.toMutableList() // Filter by name or description
        }
        notifyDataSetChanged() // Notify that the dataset has changed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_placeaq, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = filteredPlaceList[position] // Use filtered list

        // Set the place name and description
        holder.placeName.text = place.name
        holder.placeDescription.text = place.description

        // Dynamically load image using improved ImageDatabaseLoader for Firebase Realtime Database
        // Set a tag on the ImageView to identify which place this is
        holder.placeImage.tag = place.name
        
        try {
            // Check if we have a valid image URL
            if (place.imageUrl.isNotEmpty()) {
                Log.d("PlaceAdapterAQ", "Loading image for ${place.name} with URL: ${place.imageUrl}")
                // Use ImageDatabaseLoader to load images from Firebase Realtime Database
                // The imageUrl can be in the format "db://path/to/image" for database references
                // or a regular URL for direct loading
                ImageDatabaseLoader.loadImage(holder.placeImage, place.imageUrl)
            } else {
                Log.d("PlaceAdapterAQ", "No image URL for ${place.name}, using local resource")
                // No URL provided, use local image resource based on place name
                ImageDatabaseLoader.loadLocalImageByName(holder.placeImage, place.name)
            }
        } catch (e: Exception) {
            Log.e("PlaceAdapterAQ", "Error loading image for ${place.name}: ${e.message}")
            // If there's an error loading the image, fall back to local resources
            ImageDatabaseLoader.loadLocalImageByName(holder.placeImage, place.name)
        }
        
        // Method removed as this functionality is now handled by ImageDatabaseLoader
        
        // Show the "View" button only for the selected position
        holder.viewButton.visibility = if (selectedPosition == position) View.VISIBLE else View.GONE

        // Handle click on place item to toggle button visibility
        holder.itemView.setOnClickListener {
            selectedPosition = if (selectedPosition == position) {
                -1 // Hide the button if clicked again
            } else {
                position // Show the button for the clicked item
            }
            notifyItemChanged(position) // Notify only the clicked item
        }

        // Handle the "View" button click to navigate to PlaceDetailActivity
        holder.viewButton.setOnClickListener {
            val context = it.context
            val intent = Intent(context, PlaceDetailActivity::class.java).apply {
                putExtra("PLACE_NAME", place.name)
                putExtra("PLACE_DESCRIPTION", place.description)
                putExtra("PLACE_IMAGE_URL", place.imageUrl) // Add other place details if needed
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return filteredPlaceList.size // Use filtered list size
    }

    // ViewHolder class
    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.place_name)
        val placeDescription: TextView = itemView.findViewById(R.id.place_description)
        val placeImage: ImageView = itemView.findViewById(R.id.testImage)
        val viewButton: Button = itemView.findViewById(R.id.view_button)

        init {
            if (placeName == null || placeDescription == null || viewButton == null) {
                throw NullPointerException("Views not found in the layout")
            }
        }
    }
}