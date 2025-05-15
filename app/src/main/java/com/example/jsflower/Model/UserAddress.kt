package com.example.jsflower.Model

data class UserAddress(
    val id: String = "",
    val userId: String = "",
    val address: String = "",
    val isDefault: Boolean = false,
    val recipientName: String = "",
    val phoneNumber: String = ""
)