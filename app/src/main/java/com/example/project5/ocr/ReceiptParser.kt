package com.example.project5.ocr

import com.example.project5.data.model.ExpenseItem
import org.json.JSONArray
import org.json.JSONObject

object ReceiptParser {

    private val VALID_CATEGORIES = listOf(
        "Продукты", "Одежда", "Электроника", "Рестораны", "Транспорт",
        "Развлечения", "Здоровье", "Красота", "Бытовая химия",
        "Хозтовары", "Другое"
    )

    suspend fun parseReceiptWithGPT(
        receiptText: String,
        apiKey: String,
        folderId: String
    ): ReceiptParseResult {

        val systemPrompt = """
Ты — парсер кассовых чеков.
Верни JSON:
{
  "total": number,
  "products": [
    {"name": string, "price": number, "category": string}
  ]
}
Запрет:
• Никаких пояснений.
• Никаких комментариев.
• Только JSON.
Категории: ${VALID_CATEGORIES.joinToString()}
""".trimIndent()

        val userPrompt = receiptText.take(5000)

        val body = JSONObject().apply {
            put("modelUri", "gpt://$folderId/yandexgpt-lite/latest")

            put("completionOptions", JSONObject().apply {
                put("temperature", 0)
                put("maxTokens", 1000)
            })

            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("text", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("text", userPrompt)
                })
            })
        }.toString()

        val raw = YandexGptClient.makeGptRequest(body, apiKey)

        return parseGptResponse(raw)
    }

    private fun parseGptResponse(json: String): ReceiptParseResult {
        val root = JSONObject(json)

        if (root.has("error")) {
            val err = root.getJSONObject("error").optString("message")
            throw Exception("GPT error: $err")
        }

        val text =
            root.getJSONObject("result")
                .getJSONArray("alternatives")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("text")

        val clean = extractJson(text)
        val obj = JSONObject(clean)

        val total = obj.optDouble("total", 0.0)
        val arr = obj.optJSONArray("products") ?: JSONArray()

        val items = mutableListOf<ExpenseItem>()

        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)

            val name = p.optString("name").trim()
            val price = p.optDouble("price", 0.0)
            var cat = p.optString("category", "Другое")

            if (name.isBlank() || price <= 0) continue
            if (cat !in VALID_CATEGORIES) cat = "Другое"

            items += ExpenseItem(
                name = name,
                price = price,
                category = cat
            )
        }

        return ReceiptParseResult(total, items.distinctBy { it.name.lowercase() })
    }

    private fun extractJson(text: String): String {
        val s = text.indexOf('{')
        val e = text.lastIndexOf('}') + 1
        if (s < 0 || e <= s) throw Exception("GPT не вернул JSON:\n$text")
        return text.substring(s, e)
    }
}

data class ReceiptParseResult(
    val total: Double,
    val products: List<ExpenseItem>
)