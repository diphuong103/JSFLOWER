package com.example.jsflower.adaptar

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.databinding.RecentBuyItemBinding

class RecentBuyAdapter(
    private var context: Context,
    private var flowerNameList: ArrayList<String>,
    private var flowerImageList: ArrayList<String>,
    private var flowerPriceList: ArrayList<String>,
    private var flowerQuantityList: ArrayList<Int>,
    private var orderStatusList: ArrayList<String>,
) : RecyclerView.Adapter<RecentBuyAdapter.RecentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding = RecentBuyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentViewHolder(binding)
    }

    override fun getItemCount(): Int = flowerNameList.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class RecentViewHolder(private val binding: RecentBuyItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                buyAgainFloweNameTextView.text = flowerNameList[position]
                buyAgainFlowerPriceTextView.text = flowerPriceList[position]
                flowerQuantity.text = flowerQuantityList[position].toString()
                val uri = Uri.parse(flowerImageList[position])
                Glide.with(context).load(uri).into(buyAgainFloweImageView)

                // Hiển thị trạng thái đơn hàng
                val status = orderStatusList.getOrNull(position) ?: ""
                when (status.lowercase()) {
                    "delivered" -> {
                        statusTextView.text = "Đã giao thành công"
                        statusTextView.setTextColor(Color.GREEN)
                        statusTextView.visibility = android.view.View.VISIBLE
                    }
                    "canceled" -> {
                        statusTextView.text = "Đã hủy"
                        statusTextView.setTextColor(Color.RED)
                        statusTextView.visibility = android.view.View.VISIBLE
                    }
                    else -> {
                        statusTextView.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }
}
