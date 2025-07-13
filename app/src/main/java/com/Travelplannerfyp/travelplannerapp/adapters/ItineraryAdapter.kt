package com.Travelplannerfyp.travelplannerapp.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.ItineraryItem

class ItineraryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var groupedItems: Map<String, List<ItineraryItem>> = emptyMap()
    private var dayOrder: List<String> = emptyList()
    private var expandedDays: MutableSet<String> = mutableSetOf()
    private var isEmpty: Boolean = false

    fun setItinerary(itinerary: Map<String, List<ItineraryItem>>) {
        groupedItems = itinerary
        dayOrder = itinerary.keys.sorted()
        expandedDays.clear()
        isEmpty = itinerary.isEmpty()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (isEmpty) return 2 // Empty state
        var count = 0
        for (day in dayOrder) {
            if (count == position) return 0 // Day header
            count++
            if (expandedDays.contains(day)) {
                val items = groupedItems[day] ?: emptyList()
                if (position < count + items.size) return 1 // Itinerary item
                count += items.size
            }
        }
        return 2 // Fallback to empty
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> DayHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_itinerary_day_header, parent, false))
            1 -> ItineraryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_itinerary, parent, false))
            else -> EmptyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_itinerary_empty, parent, false))
        }
    }

    override fun getItemCount(): Int {
        if (isEmpty) return 1
        var count = 0
        for (day in dayOrder) {
            count++ // header
            if (expandedDays.contains(day)) {
                count += groupedItems[day]?.size ?: 0
            }
        }
        return count
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (isEmpty && holder is EmptyViewHolder) {
            holder.emptyTextView.text = "No itinerary added yet."
            holder.emptyTextView.setTypeface(null, Typeface.BOLD)
            return
        }
        var pos = 0
        for (day in dayOrder) {
            if (pos == position && holder is DayHeaderViewHolder) {
                holder.dayTextView.text = day.capitalize()
                holder.dayTextView.setTypeface(null, Typeface.BOLD)
                holder.itemView.setOnClickListener {
                    if (expandedDays.contains(day)) expandedDays.remove(day) else expandedDays.add(day)
                    notifyDataSetChanged()
                }
                holder.expandIndicator.text = if (expandedDays.contains(day)) "▼" else "►"
                return
            }
            pos++
            if (expandedDays.contains(day)) {
                val items = groupedItems[day] ?: emptyList()
                if (position < pos + items.size) {
                    val item = items[position - pos]
                    if (holder is ItineraryViewHolder) {
                        holder.timeTextView.text = item.time
                        holder.titleTextView.text = item.title
                        holder.descriptionTextView.text = item.description
                        holder.descriptionTextView.visibility = if (item.description.isEmpty()) View.GONE else View.VISIBLE
                    }
                    return
                }
                pos += items.size
            }
        }
    }

    class DayHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        val expandIndicator: TextView = itemView.findViewById(R.id.expandIndicator)
    }
    class ItineraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
    }
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emptyTextView: TextView = itemView.findViewById(R.id.emptyTextView)
    }
} 