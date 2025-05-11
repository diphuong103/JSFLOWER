package com.example.jsflower.adaptar

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.Model.CategoryModel
import com.example.jsflower.R

class CategoryAdapter(
    private val context: Context,
    private var categories: ArrayList<CategoryModel>,
    private val listener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    interface OnCategoryClickListener {
        fun onCategoryClick(category: CategoryModel, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.category_item, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val category = categories[position]

        holder.categoryName.text = category.name

        // Load image using Glide if image URL is not empty
        if (category.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(category.imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground) // Using a system drawable as placeholder
                .into(holder.categoryImage)
        } else {
            // Use default image if URL is empty
            holder.categoryImage.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Highlight selected category
        if (selectedPosition == position) {
            holder.categoryCard.setCardBackgroundColor(context.getColor(R.color.startcolor))
            holder.categoryName.setTextColor(context.getColor(R.color.selected_category_text))
        } else {
            holder.categoryCard.setCardBackgroundColor(context.getColor(R.color.unselected_category_bg))
            holder.categoryName.setTextColor(context.getColor(R.color.unselected_category_text))
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = position

            // Update the previously selected and newly selected items
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)

            // Notify the listener about the click
            listener.onCategoryClick(category, position)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    // Method to update categories list
    fun updateCategories(newCategories: ArrayList<CategoryModel>) {
        this.categories = newCategories
        selectedPosition = 0 // Reset selection to first item
        notifyDataSetChanged()
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryImage: ImageView = itemView.findViewById(R.id.category_image)
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        val categoryCard: CardView = itemView.findViewById(R.id.category_card)
    }
}