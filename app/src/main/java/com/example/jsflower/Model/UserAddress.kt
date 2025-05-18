package com.example.jsflower.Model

data class UserAddress(
    var id: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isDefault: Boolean = false
)