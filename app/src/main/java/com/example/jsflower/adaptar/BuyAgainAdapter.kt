package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.databinding.BuyAgainItemBinding

class BuyAgainAdapter(
    private val flowerNameList: List<String>,
    private val flowerPriceList: List<String>,
    private val flowerImageList: List<String>,
    private val context: Context,
    private val orderStatusList: List<String> // Danh sách trạng thái đơn hàng
) : RecyclerView.Adapter<BuyAgainAdapter.ViewHolder>() {

    class ViewHolder(val binding: BuyAgainItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = flowerNameList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flowerName = flowerNameList[position]
        val flowerPrice = flowerPriceList[position]
        val flowerImage = flowerImageList[position]

        holder.binding.buyAgainFlowerName.text = flowerName
        holder.binding.buyAgainFlowerPrice.text = flowerPrice

        // Set click listener for the entire item view
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java)
            intent.putExtra("MenuItemName", flowerName)
            // Pass additional data that DetailsActivity might need
            intent.putExtra("MenuItemPrice", flowerPrice)
            intent.putExtra("MenuItemImage", flowerImage)
            context.startActivity(intent)
        }

        // Load image safely - check if the image URL is not null or empty
        if (!flowerImage.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(flowerImage)
                Glide.with(context).load(uri).into(holder.binding.buyAgainFlowerImage)
            } catch (e: Exception) {
                // Log error or handle null URI case
                // You might want to load a placeholder image here
            }
        }

        // Hiển thị trạng thái đơn hàng
        val status = orderStatusList.getOrNull(position) ?: ""
        when (status) {
            "delivered" -> {
                holder.binding.buyAgainStatus.text = "Đã giao thành công"
                holder.binding.buyAgainStatus.setTextColor(Color.GREEN)
                holder.binding.buyAgainStatus.visibility = View.VISIBLE
                holder.binding.buyAgainButton.visibility = View.VISIBLE
            }
            "canceled" -> {
                holder.binding.buyAgainStatus.text = "Đã hủy"
                holder.binding.buyAgainStatus.setTextColor(Color.RED)
                holder.binding.buyAgainStatus.visibility = View.VISIBLE
                holder.binding.buyAgainButton.visibility = View.GONE // Ẩn nút mua lại nếu hủy
            }
            else -> {
                holder.binding.buyAgainStatus.visibility = View.GONE
                holder.binding.buyAgainButton.visibility = View.VISIBLE
            }
        }

        holder.binding.buyAgainButton.setOnClickListener {
            // Implement buy again functionality
            val itemName = flowerNameList[position]
            addToCart(itemName, flowerPrice, flowerImage)
        }
    }

    private fun addToCart(flowerName: String, flowerPrice: String, flowerImage: String) {
        // This method replaces the onBuyAgainClick callback
        // Implement the "Buy Again" functionality directly in the adapter
        try {
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = firebaseAuth.currentUser?.uid ?: return
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()

            val cartItem = com.example.jsflower.Model.CartItems(
                flowerName = flowerName,
                flowerPrice = flowerPrice,
                flowerImage = flowerImage,
                flowerQuantity = 1,
                flowerDescription = "",
                flowerIngredient = ""
            )

            val cartItemRef = database.getReference("users/$userId/CartItems")
            cartItemRef.push().setValue(cartItem)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(context, "Đã thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    android.widget.Toast.makeText(context, "Không thể thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Lỗi: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}