package com.example.project5.domain.budget

import com.example.project5.data.local.BudgetDao
import com.example.project5.data.model.Budget
import com.example.project5.data.repository.ReceiptRepository
import com.example.project5.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class BudgetManager(
    private val budgetDao: BudgetDao,
    private val receiptRepo: ReceiptRepository
) {

    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun addOrUpdateBudget(budget: Budget) {
        val existing = budgetDao.getBudgetForCategory(budget.category)

        if (existing != null) {
            budgetDao.update(existing.copy(limit = budget.limit))
        } else {
            budgetDao.insert(budget)
        }
    }

    suspend fun getBudgetStatus(category: String): BudgetStatus? {
        val budget = budgetDao.getBudgetForCategory(category) ?: return null
        return calculateStatus(budget)
    }

    suspend fun getAllBudgetStatuses(): List<BudgetStatus> {
        val budgets = budgetDao.getAllBudgetsOnce()
        return budgets.map { calculateStatus(it) }
    }

    // единая функция расчёта
    private suspend fun calculateStatus(budget: Budget): BudgetStatus {
        val start = DateUtils.startOfMonth(System.currentTimeMillis(), 0)
        val end = DateUtils.endOfMonth(System.currentTimeMillis(), 0)

        val spent = receiptRepo.getTotalByCategoryForPeriod(
            budget.category,
            start,
            end
        )

        val percentage = if (budget.limit > 0) {
            (spent / budget.limit) * 100
        } else {
            0.0
        }

        val isExceeded = isOverLimit(spent, budget.limit)
        val isNearLimit = isNearLimit(spent, budget.limit)

        return BudgetStatus(
            budget = budget,
            spent = spent,
            percentage = percentage,
            isExceeded = isExceeded,
            isNearLimit = isNearLimit
        )
    }

    fun isOverLimit(spent: Double, limit: Double): Boolean {
        return limit > 0 && spent > limit
    }

    fun isNearLimit(spent: Double, limit: Double): Boolean {
        return limit > 0 && spent >= limit * 0.85
    }
}

data class BudgetStatus(
    val budget: Budget,
    val spent: Double,
    val percentage: Double,
    val isExceeded: Boolean,
    val isNearLimit: Boolean
)