package com.example.jsflower.adaptar

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.databinding.BuyAgainItemBinding

class BuyAgainAdapter(
    private val buyAgainFlowerName: List<String>,
    private val buyAgainFlowerPrice: List<String>,
    private val buyAgainFlowerImage: List<String>,
    private val context: Context,
    private val onBuyAgainClick: (String) -> Unit
) : RecyclerView.Adapter<BuyAgainAdapter.BuyAgainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyAgainViewHolder {
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuyAgainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuyAgainViewHolder, position: Int) {
        holder.bind(
            buyAgainFlowerName[position],
            buyAgainFlowerPrice[position],
            buyAgainFlowerImage[position]
        )
    }

    override fun getItemCount(): Int = buyAgainFlowerName.size

    inner class BuyAgainViewHolder(private val binding: BuyAgainItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(flowerName: String, flowerPrice: String, flowerImage: String) {
            binding.buyAgainFlowerName.text = flowerName
            binding.buyAgainFlowerPrice.text = flowerPrice
            val uri = Uri.parse(flowerImage)
            Glide.with(context).load(uri).into(binding.buyAgainFlowerImage)

            binding.buyAgainFlowerButton.setOnClickListener {
                onBuyAgainClick(flowerName)
            }
        }
    }
}
