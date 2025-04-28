package com.example.jsflower.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.databinding.MenuItemBinding

class MenuAdapter(
    private val menuItemsName: MutableList<String>,
    private val menuItemPrice: MutableList<String>,
    private val menuImage: MutableList<Int>
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(
            menuItemsName[position],
            menuItemPrice[position],
            menuImage[position]
        )
    }

    override fun getItemCount(): Int = menuItemsName.size

    class MenuViewHolder(private val binding: MenuItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String, price: String, imageResId: Int) {
            binding.apply {
                menuFlowerName.text = name
                menuPrice.text = price
                menuImage.setImageResource(imageResId)
            }
        }
    }
}
