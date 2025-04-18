package com.example.travelplannerapp.Adapter



import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.PlaceDetailActivity
import com.example.travelplannerapp.R
import com.example.travelplannerapp.models.places
import com.squareup.picasso.Picasso

class placeadapter(private var placeList: MutableList<places>) :
    RecyclerView.Adapter<placeadapter.PlaceViewHolder>() {

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = filteredPlaceList[position] // Use filtered list

        // Set the place name and description
        holder.placeName.text = place.name
        holder.placeDescription.text = place.description

        // Dynamically load image using Picasso (or you can use Glide)
        if (place.imageUrl.isNotEmpty()) {
            Picasso.get().load(place.imageUrl).into(holder.placeImage)
        } else {
            holder.placeImage.setImageResource(R.drawable.hunza) // Default image if no image URL
        }

        // Show the "View" button only for the selected position
        holder.viewButton.visibility = if (selectedPosition == position) View.VISIBLE else View.GONE

        // Handle click on place item to toggle button visibility
        holder.itemView.setOnClickListener {
            selectedPosition = if (selectedPosition == position) {
                -1 // Hide the button if clicked again
            } else {
                position // Show the button for the clicked item
            }
            notifyDataSetChanged() // Refresh the RecyclerView
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
        val viewButton: Button = itemView.findViewById(R.id.view_button) // Button to view details
    }
}