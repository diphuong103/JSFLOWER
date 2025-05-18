package com.example.jsflower

import android.app.Activity
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
import com.google.firebase.database.*

class ListAddressActivity : AppCompatActivity(), AddressAdapter.AddressClickListener {

    private lateinit var rvAddresses: RecyclerView
    private lateinit var btnAddAddress: Button
    private lateinit var addressAdapter: AddressAdapter

    private val addresses = mutableListOf<UserAddress>()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Constant for database path
    private val DATABASE_PATH = "users"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_address)

        rvAddresses = findViewById(R.id.rvAddresses)
        btnAddAddress = findViewById(R.id.btnAddAddress)

        addressAdapter = AddressAdapter(addresses, this)
        rvAddresses.layoutManager = LinearLayoutManager(this)
        rvAddresses.adapter = addressAdapter

        loadAddresses()

        btnAddAddress.setOnClickListener {
            val intent = Intent(this, AddEditAddressActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadAddresses()
    }

    private fun loadAddresses() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userRef = database.reference
            .child(DATABASE_PATH)
            .child(currentUser.uid)
            .child("listaddress")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                addresses.clear()
                for (child in snapshot.children) {
                    val id = child.key.toString()
                    val address = child.child("address").getValue(String::class.java) ?: ""
                    val latitude = child.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = child.child("longitude").getValue(Double::class.java) ?: 0.0
                    val isDefault = child.child("isDefault").getValue(Boolean::class.java) ?: false

                    val userAddress = UserAddress(id, address, latitude, longitude, isDefault)
                    addresses.add(userAddress)
                }
                // Sort addresses to show default address first
                addresses.sortByDescending { it.isDefault }
                addressAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ListAddressActivity,
                    "Error loading addresses: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onAddressClick(address: UserAddress) {
        val intent = Intent()
        intent.putExtra("selectedAddress", address.address)
        intent.putExtra("selectedLatitude", address.latitude)
        intent.putExtra("selectedLongitude", address.longitude)
        intent.putExtra("selectedAddressId", address.id)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onEditClick(address: UserAddress) {
        val intent = Intent(this, AddEditAddressActivity::class.java)
        intent.putExtra("address_id", address.id)
        startActivity(intent)
    }

    override fun onDeleteClick(address: UserAddress) {
        val currentUser = auth.currentUser ?: return

        AlertDialog.Builder(this)
            .setTitle("Delete Address")
            .setMessage("Are you sure you want to delete this address?")
            .setPositiveButton("Delete") { _, _ ->
                val addressRef = database.reference
                    .child(DATABASE_PATH)
                    .child(currentUser.uid)
                    .child("listaddress")
                    .child(address.id ?: return@setPositiveButton)

                addressRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show()
                        loadAddresses()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error deleting address: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}