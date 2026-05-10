package com.example.project5.domain.achievements

import com.example.project5.data.model.Achievement
import com.example.project5.data.repository.ReceiptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AchievementManager(private val receiptRepo: ReceiptRepository) {

    suspend fun checkAchievements(): List<Achievement> = withContext(Dispatchers.IO) {

        val receipts = receiptRepo.getAll()
        val achievements = mutableListOf<Achievement>()

        val totalSpent = receipts.sumOf { it.total }
        val totalCount = receipts.size

        val categories = receipts
            .flatMap { it.items }
            .map { it.category }
            .distinct()

        var id = 1L

        // Чеки
        if (totalCount >= 1) {
            achievements.add(Achievement(id++, "Первый чек", "Вы добавили первый чек"))
        }
        if (totalCount >= 5) {
            achievements.add(Achievement(id++, "Новичок", "Добавлено 5 чеков"))
        }
        if (totalCount >= 20) {
            achievements.add(Achievement(id++, "Опытный", "Добавлено 20 чеков"))
        }
        if (totalCount >= 50) {
            achievements.add(Achievement(id++, "Финансовый мастер", "Добавлено 50 чеков"))
        }

        // Траты
        if (totalSpent >= 10_000) {
            achievements.add(Achievement(id++, "Первые траты", "Потрачено более 10 000 ₽"))
        }
        if (totalSpent >= 50_000) {
            achievements.add(Achievement(id++, "Активный покупатель", "Потрачено более 50 000 ₽"))
        }
        if (totalSpent >= 100_000) {
            achievements.add(Achievement(id++, "Транжира", "Потрачено более 100 000 ₽"))
        }

        // Категории
        if (categories.size >= 3) {
            achievements.add(Achievement(id++, "Разнообразие", "Траты в 3 категориях"))
        }
        if (categories.size >= 5) {
            achievements.add(Achievement(id++, "Широкий размах", "Траты в 5 категориях"))
        }

        // Активность
        val uniqueDays = receipts
            .map { it.date / (1000 * 60 * 60 * 24) }
            .distinct()

        if (uniqueDays.size >= 3) {
            achievements.add(Achievement(id++, "Постоянство", "3 дня с чеками"))
        }
        if (uniqueDays.size >= 7) {
            achievements.add(Achievement(id++, "Неделя контроля", "7 дней активности"))
        }

        // Средний чек
        val avg = if (totalCount > 0) totalSpent / totalCount else 0.0

        if (avg <= 1_000 && totalCount >= 5) {
            achievements.add(Achievement(id++, "Экономный", "Средний чек < 1 000 ₽"))
        }

        if (avg >= 5_000 && totalCount >= 5) {
            achievements.add(Achievement(id++, "Любитель трат", "Средний чек > 5 000 ₽"))
        }

        achievements
    }
}