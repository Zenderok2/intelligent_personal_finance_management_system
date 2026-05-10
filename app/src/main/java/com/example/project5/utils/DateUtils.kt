package com.example.project5.utils

import java.util.Calendar

object DateUtils {
    fun startOfMonth(reference: Long, monthsAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = reference
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -monthsAgo)
        return cal.timeInMillis
    }

    fun endOfMonth(reference: Long, monthsAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = reference
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        cal.add(Calendar.MONTH, -monthsAgo)
        return cal.timeInMillis
    }

    fun getYearMonth(timestamp: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        return "$year-${"%02d".format(month)}"
    }
}