package com.example.jsflower

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.databinding.ActivityPayOutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale
import com.example.jsflower.Feature.PdfGenerator

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
    private lateinit var flowerItemKeys: ArrayList<String>
    private lateinit var flowerItemQuantities: ArrayList<Int>

    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String

    private val REQUEST_SELECT_ADDRESS = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference

        flowerItemName = intent.getStringArrayListExtra("FlowerItemName") ?: arrayListOf()
        flowerItemPrice = intent.getStringArrayListExtra("FlowerItemPrice") ?: arrayListOf()
        flowerItemImage = intent.getStringArrayListExtra("FlowerItemImage") ?: arrayListOf()
        flowerItemDesciption =
            intent.getStringArrayListExtra("FlowerItemDescription") ?: arrayListOf()
        flowerItemIngredient =
            intent.getStringArrayListExtra("FlowerItemIngredient") ?: arrayListOf()
        flowerItemQuantities =
            intent.getIntegerArrayListExtra("FlowerItemQuantities") ?: arrayListOf()
        flowerItemKeys = intent.getStringArrayListExtra("FlowerItemKey") ?: arrayListOf()

        setUserData()

        total = calculateTotal()
        binding.etTotal.isEnabled = false
        binding.etTotal.setText(formatCurrency(total))

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
                Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val selectedRadio = findViewById<RadioButton>(selectedPaymentId)
            val paymentMethod = selectedRadio.text.toString()

            name = binding.etName.text.toString().trim()
            address = binding.etAddress.text.toString().trim()
            phone = binding.etPhone.text.toString().trim()

            placeOrder(paymentMethod, selectedPaymentId == R.id.radioZalo)

            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "CongratsBottomSheet")
        }

        binding.btnBack.setOnClickListener {
            finish()
        }


        binding.layoutAddress.setOnClickListener {
            val intent = Intent(this, ListAddressActivity::class.java)
            startActivityForResult(intent, REQUEST_SELECT_ADDRESS)
        }
    }
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_ADDRESS && resultCode == Activity.RESULT_OK) {
            val selectedAddress = data?.getStringExtra("selectedAddress")
            if (!selectedAddress.isNullOrEmpty()) {
                binding.etAddress.setText(selectedAddress)
            }
        }
    }


    private fun placeOrder(paymentMethod: String, isPaidOnline: Boolean) {
        if (userId.isEmpty()) {
            Toast.makeText(this, "Bạn cần đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show()
            return
        }

        val time = System.currentTimeMillis()
        val orderPushKey = databaseReference.child("OrderDetails").push().key ?: return

        val productKeyQuantityMap = mutableMapOf<String, Int>()
        val minSize = minOf(flowerItemKeys.size, flowerItemQuantities.size)

        for (i in 0 until minSize) {
            val key = flowerItemKeys[i]
            val quantity = flowerItemQuantities[i]
            if (key.isNotEmpty()) {
                productKeyQuantityMap[key] = quantity
            }
        }

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
            orderPushKey,
            orderAccepted = false,
            paymentReceived = isPaidOnline,
            flowerKey = productKeyQuantityMap.toString()
        )

        databaseReference.child("OrderDetails").child(orderPushKey).setValue(orderDetails)
            .addOnSuccessListener {
                addOrderToHistory(orderDetails)
                saveOrderToOrders(orderPushKey, productKeyQuantityMap, isPaidOnline)
                clearCart(userId)

                pushNotificationToAdmin(orderDetails)


                val invoiceFile = PdfGenerator.createInvoicePDF(this, orderDetails)
                if (invoiceFile != null) {
                    binding.viewInvoiceButton.visibility = View.VISIBLE
                    binding.viewInvoiceButton.setOnClickListener {
                        val uri = FileProvider.getUriForFile(this, "$packageName.provider", invoiceFile)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    }
                } else {
                    binding.viewInvoiceButton.visibility = View.GONE
                }

                // Hiển thị nút xem hóa đơn
                binding.viewInvoiceButton.visibility = View.VISIBLE

                val bottomSheetDialog = CongratsBottomSheet()
                bottomSheetDialog.show(supportFragmentManager, "CongratsBottomSheet")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Đặt hàng thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("users").child(userId).child("BuyHistory")
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
        val userRef = databaseReference.child("users").child(user.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.etName.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                binding.etPhone.setText(snapshot.child("phone").getValue(String::class.java) ?: "")

                // Lấy địa chỉ mặc định trong listaddress
                val addressSnapshot = snapshot.child("listaddress")
                var defaultAddress: String? = null
                for (addressNode in addressSnapshot.children) {
                    val isDefault = addressNode.child("isDefault").getValue(Boolean::class.java) ?: false
                    if (isDefault) {
                        defaultAddress = addressNode.child("address").getValue(String::class.java)
                        break
                    }
                }

                if (!defaultAddress.isNullOrEmpty()) {
                    binding.etAddress.setText(defaultAddress)
                } else {
                    binding.etAddress.setText(snapshot.child("address").getValue(String::class.java) ?: "")
                }
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

        databaseReference.child("users").child(userId).updateChildren(updatedData)
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

    private fun clearCart(userId: String) {
        databaseReference.child("users").child(userId).child("CartItems")
            .removeValue()
            .addOnSuccessListener {
                Log.d("PayOut", "Đã xóa giỏ hàng sau khi đặt hàng")
            }
            .addOnFailureListener {
                Log.e("PayOut", "Lỗi khi xóa giỏ hàng: ${it.message}")
            }
    }

    private fun saveOrderToOrders(
        orderKey: String,
        productKeyQuantityMap: MutableMap<String, Int>,
        paymentReceived: Boolean
    ) {
        val ordersRef = databaseReference.child("users").child(userId).child("orders").child(orderKey)

        val orderItems = ArrayList<Map<String, Any>>()
        for (i in flowerItemName.indices) {
            val productKey = flowerItemKeys.getOrElse(i) { "" }
            val item = mapOf(
                "name" to flowerItemName.getOrElse(i) { "" },
                "price" to flowerItemPrice.getOrElse(i) { "" },
                "quantity" to flowerItemQuantities.getOrElse(i) { 1 },
                "image" to flowerItemImage.getOrElse(i) { "" },
                "productKey" to productKey
            )
            orderItems.add(item)
        }

        val orderMap = mapOf(
            "items" to orderItems,
            "total" to total,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to if (paymentReceived) "paid" else "pending",
            "customerName" to name,
            "customerAddress" to address,
            "customerPhone" to phone,
            "userId" to userId,
            "productKeys" to productKeyQuantityMap,
            "paymentReceived" to paymentReceived,
            "orderAccepted" to false
        )

        ordersRef.setValue(orderMap)
            .addOnSuccessListener {
                Log.d("PayOut", "Đơn hàng đã được lưu vào node orders")
            }
            .addOnFailureListener {
                Log.e("PayOut", "Lỗi khi lưu đơn hàng vào node orders: ${it.message}")
            }
    }

    private fun pushNotificationToAdmin(orderDetails: OrderDetails) {
        val adminUserId = "A1y4z8nuLVMjod8FnwKtVv5gzxp1"

        val notificationRef = databaseReference.child("notifications").child("admin").child(adminUserId)

        val notificationData = mapOf(
            "title" to "Đơn hàng mới",
            "body" to "Bạn có đơn hàng mới từ ${orderDetails.userName}",
            "timestamp" to System.currentTimeMillis(),
            "type" to "order",
            "isRead" to false
        )

        notificationRef.push().setValue(notificationData)
            .addOnSuccessListener {
                Log.d("PayOut", "Đã lưu thông báo đơn hàng tới admin")
            }
            .addOnFailureListener { e ->
                Log.e("PayOut", "Lỗi lưu thông báo: ${e.message}")
            }
    }

}
