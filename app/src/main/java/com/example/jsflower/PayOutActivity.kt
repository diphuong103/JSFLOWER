package com.example.jsflower

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.example.jsflower.databinding.ActivityPayOutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PayOutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPayOutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private lateinit var flowerItemName: ArrayList<String>
    private lateinit var flowerItemPrice: ArrayList<String>
    private lateinit var flowerItemImage: ArrayList<String>
    private lateinit var flowerItemDesciption: ArrayList<String>
    private lateinit var flowerItemIngredient: ArrayList<String>
    private lateinit var flowerItemQuantities: ArrayList<String>

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
        flowerItemDesciption = intent.getStringArrayListExtra("FlowerItemDesciption") ?: arrayListOf()
        flowerItemIngredient = intent.getStringArrayListExtra("FlowerItemIngredient") ?: arrayListOf()
        val quantities = intent.getIntegerArrayListExtra("FlowerItemQuantities")
        if (quantities != null) {
            flowerItemQuantities = ArrayList(quantities.map { it.toString() })
        } else {
            flowerItemQuantities = arrayListOf()
            Toast.makeText(this, "Không có dữ liệu số lượng sản phẩm", Toast.LENGTH_SHORT).show()
        }



        // Tính tổng tiền
        calculateTotal()

        // Thiết lập các trường là không được chỉnh sửa ban đầu
        setEditTextsEnabled(false)

        // Lấy thông tin người dùng
        setUserData()

        // Xử lý nút chỉnh sửa
        binding.btnEdit.setOnClickListener {
            if (!isEditing) {
                // Bật chế độ chỉnh sửa
                isEditing = true
                setEditTextsEnabled(true)
                Toast.makeText(this, "Bạn có thể chỉnh sửa thông tin", Toast.LENGTH_SHORT).show()
            } else {
                // Lưu thông tin đã chỉnh sửa
                saveUserData()
            }
        }

        binding.btnSave.setOnClickListener {
            // Kiểm tra thông tin trước khi hiển thị bottom sheet
            if (validateData()) {
                val bottomSheetDialog = CongratsBottomSheet()
                bottomSheetDialog.show(supportFragmentManager, "Test")
            } else {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun calculateTotal() {
        total = 0.0
        val nameToTotalQuantity = mutableMapOf<String, Int>()
        val nameToPrice = mutableMapOf<String, Double>()

        for (i in flowerItemName.indices) {
            val name = flowerItemName[i]
            val price = flowerItemPrice.getOrNull(i)?.replace("đ", "")?.trim()?.toDoubleOrNull() ?: 0.0
            val quantity = flowerItemQuantities.getOrNull(i)?.toIntOrNull() ?: 0

            // Gộp số lượng
            nameToTotalQuantity[name] = nameToTotalQuantity.getOrDefault(name, 0) + quantity

            // Giá giữ lại giá đầu tiên (hoặc bạn có thể kiểm tra đồng nhất nếu cần)
            if (!nameToPrice.containsKey(name)) {
                nameToPrice[name] = price
            }
        }

        for ((name, quantity) in nameToTotalQuantity) {
            val price = nameToPrice[name] ?: 0.0
            total += price * quantity
        }

        binding.etTotal.setText(String.format("%.0f đ", total))
    }



    private fun setEditTextsEnabled(enabled: Boolean) {
        binding.etName.isEnabled = enabled
        binding.etAddress.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etTotal.isEnabled = false  // Tổng tiền không cho phép chỉnh sửa
    }

    private fun validateData(): Boolean {
        val name = binding.etName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        return name.isNotEmpty() && address.isNotEmpty() && phone.isNotEmpty()
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
                    Toast.makeText(this@PayOutActivity, "Lỗi khi tải thông tin", Toast.LENGTH_SHORT).show()
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

            if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
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
}