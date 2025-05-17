package com.example.jsflower.adaptar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.Model.ReviewModel
import com.example.jsflower.R

class   ReviewAdapter(private val reviewsList: List<ReviewModel>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: ImageView = itemView.findViewById(R.id.reviewUserImage)
        val userName: TextView = itemView.findViewById(R.id.reviewUserName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        val reviewDate: TextView = itemView.findViewById(R.id.reviewDate)
        val reviewComment: TextView = itemView.findViewById(R.id.reviewComment)
        val reviewImagesRecyclerView: RecyclerView = itemView.findViewById(R.id.reviewImageRecyclerView)


        // Add reference to image recycler view if you implement image attachments
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewsList[position]

        holder.userName.text = review.userName
        holder.ratingBar.rating = review.rating
        holder.reviewDate.text = review.date
        holder.reviewComment.text = review.comment

        // Load user image if available
        if (review.userImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(review.userImage)
                .placeholder(R.drawable.user) // Add a placeholder image to your drawable resources
                .circleCrop()
                .into(holder.userImage)
        } else {
            // Set default profile image
            holder.userImage.setImageResource(R.drawable.user)
        }

        if (review.images != null && review.images.isNotEmpty()) {
            holder.reviewImagesRecyclerView.visibility = View.VISIBLE
            setupReviewImagesRecyclerView(holder.reviewImagesRecyclerView, review.images)
        } else {
            holder.reviewImagesRecyclerView.visibility = View.GONE
        }

    }
    private fun setupReviewImagesRecyclerView(recyclerView: RecyclerView, images: List<String>) {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ReviewImageAdapter(recyclerView.context, images)
    }



    override fun getItemCount(): Int = reviewsList.size
}