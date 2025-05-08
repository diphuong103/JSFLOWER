package com.example.jsflower.Fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    // Khai báo ValueEventListener để có thể hủy đăng ký khi cần
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

        // Thiết lập listener cho lịch sử mua hàng
        setupBuyHistoryListener()

        binding.rencenBuyItem.setOnClickListener{
            seeItemsRecentBuy()
        }

        binding.receivedButton.setOnClickListener {
            updateOrderStatus()
        }

        return binding.root
    }

    private fun setupBuyHistoryListener() {
        binding.rencenBuyItem.visibility = View.INVISIBLE
        userId = auth.currentUser?.uid ?: ""

        val buyItemRef = database.reference.child("user").child(userId).child("BuyHistory")
        val sortingQuery = buyItemRef.orderByChild("currentTime")

        buyHistoryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfOrderItem.clear()
                for (buySnapshot in snapshot.children) {
                    val buyHistoryItem = buySnapshot.getValue(OrderDetails::class.java)
                    buyHistoryItem?.let {
                        listOfOrderItem.add(it)
                    }
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

        // Sử dụng addValueEventListener thay vì addListenerForSingleValueEvent
        sortingQuery.addValueEventListener(buyHistoryListener)
    }

    private fun setupOrderStatusListener() {
        // Chỉ thiết lập nếu có đơn hàng
        if (listOfOrderItem.isNotEmpty()) {
            val itemPushKey = listOfOrderItem[0].itemPushKey
            if (itemPushKey != null) {
                val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey)

                orderStatusListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isReceived = snapshot.child("paymentReceived").getValue(Boolean::class.java) ?: false
                        val isAccepted = snapshot.child("orderAccepted").getValue(Boolean::class.java) ?: false

                        with(binding) {
                            if (isAccepted) {
                                statusCircle.background.setTint(Color.GREEN)
                                if (!isReceived) {
                                    receivedButton.visibility = View.VISIBLE
                                } else {
                                    receivedButton.visibility = View.INVISIBLE
                                }
                            } else {
                                statusCircle.background.setTint(Color.GRAY)
                                receivedButton.visibility = View.INVISIBLE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Xử lý lỗi nếu cần
                    }
                }

                completeOrderRef.addValueEventListener(orderStatusListener)
            }
        }
    }

    private fun updateOrderStatus() {
        val itemPushKey = listOfOrderItem[0].itemPushKey
        val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey!!)
        completeOrderRef.child("paymentReceived").setValue(true)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã xác nhận nhận hàng", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show()
            }
    }

    private fun seeItemsRecentBuy() {
        listOfOrderItem.firstOrNull()?.let { recentBuy ->
            val intent = Intent(requireContext(), RecentOrderItems::class.java)
            intent.putExtra("RecentBuyOrderItem", ArrayList(listOfOrderItem) as Serializable)
            startActivity(intent)
        }
    }

    private fun setDataInRecentBuyItem() {
        binding.rencenBuyItem.visibility = View.VISIBLE
        val recentOrderItem = listOfOrderItem.firstOrNull()

        recentOrderItem?.let {
            with(binding) {
                buyAgainFlowerName.text = it.flowerNames?.firstOrNull() ?: ""
                buyAgainFlowerPrice.text = it.flowerPrices?.firstOrNull() ?: ""

                val image = it.flowerImages?.firstOrNull() ?: ""
                val uri = Uri.parse(image)
                Glide.with(requireContext()).load(uri).into(buyAgainFlowerImage)

                // Trạng thái đơn hàng được cập nhật trong setupOrderStatusListener
            }
        }
    }

    // Thêm sản phẩm vào giỏ hàng khi người dùng nhấn "Mua lại"
    private fun buyAgain(cartItem: CartItems) {
        val userId = auth.currentUser?.uid ?: ""
        val cartItemRef = database.getReference("user/$userId/CartItems")

        cartItemRef.push().setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Không thể thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setPreviousBuyItemsRecyclerView() {
        val buyAgainFlowerName = mutableListOf<String>()
        val buyAgainFlowerPrice = mutableListOf<String>()
        val buyAgainFlowerImage = mutableListOf<String>()

        for (i in 1 until listOfOrderItem.size) {
            listOfOrderItem[i].flowerNames?.firstOrNull()?.let {
                buyAgainFlowerName.add(it)
            }
            listOfOrderItem[i].flowerPrices?.firstOrNull()?.let {
                buyAgainFlowerPrice.add(it)
            }
            listOfOrderItem[i].flowerImages?.firstOrNull()?.let {
                buyAgainFlowerImage.add(it)
            }
        }

        val rv = binding.buyAgainRecyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())
        buyAgainAdapter = BuyAgainAdapter(buyAgainFlowerName, buyAgainFlowerPrice, buyAgainFlowerImage, requireContext())
        rv.adapter = buyAgainAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Hủy đăng ký các listener khi fragment bị hủy để tránh rò rỉ bộ nhớ
        try {
            val buyItemRef = database.reference.child("user").child(userId).child("BuyHistory")
            val sortingQuery = buyItemRef.orderByChild("currentTime")
            sortingQuery.removeEventListener(buyHistoryListener)

            if (listOfOrderItem.isNotEmpty()) {
                val itemPushKey = listOfOrderItem[0].itemPushKey
                if (itemPushKey != null) {
                    val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey)
                    if (::orderStatusListener.isInitialized) {
                        completeOrderRef.removeEventListener(orderStatusListener)
                    }
                }
            }
        } catch (e: Exception) {
            // Xử lý ngoại lệ nếu có
        }
    }
}