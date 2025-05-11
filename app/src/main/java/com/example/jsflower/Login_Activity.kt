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
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Login_Activity : AppCompatActivity() {
    private val TAG = "FacebookLogin"
    private lateinit var callbackManager: CallbackManager
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

        // Initialize Facebook Callback Manager
        callbackManager = CallbackManager.Factory.create()

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

        binding.forgetPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))

        }

        // Đăng nhập bằng Facebook
        binding.customFbButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )
        }

        // Đăng ký callback cho Facebook Login
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d(TAG, "facebook:onSuccess:$loginResult")
                    handleFacebookAccessToken(loginResult.accessToken)
                }

                override fun onCancel() {
                    Log.d(TAG, "facebook:onCancel")
                    Toast.makeText(this@Login_Activity, "Đăng nhập Facebook bị hủy", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    Log.d(TAG, "facebook:onError", error)
                    Toast.makeText(this@Login_Activity, "Lỗi đăng nhập Facebook: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

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

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    saveUserDataFB(user)
                    updateUi(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Xác thực Facebook thất bại.",
                        Toast.LENGTH_SHORT).show()
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

    private fun saveUserDataFB(user: FirebaseUser?) {
        val name = user?.displayName ?: "Tên Facebook"
        val email = user?.email ?: "Không có email"
        val uid = user?.uid ?: return
        val userModel = UserModel(name, email)

        database.child("user").child(uid).setValue(userModel)
            .addOnSuccessListener { Log.d("Firebase", "Lưu user Facebook thành công") }
            .addOnFailureListener { Log.e("Firebase", "Lỗi lưu user Facebook", it) }
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
            val drawableEnd = editText.compoundDrawables[2]
            if (drawableEnd != null && event.action == MotionEvent.ACTION_UP) {
                val drawableWidth = drawableEnd.bounds.width()
                val extraPadding = editText.paddingEnd

                if (event.rawX >= (editText.right - drawableWidth - extraPadding)) {
                    isPasswordVisible = !isPasswordVisible

                    if (isPasswordVisible) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.transformationMethod = null
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(this, R.drawable.lock_svgrepo_com),
                            null,
                            ContextCompat.getDrawable(this, R.drawable.hear_no_evil_monkey_svgrepo_com),
                            null
                        )
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.transformationMethod = PasswordTransformationMethod.getInstance()
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(this, R.drawable.lock_svgrepo_com),
                            null,
                            ContextCompat.getDrawable(this, R.drawable.see_no_evil_monkey_svgrepo_com),
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

    // Handle activity result for Facebook login
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}