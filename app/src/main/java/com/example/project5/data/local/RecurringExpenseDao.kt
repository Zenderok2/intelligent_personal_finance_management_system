package com.example.project5.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.project5.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Insert
    suspend fun insert(expense: RecurringExpense)

    @Query("SELECT * FROM recurring_expenses")
    fun getAll(): Flow<List<RecurringExpense>>

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}