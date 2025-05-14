package com.example.jsflower

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.databinding.ActivityChatBinding
import com.example.jsflower.Adapter.ChatAdapter
import com.example.jsflower.Model.ChatModel
import com.example.jsflower.ImgBB.ImgBBUploadService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: ChatAdapter
    private val messages = mutableListOf<ChatModel>()
    private lateinit var database: DatabaseReference

    private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user_id"
    private var selectedFileUri: Uri? = null

    private val imgBBUploadService = ImgBBUploadService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        // Định nghĩa Adapter và RecyclerView
        messageAdapter = ChatAdapter(messages, currentUserId)
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.messagesRecyclerView.adapter = messageAdapter

        // Các sự kiện
        binding.backButton.setOnClickListener { finish() }
        binding.attachButton.setOnClickListener { openGallery() }
        binding.sendButton.setOnClickListener { sendMessageOrImage() }

        loadMessages()
        markMessagesAsRead()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun loadMessages() {
        // Lắng nghe tin nhắn từ Firebase - sắp xếp theo timestamp
        database.child("chats").child(currentUserId).child("messages")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    snapshot.children.forEach {
                        val message = it.getValue(ChatModel::class.java)
                        if (message != null) {
                            messages.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged() // Cập nhật RecyclerView
                    if (messages.isNotEmpty()) {
                        binding.messagesRecyclerView.scrollToPosition(messages.size - 1) // Cuộn xuống cuối
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markMessagesAsRead() {
        database.child("chats").child(currentUserId).child("messages")
            .orderByChild("isRead")
            .equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(ChatModel::class.java)
                        if (message != null && message.userId == "admin") {
                            messageSnapshot.ref.child("isRead").setValue(true)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to update message status", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessageOrImage() {
        val messageText = binding.messageInput.text.toString().trim()
        if (selectedFileUri != null) {
            uploadImageAndSend()
        } else if (messageText.isNotEmpty()) {
            sendTextMessage(messageText)
        }
    }

    private fun sendTextMessage(text: String) {
        val chatMessage = ChatModel(
            userId = "client", // Role là client
            senderId = currentUserId, // ID của user hiện tại
            message = text,
            imageUrl = "",
            timestamp = System.currentTimeMillis(),
            isRead = false  // New message from client
        )

        // Lưu tin nhắn vào Firebase
        database.child("chats").child(currentUserId).child("messages").push().setValue(chatMessage)
            .addOnSuccessListener {
                binding.messageInput.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageAndSend() {
        selectedFileUri?.let { uri ->
            binding.sendButton.isEnabled = false
            imgBBUploadService.uploadImage(this, uri, object : ImgBBUploadService.UploadCallback {
                override fun onSuccess(imageUrl: String) {
                    val chatMessage = ChatModel(
                        userId = "client", // Role là client
                        senderId = currentUserId, // ID của user hiện tại
                        message = "",
                        imageUrl = imageUrl,
                        timestamp = System.currentTimeMillis(),
                        isRead = false  // New message from client
                    )

                    database.child("chats").child(currentUserId).child("messages").push().setValue(chatMessage)
                        .addOnSuccessListener {
                            binding.messageInput.text.clear()
                            selectedFileUri = null
                            binding.sendButton.isEnabled = true
                            Toast.makeText(this@ChatActivity, "Image sent", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            binding.sendButton.isEnabled = true
                            Toast.makeText(this@ChatActivity, "Failed to send image", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onFailure(errorMessage: String) {
                    binding.sendButton.isEnabled = true
                    Toast.makeText(this@ChatActivity, "Upload failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                binding.messageInput.setText("Image selected")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Mark messages as read when returning to chat
        markMessagesAsRead()
    }
}