package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
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
import com.google.firebase.database.ValueEventListener

class MenuAdapter(
    private val menuItems: MutableList<MenuItem>,
    private val context: Context,
    private val onAddToCart: ((MenuItem) -> Unit)? = null,
    private val listener: MenuAdapterListener? = null
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    // Interface for handling other existing callbacks
    interface MenuAdapterListener {
        fun onItemClick(menuItem: MenuItem)
        fun onAddToCartClick(menuItem: MenuItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.bind(menuItem)

        // Load product tags and discount price
        loadProductTagsAndDiscountPrice(menuItem, holder, position)

        // Load reviews for product
        loadProductReviews(menuItem.key, holder)
    }

    override fun getItemCount(): Int = menuItems.size

    private fun loadProductTagsAndDiscountPrice(menuItem: MenuItem, holder: MenuViewHolder, position: Int) {
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

                // Get discount price directly from Firebase and ensure non-null original price
                val discountPrice = snapshot.child("discountPrice").getValue(String::class.java)
                val originalPrice = menuItem.flowerPrice ?: "0"

                // Create a new MenuItem with updated pricing
                val updatedMenuItem = MenuItem(
                    menuItem.flowerName,
                    originalPrice, // Always store the original price
                    menuItem.flowerDescription,
                    menuItem.flowerImage,
                    menuItem.flowerIngredient,
                    menuItem.key,
                    menuItem.tags,
                    discountPrice // Store discount price if available
                )

                // Update UI based on whether there's a discount
                if (discountPrice != null && discountPrice != originalPrice) {
                    // Show both prices if there's a discount
                    holder.binding.realPrice.apply {
                        text = originalPrice
                        visibility = View.VISIBLE
                        paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    holder.binding.menusalePrice.text = discountPrice
                } else {
                    // Show only the original price if no discount
                    holder.binding.realPrice.visibility = View.GONE
                    holder.binding.menusalePrice.text = originalPrice
                }

                // Update item in list with updated pricing
                menuItems[position] = updatedMenuItem

                // Setup click listeners with the updated menuItem
                setupItemClickListener(holder, updatedMenuItem)
                setupAddToCartButton(holder, updatedMenuItem)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error - use original price
                holder.binding.tagBadge.visibility = View.GONE
                holder.binding.realPrice.visibility = View.GONE

                val originalPrice = menuItem.flowerPrice ?: "0"
                holder.binding.menusalePrice.text = originalPrice

                setupItemClickListener(holder, menuItem)
                setupAddToCartButton(holder, menuItem)
            }
        })
    }

    private fun setupItemClickListener(holder: MenuViewHolder, menuItem: MenuItem) {
        holder.itemView.setOnClickListener {
            // Try to use the interface method first
            if (listener != null) {
                listener.onItemClick(menuItem)
            } else {
                // Fall back to the original implementation
                val discountPrice = menuItem.discountedPrice
                val originalPrice = menuItem.flowerPrice ?: "0"

                // Make sure we're not passing null for originalPrice
                val safeOriginalPrice = if (originalPrice.isNullOrEmpty()) "0" else originalPrice

                // Always pass the current displayed price as MenuItemPrice (either discounted or original)
                // Always pass the original price as MenuItemOriginalPrice
                val intent = Intent(context, DetailsActivity::class.java).apply {
                    putExtra("MenuItemName", menuItem.flowerName)
                    putExtra("MenuItemPrice", discountPrice ?: safeOriginalPrice)
                    putExtra("MenuItemOriginalPrice", safeOriginalPrice) // Make sure it's never null
                    putExtra("MenuItemImage", menuItem.flowerImage)
                    putExtra("MenuItemDescription", menuItem.flowerDescription)
                    putExtra("MenuItemIngredient", menuItem.flowerIngredient)
                    putExtra("MenuItemKey", menuItem.key)
                    putExtra("TAG", menuItem.tags)
                }

                // Log the data being sent to DetailsActivity for debugging
                println("DEBUG: Sending data to DetailsActivity")
                println("DEBUG: MenuItemName = ${menuItem.flowerName}")
                println("DEBUG: MenuItemPrice = ${discountPrice ?: safeOriginalPrice}")
                println("DEBUG: MenuItemOriginalPrice = $safeOriginalPrice")
                println("DEBUG: MenuItemKey = ${menuItem.key}")

                context.startActivity(intent)
            }
        }
    }

    private fun setupAddToCartButton(holder: MenuViewHolder, menuItem: MenuItem) {
        holder.binding.menuAddToCart.setOnClickListener {
            // Try to use the onAddToCart callback first (for new implementation)
            if (onAddToCart != null) {
                onAddToCart.invoke(menuItem)
            }
            // Then try to use the interface method (for existing implementations)
            else if (listener != null) {
                listener.onAddToCartClick(menuItem)
            }
        }
    }

    inner class MenuViewHolder(val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.menuFlowerName.text = menuItem.flowerName

            // Always show the currently applicable price (discounted or original)
            val priceToShow = menuItem.discountedPrice ?: menuItem.flowerPrice ?: "0"
            binding.menusalePrice.text = priceToShow

            // Original price is only shown when there is a discount
            val originalPrice = menuItem.flowerPrice ?: ""
            binding.realPrice.text = originalPrice

            if (menuItem.discountedPrice != null && menuItem.discountedPrice != menuItem.flowerPrice) {
                binding.realPrice.visibility = View.VISIBLE
                binding.realPrice.paintFlags = binding.realPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.realPrice.visibility = View.GONE
            }

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

    private fun loadProductReviews(productId: String, holder: MenuViewHolder) {
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