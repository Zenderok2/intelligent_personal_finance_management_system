package com.example.project5.domain.recommendations

import android.util.Log
import com.example.project5.data.model.Recommendation
import com.example.project5.data.repository.ReceiptRepository
import com.example.project5.domain.budget.BudgetManager
import com.example.project5.domain.budget.BudgetStatus
import com.example.project5.ocr.YandexGptClient
import com.example.project5.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class RecommendationEngine(
    private val receiptRepo: ReceiptRepository,
    private val budgetManager: BudgetManager,
    private val apiKey: String,
    private val folderId: String
) {

    suspend fun generateRecommendations(): List<Recommendation> = withContext(Dispatchers.IO) {

        val recommendations = mutableListOf<Recommendation>()

        val now = System.currentTimeMillis()
        val startThisMonth = DateUtils.startOfMonth(now, 0)
        val endThisMonth = DateUtils.endOfMonth(now, 0)
        val startLastMonth = DateUtils.startOfMonth(now, 1)
        val endLastMonth = DateUtils.endOfMonth(now, 1)

        // Получаем данные параллельно
        val spentThis = receiptRepo.getTotalForPeriod(startThisMonth, endThisMonth)
        val spentLast = receiptRepo.getTotalForPeriod(startLastMonth, endLastMonth)
        val categories = receiptRepo.getCategoryTotals()
        val budgetStatuses = budgetManager.getAllBudgetStatuses()

        // Общая динамика
        if (spentLast > 0) {
            val change = ((spentThis - spentLast) / spentLast) * 100

            when {
                change > 20 -> recommendations.add(
                    Recommendation(
                        title = "Рост расходов",
                        message = "Расходы выросли на ${"%.0f".format(change)}%",
                        type = "warning",
                        priority = 2
                    )
                )

                change < -10 -> recommendations.add(
                    Recommendation(
                        title = "Отлично!",
                        message = "Вы сократили расходы на ${"%.0f".format(abs(change))}%",
                        type = "achievement",
                        priority = 4
                    )
                )
            }
        }

        // Анализ категорий
        categories.forEach { (category, amount) ->
            if (spentThis > 0 && amount > spentThis * 0.4) {
                recommendations.add(
                    Recommendation(
                        title = "Высокие траты",
                        message = "$category занимает большую часть бюджета (${amount.toInt()} ₽)",
                        type = "warning",
                        priority = 3
                    )
                )
            }
        }

        // Проверка бюджетов
        budgetStatuses.forEach { status ->
            when {
                status.isExceeded -> recommendations.add(
                    Recommendation(
                        title = "Превышен бюджет",
                        message = "Вы превысили лимит по ${status.budget.category}",
                        type = "danger",
                        priority = 1
                    )
                )

                status.isNearLimit -> recommendations.add(
                    Recommendation(
                        title = "Почти лимит",
                        message = "Вы почти достигли лимита по ${status.budget.category}",
                        type = "warning",
                        priority = 2
                    )
                )
            }
        }

        recommendations
            .distinctBy { it.title }
            .sortedByDescending { it.priority }
    }

    /**
     * Генерация prompt для AI
     */
    private fun buildPrompt(
        budgets: List<BudgetStatus>,
        categories: Map<String, Double>
    ): String {
        val categoryText = categories.entries.joinToString("\n") {
            "- ${it.key}: ${it.value.toInt()} ₽"
        }

        val budgetText = budgets.joinToString("\n") {
            "- ${it.budget.category}: ${it.spent.toInt()}/${it.budget.limit.toInt()} ₽"
        }

        return """
Ты финансовый ассистент.

Категории трат:
$categoryText

Бюджеты:
$budgetText

Дай 3 кратких совета по оптимизации расходов.
Ответ в виде списка.
""".trimIndent()
    }

    /**
     * AI рекомендации
     */
    suspend fun generateAIRecommendations(): String = withContext(Dispatchers.IO) {
        try {
            val budgets = budgetManager.getAllBudgetStatuses()
            val categories = receiptRepo.getCategoryTotals()

            if (categories.isEmpty()) {
                return@withContext "Недостаточно данных для рекомендаций"
            }

            val prompt = buildPrompt(budgets, categories)

            YandexGptClient.generateText(prompt, apiKey, folderId)

        } catch (e: Exception) {
            Log.e("RecommendationEngine", "AI error", e)
            "Не удалось получить рекомендации 😢"
        }
    }
}