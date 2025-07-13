package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.EnhancedTrip
import com.Travelplannerfyp.travelplannerapp.models.Hotel
import com.Travelplannerfyp.travelplannerapp.models.TripJoinStatus
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class EnhancedTripAdapter(
    private val trips: List<EnhancedTrip>,
    private val currentUserId: String,
    private val onTripClick: (EnhancedTrip) -> Unit,
    private val onJoinTrip: (EnhancedTrip) -> Unit
) : RecyclerView.Adapter<EnhancedTripAdapter.TripViewHolder>() {

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripImageView: ShapeableImageView = itemView.findViewById(R.id.tripImageView)
        private val tripTitleTextView: TextView = itemView.findViewById(R.id.tripTitleTextView)
        private val tripLocationTextView: TextView = itemView.findViewById(R.id.tripLocationTextView)
        private val tripDatesTextView: TextView = itemView.findViewById(R.id.tripDatesTextView)
        private val tripSeatsTextView: TextView = itemView.findViewById(R.id.tripSeatsTextView)
        private val tripOrganizerTextView: TextView = itemView.findViewById(R.id.tripOrganizerTextView)
        private val tripPriceTextView: TextView = itemView.findViewById(R.id.tripPriceTextView)
        private val joinTripButton: MaterialButton = itemView.findViewById(R.id.joinTripButton)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        
        // Hotel views
        private val hotelCardView: View = itemView.findViewById(R.id.hotelCardView)
        private val hotelImageView: ShapeableImageView = itemView.findViewById(R.id.hotelImageView)
        private val hotelNameTextView: TextView = itemView.findViewById(R.id.hotelNameTextView)
        private val hotelPriceTextView: TextView = itemView.findViewById(R.id.hotelPriceTextView)
        private val hotelAmenitiesTextView: TextView = itemView.findViewById(R.id.hotelAmenitiesTextView)

        fun bind(trip: EnhancedTrip) {
            // Set trip details with better formatting
            tripTitleTextView.text = trip.placeName
            tripLocationTextView.text = trip.placeDescription
            tripDatesTextView.text = formatDates(trip.startDate, trip.endDate)
            tripSeatsTextView.text = "Seats: ${trip.seatsAvailable}"
            tripOrganizerTextView.text = "By: ${trip.organizerName}"
            tripPriceTextView.text = formatPrice(trip.price)
            loadTripImage(trip.placeImageUrl, tripImageView)
            // Remove Join/Your Trip/Joined buttons for now
            joinTripButton.visibility = View.GONE
            statusTextView.visibility = View.GONE
            // Display hotel information if available
            displayHotelInfo(trip.hotel)
            // Set click listeners
            itemView.setOnClickListener { onTripClick(trip) }
            // Add hotel card click listener
            hotelCardView.setOnClickListener {
                trip.hotel?.let { hotel ->
                    val context = itemView.context
                    val intent = android.content.Intent(context, com.Travelplannerfyp.travelplannerapp.HotelDetailActivity::class.java).apply {
                        putExtra("name", hotel.name)
                        putExtra("description", hotel.description)
                        putExtra("rating", hotel.rating)
                        putExtra("price", hotel.pricePerNight)
                        putExtra("imageName", hotel.imageName)
                        putExtra("imageUrl", hotel.imageUrl)
                        putStringArrayListExtra("amenities", ArrayList(hotel.amenities))
                    }
                    context.startActivity(intent)
                }
            }
        }
        
        private fun displayHotelInfo(hotel: Hotel?) {
            if (hotel != null && (hotel.name.isNotEmpty() || hotel.pricePerNight.isNotEmpty())) {
                hotelCardView.visibility = View.VISIBLE
                hotelNameTextView.text = hotel.name.takeIf { it.isNotEmpty() } ?: "Hotel"
                if (hotel.pricePerNight.isNotEmpty()) {
                    hotelPriceTextView.text = "${CurrencyUtils.formatAsPKR(hotel.pricePerNight)} per night"
                    hotelPriceTextView.visibility = View.VISIBLE
                } else {
                    hotelPriceTextView.text = "Price not set"
                    hotelPriceTextView.visibility = View.VISIBLE
                }
                if (hotel.description.isNotEmpty()) {
                    hotelAmenitiesTextView.text = hotel.description
                    hotelAmenitiesTextView.visibility = View.VISIBLE
                } else {
                    hotelAmenitiesTextView.text = "Amenities: Not listed"
                    hotelAmenitiesTextView.visibility = View.VISIBLE
                }
                if (hotel.imageUrl.isNotEmpty()) {
                    try {
                        com.squareup.picasso.Picasso.get()
                            .load(hotel.imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(hotelImageView)
                    } catch (e: Exception) {
                        hotelImageView.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else if (hotel.imageName.isNotEmpty()) {
                    val resourceId = getDrawableResourceId(hotel.imageName)
                    if (resourceId != 0) {
                        hotelImageView.setImageResource(resourceId)
                    } else {
                        hotelImageView.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    hotelImageView.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
                hotelCardView.visibility = View.GONE
            }
        }

        private fun formatDates(startDate: String, endDate: String): String {
            return if (startDate == endDate) {
                startDate
            } else {
                "$startDate - $endDate"
            }
        }

        private fun formatPrice(price: String): String {
            return if (price.isNotEmpty()) {
                CurrencyUtils.formatAsPKR(price)
            } else {
                "Price not set"
            }
        }

        private fun getTripStatus(trip: EnhancedTrip): TripJoinStatus {
            return when {
                trip.organizerId == currentUserId -> TripJoinStatus.OWNED
                trip.joinedUsers.contains(currentUserId) -> TripJoinStatus.JOINED
                else -> TripJoinStatus.NOT_JOINED
            }
        }

        private fun updateTripStatusUI(status: TripJoinStatus) {
            when (status) {
                TripJoinStatus.OWNED -> {
                    joinTripButton.visibility = View.GONE
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Your Trip"
                    statusTextView.setBackgroundResource(R.drawable.your_trip_badge_background)
                }
                TripJoinStatus.JOINED -> {
                    joinTripButton.visibility = View.GONE
                    statusTextView.visibility = View.VISIBLE
                    statusTextView.text = "Joined ✅"
                    statusTextView.setBackgroundResource(R.drawable.joined_badge_background)
                }
                TripJoinStatus.NOT_JOINED -> {
                    joinTripButton.visibility = View.VISIBLE
                    statusTextView.visibility = View.GONE
                    joinTripButton.text = "Join Trip"
                    joinTripButton.isEnabled = true
                }
            }
        }

        private fun loadTripImage(imageUrl: String, imageView: ShapeableImageView) {
            if (imageUrl.isNotEmpty()) {
                try {
                    // Try to load from drawable resources first
                    val resourceId = getDrawableResourceId(imageUrl)
                    if (resourceId != 0) {
                        imageView.setImageResource(resourceId)
                    } else {
                        // If not found in drawables, use placeholder
                        imageView.setImageResource(R.drawable.placeholder_image)
                    }
                } catch (e: Exception) {
                    imageView.setImageResource(R.drawable.placeholder_image)
                }
            } else {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }

        private fun getDrawableResourceId(imageName: String): Int {
            return try {
                val context = tripImageView.context
                // Clean the image name and try multiple variations
                val cleanName = imageName.replace(".jpg", "").replace(".png", "").replace(".jpeg", "").replace(".webp", "")
                
                // Try different variations of the name
                val possibleNames = listOf(
                    cleanName,
                    cleanName.lowercase(),
                    cleanName.replace(" ", "_"),
                    cleanName.replace("_", ""),
                    cleanName.replace("-", "_"),
                    cleanName.replace("-", "")
                )
                
                for (name in possibleNames) {
                    val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
                    if (resourceId != 0) {
                        return resourceId
                    }
                }
                
                // If still not found, try to match based on place name patterns
                val placeName = imageName.lowercase()
                when {
                    placeName.contains("hunza") -> return context.resources.getIdentifier("hunza", "drawable", context.packageName)
                    placeName.contains("murree") -> return context.resources.getIdentifier("murree", "drawable", context.packageName)
                    placeName.contains("naran") -> return context.resources.getIdentifier("naran", "drawable", context.packageName)
                    placeName.contains("skardu") -> return context.resources.getIdentifier("skardu", "drawable", context.packageName)
                    placeName.contains("chitral") -> return context.resources.getIdentifier("chitral", "drawable", context.packageName)
                    placeName.contains("gilgit") -> return context.resources.getIdentifier("gilgit", "drawable", context.packageName)
                    placeName.contains("fairy") -> return context.resources.getIdentifier("fairy_meadows", "drawable", context.packageName)
                    placeName.contains("attabad") -> return context.resources.getIdentifier("attabad", "drawable", context.packageName)
                    placeName.contains("khunjerab") -> return context.resources.getIdentifier("khunjerab", "drawable", context.packageName)
                    placeName.contains("neelum") -> return context.resources.getIdentifier("neelumvalley", "drawable", context.packageName)
                    placeName.contains("ratti") -> return context.resources.getIdentifier("rattigali", "drawable", context.packageName)
                    placeName.contains("balakot") -> return context.resources.getIdentifier("balakot", "drawable", context.packageName)
                    placeName.contains("ziarat") -> return context.resources.getIdentifier("ziarat", "drawable", context.packageName)
                    placeName.contains("lahore") -> return context.resources.getIdentifier("lahore", "drawable", context.packageName)
                    placeName.contains("islamabad") -> return context.resources.getIdentifier("islamabad", "drawable", context.packageName)
                    placeName.contains("karachi") -> return context.resources.getIdentifier("karachi", "drawable", context.packageName)
                    placeName.contains("kotli") -> return context.resources.getIdentifier("kotli", "drawable", context.packageName)
                }
                
                0
            } catch (e: Exception) {
                0
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.enhanced_trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount(): Int = trips.size
}