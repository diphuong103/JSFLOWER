package com.example.jsflower.Model

data class CartItems(
    val flowerName: String? = null,
    val flowerPrice: String? = null,
    val flowerDescription: String? = null,
    val flowerImage: String? = null,
    val flowerQuantity: Int? = 1,
    val flowerIngredient: String? = null,
    val quantity: Int? = 1,
    val flowerKey: String? = null,
    var discountedPrice: String? = null
){

}
