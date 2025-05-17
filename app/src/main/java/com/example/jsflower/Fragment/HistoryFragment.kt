package com.example.jsflower.Fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.Model.CartItems
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.R
import com.example.jsflower.RecentOrderItems
import com.example.jsflower.adaptar.BuyAgainAdapter
import com.example.jsflower.adaptar.RecentOrderAdapter
import com.example.jsflower.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.Serializable

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private lateinit var recentOrderAdapter: RecentOrderAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var listOfOrderItem: MutableList<OrderDetails> = mutableListOf()

    private lateinit var buyHistoryListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""

        setupRecyclerViews()
        setupBuyHistoryListener()

        return binding.root
    }

    private fun setupRecyclerViews() {
        binding.recentOrdersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.buyAgainRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupBuyHistoryListener() {
        val buyItemRef = database.reference.child("users").child(userId).child("BuyHistory")
        val sortingQuery = buyItemRef.orderByChild("currentTime")

        buyHistoryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfOrderItem.clear()
                for (buySnapshot in snapshot.children) {
                    val buyHistoryItem = buySnapshot.getValue(OrderDetails::class.java)
                    buyHistoryItem?.let { listOfOrderItem.add(it) }
                }

                listOfOrderItem.reverse() // Newest first

                if (listOfOrderItem.isNotEmpty()) {
                    setRecentOrdersRecyclerView()
                    setPreviousBuyItemsRecyclerView()
                } else {
                    binding.textViewRecentOrders.visibility = View.GONE
                    binding.textViewHistory.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Không thể tải lịch sử mua hàng", Toast.LENGTH_SHORT).show()
            }
        }

        sortingQuery.addValueEventListener(buyHistoryListener)
    }

    private fun setRecentOrdersRecyclerView() {
        binding.textViewRecentOrders.visibility = View.VISIBLE

        val recentOrders = listOfOrderItem.filter {
            it.status != "canceled" && it.status != "delivered"
        }.take(3)

        if (recentOrders.isEmpty()) {
            binding.textViewRecentOrders.visibility = View.GONE
            binding.recentOrdersRecyclerView.visibility = View.GONE
            return
        }

        binding.recentOrdersRecyclerView.visibility = View.VISIBLE

        recentOrderAdapter = RecentOrderAdapter(
            recentOrders,
            requireContext(),
            onViewOrderClick = { orderDetails ->
                val intent = Intent(requireContext(), RecentOrderItems::class.java)
                intent.putExtra("RecentBuyOrderItem", ArrayList(listOf(orderDetails)) as Serializable)
                startActivity(intent)
            },
            onCancelOrderClick = { orderDetails ->
                cancelOrder(orderDetails)
            },
            onReceivedClick = { orderDetails ->
                updateOrderReceived(orderDetails)
            }
        )

        binding.recentOrdersRecyclerView.adapter = recentOrderAdapter
    }

    private fun updateOrderReceived(orderDetails: OrderDetails) {
        val itemPushKey = orderDetails.itemPushKey ?: return
        val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey)
        val historyRef = database.reference.child("users").child(userId).child("BuyHistory").child(itemPushKey)

        val updates = mapOf(
            "paymentReceived" to true,
            "status" to "delivered"
        )

        completeOrderRef.updateChildren(updates)
            .addOnSuccessListener {
                historyRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Đã xác nhận nhận hàng", Toast.LENGTH_SHORT).show()
                        setupBuyHistoryListener()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Không thể cập nhật lịch sử", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelOrder(orderDetails: OrderDetails) {
        val itemPushKey = orderDetails.itemPushKey ?: return

        val orderRef = database.reference.child("OrderDetails").child(itemPushKey)
        val historyRef = database.reference.child("users").child(userId).child("BuyHistory").child(itemPushKey)
        val cancelRef = database.reference.child("CanceledOrder").child(itemPushKey)

        val updates = mapOf(
            "status" to "canceled"
        )

        cancelRef.setValue(orderDetails)
            .addOnSuccessListener {
                orderRef.removeValue()
                historyRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đơn hàng đã được hủy và lưu vào lịch sử", Toast.LENGTH_SHORT).show()
                        setupBuyHistoryListener()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Hủy đơn thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setPreviousBuyItemsRecyclerView() {
        binding.textViewHistory.visibility = View.VISIBLE

        val completedOrders = listOfOrderItem.filter {
            it.status == "delivered" || it.status == "canceled"
        }

        if (completedOrders.isEmpty()) {
            binding.textViewHistory.visibility = View.GONE
            binding.buyAgainRecyclerView.visibility = View.GONE
            return
        }

        binding.buyAgainRecyclerView.visibility = View.VISIBLE

        val buyAgainFlowerName = mutableListOf<String>()
        val buyAgainFlowerPrice = mutableListOf<String>()
        val buyAgainFlowerImage = mutableListOf<String>()
        val orderStatusList = mutableListOf<String>()

        for (order in completedOrders) {
            order.flowerNames?.firstOrNull()?.let { buyAgainFlowerName.add(it) }
            order.flowerPrices?.firstOrNull()?.let { buyAgainFlowerPrice.add(it) }
            order.flowerImages?.firstOrNull()?.let { buyAgainFlowerImage.add(it) }
            orderStatusList.add(order.status ?: "unknown")
        }

        buyAgainAdapter = BuyAgainAdapter(
            buyAgainFlowerName,
            buyAgainFlowerPrice,
            buyAgainFlowerImage,
            requireContext(),
            orderStatusList
        )

        binding.buyAgainRecyclerView.adapter = buyAgainAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            val buyItemRef = database.reference.child("users").child(userId).child("BuyHistory")
            val sortingQuery = buyItemRef.orderByChild("currentTime")
            sortingQuery.removeEventListener(buyHistoryListener)
        } catch (e: Exception) {
            // Handle exception nếu cần
        }
    }
}