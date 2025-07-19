package com.Travelplannerfyp.travelplannerapp
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.Travelplannerfyp.travelplannerapp.R
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(private val context: Context, private var tripList: List<Trip>) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]
        holder.bind(trip)
        // Display price as Rs. <pricePerPerson>
        holder.itemView.findViewById<TextView>(R.id.tripPriceTextView)?.text =
            if (!trip.pricePerPerson.isNullOrEmpty()) "Rs. ${trip.pricePerPerson}" else "Price: Not set"
        // Add debug logging to track binding
        Log.d("TripAdapter", "Binding trip at position $position: ${trip.name}, imageUrl: ${trip.imageUrl}, imageResId: ${trip.imageResId}")

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("trip_name", trip.name)
                putExtra("trip_location", trip.location)
                putExtra("trip_description", trip.description)
                putExtra("trip_image_url", trip.imageUrl)
                // Pass image name instead of resource ID
                putExtra("trip_image_name", trip.imageName ?: "placeholder_image") // Assuming Trip has imageName
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        // Add logging to track item count
        Log.d("TripAdapter", "getItemCount called, returning ${tripList.size} items")
        return tripList.size
    }

    fun updateTrips(newTrips: List<Trip>) {
        Log.d("TripAdapter", "updateTrips called with ${newTrips.size} trips")
        tripList = newTrips
        notifyDataSetChanged()
    }

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripImage: ImageView = itemView.findViewById(R.id.tripImageView)
        private val tripName: TextView = itemView.findViewById(R.id.tripTitleTextView)
        private val tripLocation: TextView = itemView.findViewById(R.id.tripLocationTextView)
        private val tripDescription: TextView = itemView.findViewById(R.id.tripDescriptionTextView)

        fun bind(trip: Trip) {
            tripName.text = trip.name
            tripLocation.text = trip.location
            tripDescription.text = trip.description

            // Add logging before image loading
            Log.d("TripViewHolder", "Binding trip: ${trip.name}, imageUrl: ${trip.imageUrl}, imageResId: ${trip.imageResId}")

            // Use TripImageLoader to handle image loading
            com.Travelplannerfyp.travelplannerapp.utils.TripImageLoader.loadTripImage(
                itemView.context,
                tripImage,
                trip.imageUrl,
                trip.imageResId,
                trip.name ?: "Unknown Trip"
            )
        }
    }
}
