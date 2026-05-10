package com.example.project5.domain.forecast

import com.example.project5.data.repository.ReceiptRepository
import com.example.project5.ocr.YandexGptClient
import com.example.project5.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ForecastManager(
    private val receiptRepo: ReceiptRepository,
    private val apiKey: String,
    private val folderId: String
) {

    // Основной прогноз
    suspend fun predictNextMonthSpending(): Double = withContext(Dispatchers.IO) {

        val receipts = receiptRepo.getAllReceipts().first()

        if (receipts.isEmpty()) return@withContext 0.0

        // если чеков мало => простой прогноз
        if (receipts.size < 3) {
            return@withContext receipts.sumOf { it.total }
        }

        val monthlyTotals = receipts
            .groupBy { DateUtils.getYearMonth(it.date) }
            .toSortedMap()
            .map { (_, list) -> list.sumOf { it.total } }

        // если только 1 месяц => считаем по среднему чеку
        if (monthlyTotals.size < 2) {
            val avgCheck = receipts.map { it.total }.average()
            return@withContext avgCheck * 30
        }

        // взвешенный прогноз
        val weights = monthlyTotals.mapIndexed { index, _ -> index + 1 }

        val weightedSum = monthlyTotals.mapIndexed { index, value ->
            value * weights[index]
        }.sum()

        val weightTotal = weights.sum()

        weightedSum / weightTotal
    }

    // AI прогноз
    suspend fun predictWithAI(): String = withContext(Dispatchers.IO) {
        try {
            val receipts = receiptRepo.getAllReceipts().first()

            if (receipts.isEmpty()) {
                return@withContext "Нет данных для прогноза 😢"
            }

            // проверка по количеству чеков
            if (receipts.size < 3) {
                val total = receipts.sumOf { it.total }.roundToInt()

                return@withContext """
Прогноз: $total ₽
Комментарий: Недостаточно данных для точного анализа.
"""
            }

            // группировка по месяцам
            val monthlyTotals = receipts
                .groupBy { DateUtils.getYearMonth(it.date) }
                .toSortedMap()
                .map { (_, list) -> list.sumOf { it.total } }

            val historyText = monthlyTotals.mapIndexed { index, value ->
                "Месяц ${index + 1}: ${value.roundToInt()}₽"
            }.joinToString("\n")

            val checksInfo = """
Всего чеков: ${receipts.size}
Общая сумма: ${receipts.sumOf { it.total }.roundToInt()}₽
Средний чек: ${receipts.map { it.total }.average().roundToInt()}₽
"""

            val prompt = """
Ты финансовый аналитик. Дай точный прогноз.

Данные пользователя:
$checksInfo

Расходы по месяцам:
$historyText

Правила:
- НЕ пиши, что данных мало
- НЕ задавай вопросов
- Используй даже небольшие данные
- Дай конкретный прогноз

Формат:
Прогноз: XXXX ₽
Комментарий: ...
"""

            YandexGptClient.generateText(prompt, apiKey, folderId)

        } catch (e: Exception) {
            "Не удалось построить прогноз 😢"
        }
    }

    // Проверка перерасхода
    suspend fun isOverspending(budgetLimit: Double): Boolean {
        val start = DateUtils.startOfMonth(System.currentTimeMillis(), 0)
        val end = DateUtils.endOfMonth(System.currentTimeMillis(), 0)
        val spent = receiptRepo.getTotalForPeriod(start, end)
        return spent > budgetLimit
    }
}