package com.example.jsflower

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageUrl = intent.getStringExtra("image_url")
        val photoView = findViewById<PhotoView>(R.id.fullImageView)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)

        Glide.with(this)
            .load(imageUrl)
            .into(photoView)

        closeButton.setOnClickListener {
            finish()
        }
    }
}
