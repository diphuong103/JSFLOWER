package com.example.jsflower.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.jsflower.ImgBB.FileUtils
import com.example.jsflower.ImgBB.ImgBBApi
import com.example.jsflower.ImgBB.ImgBBResponse
import com.example.jsflower.R
import com.example.jsflower.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var isEditing = false
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val IMGBB_API_KEY = "d5e2f70fce99fcedeb7507ebc4eb1aed"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setEditTextsEnabled(false)
        loadUserData()

        binding.editAvatarButton.visibility = View.GONE
        setEditTextsEnabled(false)
        loadUserData()

        binding.editButton.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                setEditTextsEnabled(true)
                binding.editAvatarButton.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Bạn có thể chỉnh sửa thông tin", Toast.LENGTH_SHORT).show()
            } else {
                saveUserData()
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            if (isEditing) saveUserData()
            else Toast.makeText(requireContext(), "Nhấn nút chỉnh sửa để thay đổi thông tin", Toast.LENGTH_SHORT).show()
        }

        binding.editAvatarButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        return binding.root
    }

    private fun setEditTextsEnabled(enabled: Boolean) {
        binding.etName.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etAddress.isEnabled = enabled
        binding.etEmail.isEnabled = false
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        database.child("user").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""
                    val imageUri = snapshot.child("imageUrl").getValue(String::class.java) ?: ""
                    val email = user.email ?: ""

                    binding.etName.setText(name)
                    binding.etPhone.setText(phone)
                    binding.etAddress.setText(address)
                    binding.etEmail.setText(email)

                    if (imageUri.isNotEmpty()) {
                        Glide.with(requireContext()).load(imageUri).into(binding.profileAvatar)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveUserData() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        // Nếu có chọn ảnh mới, upload ảnh và lấy URL
        if (selectedImageUri != null) {
            uploadImageWithRetrofit(selectedImageUri!!)
        } else {
            // Nếu không có ảnh mới, lưu thông tin mà không thay đổi ảnh
            val userMap = mapOf(
                "name" to name,
                "phone" to phone,
                "address" to address
            )

            database.child("user").child(userId).updateChildren(userMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    setEditTextsEnabled(false)
                    isEditing = false
                    binding.editButton.setImageResource(R.drawable.edit)
                    binding.editAvatarButton.visibility = View.GONE
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                selectedImageUri = uri
                binding.profileAvatar.setImageURI(uri)
            } else {
                Toast.makeText(requireContext(), "Không thể lấy ảnh đã chọn", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageWithRetrofit(uri: Uri) {
        val filePath = FileUtils.getPath(requireContext(), uri)
        if (filePath == null) {
            Toast.makeText(requireContext(), "Không thể lấy đường dẫn ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestBody)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.imgbb.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ImgBBApi::class.java)

        api.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.data?.url
                    saveUserDataWithImageUrl(imageUrl)
                } else {
                    Toast.makeText(requireContext(), "Lỗi upload ảnh", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Upload thất bại: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserDataWithImageUrl(imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return

        val user = auth.currentUser ?: return
        val userId = user.uid
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        val userMap = mapOf(
            "name" to name,
            "phone" to phone,
            "address" to address,
            "imageUrl" to imageUrl // Lưu URL ảnh vào Firebase
        )

        database.child("user").child(userId).updateChildren(userMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cập nhật thông tin và ảnh đại diện thành công", Toast.LENGTH_SHORT).show()
                setEditTextsEnabled(false)
                isEditing = false
                binding.editButton.setImageResource(R.drawable.edit)
                binding.editAvatarButton.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
