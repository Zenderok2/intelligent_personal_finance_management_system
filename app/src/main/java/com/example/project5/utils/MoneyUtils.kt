package com.example.project5.utils

import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    fun format(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("ru", "RU")).format(amount)
    }
}