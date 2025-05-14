package com.example.jsflower.adaptar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.R
import com.google.android.material.chip.Chip

class FilterChipAdapter(
    private var filters: List<String>,
    private val onRemoveFilter: (String) -> Unit
) : RecyclerView.Adapter<FilterChipAdapter.FilterViewHolder>() {

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filterChip: Chip = itemView.findViewById(R.id.filterChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_chip, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        holder.filterChip.text = filter

        // Set up close icon click listener
        holder.filterChip.setOnCloseIconClickListener {
            onRemoveFilter(filter)
        }
    }

    override fun getItemCount(): Int = filters.size

    fun updateFilters(newFilters: List<String>) {
        filters = newFilters
        notifyDataSetChanged()
    }
}