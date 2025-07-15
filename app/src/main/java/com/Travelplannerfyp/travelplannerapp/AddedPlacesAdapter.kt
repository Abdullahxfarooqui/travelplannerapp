package com.Travelplannerfyp.travelplannerapp
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R

class AddedPlacesAdapter (private val addedPlacesList: List<String>) :
    RecyclerView.Adapter<AddedPlacesAdapter.AddedPlaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddedPlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_place, parent, false)
        return AddedPlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddedPlaceViewHolder, position: Int) {
        val place = addedPlacesList[position]
        holder.placeNameTextView.text = place
    }

    override fun getItemCount(): Int {
        return addedPlacesList.size
    }

    inner class AddedPlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeNameTextView: TextView = itemView.findViewById(R.id.place_name)
    }
}