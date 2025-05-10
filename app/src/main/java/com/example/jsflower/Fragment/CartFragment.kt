package com.example.jsflower.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.PayOutActivity
import com.example.jsflower.adaptar.CartAdapter
import com.example.jsflower.databinding.FragmentCartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartFragment : Fragment(), CartAdapter.CartItemActionListener {

    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter
    private lateinit var emptyCartTextView: TextView

    private lateinit var userId: String

    private var flowerNames = mutableListOf<String>()
    private var flowerPrices = mutableListOf<String>()
    private var flowerDescriptions = mutableListOf<String>()
    private var flowerImagesUri = mutableListOf<String>()
    private var flowerIngredients = mutableListOf<String>()
    private var quantity = mutableListOf<Int>()
    private var itemIds = mutableListOf<String>()

    private var flowerKeys = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""

        // Gán TextView giỏ hàng trống
        emptyCartTextView = binding.emptyCartMessage

        // Setup RecyclerView
        binding.CartRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.btnBack.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.proceedButton.setOnClickListener {
            if (::cartAdapter.isInitialized && flowerNames.isNotEmpty()) {
                val updatedQuantities = cartAdapter.getUpdatedItemsQuantities()
                orderNow(
                    flowerNames,
                    flowerPrices,
                    flowerDescriptions,
                    flowerImagesUri,
                    flowerIngredients,
                    updatedQuantities,
                    flowerKeys  // Truyền flowerKeys thay vì itemIds
                )
            } else {
                Toast.makeText(context, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        refreshCartData()
    }

    private fun refreshCartData() {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để xem giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }
        getCartItems()
    }

    private fun getCartItems() {
        if (userId.isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            updateCartVisibility()
            return
        }


        val flowerRef = database.reference.child("user").child(userId).child("CartItems")

        // Clear old data
        flowerNames.clear()
        flowerPrices.clear()
        flowerDescriptions.clear()
        flowerImagesUri.clear()
        flowerIngredients.clear()
        quantity.clear()
        itemIds.clear()

        flowerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    updateCartVisibility()
                    return
                }

                for (itemSnapshot in snapshot.children) {
                    val cartItem = itemSnapshot.getValue(com.example.jsflower.Model.CartItems::class.java)
                    val itemId = itemSnapshot.key ?: continue

                    if (cartItem != null &&
                        !cartItem.flowerName.isNullOrEmpty() &&
                        !cartItem.flowerPrice.isNullOrEmpty() &&
                        !cartItem.flowerImage.isNullOrEmpty()) {

                        flowerNames.add(cartItem.flowerName!!)
                        flowerPrices.add(cartItem.flowerPrice!!)
                        flowerDescriptions.add(cartItem.flowerDescription ?: "")
                        flowerImagesUri.add(cartItem.flowerImage!!)
                        flowerIngredients.add(cartItem.flowerIngredient ?: "")
                        quantity.add(cartItem.flowerQuantity ?: 1)

                        // Lưu cartItem key (để xóa item) và flowerKey (để truy xuất sản phẩm gốc)
                        itemIds.add(itemId)
                        // Đảm bảo lưu flowerKey từ CartItems
                        flowerKeys.add(cartItem.flowerKey ?: "")
                    }
                }

                updateCartVisibility()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Không lấy được dữ liệu", Toast.LENGTH_SHORT).show()
                updateCartVisibility()
            }
        })
    }

    private fun updateCartVisibility() {
        if (flowerNames.isNotEmpty()) {
            setAdapter()
            binding.CartRecyclerView.visibility = View.VISIBLE
            emptyCartTextView.visibility = View.GONE
            binding.proceedButton.isEnabled = true
        } else {
            binding.CartRecyclerView.visibility = View.GONE
            emptyCartTextView.visibility = View.VISIBLE
            emptyCartTextView.text = "Giỏ hàng của bạn đang trống"
            binding.proceedButton.isEnabled = false
        }
    }

    private fun setAdapter() {
        cartAdapter = CartAdapter(
            requireContext(),
            flowerNames,
            flowerPrices,
            flowerDescriptions,
            flowerImagesUri,
            flowerIngredients,
            quantity,
            this
        )
        binding.CartRecyclerView.adapter = cartAdapter
    }

    private fun orderNow(
        flowerName: MutableList<String>,
        flowerPrice: MutableList<String>,
        flowerDescription: MutableList<String>,
        flowerImage: MutableList<String>,
        flowerIngredients: MutableList<String>,
        flowerQuantities: MutableList<Int>,
        flowerKeys: MutableList<String>  // Sử dụng flowerKeys thay vì flowerKey
    ) {
        val intent = Intent(requireContext(), PayOutActivity::class.java).apply {
            putExtra("FlowerItemName", ArrayList(flowerName))
            putExtra("FlowerItemPrice", ArrayList(flowerPrice))
            putExtra("FlowerItemImage", ArrayList(flowerImage))
            putExtra("FlowerItemDescription", ArrayList(flowerDescription))
            putExtra("FlowerItemIngredient", ArrayList(flowerIngredients))
            putExtra("FlowerItemQuantities", ArrayList(flowerQuantities))
            putExtra("FlowerItemKey", ArrayList(flowerKeys))  // Truyền đúng flowerKeys
        }
        startActivity(intent)
    }


    override fun onCartItemDelete(position: Int) {
        if (position < 0 || position >= itemIds.size) {
            Toast.makeText(context, "Lỗi khi xóa sản phẩm", Toast.LENGTH_SHORT).show()
            return
        }

        val itemId = itemIds[position]
        val itemName = flowerNames[position]

        val flowerRef = database.reference
            .child("user")
            .child(userId)
            .child("CartItems")
            .child(itemId)

        flowerRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Đã xóa $itemName", Toast.LENGTH_SHORT).show()
                refreshCartData()
            } else {
                Toast.makeText(context, "Không thể xóa sản phẩm", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onQuantityChanged(position: Int, newQuantity: Int) {
        if (position < 0 || position >= itemIds.size) return

        val itemId = itemIds[position]
        val flowerRef = database.reference
            .child("user")
            .child(userId)
            .child("CartItems")
            .child(itemId)
            .child("flowerQuantity")

        flowerRef.setValue(newQuantity).addOnCompleteListener { task ->
            if (task.isSuccessful && position < quantity.size) {
                quantity[position] = newQuantity
            } else {
                Toast.makeText(context, "Không thể cập nhật số lượng", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
