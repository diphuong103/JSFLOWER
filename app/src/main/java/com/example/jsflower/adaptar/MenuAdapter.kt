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

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val requireContext: Context,

    ) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }



    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Gọi sự kiện từ interface
                    openDetailsActivity(position)


                }
            }
        }

        private fun openDetailsActivity(position: Int) {
            val menuItem = menuItems[position]

            // a intent to open details activity and pass data
            val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.flowerName)
                putExtra("MenuItemImage", menuItem.flowerImage)
                putExtra("MenuItemDescription", menuItem.flowerDescription)
                putExtra("MenuItemIngredient", menuItem.flowerIngredient)
                putExtra("MenuItemPrice", menuItem.flowerPrice)
            }

            requireContext.startActivity(intent)

        }


        // set data in recyclerview items name, price, image
        fun bind(menuItem: MenuItem) {
            binding.apply {
                menuFlowerName.text = menuItem.flowerName
                menuPrice.text = menuItem.flowerPrice
                Glide.with(requireContext)
                    .load(Uri.parse(menuItem.flowerImage))
                    .into(menuImage)
            }
        }
    }

}
