package com.example.jsflower.Model

data class BannerModel(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val isActive: Boolean = true
)