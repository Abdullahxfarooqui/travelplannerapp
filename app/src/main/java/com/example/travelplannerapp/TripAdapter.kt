package com.example.travelplannerapp
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class TripAdapter(private val context: Context, private var tripList: List<Trip>) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]
        holder.bind(trip)


        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("trip_name", trip.name)
                putExtra("trip_location", trip.location)
                putExtra("trip_description", trip.description)
                putExtra("trip_image", trip.imageResId ?: 0)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = tripList.size

    fun updateTrips(newTrips: List<Trip>) {
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

            Log.d("AdapterDebug", "Setting Description: ${trip.description}")

            // Load image correctly
            if (trip.imageResId != null && trip.imageResId != 0) {
                tripImage.setImageResource(trip.imageResId)
            } else {
                tripImage.setImageResource(R.drawable.placeholder_image) // Default image
            }
        }
    }
}
