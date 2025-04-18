package com.example.travelplannerapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.R
import com.example.travelplannerapp.models.Hotel
import com.squareup.picasso.Picasso

class HotelAdapter(
    private var hotelList: List<Hotel>,
    private val onItemClick: (Hotel) -> Unit
) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.hotel_item, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        val hotel = hotelList[position]

        holder.hotelName.text = hotel.name
        holder.hotelDescription.text = hotel.description
        holder.hotelRating.text = "Rating: ${hotel.rating}"
        holder.hotelPrice.text = "Price: ${hotel.price}"

        if (hotel.imageUrl.isNotEmpty()) {
            Picasso.get().load(hotel.imageUrl).into(holder.hotelImage)
        } else {
            holder.hotelImage.setImageResource(R.drawable.placeholder)
        }

        holder.itemView.setOnClickListener {
            onItemClick(hotel)
        }
    }

    override fun getItemCount(): Int = hotelList.size

    inner class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hotelName: TextView = itemView.findViewById(R.id.hotel_name)
        val hotelDescription: TextView = itemView.findViewById(R.id.hotel_description)
        val hotelRating: TextView = itemView.findViewById(R.id.hotel_rating)
        val hotelPrice: TextView = itemView.findViewById(R.id.hotel_price)
        val hotelImage: ImageView = itemView.findViewById(R.id.hotel_image)
    }
}