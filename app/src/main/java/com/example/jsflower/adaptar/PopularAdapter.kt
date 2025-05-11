package com.example.jsflower.adaptar

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.DetailsActivity
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.databinding.MenuItemBinding

class PopularAdapter(
    private val popularItems: List<MenuItem>,
    private val context: Context,
    private val onAddToCart: (MenuItem) -> Unit
) : RecyclerView.Adapter<PopularAdapter.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        val menuItem = popularItems[position]
        holder.bind(menuItem)

        // Set click listener for the "Add to Cart" button
        holder.binding.menuAddToCart.setOnClickListener {
            onAddToCart(menuItem) // Add item to cart
        }

        // Set click listener for the item itself to go to the DetailsActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.flowerName)
                putExtra("MenuItemPrice", menuItem.flowerPrice)
                putExtra("MenuItemImage", menuItem.flowerImage)
                putExtra("MenuItemDescription", menuItem.flowerDescription)
                putExtra("MenuItemIngredient", menuItem.flowerIngredient)
                putExtra("MenuItemKey", menuItem.key)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = popularItems.size

    inner class PopularViewHolder(val binding: MenuItemBinding) :
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

    // Optional: Use this to add space between items in the RecyclerView
    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = state.itemCount

            if (position < itemCount - 1) {
                outRect.bottom = verticalSpaceHeight
            } else {
                outRect.bottom = 0 // Don't add space for the last item
            }
        }
    }
}
