package com.example.jsflower.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.CongratsBottomSheet
import com.example.jsflower.Model.CartItems
import com.example.jsflower.PayOutActivity
import com.example.jsflower.R
import com.example.jsflower.adaptar.CartAdapter
import com.example.jsflower.databinding.FragmentCartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class CartFragment : Fragment() {
    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var flowerNames: MutableList<String>
    private lateinit var flowerPrices: MutableList<String>
    private lateinit var flowerDescriptions: MutableList<String>
    private lateinit var flowerImagesUri: MutableList<String>
    private lateinit var flowerIngredients: MutableList<String>
    private lateinit var quantity: MutableList<Int>
    private lateinit var cartAdapter: CartAdapter

    private lateinit var userId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()


        getCartItems()


        binding.proceedButton.setOnClickListener {
            val intent = Intent(requireContext(), PayOutActivity::class.java)
            startActivity(intent)
        }



        return binding.root
    }

    private fun getCartItems() {
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""
        val flowerRef: DatabaseReference =
            database.reference.child("user").child(userId).child("CartItems")

        // Khởi tạo các danh sách
        flowerNames = mutableListOf()
        flowerPrices = mutableListOf()
        flowerDescriptions = mutableListOf()
        flowerIngredients = mutableListOf()
        flowerImagesUri = mutableListOf()
        quantity = mutableListOf()


        flowerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (flowerSnapshot in snapshot.children) {
                    val cartItems = flowerSnapshot.getValue(CartItems::class.java)

                    cartItems?.flowerName?.let {
                        flowerNames.add(it)
                    }

                    cartItems?.flowerPrice?.let {
                        flowerPrices.add(it)
                    }

                    cartItems?.flowerDescription?.let {
                        flowerDescriptions.add(it)
                    }
                    cartItems?.flowerImage?.let {
                        flowerImagesUri.add(it)

                    }

                    cartItems?.flowerQuantity?.let {
                        quantity.add(it)
                    }

                    cartItems?.flowerIngredient?.let {
                        flowerIngredients.add(it)
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Không lấy được dữ liệu", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun setAdapter() {
        val adapter = CartAdapter(
            requireContext(),
            flowerNames,
            flowerPrices,
            flowerDescriptions,
            flowerImagesUri,
            quantity,
            flowerIngredients
        )
        binding.CartRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.CartRecyclerView.adapter = adapter
    }


    companion object {
    }

}