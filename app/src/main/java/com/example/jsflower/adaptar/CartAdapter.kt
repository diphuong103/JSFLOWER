package com.example.jsflower.adaptar

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
    private val cartQuantity: MutableList<Int>,
    private val cartIngredient: MutableList<String>
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {


    // instance Firebase
    private val auth = FirebaseAuth.getInstance()


    init {
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        val cartItemNumber = cartItems.size

        itemQuantities = IntArray(cartItemNumber) { 1 }
        cartItemsReference = database.reference.child("user").child(userId).child("CartItems")
    }

    companion object {
        private var itemQuantities: IntArray = intArrayOf()
        private lateinit var cartItemsReference: DatabaseReference
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartAdapter.CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartAdapter.CartViewHolder, position: Int) {
        holder.bind(position)
    }


    override fun getItemCount(): Int = cartItems.size


    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                val quality = itemQuantities[position]
                cardFlowerName.text = cartItems[position]
                cartItemPrice.text = cartItemPrices[position]

                Log.d("CartAdapter", "Position: $position")
                Log.d("CartAdapter", "Name: ${cartItems[position]}")
                Log.d("CartAdapter", "Description: ${cartDescription[position]}")
                Log.d("CartAdapter", "Image URI: ${cartImages[position]}")

                val uriString = cartImages[position]
                if (!uriString.isNullOrEmpty()) {
                    Glide.with(binding.root.context)
                        .load(uriString)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.error_image)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.e("Glide", "Load ảnh thất bại: $uriString", e)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.d("Glide", "Tải ảnh thành công: $uriString")
                                return false
                            }
                        })
                        .into(binding.cartImage)
                } else {
                    binding.cartImage.setImageResource(R.drawable.placeholder)
                }

                cartItemQuantity.text = quality.toString()

                minusbutton.setOnClickListener {
                    deceaseQuantity(position)
                }

                plusebutton.setOnClickListener {
                    increaseQuantity(position)
                }

                deleteButton.setOnClickListener {
                    val itemPosition = adapterPosition
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        deleteItem(itemPosition)
                    }
                }
            }
        }

        private fun increaseQuantity(position: Int) {
            if (itemQuantities[position] < 10) {
                itemQuantities[position]++
                binding.cartItemQuantity.text = itemQuantities[position].toString()
            }
        }

        private fun deceaseQuantity(position: Int) {
            if (itemQuantities[position] > 1) {
                itemQuantities[position]--
                binding.cartItemQuantity.text = itemQuantities[position].toString()
            }
        }

        private fun deleteItem(position: Int) {
            // Kiểm tra và tránh race condition
            if (position < 0 || position >= cartItems.size) {
                Log.e("CartAdapter", "Vị trí không hợp lệ: $position, kích thước: ${cartItems.size}")
                return
            }

            val positionRetrieve = position
            getUniqueKetAtposition(positionRetrieve) { uniquetKey ->
                if (uniquetKey != null) {
                    removeItem(position, uniquetKey)
                } else {
                    Log.e("CartAdapter", "Không tìm thấy uniqueKey cho vị trí: $position")
                    Toast.makeText(context, "Không thể xóa mục này", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun removeItem(position: Int, uniquetKey: String) {
        // Kiểm tra lại vị trí hợp lệ trước khi xóa
        if (position < 0 || position >= cartItems.size) {
            Log.e("CartAdapter", "removeItem: Vị trí không hợp lệ: $position, kích thước: ${cartItems.size}")
            return
        }

        cartItemsReference.child(uniquetKey).removeValue().addOnSuccessListener {
            try {
                // Xóa an toàn từ tất cả các danh sách
                synchronized(this) {
                    if (position < cartItems.size) cartItems.removeAt(position)
                    if (position < cartImages.size) cartImages.removeAt(position)
                    if (position < cartDescription.size) cartDescription.removeAt(position)
                    if (position < cartQuantity.size) cartQuantity.removeAt(position)
                    if (position < cartItemPrices.size) cartItemPrices.removeAt(position)
                    if (position < cartIngredient.size) cartIngredient.removeAt(position)

                    // Cập nhật itemQuantities
                    val newItemQuantities = IntArray(itemQuantities.size - 1)
                    var newIndex = 0
                    for (i in itemQuantities.indices) {
                        if (i != position) {
                            if (newIndex < newItemQuantities.size) {
                                newItemQuantities[newIndex] = itemQuantities[i]
                                newIndex++
                            }
                        }
                    }
                    itemQuantities = newItemQuantities

                    // Thông báo thay đổi
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, cartItems.size)
                    Toast.makeText(context, "Xóa thành công", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CartAdapter", "Lỗi khi xóa mục: ${e.message}", e)
                Toast.makeText(context, "Lỗi khi xóa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("CartAdapter", "Xóa dữ liệu Firebase thất bại: ${e.message}", e)
            Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUniqueKetAtposition(positionRetrieve: Int, onComplete: (String?) -> Unit) {
        if (positionRetrieve < 0 || positionRetrieve >= cartItems.size) {
            Log.e("CartAdapter", "getUniqueKetAtposition: Vị trí không hợp lệ: $positionRetrieve")
            onComplete(null)
            return
        }

        cartItemsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var uniqueKey: String? = null
                val keys = snapshot.children.mapNotNull { it.key }

                if (positionRetrieve < keys.size) {
                    uniqueKey = keys.elementAtOrNull(positionRetrieve)
                }

                onComplete(uniqueKey)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartAdapter", "Database error: ${error.message}")
                onComplete(null)
            }
        })
    }
}