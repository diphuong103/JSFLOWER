package com.example.jsflower

import AddEditAddressActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jsflower.Model.UserAddress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListAddressActivity : AppCompatActivity(), AddressAdapter.AddressClickListener {

    private lateinit var rvAddresses: RecyclerView
    private lateinit var btnAddAddress: Button
    private lateinit var addressAdapter: AddressAdapter

    private val addresses = mutableListOf<UserAddress>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_address)

        // Initialize views
        rvAddresses = findViewById(R.id.rvAddresses)
        btnAddAddress = findViewById(R.id.btnAddAddress)

        // Setup RecyclerView
        addressAdapter = AddressAdapter(addresses, this)
        rvAddresses.layoutManager = LinearLayoutManager(this)
        rvAddresses.adapter = addressAdapter

        // Load addresses from Firebase
        loadAddresses()

        // Setup "Add Address" button
        btnAddAddress.setOnClickListener {
            val intent = Intent(this, AddEditAddressActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload addresses when returning to this activity
        loadAddresses()
    }

    private fun loadAddresses() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        addresses.clear()

        db.collection("addresses")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val address = document.toObject(UserAddress::class.java).copy(id = document.id)
                    addresses.add(address)
                }
                addressAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tải địa chỉ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onAddressClick(address: UserAddress) {
        // Handle address selection (e.g., for checkout)
        val intent = Intent()
        intent.putExtra("selectedAddressId", address.id)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onEditClick(address: UserAddress) {
        val intent = Intent(this, AddEditAddressActivity::class.java)
        intent.putExtra("addressId", address.id)
        startActivity(intent)
    }

    override fun onDeleteClick(address: UserAddress) {
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Xóa địa chỉ")
            .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAddress(address.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteAddress(addressId: String) {
        db.collection("addresses").document(addressId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show()
                loadAddresses() // Reload the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi xóa địa chỉ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}