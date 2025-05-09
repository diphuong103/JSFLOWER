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
import com.example.jsflower.databinding.ActivitySignBinding
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

class SignActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false
    private val binding: ActivitySignBinding by lazy {
        ActivitySignBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupPasswordVisibilityToggle()

        // Cấu hình Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Thay bằng Web client ID từ Firebase
            .requestEmail()
            .build()

        // Khởi tạo Firebase Auth và Database
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        // Khởi tạo Google SignIn client
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Đăng kí tài khoản
        binding.createButton.setOnClickListener {
            val username = binding.editTextUserName.text.toString().trim()
            val email = binding.editTextTextEmailAddress2.text.toString().trim()
            val password = binding.editTextTextPassword2.text.toString().trim()

            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            } else {
                createAccount(username, email, password)
            }
        }

        // Đăng nhập qua Google
        binding.googleButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        }

        // Đăng xuất
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }


        // Chuyển qua màn hình đăng nhập
        binding.alreadyhavebutton.setOnClickListener {
            startActivity(Intent(this, Login_Activity::class.java))
        }
    }

    private fun createAccount(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show()
                saveUserData(username, email)
                updateUi(auth.currentUser)
            } else {
                Toast.makeText(this, "Tạo tài khoản thất bại", Toast.LENGTH_SHORT).show()
                Log.e("SignActivity", "Tạo tài khoản thất bại", task.exception)
            }
        }
    }

    private fun saveUserData(username: String, email: String) {
        val user = UserModel(username, email)
        val userId = auth.currentUser!!.uid
        database.child("user").child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("Firebase", "Lưu user thành công")
            }
            .addOnFailureListener {
                Log.e("Firebase", "Lỗi khi lưu user", it)
            }
    }

    private fun saveUserDataGG(user: FirebaseUser?) {
        val userName = user?.displayName ?: "Người dùng Google"
        val email = user?.email ?: "Không có email"

        val userModel = UserModel(userName, email)
        val userId = user?.uid

        userId?.let {
            database.child("user").child(it).setValue(userModel)
                .addOnSuccessListener {
                    Log.d("Firebase", "Lưu user Google thành công")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Lỗi khi lưu user Google", it)
                }
        }
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
                        Toast.makeText(this, "Đăng nhập Google thành công", Toast.LENGTH_SHORT).show()
                        saveUserDataGG(auth.currentUser)
                        updateUi(auth.currentUser)
                    } else {
                        Log.e("SignActivity", "Firebase Auth thất bại: ${authTask.exception?.message}")
                        Toast.makeText(this, "Đăng nhập Firebase thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Log.e("SignActivity", "Google sign-in failed: ${e.statusCode} - ${e.message}")
                Toast.makeText(this, "Lỗi đăng nhập Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Đã huỷ chọn tài khoản Google", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordVisibilityToggle() {
        val editText = binding.editTextTextPassword2

        editText.setOnTouchListener { _, event ->
            val drawableEnd = editText.compoundDrawables[2] // drawableEnd
            if (drawableEnd != null && event.action == MotionEvent.ACTION_UP) {
                val drawableWidth = drawableEnd.bounds.width()
                val extraPadding = editText.paddingEnd // Include paddingEnd

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
