package com.example.jsflower.adaptar

import android.content.Context
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
) : RecyclerView.Adapter<RecentBuyAdapter.RecentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding =
            RecentBuyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
                flowerName.text = flowerNameList[position]
                flowerPrice.text = flowerPriceList[position]
                recentQuantity.text = flowerQuantityList[position].toString()
                val uri = Uri.parse(flowerImageList[position])
                Glide.with(context).load(uri).into(flowerImage)
            }
        }
    }
}
