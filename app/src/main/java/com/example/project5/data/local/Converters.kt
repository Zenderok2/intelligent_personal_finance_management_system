package com.example.project5.data.local

import androidx.room.TypeConverter
import com.example.project5.data.model.ExpenseItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromExpenseItemList(value: List<ExpenseItem>?): String {
        return gson.toJson(value ?: emptyList<ExpenseItem>())
    }

    @TypeConverter
    fun toExpenseItemList(value: String?): List<ExpenseItem> {
        if (value.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<ExpenseItem>>() {}.type
            gson.fromJson<List<ExpenseItem>>(value, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}