package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.R
import com.example.jsflower.databinding.MenuItemBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class PopularAdapter(
    private val popularItems: List<MenuItem>,
    private val context: Context,
    private val onAddToCart: (MenuItem) -> Unit
) : RecyclerView.Adapter<PopularAdapter.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        val menuItem = popularItems[position]
        holder.bind(menuItem)

        // Set click listener for the "Add to Cart" button
        holder.binding.menuAddToCart.setOnClickListener {
            onAddToCart(menuItem) // Add item to cart
        }

        // Load product tags and apply discounts
        loadProductTagsAndApplyDiscount(menuItem, holder)

        // Load reviews for product
        loadProductReviews(menuItem.key, holder)

        // Set item click listener to open details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.flowerName)
                putExtra("MenuItemPrice", menuItem.flowerPrice)
                putExtra("MenuItemImage", menuItem.flowerImage)
                putExtra("MenuItemDescription", menuItem.flowerDescription)
                putExtra("MenuItemIngredient", menuItem.flowerIngredient)
                putExtra("MenuItemKey", menuItem.key)
                putExtra("TAG", menuItem.tags)
                println("DEBUG: TAG = ${menuItem.tags}")
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = popularItems.size

    private fun loadProductTagsAndApplyDiscount(menuItem: MenuItem, holder: PopularViewHolder) {
        val tagsRef = FirebaseDatabase.getInstance().reference.child("list").child(menuItem.key).child("tags")

        tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Default: No discount
                var discount = 0.0
                var tagText = ""
                var tagBgColor = R.drawable.sale_badge_shape // Default background

                // Try to handle different formats of tags in the database
                try {
                    // First try: Check if tags is stored as a single String
                    val tagString = snapshot.getValue(String::class.java)
                    if (tagString != null) {
                        // Process the tag string
                        when {
                            tagString.contains("Sale", ignoreCase = true) -> {
                                discount = 0.25 // 25% discount
                                tagText = "SALE"
                                tagBgColor = R.drawable.sale_badge_shape // Assuming this is red
                            }
                            tagString.contains("Nổi bật", ignoreCase = true) -> {
                                discount = 0.20 // 20% discount
                                tagText = "NỔI BẬT"
                                // You might need to create a different background drawable for featured items
                            }
                            tagString.contains("Mới", ignoreCase = true) -> {
                                discount = 0.15 // 15% discount
                                tagText = "MỚI"
                                // You might need to create a different background drawable for new items
                            }
                        }
                    } else {
                        // If no string value found, try to get it as a map
                        val tagsMap = snapshot.getValue() as? Map<*, *>
                        if (tagsMap != null) {
                            // Check for specific tag keys in the map
                            if (tagsMap.containsKey("Sale")) {
                                discount = 0.25
                                tagText = "SALE"
                                tagBgColor = R.drawable.sale_badge_shape
                            } else if (tagsMap.containsKey("Nổi bật")) {
                                discount = 0.20
                                tagText = "NỔI BẬT"
                            } else if (tagsMap.containsKey("Mới")) {
                                discount = 0.15
                                tagText = "MỚI"
                            }
                        }
                    }
                } catch (e: Exception) {
                    // In case of any exceptions during reading tags, log the error
                    e.printStackTrace()
                }

                // Apply tag styling if there's a tag
                if (tagText.isNotEmpty()) {
                    holder.binding.tagBadge.apply {
                        text = tagText
                        visibility = View.VISIBLE
                        background = ContextCompat.getDrawable(context, tagBgColor)
                    }
                } else {
                    holder.binding.tagBadge.visibility = View.GONE
                }

                // Calculate and format prices
                val originalPrice = menuItem.flowerPrice?.toDoubleOrNull() ?: 0.0

                if (discount > 0) {
                    // Format original price with strikethrough
                    holder.binding.realPrice.apply {
                        text = formatPrice(originalPrice)
                        visibility = View.VISIBLE
                    }

                    // Calculate and format sale price
                    val salePrice = originalPrice * (1 - discount)
                    holder.binding.menusalePrice.text = formatPrice(salePrice)
                } else {
                    // No discount, show only original price
                    holder.binding.realPrice.visibility = View.GONE
                    holder.binding.menusalePrice.text = formatPrice(originalPrice)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error - default to showing original price
                holder.binding.tagBadge.visibility = View.GONE
                holder.binding.realPrice.visibility = View.GONE
                holder.binding.menusalePrice.text = menuItem.flowerPrice ?: "0"
            }
        })
    }

    // Format price in Vietnamese currency format
    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return formatter.format(price)
    }

    inner class PopularViewHolder(val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.menuFlowerName.text = menuItem.flowerName

            // Initially set visible prices (will be updated when tags load)
            binding.menusalePrice.text = menuItem.flowerPrice ?: "0"
            binding.realPrice.visibility = View.GONE
            binding.tagBadge.visibility = View.GONE

            // Load image
            try {
                Glide.with(context)
                    .load(Uri.parse(menuItem.flowerImage))
                    .into(binding.menuImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadProductReviews(productId: String, holder: PopularViewHolder) {
        val reviewsRef = FirebaseDatabase.getInstance().reference.child("reviews").child(productId)

        reviewsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRating = 0.0
                var totalReviews = 0
                var avgRating = 0.0

                // Loop through all reviews and calculate the average rating and total reviews
                for (reviewSnapshot in snapshot.children) {
                    val rating = reviewSnapshot.child("rating").getValue(Double::class.java) ?: 0.0
                    totalRating += rating
                    totalReviews++
                }

                // Calculate average rating
                if (totalReviews > 0) {
                    avgRating = totalRating / totalReviews
                }

                // Update UI with the rating
                holder.binding.ratingBar.rating = avgRating.toFloat()
                holder.binding.ratingText.text = "($totalReviews)"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error - default to empty ratings
                holder.binding.ratingBar.rating = 0f
                holder.binding.ratingText.text = "(0)"
            }
        })
    }
}

// Optional: Use this to add space between items in the RecyclerView
class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        if (position < itemCount - 1) {
            outRect.bottom = verticalSpaceHeight
        } else {
            outRect.bottom = 0 // Don't add space for the last item
        }
    }
}
