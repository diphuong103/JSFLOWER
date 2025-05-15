package com.example.jsflower

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.Model.UserAddress

class AddressAdapter(
    private val addresses: List<UserAddress>,
    private val listener: AddressClickListener
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    interface AddressClickListener {
        fun onAddressClick(address: UserAddress)
        fun onEditClick(address: UserAddress)
        fun onDeleteClick(address: UserAddress)
    }

    class AddressViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditAddress)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = addresses[position]
        holder.tvAddress.text = address.address

        // Set click listeners
        holder.itemView.setOnClickListener {
            listener.onAddressClick(address)
        }

        holder.btnEdit.setOnClickListener {
            listener.onEditClick(address)
        }

        holder.btnDelete.setOnClickListener {
            listener.onDeleteClick(address)
        }
    }

    override fun getItemCount() = addresses.size
}