package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val context: Context,
    private val onAddToCartClick: ((MenuItem) -> Unit)? = null
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.bind(menuItem)

        // Load product tags and apply discounts
        loadProductTagsAndApplyDiscount(menuItem, holder)

        // Load reviews for product
        loadProductReviews(menuItem.key, holder)
    }

    override fun getItemCount(): Int = menuItems.size

    private fun loadProductTagsAndApplyDiscount(menuItem: MenuItem, holder: MenuViewHolder) {
        val tagsRef = FirebaseDatabase.getInstance().reference.child("list").child(menuItem.key).child("tags")

        tagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Default: No discount
                var discount = 0.0
                var tagText = ""
                var tagBgColor = R.drawable.sale_badge_shape

                try {
                    val tagString = snapshot.getValue(String::class.java)
                    if (tagString != null) {
                        when {
                            tagString.contains("Sale", ignoreCase = true) -> {
                                discount = 0.25
                                tagText = "SALE"
                                tagBgColor = R.drawable.sale_badge_shape
                            }
                            tagString.contains("Nổi bật", ignoreCase = true) -> {
                                discount = 0.20
                                tagText = "NỔI BẬT"
                            }
                            tagString.contains("Mới", ignoreCase = true) -> {
                                discount = 0.15
                                tagText = "MỚI"
                            }
                        }
                    } else {
                        val tagsMap = snapshot.getValue() as? Map<*, *>
                        if (tagsMap != null) {
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
                    e.printStackTrace()
                }

                if (tagText.isNotEmpty()) {
                    holder.binding.tagBadge.apply {
                        text = tagText
                        visibility = View.VISIBLE
                        background = ContextCompat.getDrawable(context, tagBgColor)
                    }
                } else {
                    holder.binding.tagBadge.visibility = View.GONE
                }

                val originalPrice = menuItem.flowerPrice?.toDoubleOrNull() ?: 0.0
                var finalPrice = originalPrice

                if (discount > 0) {
                    holder.binding.realPrice.apply {
                        text = formatPrice(originalPrice)
                        visibility = View.VISIBLE
                        paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    }

                    finalPrice = originalPrice * (1 - discount)
                    holder.binding.menusalePrice.text = formatPrice(finalPrice)
                } else {
                    holder.binding.realPrice.visibility = View.GONE
                    holder.binding.menusalePrice.text = formatPrice(originalPrice)
                }

                val finalPriceString = formatPrice(finalPrice)

                setupItemClickListener(holder, menuItem, finalPriceString)
                setupAddToCartButton(holder, menuItem, finalPriceString)
            }

            override fun onCancelled(error: DatabaseError) {
                holder.binding.tagBadge.visibility = View.GONE
                holder.binding.realPrice.visibility = View.GONE
                holder.binding.menusalePrice.text = menuItem.flowerPrice ?: "0"

                setupItemClickListener(holder, menuItem, menuItem.flowerPrice ?: "0")
                setupAddToCartButton(holder, menuItem, menuItem.flowerPrice ?: "0")
            }
        })
    }

    private fun setupItemClickListener(holder: MenuViewHolder, menuItem: MenuItem, finalPrice: String) {
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

    private fun setupAddToCartButton(holder: MenuViewHolder, menuItem: MenuItem, finalPrice: String) {
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
            onAddToCartClick?.invoke(updatedMenuItem)
            Toast.makeText(context, "Đã thêm sản phẩm vào giỏ hàng thành công <3", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return formatter.format(price)
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

    inner class MenuViewHolder(val binding: MenuItemBinding) : RecyclerView.ViewHolder(binding.root) {
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
}