package com.example.jsflower.Model

data class MenuItem(
    val flowerName: String? = null,
    val flowerPrice: String? = null,
    val flowerDescription: String? = null,
    val flowerImage: String? = null,
    val flowerIngredient: String? = null,
    var key: String = "",
    var categoryId: String? = null,
    val tags: String ?=null
)
