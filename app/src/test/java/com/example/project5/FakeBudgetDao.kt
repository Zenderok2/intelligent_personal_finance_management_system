package com.example.project5

import com.example.project5.data.local.BudgetDao
import com.example.project5.data.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeBudgetDao : BudgetDao {

    override fun getAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())

    override suspend fun getAllBudgetsOnce(): List<Budget> = emptyList()

    override suspend fun getBudgetForCategory(category: String): Budget? = null

    override suspend fun insert(budget: Budget) {}

    override suspend fun update(budget: Budget) {}
    override suspend fun delete(budget: Budget) {
        TODO("Not yet implemented")
    }
}