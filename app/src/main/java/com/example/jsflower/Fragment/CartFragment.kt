package com.example.jsflower.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.Model.CartItems
import com.example.jsflower.PayOutActivity
import com.example.jsflower.adaptar.CartAdapter
import com.example.jsflower.databinding.FragmentCartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartFragment : Fragment() {
    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter

    private lateinit var flowerNames: MutableList<String>
    private lateinit var flowerPrices: MutableList<String>
    private lateinit var flowerDescriptions: MutableList<String>
    private lateinit var flowerImagesUri: MutableList<String>
    private lateinit var flowerIngredients: MutableList<String>
    private lateinit var quantity: MutableList<Int>

    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""

        getCartItems()

        binding.btnBack.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.proceedButton.setOnClickListener {
            if (::cartAdapter.isInitialized && flowerNames.isNotEmpty()) {
                val flowerQuantities = cartAdapter.getUpdatedItemsQuantities()

                orderNow(
                    flowerNames,
                    flowerPrices,
                    flowerDescriptions,
                    flowerImagesUri,
                    flowerIngredients,
                    flowerQuantities
                )
            } else {
                Toast.makeText(context, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun getCartItems() {
        val flowerRef = database.reference.child("user").child(userId).child("CartItems")

        flowerNames = mutableListOf()
        flowerPrices = mutableListOf()
        flowerDescriptions = mutableListOf()
        flowerImagesUri = mutableListOf()
        flowerIngredients = mutableListOf()
        quantity = mutableListOf()

        flowerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (flowerSnapshot in snapshot.children) {
                    val cartItem = flowerSnapshot.getValue(CartItems::class.java)
                    cartItem?.flowerName?.let { flowerNames.add(it) }
                    cartItem?.flowerPrice?.let { flowerPrices.add(it) }
                    cartItem?.flowerDescription?.let { flowerDescriptions.add(it) }
                    cartItem?.flowerImage?.let { flowerImagesUri.add(it) }
                    cartItem?.flowerIngredient?.let { flowerIngredients.add(it) }
                    cartItem?.flowerQuantity?.let { quantity.add(it) }
                }

                if (flowerNames.isNotEmpty()) {
                    setAdapter()
                } else {
                    // Hiển thị thông báo giỏ hàng trống nếu cần
                    binding.CartRecyclerView.visibility = View.GONE
                    // Tùy chọn: Thêm một TextView để hiển thị thông báo giỏ hàng trống
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Không lấy được dữ liệu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setAdapter() {
        cartAdapter = CartAdapter(
            requireContext(),
            flowerNames,
            flowerPrices,
            flowerDescriptions,
            flowerImagesUri,
            flowerIngredients,
            quantity
        )

        binding.CartRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.CartRecyclerView.adapter = cartAdapter
    }

    private fun orderNow(
        flowerName: MutableList<String>,
        flowerPrice: MutableList<String>,
        flowerDescription: MutableList<String>,
        flowerImage: MutableList<String>,
        flowerIngredients: MutableList<String>,
        flowerQuantities: MutableList<Int>
    ) {
        if (isAdded && context != null) {
            val intent = Intent(requireContext(), PayOutActivity::class.java)
            intent.putExtra("FlowerItemName", ArrayList(flowerName))
            intent.putExtra("FlowerItemPrice", ArrayList(flowerPrice))
            intent.putExtra("FlowerItemImage", ArrayList(flowerImage))
            intent.putExtra("FlowerItemDescription", ArrayList(flowerDescription))
            intent.putExtra("FlowerItemIngredient", ArrayList(flowerIngredients))
            intent.putExtra("FlowerItemQuantities", ArrayList(flowerQuantities))
            startActivity(intent)
        }
    }
}