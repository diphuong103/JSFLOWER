package com.example.jsflower.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        retrieveBuyHistory()

        binding.rencenBuyItem.setOnClickListener{
            seeItemsRecentBuy()
        }

        return binding.root
    }

    private fun seeItemsRecentBuy() {
        listOfOrderItem.firstOrNull()?.let {
            recentBuy ->
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
            }
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

    private fun retrieveBuyHistory() {
        binding.rencenBuyItem.visibility = View.INVISIBLE
        userId = auth.currentUser?.uid ?: ""

        val buyItemRef = database.reference.child("user").child(userId).child("BuyHistory")
        val sortingQuery = buyItemRef.orderByChild("currentTime")

        sortingQuery.addListenerForSingleValueEvent(object : ValueEventListener {
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
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }
}
