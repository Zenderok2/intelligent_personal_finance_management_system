package com.example.project5

import com.example.project5.utils.MoneyUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyUtilsTest {

    @Test
    fun formatShouldReturnCurrencyString() {
        val result = MoneyUtils.format(1234.5)

        assertTrue(result.contains("₽"))
        assertTrue(result.isNotEmpty())
    }



}

