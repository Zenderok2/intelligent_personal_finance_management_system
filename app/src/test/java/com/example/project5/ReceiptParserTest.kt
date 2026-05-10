package com.example.project5

import com.example.project5.ocr.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Method

class ReceiptParserTest {

    @Test
    fun parseGptResponse_shouldParseCorrectJson() {
        val fakeJson = """
        {
          "result": {
            "alternatives": [
              {
                "message": {
                  "text": "{ \"total\": 150.0, \"products\": [{\"name\":\"Молоко\",\"price\":100,\"category\":\"Продукты\"}] }"
                }
              }
            ]
          }
        }
        """.trimIndent()

        val method: Method = ReceiptParser::class.java
            .getDeclaredMethod("parseGptResponse", String::class.java)

        method.isAccessible = true

        val result = method.invoke(ReceiptParser, fakeJson)

        val totalField = result.javaClass.getDeclaredField("total")
        totalField.isAccessible = true

        val total = totalField.getDouble(result)

        assertEquals(150.0, total, 0.01)
    }
}