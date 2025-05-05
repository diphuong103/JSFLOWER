package com.example.jsflower.ImgBB

import com.google.gson.annotations.SerializedName

data class ImgBBResponse(
    val data: ImgBBData,
    val success: Boolean,
    val status: Int
)

data class ImgBBData(
    val id: String,
    val title: String?,
    val url: String,
    @SerializedName("display_url")
    val displayUrl: String,
    val size: Int,
    val time: Long,
    val expiration: Long,
    val delete_url: String
)