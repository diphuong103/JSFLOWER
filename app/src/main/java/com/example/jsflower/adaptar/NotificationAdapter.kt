package com.example.jsflower.adaptar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.databinding.NotifactionItemBinding

class NotificationAdapter(private var notification : ArrayList<String>, private var notificationImage: ArrayList<Int>) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
       val binding = NotifactionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return NotificationViewHolder(binding)
    }

    override fun getItemCount(): Int = notification.size

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(position)
    }

inner class NotificationViewHolder(private val binding: NotifactionItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(position: Int) {

        binding.apply {
            notificationTextView.text = notification[position]
            notificationImageView.setImageResource(notificationImage[position])
        }
    }
}
}