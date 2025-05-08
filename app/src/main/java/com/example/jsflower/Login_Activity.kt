package com.example.jsflower

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.jsflower.Model.UserModel
import com.example.jsflower.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Login_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupPasswordVisibilityToggle()

        // Firebase Auth
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        // Cấu hình Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // từ Firebase
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Đăng nhập bằng email
        binding.loginButton.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.editTextTextPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            } else {
                loginWithEmail(email, password)
            }
        }

        // Chuyển sang trang đăng ký
        binding.donthavebutton.setOnClickListener {
            startActivity(Intent(this, SignActivity::class.java))
        }

        // Đăng nhập Google
        binding.googleButton2.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    updateUi(auth.currentUser)
                } else {
                    Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Lỗi đăng nhập", task.exception)
                }
            }
    }

    private fun saveUserDataGG(user: FirebaseUser?) {
        val name = user?.displayName ?: "Tên Google"
        val email = user?.email ?: "Không có email"
        val uid = user?.uid ?: return
        val userModel = UserModel(name, email)

        database.child("user").child(uid).setValue(userModel)
            .addOnSuccessListener { Log.d("Firebase", "Lưu user Google thành công") }
            .addOnFailureListener { Log.e("Firebase", "Lỗi lưu user Google", it) }
    }

    private fun updateUi(user: FirebaseUser?) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Toast.makeText(this, "Đăng nhập Google thành công", Toast.LENGTH_SHORT)
                            .show()
                        saveUserDataGG(auth.currentUser)
                        updateUi(auth.currentUser)
                    } else {
                        Log.e("LoginActivity", "Firebase Auth thất bại", authTask.exception)
                        Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Google Sign-In thất bại: ${e.statusCode} - ${e.message}")
                Toast.makeText(this, "Lỗi Google Sign-In: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordVisibilityToggle() {
        val editText = binding.editTextTextPassword

        editText.setOnTouchListener { _, event ->
            val drawableEnd = editText.compoundDrawables[2] // drawableEnd
            if (drawableEnd != null && event.action == MotionEvent.ACTION_UP) {
                val drawableWidth = drawableEnd.bounds.width()
                val extraPadding = editText.paddingEnd // 👈 quan trọng: tính cả paddingEnd

                if (event.rawX >= (editText.right - drawableWidth - extraPadding)) {
                    isPasswordVisible = !isPasswordVisible

                    if (isPasswordVisible) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.transformationMethod = null
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(this, R.drawable.lock),
                            null,
                            ContextCompat.getDrawable(this, R.drawable.eye),
                            null
                        )
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.transformationMethod = PasswordTransformationMethod.getInstance()
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(this, R.drawable.lock),
                            null,
                            ContextCompat.getDrawable(this, R.drawable.eye_hide),
                            null
                        )
                    }

                    editText.setSelection(editText.text.length)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

}
