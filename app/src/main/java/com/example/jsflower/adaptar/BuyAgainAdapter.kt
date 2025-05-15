package com.example.jsflower.adaptar

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.databinding.BuyAgainItemBinding

class BuyAgainAdapter(
    private val flowerNameList: List<String>,
    private val flowerPriceList: List<String>,
    private val flowerImageList: List<String>,
    private val context: Context,
    private val orderStatusList: List<String>, // Danh sách trạng thái đơn hàng
    private val onBuyAgainClick: (String) -> Unit
) : RecyclerView.Adapter<BuyAgainAdapter.ViewHolder>() {

    class ViewHolder(val binding: BuyAgainItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = flowerNameList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.buyAgainFloweNameTextView.text = flowerNameList[position]
        holder.binding.buyAgainFlowerPriceTextView.text = flowerPriceList[position]

        val uri = Uri.parse(flowerImageList[position])
        Glide.with(context).load(uri).into(holder.binding.buyAgainFloweImageView)

        // Hiển thị trạng thái đơn hàng
        val status = orderStatusList.getOrNull(position) ?: ""
        when (status) {
            "delivered" -> {
                holder.binding.statusTextView.text = "Đã giao thành công"
                holder.binding.statusTextView.setTextColor(Color.GREEN)
                holder.binding.statusTextView.visibility = View.VISIBLE
                holder.binding.buyAgainButton.visibility = View.VISIBLE
            }
            "canceled" -> {
                holder.binding.statusTextView.text = "Đã hủy"
                holder.binding.statusTextView.setTextColor(Color.RED)
                holder.binding.statusTextView.visibility = View.VISIBLE
                holder.binding.buyAgainButton.visibility = View.GONE // Ẩn nút mua lại nếu hủy
            }
            else -> {
                holder.binding.statusTextView.visibility = View.GONE
                holder.binding.buyAgainButton.visibility = View.VISIBLE
            }
        }

        holder.binding.buyAgainButton.setOnClickListener {
            onBuyAgainClick(flowerNameList[position])
        }
    }
}
