package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.Model.CartItems
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.R
import com.example.jsflower.databinding.MenuItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class PopularAdapter(
    private val popularItems: MutableList<MenuItem>,
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

        // Load product tags and get discount price from Firebase
        loadProductTagsAndDiscountPrice(menuItem, holder, position)

        // Load reviews for product
        loadProductReviews(menuItem.key, holder)
    }

    override fun getItemCount(): Int = popularItems.size

    private fun loadProductTagsAndDiscountPrice(menuItem: MenuItem, holder: PopularViewHolder, position: Int) {
        // Get reference to the product in Firebase
        val productRef = FirebaseDatabase.getInstance().reference.child("list").child(menuItem.key)

        // Add listener to get tags and discountPrice
        productRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get product tags to display badge
                var tagText = ""
                var tagBgColor = R.drawable.sale_badge_shape

                // Handle tags to display appropriate badge
                val tagsSnapshot = snapshot.child("tags")
                try {
                    val tagString = tagsSnapshot.getValue(String::class.java)
                    if (tagString != null) {
                        when {
                            tagString.contains("Sale", ignoreCase = true) -> {
                                tagText = "SALE"
                                tagBgColor = R.drawable.sale_badge_shape
                            }
                            tagString.contains("Nổi bật", ignoreCase = true) -> {
                                tagText = "NỔI BẬT"
                            }
                            tagString.contains("Mới", ignoreCase = true) -> {
                                tagText = "MỚI"
                            }
                        }
                    } else {
                        val tagsMap = tagsSnapshot.getValue() as? Map<*, *>
                        if (tagsMap != null) {
                            if (tagsMap.containsKey("Sale")) {
                                tagText = "SALE"
                                tagBgColor = R.drawable.sale_badge_shape
                            } else if (tagsMap.containsKey("Nổi bật")) {
                                tagText = "NỔI BẬT"
                            } else if (tagsMap.containsKey("Mới")) {
                                tagText = "MỚI"
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Display tag badge if available
                if (tagText.isNotEmpty()) {
                    holder.binding.tagBadge.apply {
                        text = tagText
                        visibility = View.VISIBLE
                        background = ContextCompat.getDrawable(context, tagBgColor)
                    }
                } else {
                    holder.binding.tagBadge.visibility = View.GONE
                }

                // Get discount price directly from Firebase
                val discountPrice = snapshot.child("discountPrice").getValue(String::class.java)
                val originalPrice = menuItem.flowerPrice

                // Update UI based on discount price
                if (discountPrice != null && discountPrice != originalPrice && originalPrice != null) {
                    // Show both prices if there's a discount
                    holder.binding.realPrice.apply {
                        text = originalPrice
                        visibility = View.VISIBLE
                        paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    holder.binding.menusalePrice.text = discountPrice
                    menuItem.discountedPrice = discountPrice
                } else {
                    // Show only the original price if no discount
                    holder.binding.realPrice.visibility = View.GONE
                    holder.binding.menusalePrice.text = originalPrice ?: "0"
                    menuItem.discountedPrice = originalPrice
                }

                // Update item in list
                popularItems[position] = menuItem

                // Setup click listeners
                setupItemClickListener(holder, menuItem, menuItem.discountedPrice ?: originalPrice ?: "0")
                setupAddToCartButton(holder, menuItem, menuItem.discountedPrice ?: originalPrice ?: "0")
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error - use original price
                holder.binding.tagBadge.visibility = View.GONE
                holder.binding.realPrice.visibility = View.GONE
                holder.binding.menusalePrice.text = menuItem.flowerPrice ?: "0"

                setupItemClickListener(holder, menuItem, menuItem.flowerPrice ?: "0")
                setupAddToCartButton(holder, menuItem, menuItem.flowerPrice ?: "0")
            }
        })
    }

    private fun setupItemClickListener(holder: PopularViewHolder, menuItem: MenuItem, finalPrice: String) {
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.flowerName)
                putExtra("MenuItemPrice", finalPrice)
                putExtra("MenuItemImage", menuItem.flowerImage)
                putExtra("MenuItemDescription", menuItem.flowerDescription)
                putExtra("MenuItemIngredient", menuItem.flowerIngredient)
                putExtra("MenuItemKey", menuItem.key)
                putExtra("TAG", menuItem.tags)
            }
            context.startActivity(intent)
        }
    }

    private fun setupAddToCartButton(holder: PopularViewHolder, menuItem: MenuItem, finalPrice: String) {
        holder.binding.menuAddToCart.setOnClickListener {
            val updatedMenuItem = MenuItem(
                menuItem.flowerName,
                menuItem.flowerPrice,
                menuItem.flowerDescription,
                menuItem.flowerImage,
                menuItem.flowerIngredient,
                menuItem.key,
                menuItem.tags,
                finalPrice
            )
            onAddToCart(updatedMenuItem)
        }
    }

    inner class PopularViewHolder(val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.menuFlowerName.text = menuItem.flowerName
            binding.menusalePrice.text = menuItem.discountedPrice ?: menuItem.flowerPrice ?: "0"
            binding.realPrice.text = menuItem.flowerPrice ?: ""
            binding.tagBadge.visibility = View.GONE

            if (menuItem.discountedPrice != null && menuItem.discountedPrice != menuItem.flowerPrice) {
                binding.realPrice.visibility = View.VISIBLE
                binding.realPrice.paintFlags = binding.realPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.realPrice.visibility = View.GONE
            }

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

                for (reviewSnapshot in snapshot.children) {
                    val rating = reviewSnapshot.child("rating").getValue(Double::class.java) ?: 0.0
                    totalRating += rating
                    totalReviews++
                }

                if (totalReviews > 0) {
                    avgRating = totalRating / totalReviews
                }

                holder.binding.ratingBar.rating = avgRating.toFloat()
                holder.binding.ratingText.text = "($totalReviews)"
            }

            override fun onCancelled(error: DatabaseError) {
                holder.binding.ratingBar.rating = 0f
                holder.binding.ratingText.text = "(0)"
            }
        })
    }
}