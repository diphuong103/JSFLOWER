package com.example.jsflower

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.adaptar.RecentBuyAdapter
import com.example.jsflower.databinding.ActivityRecentOrderItemsBinding

class RecentOrderItems : AppCompatActivity() {

    private lateinit var binding: ActivityRecentOrderItemsBinding

    private lateinit var allFlowerNames: ArrayList<String>
    private lateinit var allFlowerImages: ArrayList<String>
    private lateinit var allFlowerPrices: ArrayList<String>
    private lateinit var allFlowerQuantities: ArrayList<Int>
    private lateinit var allFlowerStatuss: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRecentOrderItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        val recentOrderItems = intent.getSerializableExtra("RecentBuyOrderItem") as? ArrayList<OrderDetails>
        recentOrderItems?.let { orderDetails ->
            if (orderDetails.isNotEmpty()) {
                val recentOrderItem = orderDetails[0]

                allFlowerNames = ArrayList(recentOrderItem.flowerNames)
                allFlowerImages = ArrayList(recentOrderItem.flowerImages)
                allFlowerPrices = ArrayList(recentOrderItem.flowerPrices)
                allFlowerQuantities = ArrayList(recentOrderItem.flowerQuantities)
                allFlowerStatuss = ArrayList(listOf(recentOrderItem.status))


                setAdapter()
            }
        }
    }

    private fun setAdapter() {
        val rv = binding.recentBuyRecyclerView
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = RecentBuyAdapter(this, allFlowerNames, allFlowerImages, allFlowerPrices, allFlowerQuantities, allFlowerStatuss)
        rv.adapter = adapter
    }
}
