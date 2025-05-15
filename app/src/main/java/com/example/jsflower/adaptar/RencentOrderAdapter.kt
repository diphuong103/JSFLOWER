// Cập nhật RecentOrderAdapter để xử lý hiển thị trạng thái đơn hàng
package com.example.jsflower.adaptar

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RecentOrderAdapter(
    private val ordersList: List<OrderDetails>,
    private val context: Context,
    private val onViewOrderClick: (OrderDetails) -> Unit,
    private val onCancelOrderClick: (OrderDetails) -> Unit,
    private val onReceivedClick: (OrderDetails) -> Unit
) : RecyclerView.Adapter<RecentOrderAdapter.RecentOrderViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val statusListeners = mutableMapOf<String, ValueEventListener>()

    class RecentOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flowerImage: ImageView = itemView.findViewById(R.id.recentOrderFlowerImage)
        val flowerName: TextView = itemView.findViewById(R.id.recentOrderFlowerName)
        val flowerPrice: TextView = itemView.findViewById(R.id.recentOrderFlowerPrice)
        val statusCircle: CardView = itemView.findViewById(R.id.recentOrderStatusCircle)
        val statusText: TextView = itemView.findViewById(R.id.recentOrderStatusText) // Thêm TextView hiển thị trạng thái
        val receivedButton: Button = itemView.findViewById(R.id.recentOrderReceivedButton)
        val cancelButton: Button = itemView.findViewById(R.id.recentOrderCancelButton)
        val orderContainer: ConstraintLayout = itemView.findViewById(R.id.recentOrderContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentOrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_order_item, parent, false)
        return RecentOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentOrderViewHolder, position: Int) {
        val orderDetails = ordersList[position]

        // Set order item details
        holder.flowerName.text = orderDetails.flowerNames?.firstOrNull() ?: ""
        holder.flowerPrice.text = orderDetails.flowerPrices?.firstOrNull() ?: ""

        // Load image
        val imageUrl = orderDetails.flowerImages?.firstOrNull() ?: ""
        val uri = Uri.parse(imageUrl)
        Glide.with(context).load(uri).into(holder.flowerImage)

        // Set up click listener for viewing order details
        holder.orderContainer.setOnClickListener {
            onViewOrderClick(orderDetails)
        }

        // Set up cancel button
        holder.cancelButton.setOnClickListener {
            onCancelOrderClick(orderDetails)
        }

        // Set up received button
        holder.receivedButton.setOnClickListener {
            onReceivedClick(orderDetails)
        }

        // Check order status from OrderDetails model directly
        updateOrderStatus(orderDetails, holder)
    }

    private fun updateOrderStatus(orderDetails: OrderDetails, holder: RecentOrderViewHolder) {
        // Kiểm tra trạng thái từ OrderDetails trước
        val status = orderDetails.status ?: "pending"
        val isAccepted = orderDetails.orderAccepted ?: false

        with(holder) {
            when (status) {
                "delivered" -> {
                    statusCircle.setCardBackgroundColor(Color.GREEN)
                    statusText.text = "Đã giao hàng"
                    receivedButton.visibility = View.GONE
                    cancelButton.visibility = View.GONE
                }
                "canceled" -> {
                    statusCircle.setCardBackgroundColor(Color.RED)
                    statusText.text = "Đã hủy"
                    receivedButton.visibility = View.GONE
                    cancelButton.visibility = View.GONE
                }
                else -> {
                    // Đơn hàng đang xử lý
                    if (isAccepted) {
                        statusCircle.setCardBackgroundColor(Color.GREEN)
                        statusText.text = "Đang giao hàng"
                        receivedButton.visibility = View.VISIBLE
                        cancelButton.visibility = View.GONE
                    } else {
                        statusCircle.setCardBackgroundColor(Color.GRAY)
                        statusText.text = "Chờ xác nhận"
                        receivedButton.visibility = View.GONE
                        cancelButton.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Cập nhật trạng thái từ Firebase để có thông tin mới nhất
        updateStatusFromFirebase(orderDetails, holder)
    }

    private fun updateStatusFromFirebase(orderDetails: OrderDetails, holder: RecentOrderViewHolder) {
        val itemPushKey = orderDetails.itemPushKey ?: return
        val completeOrderRef = database.reference.child("CompletedOrder").child(itemPushKey)

        // Remove any existing listener for this holder to prevent memory leaks
        statusListeners[itemPushKey]?.let {
            completeOrderRef.removeEventListener(it)
        }

        val statusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isDelivered = snapshot.child("delivered").getValue(Boolean::class.java) ?: false
                val isAccepted = snapshot.child("orderAccepted").getValue(Boolean::class.java) ?: false
                val status = snapshot.child("status").getValue(String::class.java)

                with(holder) {
                    when (status) {
                        "delivered" -> {
                            statusCircle.setCardBackgroundColor(Color.GREEN)
                            statusText.text = "Đã giao hàng"
                            receivedButton.visibility = View.GONE
                            cancelButton.visibility = View.GONE
                        }
                        "canceled" -> {
                            statusCircle.setCardBackgroundColor(Color.RED)
                            statusText.text = "Đã hủy"
                            receivedButton.visibility = View.GONE
                            cancelButton.visibility = View.GONE
                        }
                        else -> {
                            // Phụ thuộc vào trạng thái orderAccepted
                            if (isAccepted) {
                                statusCircle.setCardBackgroundColor(Color.GREEN)
                                statusText.text = "Đang giao hàng"
                                receivedButton.visibility = View.VISIBLE
                                cancelButton.visibility = View.GONE
                            } else {
                                statusCircle.setCardBackgroundColor(Color.GRAY)
                                statusText.text = "Chờ xác nhận"
                                receivedButton.visibility = View.GONE
                                cancelButton.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Default to gray if error
                holder.statusCircle.setCardBackgroundColor(Color.GRAY)
            }
        }

        // Save listener reference to clean up later
        statusListeners[itemPushKey] = statusListener
        completeOrderRef.addValueEventListener(statusListener)
    }

    override fun getItemCount(): Int = ordersList.size

    override fun onViewRecycled(holder: RecentOrderViewHolder) {
        super.onViewRecycled(holder)
        // Clean up listeners for recycled views
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION && position < ordersList.size) {
            val itemPushKey = ordersList[position].itemPushKey
            itemPushKey?.let {
                statusListeners[it]?.let { listener ->
                    val ref = database.reference.child("CompletedOrder").child(it)
                    ref.removeEventListener(listener)
                    statusListeners.remove(it)
                }
            }
        }
    }
}