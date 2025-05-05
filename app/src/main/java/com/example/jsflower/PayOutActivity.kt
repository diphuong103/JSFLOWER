package com.example.jsflower

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.databinding.ActivityPayOutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale

class PayOutActivity : AppCompatActivity() {
    private lateinit var userId: String
    private lateinit var binding: ActivityPayOutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String

    private lateinit var flowerItemName: ArrayList<String>
    private lateinit var flowerItemPrice: ArrayList<String>
    private lateinit var flowerItemImage: ArrayList<String>
    private lateinit var flowerItemDesciption: ArrayList<String>
    private lateinit var flowerItemIngredient: ArrayList<String>
    private lateinit var flowerItemQuantities: ArrayList<Int>

    private var isEditing = false
    private var total = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference()

        // Nhận dữ liệu từ Intent
        flowerItemName = intent.getStringArrayListExtra("FlowerItemName") ?: arrayListOf()
        flowerItemPrice = intent.getStringArrayListExtra("FlowerItemPrice") ?: arrayListOf()
        flowerItemImage = intent.getStringArrayListExtra("FlowerItemImage") ?: arrayListOf()
        flowerItemDesciption =
            intent.getStringArrayListExtra("FlowerItemDescription") ?: arrayListOf()
        flowerItemIngredient =
            intent.getStringArrayListExtra("FlowerItemIngredient") ?: arrayListOf()
        flowerItemQuantities =
            intent.getIntegerArrayListExtra("FlowerItemQuantities") ?: arrayListOf()

        Log.d("PayOut", "Quantity list: $flowerItemQuantities")
        Log.d("PayOut", "Price list: $flowerItemPrice")

        // Lấy thông tin người dùng
        setUserData()

        // Tính tổng tiền
        total = calculateTotal()
        binding.etTotal.isEnabled = false
        binding.etTotal.setText(formatCurrency(total))

        // Thiết lập các trường là không được chỉnh sửa ban đầu
        setEditTextsEnabled(false)

        // Xử lý nút chỉnh sửa
        binding.btnEdit.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                setEditTextsEnabled(true)
                binding.btnEdit.setImageResource(R.drawable.save) // Giả sử có icon save
                binding.btnOrder.isEnabled = false
                Toast.makeText(this, "Bạn có thể chỉnh sửa thông tin", Toast.LENGTH_SHORT).show()
            } else {
                saveUserData()
            }
        }

        binding.btnOrder.setOnClickListener {
            if (validateData()) {
                saveOrderToDatabase()
                val bottomSheetDialog = CongratsBottomSheet()

                name = binding.etName.text.toString().trim()
                address = binding.etAddress.text.toString().trim()
                phone = binding.etPhone.text.toString().trim()

                placeOrder()

                bottomSheetDialog.show(supportFragmentManager, "CongratsBottomSheet")
            } else {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }

        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun placeOrder() {
        userId = auth.currentUser?.uid ?: ""
        val time = System.currentTimeMillis()
        val itemPushKey = databaseReference.child("OrderDetails").push().key
        val orderDetails = OrderDetails(
            userId,
            name,
            flowerItemName,
            flowerItemPrice,
            flowerItemImage,
            flowerItemQuantities,
            address,
            total,
            phone,
            time,
            itemPushKey,
            false,
            false
        )
        val orderRef = databaseReference.child("OrderDetails").child(itemPushKey!!)
        orderRef.setValue(orderDetails).addOnSuccessListener {
            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "CongratsBottomSheet")
            addOrderToHistory(orderDetails)
        }
            .addOnFailureListener{
                Toast.makeText(this, "Đặt hàng thất bại -_-", Toast.LENGTH_SHORT).show()
            }

    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("user").child(userId).child("BuyHistory")
            .child(orderDetails.itemPushKey!!).setValue(orderDetails).addOnSuccessListener {

            }
    }

    private fun formatCurrency(amount: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return numberFormat.format(amount)
    }

    private fun calculateTotal(): Double {
        total = 0.0
        for (i in 0 until flowerItemPrice.size) {
            val priceString = flowerItemPrice[i].replace("\\D".toRegex(), "")
            val priceInt = priceString.toIntOrNull() ?: 0

            val quantity = if (i < flowerItemQuantities.size) flowerItemQuantities[i] else 1
            total += priceInt * quantity
        }
        return total
    }

    private fun setEditTextsEnabled(enabled: Boolean) {
        binding.etName.isEnabled = enabled
        binding.etAddress.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etTotal.isEnabled = false // Tổng tiền luôn không được chỉnh sửa
    }

    private fun validateData(): Boolean {
        val name = binding.etName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Vui lòng nhập tên"
            return false
        }

        if (address.isEmpty()) {
            binding.etAddress.error = "Vui lòng nhập địa chỉ"
            return false
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = "Vui lòng nhập số điện thoại"
            return false
        }

        // Kiểm tra định dạng số điện thoại
        if (!isValidPhoneNumber(phone)) {
            binding.etPhone.error = "Số điện thoại không hợp lệ"
            return false
        }

        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Đơn giản kiểm tra xem số điện thoại có ít nhất 10 chữ số và chỉ chứa số
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun setUserData() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid
            val userReference = databaseReference.child("user").child(userId)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val address = snapshot.child("address").getValue(String::class.java) ?: ""
                        val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                        binding.etName.setText(name)
                        binding.etAddress.setText(address)
                        binding.etPhone.setText(phone)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PayOutActivity, "Lỗi khi tải thông tin", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    private fun saveUserData() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid
            val name = binding.etName.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (!validateData()) {
                return
            }

            val userMap = mapOf(
                "name" to name,
                "address" to address,
                "phone" to phone
            )

            databaseReference.child("user").child(userId).updateChildren(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    setEditTextsEnabled(false)
                    isEditing = false
                    binding.btnEdit.setImageResource(R.drawable.edit)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveOrderToDatabase() {
        val user = auth.currentUser
        user?.let { firebaseUser ->
            val userId = firebaseUser.uid
            val orderReference =
                databaseReference.child("user").child(userId).child("orders").push()

            val orderItems = ArrayList<Map<String, Any>>()

            for (i in flowerItemName.indices) {
                val item = HashMap<String, Any>()
                item["name"] = flowerItemName[i]
                item["price"] = flowerItemPrice[i]
                item["quantity"] = flowerItemQuantities.getOrElse(i) { 1 }
                item["image"] = flowerItemImage[i]
                orderItems.add(item)
            }

            val orderMap = HashMap<String, Any>()
            orderMap["items"] = orderItems
            orderMap["total"] = total
            orderMap["timestamp"] = ServerValue.TIMESTAMP
            orderMap["status"] = "pending"
            orderMap["customerName"] = binding.etName.text.toString().trim()
            orderMap["customerAddress"] = binding.etAddress.text.toString().trim()
            orderMap["customerPhone"] = binding.etPhone.text.toString().trim()

            orderReference.setValue(orderMap)
                .addOnSuccessListener {
                    // Xóa giỏ hàng sau khi đặt hàng thành công
                    clearCart(userId)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lưu đơn hàng thất bại", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearCart(userId: String) {
        databaseReference.child("user").child(userId).child("CartItems")
            .removeValue()
            .addOnSuccessListener {
                Log.d("PayOut", "Đã xóa giỏ hàng sau khi đặt hàng")
            }
            .addOnFailureListener {
                Log.e("PayOut", "Lỗi khi xóa giỏ hàng: ${it.message}")
            }
    }
}