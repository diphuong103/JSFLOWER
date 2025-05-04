package com.example.jsflower

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.jsflower.Model.CartItems
import com.example.jsflower.databinding.ActivityDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DetailsActivity : AppCompatActivity() {
    private lateinit var  binding : ActivityDetailsBinding

    private lateinit var auth : FirebaseAuth

    private var flowerName: String ? = null
    private var flowerImage: String ? = null
    private var flowerDescriptions: String ? = null
    private var flowerIngredients: String ? = null
    private var flowerPrice : String ? = null

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        flowerName = intent.getStringExtra("MenuItemName")
        flowerDescriptions = intent.getStringExtra("MenuItemDescription")
        flowerIngredients = intent.getStringExtra("MenuItemIngredient")
        flowerPrice = intent.getStringExtra("MenuItemPrice")
        flowerImage = intent.getStringExtra("MenuItemImage")

        with(binding){
            detailFlowerName.text = flowerName
            detailDescriptionTextView.text = flowerDescriptions
            detailIngredients.text = flowerIngredients
            Glide.with(this@DetailsActivity).load(Uri.parse(flowerImage)).into(detailsFlowerImageView)
        }

        binding.imageButton.setOnClickListener {
            finish()
        }

        binding.addToCartButton.setOnClickListener{
            addItemToCart()
        }

        }
    private fun addItemToCart()
    {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid?:""

        //tao doi tuong the
        val cartItem = CartItems(flowerName.toString(), flowerPrice.toString(), flowerDescriptions.toString(), flowerImage.toString(), 1)

        // luu du lieu cartitem tu firebase
        database.child("user").child(userId).child("CartItems").push().setValue(cartItem).addOnSuccessListener {
            Toast.makeText(this, "Thêm sản phẩm vào giỏ hàng thành công <3", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this, "Thêm sản phẩm vào giỏ hàng thất bại -_-", Toast.LENGTH_SHORT).show()
        }
    }
}
