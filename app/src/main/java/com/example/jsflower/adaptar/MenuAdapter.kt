package com.example.jsflower.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.DetailsActivity
import com.example.jsflower.databinding.MenuItemBinding

class MenuAdapter(
    private val menuItemsName: MutableList<String>,
    private val menuItemPrice: MutableList<String>,
    private val menuImage: MutableList<Int>,
    private val requireContext: Context,
    private val itemClickListener: OnItemClickListener? = null
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

    inner class MenuViewHolder(private val binding: MenuItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Gọi sự kiện từ interface
                    itemClickListener?.onItemClick(position)

                    // Mở màn DetailsActivity
                    val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                        putExtra("MenuItemName", menuItemsName[position])
                        putExtra("MenuItemImage", menuImage[position])
                    }
                    requireContext.startActivity(intent)
                }
            }
        }

        fun bind(name: String, price: String, imageResId: Int) {
            binding.apply {
                menuFlowerName.text = name
                menuPrice.text = price
                menuImage.setImageResource(imageResId)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
