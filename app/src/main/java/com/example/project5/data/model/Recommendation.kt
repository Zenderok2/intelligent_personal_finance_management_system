package com.example.project5.data.model

data class Recommendation(
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String,
    val priority: Int,
    val timestamp: Long = System.currentTimeMillis()
)