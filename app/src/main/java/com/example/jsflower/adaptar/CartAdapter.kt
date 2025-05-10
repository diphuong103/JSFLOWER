package com.example.jsflower.adaptar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jsflower.R

class CartAdapter(
    private val context: Context,
    private val flowerNameList: MutableList<String>,
    private val flowerPriceList: MutableList<String>,
    private val flowerDescriptionList: MutableList<String>,
    private val flowerImageList: MutableList<String>,
    private val flowerIngredientList: MutableList<String>,
    private val flowerQuantityList: MutableList<Int>,
    private val listener: CartItemActionListener
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // Interface để xử lý các sự kiện từ CartAdapter
    interface CartItemActionListener {
        fun onCartItemDelete(position: Int)
        fun onQuantityChanged(position: Int, newQuantity: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_item, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.cartFlowerName.text = flowerNameList[position]
        holder.cartFlowerPrice.text = flowerPriceList[position]

        // Thiết lập số lượng hiện tại
        holder.cartItemQuantity.text = flowerQuantityList[position].toString()

        // Tải hình ảnh bằng Glide
        Glide.with(context)
            .load(flowerImageList[position])
            .into(holder.cartFlowerImage)

        // Xử lý sự kiện khi người dùng nhấn nút xóa
        holder.removeButton.setOnClickListener {
            // Hiển thị dialog xác nhận trước khi xóa
            android.app.AlertDialog.Builder(context)
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?")
                .setPositiveButton("Xóa") { dialog, _ ->
                    dialog.dismiss()
                    // Đảm bảo vị trí hợp lệ
                    if (position >= 0 && position < flowerNameList.size) {
                        listener.onCartItemDelete(holder.adapterPosition)
                    }
                }
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        // Xử lý nút tăng số lượng
        holder.increaseButton.setOnClickListener {
            // Kiểm tra vị trí có còn hợp lệ không
            if (position >= 0 && position < flowerQuantityList.size) {
                val currentQuantity = flowerQuantityList[position]
                val newQuantity = currentQuantity + 1

                // Giới hạn số lượng tối đa (tùy chọn)
                val maxQuantity = 99
                if (newQuantity <= maxQuantity) {
                    // Cập nhật hiển thị
                    holder.cartItemQuantity.text = newQuantity.toString()

                    // Cập nhật giá trị trong danh sách
                    if (position < flowerQuantityList.size) {
                        flowerQuantityList[position] = newQuantity
                    }

                    // Thông báo thay đổi cho fragment
                    listener.onQuantityChanged(position, newQuantity)
                } else {
                    Toast.makeText(context, "Số lượng tối đa là $maxQuantity", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Xử lý nút giảm số lượng
        holder.decreaseButton.setOnClickListener {
            // Kiểm tra vị trí có còn hợp lệ không
            if (position >= 0 && position < flowerQuantityList.size) {
                val currentQuantity = flowerQuantityList[position]
                if (currentQuantity > 1) {
                    val newQuantity = currentQuantity - 1

                    // Cập nhật hiển thị
                    holder.cartItemQuantity.text = newQuantity.toString()

                    // Cập nhật giá trị trong danh sách
                    if (position < flowerQuantityList.size) {
                        flowerQuantityList[position] = newQuantity
                    }

                    // Thông báo thay đổi cho fragment
                    listener.onQuantityChanged(position, newQuantity)
                } else {
                    // Nếu số lượng là 1 và người dùng nhấn giảm, xóa sản phẩm
                    // Đưa ra thông báo xác nhận trước khi xóa
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Xóa sản phẩm")
                        .setMessage("Bạn có muốn xóa sản phẩm này khỏi giỏ hàng?")
                        .setPositiveButton("Xóa") { dialog, _ ->
                            dialog.dismiss()
                            listener.onCartItemDelete(position)
                        }
                        .setNegativeButton("Hủy") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return flowerNameList.size
    }

    fun getUpdatedItemsQuantities(): MutableList<Int> {
        return flowerQuantityList
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cartFlowerName: TextView = itemView.findViewById(R.id.cartItemName)
        val cartFlowerPrice: TextView = itemView.findViewById(R.id.cartItemPrice)
        val cartFlowerImage: ImageView = itemView.findViewById(R.id.cartItemImage)
        val cartItemQuantity: TextView = itemView.findViewById(R.id.cartItemQuantity)
        val increaseButton: ImageButton = itemView.findViewById(R.id.increaseQuantityBtn)
        val decreaseButton: ImageButton = itemView.findViewById(R.id.decreaseQuantityBtn)
        val removeButton: Button = itemView.findViewById(R.id.removeButton)
    }
}