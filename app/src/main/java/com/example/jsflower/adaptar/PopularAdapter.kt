package com.example.jsflower.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.DetailsActivity
import com.example.jsflower.databinding.PopulerItemBinding

class PopularAdapter(private val items: List<String>, private val price: List<String>, private val image: List<Int>, private val requireContext: Context) : RecyclerView.Adapter<PopularAdapter.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder {
        val binding = PopulerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        val item = items[position]
        val images = image[position]
        val price = price[position]
        holder.bind(item,price, images)

        holder.itemView.setOnClickListener{
            // Mở màn DetailsActivity
            val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", item)
                putExtra("MenuItemImage", images)
            }
            requireContext.startActivity(intent)

        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    class PopularViewHolder(val binding: PopulerItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private val imagesView = binding.imageView5
        fun bind(item: String,price: String,  images: Int) {
             binding.flowerNamepopuler.text = item
             binding.PricePopuler.text = price
            imagesView.setImageResource(images)
        }
    }
    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
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
                outRect.bottom = 0 // Không thêm khoảng cách cho item cuối
            }
        }
    }


}
