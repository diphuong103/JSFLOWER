package com.example.jsflower.ImgBB

data class ImgBBResponse(
    val data: Data,
    val success: Boolean,
    val status: Int
)

data class Data(
    val url: String
)