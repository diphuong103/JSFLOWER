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

    inner class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvDefault: TextView = itemView.findViewById(R.id.tvDefault)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAddressClick(addresses[position])
                }
            }

            btnEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(addresses[position])
                }
            }

            btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(addresses[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = addresses[position]
        holder.tvAddress.text = address.address

        // Show "Default" text if the address is marked as default
        if (address.isDefault) {
            holder.tvDefault.visibility = View.VISIBLE
        } else {
            holder.tvDefault.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = addresses.size
}