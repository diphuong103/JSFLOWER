package com.example.jsflower.Model

data class MenuItem(
    val flowerName: String? = null,
    val flowerPrice: String? = null,
    val flowerDescription: String? = null,
    val flowerImage: String? = null,
    val flowerIngredient: String? = null,
    var key: String = "",
    val tags: String? = null,
    var discountedPrice: String? = null,
    var categoryId: String? = null,
    var categoryName: String? = null,

)
