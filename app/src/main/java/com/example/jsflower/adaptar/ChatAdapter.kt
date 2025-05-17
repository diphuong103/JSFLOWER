package com.example.jsflower.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.Model.ChatModel
import com.example.jsflower.R
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.swiperefreshlayout.widget.CircularProgressDrawable


class ChatAdapter(private val messages: List<ChatModel>, private val currentUserId: String) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CLIENT = 1
        private const val VIEW_TYPE_ADMIN = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_CLIENT  // Tin nhắn của người gửi (client)
        } else {
            VIEW_TYPE_ADMIN  // Tin nhắn của admin
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderLayout: ConstraintLayout = itemView.findViewById(R.id.senderLayout)
        val receiverLayout: ConstraintLayout = itemView.findViewById(R.id.receiverLayout)

        val senderMessageText: TextView = itemView.findViewById(R.id.senderMessageText)
        val senderTimestamp: TextView = itemView.findViewById(R.id.senderTimestamp)
        val senderImageView: ImageView = itemView.findViewById(R.id.senderImageView)

        val receiverMessageText: TextView = itemView.findViewById(R.id.receiverMessageText)
        val receiverTimestamp: TextView = itemView.findViewById(R.id.receiverTimestamp)
        val receiverAvatar: CircleImageView = itemView.findViewById(R.id.receiverAvatar)
        val receiverImageView: ImageView = itemView.findViewById(R.id.receiverImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        val progressDrawable = CircularProgressDrawable(holder.itemView.context).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }


        if (message.userId == "client") {
            // Tin nhắn của người dùng (client)
            holder.senderLayout.visibility = View.VISIBLE
            holder.receiverLayout.visibility = View.GONE

            if (message.imageUrl.isNotEmpty()) {
                holder.senderMessageText.visibility = View.GONE
                holder.senderImageView.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(message.imageUrl)
                    .centerCrop()
                    .placeholder(progressDrawable)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.senderImageView)
            } else {
                holder.senderMessageText.visibility = View.VISIBLE
                holder.senderImageView.visibility = View.GONE
                holder.senderMessageText.text = message.message
            }

            holder.senderTimestamp.text = formattedTime
        } else if (message.userId == "admin") {
            // Tin nhắn từ admin
            holder.receiverLayout.visibility = View.VISIBLE
            holder.senderLayout.visibility = View.GONE

            if (message.imageUrl.isNotEmpty()) {
                holder.receiverMessageText.visibility = View.GONE
                holder.receiverImageView.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(message.imageUrl)
                    .centerCrop()
                    .placeholder(progressDrawable)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.receiverImageView)
            } else {
                holder.receiverMessageText.visibility = View.VISIBLE
                holder.receiverImageView.visibility = View.GONE
                holder.receiverMessageText.text = message.message
            }

            holder.receiverTimestamp.text = formattedTime
        }
    }

    override fun getItemCount(): Int = messages.size

    private fun createCircularProgressDrawable(view: View): CircularProgressDrawable {
        return CircularProgressDrawable(view.context).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }
    }

}
