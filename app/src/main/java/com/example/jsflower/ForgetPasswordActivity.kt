package com.example.jsflower

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jsflower.databinding.ActivityForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    binding.progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Đã gửi email đặt lại mật khẩu!", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(
                            this,
                            "Không thể gửi email. Kiểm tra lại địa chỉ!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, Login_Activity::class.java))
            finish()
        }
    }
}
