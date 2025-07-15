package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.models.Hotel
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.utils.HotelImageLoader
import com.squareup.picasso.Picasso


class HotelAdapter(
    private var hotelList: List<Hotel>,
    private val onItemClick: (Hotel) -> Unit,
    private val onAddToCartClick: (Hotel) -> Unit
) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.hotel_item, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        val hotel = hotelList[position]

        holder.hotelName.text = hotel.name
        holder.hotelDescription.text = hotel.description
        holder.hotelRating.rating = hotel.rating.toFloat()  // Use rating property for RatingBar
        holder.hotelPrice.text = "Price: ${hotel.price}"
        holder.hotelPriceInput.setText(hotel.pricePerNight)

        // Use unified HotelImageLoader for all hotel images
        HotelImageLoader.loadHotelImage(hotel.imageUrl, hotel.imageName, hotel.name, holder.hotelImage)
        // Handling item click
        holder.itemView.setOnClickListener {
            onItemClick(hotel)  // Call the onItemClick listener
        }

        // Handling add to cart click
        holder.addToCartButton.setOnClickListener {
            val priceInput = holder.hotelPriceInput.text.toString().trim()
            if (priceInput.isEmpty()) {
                holder.hotelPriceInput.error = "Enter price"
                return@setOnClickListener
            }
            val priceDouble = priceInput.toDoubleOrNull()
            if (priceDouble == null) {
                holder.hotelPriceInput.error = "Invalid price"
                return@setOnClickListener
            }
            // Update hotel pricePerNight as Double string for model compatibility
            val updatedHotel = hotel.copy(pricePerNight = priceDouble.toString())
            onAddToCartClick(updatedHotel)
        }
    }

    override fun getItemCount(): Int = hotelList.size
    
    private fun loadImageFromDrawable(imageName: String, holder: HotelViewHolder) {
        if (imageName.isEmpty()) {
            android.util.Log.d("HotelAdapter", "Image name is empty, using placeholder")
            holder.hotelImage.setImageResource(R.drawable.placeholder_image)
            return
        }
        
        try {
            val context = holder.itemView.context
            // Try different variations of the image name to find a match
            val rawImageName = imageName.substringBeforeLast(".").lowercase()
            android.util.Log.d("HotelAdapter", "Trying to load drawable with raw name: $rawImageName")
            
            // Try the raw name first
            var resourceId = context.resources.getIdentifier(rawImageName, "drawable", context.packageName)
            
            // If not found, try with normalized name (replace special chars with underscore)
            if (resourceId == 0) {
                val normalizedName = rawImageName.replace("[^a-z0-9]+".toRegex(), "_").trim('_')
                android.util.Log.d("HotelAdapter", "Raw name not found, trying normalized name: $normalizedName")
                resourceId = context.resources.getIdentifier(normalizedName, "drawable", context.packageName)
            }
            
            // If still not found, try with just the first part of the name (before any underscore or space)
            if (resourceId == 0) {
                val simplifiedName = rawImageName.split("_", " ", "-").first().lowercase()
                android.util.Log.d("HotelAdapter", "Normalized name not found, trying simplified name: $simplifiedName")
                resourceId = context.resources.getIdentifier(simplifiedName, "drawable", context.packageName)
            }
            
            // If a resource was found, use it
            if (resourceId != 0) {
                android.util.Log.d("HotelAdapter", "Loading hotel image from drawable, resourceId: $resourceId")
                holder.hotelImage.setImageResource(resourceId)
            } else {
                // Last resort - try some common drawable names
                val commonDrawables = arrayOf("placeholder_image", "placeholder", "ic_placeholder")
                for (commonName in commonDrawables) {
                    resourceId = context.resources.getIdentifier(commonName, "drawable", context.packageName)
                    if (resourceId != 0) {
                        android.util.Log.d("HotelAdapter", "Using common drawable: $commonName")
                        holder.hotelImage.setImageResource(resourceId)
                        return
                    }
                }
                
                // If all else fails, use placeholder
                android.util.Log.d("HotelAdapter", "No drawable found for: $rawImageName, using placeholder")
                holder.hotelImage.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            android.util.Log.e("HotelAdapter", "Error loading hotel image from drawable: ${e.message}", e)
            holder.hotelImage.setImageResource(R.drawable.placeholder_image)
        }
    }

    inner class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelName: TextView = itemView.findViewById(R.id.hotel_name)
        val hotelDescription: TextView = itemView.findViewById(R.id.hotel_description)
        val hotelRating: RatingBar = itemView.findViewById(R.id.hotel_rating)  // Use RatingBar type
        val hotelPrice: TextView = itemView.findViewById(R.id.hotel_price)
        val hotelImage: ImageView = itemView.findViewById(R.id.hotel_image)
        val addToCartButton: ImageButton = itemView.findViewById(R.id.btn_add_to_cart)
        val hotelPriceInput: android.widget.EditText = itemView.findViewById(R.id.hotel_price_input)
    }
}