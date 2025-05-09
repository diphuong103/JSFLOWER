package com.example.jsflower.Adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SuggestedProductAdapter(
    private val context: Context,
    private val productList: List<MenuItem>
) : RecyclerView.Adapter<SuggestedProductAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.suggestedProductImageView)
        val productName: TextView = itemView.findViewById(R.id.suggestedProductNameTextView)
        val productPrice: TextView = itemView.findViewById(R.id.suggestedProductPriceTextView)
        val addToCartButton: ImageButton = itemView.findViewById(R.id.addToCartImageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_suggested_product, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = productList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        // Load product image
        Glide.with(context).load(Uri.parse(product.flowerImage)).into(holder.productImage)

        holder.productName.text = product.flowerName
        holder.productPrice.text = product.flowerPrice

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", product.flowerName)
                putExtra("MenuItemDescription", product.flowerDescription)
                putExtra("MenuItemIngredient", product.flowerIngredient)
                putExtra("MenuItemPrice", product.flowerPrice)
                putExtra("MenuItemImage", product.flowerImage)
            }
            context.startActivity(intent)
        }

        // Set click listener for add to cart button
        holder.addToCartButton.setOnClickListener {
            addToCart(product)
        }
    }

    private fun addToCart(product: MenuItem) {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }

        // Create cart item from CartItems model
        val cartItem = com.example.jsflower.Model.CartItems(
            product.flowerName,
            product.flowerPrice,
            product.flowerDescription,
            product.flowerImage,
            1
        )

        // Save cart item to Firebase
        database.child("user").child(userId).child("CartItems").push().setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(context, "Thêm sản phẩm vào giỏ hàng thành công <3", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Thêm sản phẩm vào giỏ hàng thất bại -_-", Toast.LENGTH_SHORT).show()
            }
    }
}