package com.example.project5.domain.rating

import android.util.Log
import com.example.project5.data.repository.ReceiptRepository
import com.example.project5.ocr.YandexGptClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RatingCalculator(
    private val receiptRepo: ReceiptRepository,
    private val apiKey: String,
    private val folderId: String
) {

    companion object {
        private const val TAG = "RATING_AI"
    }

    suspend fun calculateRatingWithAI(): Int = withContext(Dispatchers.IO) {
        try {
            val receipts = receiptRepo.getAll()

            if (receipts.isEmpty()) {
                return@withContext 0
            }

            val total = receipts.sumOf { it.total }
            val count = receipts.size

            val prompt = """
                Оцени финансовую дисциплину пользователя от 0 до 100.
                
                Данные:
                - Количество чеков: $count
                - Общая сумма расходов: $total
                
                Верни ТОЛЬКО число от 0 до 100 без текста.
            """.trimIndent()

            Log.d(TAG, "🧠 PROMPT:\n$prompt")

            val response = YandexGptClient.generateText(
                prompt = prompt,
                apiKey = apiKey,
                folderId = folderId
            )

            Log.d(TAG, "📥 GPT RESPONSE: $response")

            val value = response.trim().filter { it.isDigit() }.toIntOrNull()

            if (value == null) {
                Log.e(TAG, "❌ Не удалось распарсить число")
                return@withContext fallbackRating(count, total)
            }

            return@withContext value.coerceIn(0, 100)

        }

        catch (e: Exception) {
            Log.e(TAG, "💥 GPT ERROR", e)

            val receipts = receiptRepo.getAll()
            val total = receipts.sumOf { it.total }

            return@withContext fallbackRating(receipts.size, total)
        }
    }

    private fun fallbackRating(count: Int, total: Double): Int {
        if (count == 0) return 0

        return when {
            total < 100 -> 90
            total < 300 -> 75
            total < 700 -> 60
            else -> 40
        }
    }
}