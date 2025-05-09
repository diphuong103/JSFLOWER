package com.example.jsflower.Model

data class ReviewModel(
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val date: String = "",
    val images: List<String> = emptyList()
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", 0f, "", "", emptyList())
}