package com.example.jsflower

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.jsflower.Fragment.CartFragment

class CartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_cart) // layout giỏ hàng
        // Load CartFragment nếu dùng Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.cart_fragment_container, CartFragment())
            .commit()
    }
}