package com.example.project5.domain.model

data class ReceiptParseResult(
    val total: Double,
    val products: List<Product>
)

data class Product(
    val name: String,
    val price: Double,
    val category: String
)