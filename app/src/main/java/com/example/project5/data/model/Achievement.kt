package com.example.project5.data.model

data class Achievement(
    val id: Long = 0,
    val title: String,
    val description: String,
    val iconRes: Int? = null,
    val unlockedAt: Long? = null
)