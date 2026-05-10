package com.example.project5

import com.example.project5.domain.budget.BudgetManager
import com.example.project5.data.local.BudgetDao
import com.example.project5.data.model.Budget
import com.example.project5.data.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class BudgetManagerTest {

    private val fakeBudgetDao = object : BudgetDao {
        override fun getAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())
        override suspend fun getAllBudgetsOnce(): List<Budget> = emptyList()
        override suspend fun getBudgetForCategory(category: String): Budget? = null
        override suspend fun insert(budget: Budget) {}
        override suspend fun update(budget: Budget) {}
        override suspend fun delete(budget: Budget) {
            TODO("Not yet implemented")
        }
    }

    private val fakeReceiptRepo = object : ReceiptRepository(
        receiptDao = FakeReceiptDao(),
        budgetDao = fakeBudgetDao,
        folderId = "",
        apiKey = "",
        context = throw RuntimeException("Not used")
    ) {
        override suspend fun getTotalByCategoryForPeriod(
            category: String,
            start: Long,
            end: Long
        ): Double {
            return 900.0
        }
    }

    private val manager = BudgetManager(fakeBudgetDao, fakeReceiptRepo)

    @Test
    fun isOverLimit_shouldReturnTrue_whenExceeded() {
        assertTrue(manager.isOverLimit(1200.0, 1000.0))
    }

    @Test
    fun isNearLimit_shouldReturnTrue_whenAbove85Percent() {
        assertTrue(manager.isNearLimit(900.0, 1000.0))
    }

    @Test
    fun isNearLimit_shouldReturnFalse_whenBelowThreshold() {
        assertFalse(manager.isNearLimit(500.0, 1000.0))
    }
}