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
import com.example.jsflower.Model.CartItems
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.RecentOrderItems
import com.example.jsflower.adaptar.BuyAgainAdapter
import com.example.jsflower.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.Serializable

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var listOfOrderItem: MutableList<OrderDetails> = mutableListOf()

    private lateinit var buyHistoryListener: ValueEventListener
    private lateinit var orderStatusListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""

        setupBuyHistoryListener()

        binding.rencenBuyItem.setOnClickListener {
            seeItemsRecentBuy()
        }

        binding.receivedButton.setOnClickListener {
            updateOrderStatus()
        }

        binding.cancelOrderButton.setOnClickListener {
            val canceledOrder = listOfOrderItem[0]
            val itemPushKey = canceledOrder.itemPushKey ?: return@setOnClickListener

            val orderRef = database.reference.child("OrderDetails").child(itemPushKey)
            val historyRef = database.reference.child("user").child(userId).child("BuyHistory")
                .child(itemPushKey)
            val cancelRef = database.reference.child("CanceledOrder").child(itemPushKey)

            cancelRef.setValue(canceledOrder)
                .addOnSuccessListener {
                    orderRef.removeValue()
                    historyRef.removeValue()
                    binding.statusCircle.background.setTint(Color.RED)
                    Toast.makeText(
                        requireContext(),
                        "Đơn hàng đã được hủy và lưu lịch sử",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Hủy đơn thất bại: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        return binding.root
    }

    private fun setupBuyHistoryListener() {
        binding.rencenBuyItem.visibility = View.INVISIBLE
        val buyItemRef = database.reference.child("user").child(userId).child("BuyHistory")
        val sortingQuery = buyItemRef.orderByChild("currentTime")

        buyHistoryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfOrderItem.clear()
                for (buySnapshot in snapshot.children) {
                    val buyHistoryItem = buySnapshot.getValue(OrderDetails::class.java)
                    buyHistoryItem?.let { listOfOrderItem.add(it) }
                }

                listOfOrderItem.reverse()
                if (listOfOrderItem.isNotEmpty()) {
                    setDataInRecentBuyItem()
                    setPreviousBuyItemsRecyclerView()
                    setupOrderStatusListener()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Không thể tải lịch sử mua hàng", Toast.LENGTH_SHORT).show()
            }
        }

        sortingQuery.addValueEventListener(buyHistoryListener)
    }

    private fun setupOrderStatusListener() {
        if (listOfOrderItem.isNotEmpty()) {
            val itemPushKey = listOfOrderItem[0].itemPushKey ?: return
            val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey)

            orderStatusListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isDelivered =
                        snapshot.child("delivered").getValue(Boolean::class.java) ?: false
                    val isAccepted =
                        snapshot.child("orderAccepted").getValue(Boolean::class.java) ?: false

                    with(binding) {
                        if (isAccepted) {
                            statusCircle.background.setTint(Color.GREEN)
                            // Hiện nút nếu chưa xác nhận đã nhận hàng
                            if (!isDelivered) {
                                receivedButton.visibility = View.VISIBLE
                            } else {
                                receivedButton.visibility = View.GONE
                            }

                            cancelOrderButton.visibility = View.GONE
                        } else {
                            statusCircle.background.setTint(Color.GRAY)
                            receivedButton.visibility = View.GONE
                            cancelOrderButton.visibility = View.VISIBLE
                        }
                    }
                }

                    override fun onCancelled(error: DatabaseError) {}
            }

            completeOrderRef.addValueEventListener(orderStatusListener)
        }
    }

    private fun updateOrderStatus() {
        val itemPushKey = listOfOrderItem[0].itemPushKey ?: return
        val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey)

        val updates = mapOf(
            "paymentReceived" to true,
            "status" to "delivered"
        )

        completeOrderRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã xác nhận nhận hàng", Toast.LENGTH_SHORT).show()
                binding.receivedButton.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show()
            }
    }

    private fun seeItemsRecentBuy() {
        listOfOrderItem.firstOrNull()?.let {
            val intent = Intent(requireContext(), RecentOrderItems::class.java)
            intent.putExtra("RecentBuyOrderItem", ArrayList(listOfOrderItem) as Serializable)
            startActivity(intent)
        }
    }

    private fun setDataInRecentBuyItem() {
        binding.rencenBuyItem.visibility = View.VISIBLE
        val recentOrderItem = listOfOrderItem.firstOrNull() ?: return

        with(binding) {
            buyAgainFlowerName.text = recentOrderItem.flowerNames?.firstOrNull() ?: ""
            buyAgainFlowerPrice.text = recentOrderItem.flowerPrices?.firstOrNull() ?: ""

            val image = recentOrderItem.flowerImages?.firstOrNull() ?: ""
            val uri = Uri.parse(image)
            Glide.with(requireContext()).load(uri).into(buyAgainFlowerImage)
        }
    }

    private fun buyAgain(flowerName: String) {
        for (order in listOfOrderItem) {
            val index = order.flowerNames?.indexOf(flowerName) ?: -1
            if (index != -1) {
                val cartItem = CartItems(
                    flowerName = flowerName,
                    flowerPrice = order.flowerPrices?.get(index) ?: "",
                    flowerImage = order.flowerImages?.get(index) ?: "",
                    flowerQuantity = 1,
                    flowerDescription = "",
                    flowerIngredient = ""
                )

                val userId = auth.currentUser?.uid ?: ""
                val cartItemRef = database.getReference("user/$userId/CartItems")

                cartItemRef.push().setValue(cartItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Không thể thêm vào giỏ hàng", Toast.LENGTH_SHORT)
                            .show()
                    }
                break
            }
        }
    }

    private fun setPreviousBuyItemsRecyclerView() {
        val buyAgainFlowerName = mutableListOf<String>()
        val buyAgainFlowerPrice = mutableListOf<String>()
        val buyAgainFlowerImage = mutableListOf<String>()

        for (i in 1 until listOfOrderItem.size) {
            listOfOrderItem[i].flowerNames?.firstOrNull()?.let { buyAgainFlowerName.add(it) }
            listOfOrderItem[i].flowerPrices?.firstOrNull()?.let { buyAgainFlowerPrice.add(it) }
            listOfOrderItem[i].flowerImages?.firstOrNull()?.let { buyAgainFlowerImage.add(it) }
        }

        val rv = binding.buyAgainRecyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())
        buyAgainAdapter = BuyAgainAdapter(
            buyAgainFlowerName,
            buyAgainFlowerPrice,
            buyAgainFlowerImage,
            requireContext(),
            onBuyAgainClick = { flowerName ->
                buyAgain(flowerName)
            }
        )
        rv.adapter = buyAgainAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            val buyItemRef = database.reference.child("user").child(userId).child("BuyHistory")
            val sortingQuery = buyItemRef.orderByChild("currentTime")
            sortingQuery.removeEventListener(buyHistoryListener)

            if (listOfOrderItem.isNotEmpty()) {
                val itemPushKey = listOfOrderItem[0].itemPushKey
                if (itemPushKey != null) {
                    val completeOrderRef =
                        database.reference.child("CompletedOrder").child(itemPushKey)
                    if (::orderStatusListener.isInitialized) {
                        completeOrderRef.removeEventListener(orderStatusListener)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }
}
