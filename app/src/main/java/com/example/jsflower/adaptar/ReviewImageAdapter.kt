package com.example.jsflower.adaptar

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.example.jsflower.FullScreenImageActivity
import com.example.jsflower.R

class ReviewImageAdapter(
    private val context: Context,
    private val images: List<String>
) : RecyclerView.Adapter<ReviewImageAdapter.ImageViewHolder>() {

    private val TAG = "ReviewImageAdapter"

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.reviewImageView)
        val progressBar: ProgressBar = itemView.findViewById(R.id.reviewImageLoading)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_review_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val imageUrl = images[position]

        // Debug log for URL
        Log.d(TAG, "Loading image at position $position with URL: $imageUrl")

        // Show progress bar initially
        holder.progressBar.visibility = View.VISIBLE

        // Clear any previous image from the recycled view
        holder.imageView.setImageDrawable(null)

        try {
            // Use RequestListener for detailed debugging
            Glide.with(context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Debug log for failures
                        Log.e(TAG, "Failed to load image at position $position: $imageUrl", e)
                        e?.let {
                            for (t in it.rootCauses) {
                                Log.e(TAG, "Root cause: ${t.message}", t)
                            }
                        }
                        holder.progressBar.visibility = View.GONE

                        // Show error message for debugging
                        Toast.makeText(context, "Failed to load image: ${e?.message}", Toast.LENGTH_SHORT).show()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Debug log for success
                        Log.d(TAG, "Successfully loaded image at position $position from $dataSource")
                        holder.progressBar.visibility = View.GONE
                        return false // Return false to allow Glide to set the resource
                    }
                })
                .into(holder.imageView)

            // Additional debugging - check if the URL is valid
            if (imageUrl.isNullOrEmpty() || !imageUrl.startsWith("http")) {
                Log.e(TAG, "Invalid URL format at position $position: $imageUrl")
            }

        } catch (e: Exception) {
            // Catch any exceptions during Glide setup
            Log.e(TAG, "Exception while setting up Glide for position $position", e)
            holder.progressBar.visibility = View.GONE
        }

        // Debug the click behavior too
        holder.imageView.setOnClickListener {
            Log.d(TAG, "Image clicked at position $position: $imageUrl")
            val intent = Intent(context, FullScreenImageActivity::class.java)
            intent.putExtra("image_url", imageUrl)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "Total review images: ${images.size}")
        return images.size
    }

    // Display image URLs for debugging
    fun debugImageUrls() {
        Log.d(TAG, "--------- DEBUG IMAGE URLS ---------")
        images.forEachIndexed { index, url ->
            Log.d(TAG, "Image[$index]: $url")
        }
        Log.d(TAG, "------------------------------------")
    }
}