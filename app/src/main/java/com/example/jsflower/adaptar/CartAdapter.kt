package com.example.jsflower.adaptar

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.R
import com.example.jsflower.databinding.CartItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<String>,
    private val cartItemPrices: MutableList<String>,
    private var cartDescription: MutableList<String>,
    private var cartImages: MutableList<String>,
    private val cartIngredient: MutableList<String>,
    private val initialQuantities: MutableList<Int>
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private var itemQuantities: MutableList<Int> = initialQuantities
    private lateinit var cartItemsReference: DatabaseReference

    init {
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        cartItemsReference = database.reference.child("user").child(userId).child("CartItems")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = cartItems.size

    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                cardFlowerName.text = cartItems[position]
                cartItemPrice.text = cartItemPrices[position]
                cartItemQuantity.text = itemQuantities[position].toString()

                // Load ảnh
                Glide.with(binding.root.context)
                    .load(cartImages[position])
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error_image)
                    .into(binding.cartImage)

                // Nút tăng
                plusebutton.setOnClickListener {
                    if (itemQuantities[position] < 10) {
                        itemQuantities[position]++
                        cartItemQuantity.text = itemQuantities[position].toString()
                    }
                }

                // Nút giảm
                minusbutton.setOnClickListener {
                    if (itemQuantities[position] > 1) {
                        itemQuantities[position]--
                        cartItemQuantity.text = itemQuantities[position].toString()
                    }
                }

                // Xoá
                deleteButton.setOnClickListener {
                    val itemPosition = adapterPosition
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        deleteItem(itemPosition)
                    }
                }
            }
        }
    }

    private fun deleteItem(position: Int) {
        if (position < 0 || position >= cartItems.size) return

        // Lưu lại vị trí hiện tại và đảm bảo nó còn hợp lệ trong danh sách
        val currentPosition = position

        getUniqueKetAtposition(currentPosition) { uniqueKey ->
            if (uniqueKey != null) {
                cartItemsReference.child(uniqueKey).removeValue().addOnSuccessListener {
                    try {
                        // Kiểm tra lại vị trí có còn hợp lệ không trước khi xóa
                        if (currentPosition < cartItems.size) {
                            cartItems.removeAt(currentPosition)
                        }
                        if (currentPosition < cartItemPrices.size) {
                            cartItemPrices.removeAt(currentPosition)
                        }
                        if (currentPosition < cartDescription.size) {
                            cartDescription.removeAt(currentPosition)
                        }
                        if (currentPosition < cartImages.size) {
                            cartImages.removeAt(currentPosition)
                        }
                        if (currentPosition < cartIngredient.size) {
                            cartIngredient.removeAt(currentPosition)
                        }
                        if (currentPosition < itemQuantities.size) {
                            itemQuantities.removeAt(currentPosition)
                        }

                        notifyItemRemoved(currentPosition)
                        notifyItemRangeChanged(currentPosition, cartItems.size)
                        Toast.makeText(context, "Xóa thành công", Toast.LENGTH_SHORT).show()
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e("CartAdapter", "Lỗi khi xóa phần tử: ${e.message}")
                        // Cập nhật lại toàn bộ adapter
                        notifyDataSetChanged()
                        Toast.makeText(context, "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getUniqueKetAtposition(position: Int, onComplete: (String?) -> Unit) {
        cartItemsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Kiểm tra xem snapshot có tồn tại không
                    if (snapshot.exists() && snapshot.childrenCount > 0) {
                        val children = snapshot.children.toList()
                        // Kiểm tra xem position có hợp lệ không
                        val key = if (position < children.size) {
                            children[position].key
                        } else {
                            Log.e("CartAdapter", "Position $position ngoài phạm vi (size: ${children.size})")
                            null
                        }
                        onComplete(key)
                    } else {
                        Log.e("CartAdapter", "Không có dữ liệu trong giỏ hàng")
                        onComplete(null)
                    }
                } catch (e: Exception) {
                    Log.e("CartAdapter", "Lỗi khi lấy key: ${e.message}")
                    onComplete(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartAdapter", "Database error: ${error.message}")
                onComplete(null)
            }
        })

            fun onCancelled(error: DatabaseError) {
                onComplete(null)
            }
    }

    fun getUpdatedItemsQuantities(): MutableList<Int> {
        return itemQuantities.toMutableList()
    }
}