package com.example.jsflower.adaptar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.databinding.BuyAgainItemBinding
import com.example.jsflower.databinding.PopulerItemBinding

class BuyAgainAdapter(private val buyAgainFlowerName: ArrayList<String>, private val buyAgainFlowerPrice:ArrayList<String>, private val buyAgainFlowerImage:ArrayList<Int>) : RecyclerView.Adapter<BuyAgainAdapter.BuyAgainViewHolder>()
{


    override fun onBindViewHolder(holder: BuyAgainAdapter.BuyAgainViewHolder, position: Int) {
        holder.bind(buyAgainFlowerName[position], buyAgainFlowerPrice[position], buyAgainFlowerImage[position])

    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BuyAgainAdapter.BuyAgainViewHolder {
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return  BuyAgainViewHolder(binding)

    }

    override fun getItemCount(): Int = buyAgainFlowerName.size

    class BuyAgainViewHolder(private val binding: BuyAgainItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(flowerName: String, flowerPrice: String, flowerImage: Int) {
            binding.buyAgainFlowerName.text = flowerName
            binding.buyAgainFlowerPrice.text = flowerPrice
                binding.buyAgainFlowerImage.setImageResource(flowerImage)

        }

    }






}