package com.example.jsflower

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.jsflower.databinding.ActivityDetailsBinding

class DetailsActivity : AppCompatActivity() {
    private lateinit var  binding : ActivityDetailsBinding

    private var flowerName: String ? = null
    private var flowerImage: String ? = null
    private var flowerDescriptions: String ? = null
    private var flowerIngredients: String ? = null
    private var flowerPrice : String ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        }
    }
