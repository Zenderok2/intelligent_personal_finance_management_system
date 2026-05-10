package com.example.project5.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String = "",

    val firestoreId: String? = null,

    val date: Long = 0,
    val total: Double = 0.0,

    val items: List<ExpenseItem> = emptyList(),

    val imagePath: String = ""
)