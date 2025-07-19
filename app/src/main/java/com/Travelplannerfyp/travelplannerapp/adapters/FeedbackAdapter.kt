package com.Travelplannerfyp.travelplannerapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.R
import com.Travelplannerfyp.travelplannerapp.models.Feedback
import com.google.android.material.button.MaterialButton

class FeedbackAdapter(
    private var feedbackList: List<Feedback>,
    private val onDelete: (Feedback) -> Unit
) : RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feedback, parent, false)
        return FeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbackList[position]
        holder.userName.text = feedback.userName
        holder.comment.text = feedback.comment
        holder.rating.rating = feedback.rating
        holder.deleteButton.setOnClickListener { onDelete(feedback) }
    }

    override fun getItemCount(): Int = feedbackList.size

    fun updateData(newFeedback: List<Feedback>) {
        feedbackList = newFeedback
        notifyDataSetChanged()
    }

    class FeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.feedbackUserName)
        val comment: TextView = itemView.findViewById(R.id.feedbackComment)
        val rating: RatingBar = itemView.findViewById(R.id.feedbackRatingBar)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteFeedbackButton)
    }
} 