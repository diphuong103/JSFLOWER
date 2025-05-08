package com.example.jsflower

import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
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
    private var isEditing = false
    private var total = 0.0

    private lateinit var flowerItemName: ArrayList<String>
    private lateinit var flowerItemPrice: ArrayList<String>
    private lateinit var flowerItemImage: ArrayList<String>
    private lateinit var flowerItemDesciption: ArrayList<String>
    private lateinit var flowerItemIngredient: ArrayList<String>
    private lateinit var flowerItemQuantities: ArrayList<Int>

    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        // Lấy dữ liệu sản phẩm từ intent
        flowerItemName = intent.getStringArrayListExtra("FlowerItemName") ?: arrayListOf()
        flowerItemPrice = intent.getStringArrayListExtra("FlowerItemPrice") ?: arrayListOf()
        flowerItemImage = intent.getStringArrayListExtra("FlowerItemImage") ?: arrayListOf()
        flowerItemDesciption = intent.getStringArrayListExtra("FlowerItemDescription") ?: arrayListOf()
        flowerItemIngredient = intent.getStringArrayListExtra("FlowerItemIngredient") ?: arrayListOf()
        flowerItemQuantities = intent.getIntegerArrayListExtra("FlowerItemQuantities") ?: arrayListOf()

        Log.d("PayOut", "Quantity list: $flowerItemQuantities")
        Log.d("PayOut", "Price list: $flowerItemPrice")

        // Load thông tin người dùng
        setUserData()

        // Tính tổng tiền
        total = calculateTotal()
        binding.etTotal.isEnabled = false
        binding.etTotal.setText(formatCurrency(total))

        // Các trường không cho chỉnh sửa ban đầu
        setEditTextsEnabled(false)

        binding.btnEdit.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                setEditTextsEnabled(true)
                binding.btnEdit.setImageResource(R.drawable.save)
                binding.btnOrder.isEnabled = false
                Toast.makeText(this, "Bạn có thể chỉnh sửa thông tin", Toast.LENGTH_SHORT).show()
            } else {
                saveUserData()
            }
        }

        binding.btnOrder.setOnClickListener {
            if (!validateData()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedPaymentId = binding.paymentMethodGroup.checkedRadioButtonId
            if (selectedPaymentId == -1) {
                Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRadio = findViewById<RadioButton>(selectedPaymentId)
            val paymentMethod = selectedRadio.text.toString()

            name = binding.etName.text.toString().trim()
            address = binding.etAddress.text.toString().trim()
            phone = binding.etPhone.text.toString().trim()

            saveOrderToDatabase()
            placeOrder(paymentMethod)

            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "CongratsBottomSheet")
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun placeOrder(paymentMethod: String) {
        userId = auth.currentUser?.uid ?: return
        val time = System.currentTimeMillis()
        val itemPushKey = databaseReference.child("OrderDetails").push().key ?: return
        val paymentReceived = paymentMethod != "Khi nhận hàng"

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
            paymentReceived
        )

        databaseReference.child("OrderDetails").child(itemPushKey).setValue(orderDetails)
            .addOnSuccessListener {
                addOrderToHistory(orderDetails)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Đặt hàng thất bại -_-", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("user").child(userId).child("BuyHistory")
            .child(orderDetails.itemPushKey!!).setValue(orderDetails)
    }

    private fun calculateTotal(): Double {
        var total = 0.0
        for (i in flowerItemPrice.indices) {
            val priceString = flowerItemPrice[i].replace("\\D".toRegex(), "")
            val price = priceString.toIntOrNull() ?: 0
            val quantity = flowerItemQuantities.getOrElse(i) { 1 }
            total += price * quantity
        }
        return total
    }

    private fun formatCurrency(amount: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return numberFormat.format(amount)
    }

    private fun setEditTextsEnabled(enabled: Boolean) {
        binding.etName.isEnabled = enabled
        binding.etAddress.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etTotal.isEnabled = false
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

        if (!isValidPhoneNumber(phone)) {
            binding.etPhone.error = "Số điện thoại không hợp lệ"
            return false
        }

        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun setUserData() {
        val user = auth.currentUser ?: return
        val userRef = databaseReference.child("user").child(user.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.etName.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                binding.etAddress.setText(snapshot.child("address").getValue(String::class.java) ?: "")
                binding.etPhone.setText(snapshot.child("phone").getValue(String::class.java) ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PayOutActivity, "Lỗi khi tải thông tin", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserData() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        if (!validateData()) return

        val updatedData = mapOf(
            "name" to binding.etName.text.toString().trim(),
            "address" to binding.etAddress.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim()
        )

        databaseReference.child("user").child(userId).updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                setEditTextsEnabled(false)
                isEditing = false
                binding.btnEdit.setImageResource(R.drawable.edit)
                binding.btnOrder.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveOrderToDatabase() {
        val user = auth.currentUser ?: return
        val orderRef = databaseReference.child("user").child(user.uid).child("orders").push()

        val orderItems = flowerItemName.indices.map { i ->
            mapOf(
                "name" to flowerItemName[i],
                "price" to flowerItemPrice[i],
                "quantity" to flowerItemQuantities.getOrElse(i) { 1 },
                "image" to flowerItemImage[i]
            )
        }

        val orderMap = mapOf(
            "items" to orderItems,
            "total" to total,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "pending",
            "customerName" to binding.etName.text.toString().trim(),
            "customerAddress" to binding.etAddress.text.toString().trim(),
            "customerPhone" to binding.etPhone.text.toString().trim()
        )

        orderRef.setValue(orderMap)
            .addOnSuccessListener { clearCart(user.uid) }
            .addOnFailureListener {
                Toast.makeText(this, "Lưu đơn hàng thất bại", Toast.LENGTH_SHORT).show()
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
