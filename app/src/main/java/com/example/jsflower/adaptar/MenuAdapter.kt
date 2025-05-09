package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.databinding.MenuItemBinding

class MenuAdapter(private val menuItems: List<MenuItem>, private val context: Context) :
    RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.bind(menuItem)

        // Set click listener for each item
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.flowerName)
                putExtra("MenuItemPrice", menuItem.flowerPrice)
                putExtra("MenuItemImage", menuItem.flowerImage)
                putExtra("MenuItemDescription", menuItem.flowerDescription)
                putExtra("MenuItemIngredient", menuItem.flowerIngredient)
                // IMPORTANT: Ensure we always pass the key
                putExtra("MenuItemKey", menuItem.key)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.menuFlowerName.text = menuItem.flowerName
            binding.menuPrice.text = menuItem.flowerPrice

            try {
                Glide.with(context)
                    .load(Uri.parse(menuItem.flowerImage))
                    .into(binding.menuImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}