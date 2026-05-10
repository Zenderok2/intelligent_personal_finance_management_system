package com.example.project5.data.local

import androidx.room.*
import com.example.project5.data.model.Receipt
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    // Вставка
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: Receipt): Long

    // Обновление
    @Update
    suspend fun update(receipt: Receipt)

    // Удаление одного чека
    @Delete
    suspend fun delete(receipt: Receipt)

    // Все чеки
    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    // Чеки пользователя
    @Query("SELECT * FROM receipts WHERE userId = :userId ORDER BY date DESC")
    fun getReceiptsByUser(userId: String): Flow<List<Receipt>>

    // Один чек
    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun getReceiptById(id: Long): Receipt?

    // Сумма за период
    @Query("""
        SELECT SUM(total) FROM receipts 
        WHERE userId = :userId AND date BETWEEN :start AND :end
    """)
    suspend fun getTotalBetween(
        userId: String,
        start: Long,
        end: Long
    ): Double?

    // Удалить все чеки пользователя
    @Query("DELETE FROM receipts WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    // Проверка существования по firestoreId
    @Query("SELECT COUNT(*) > 0 FROM receipts WHERE firestoreId = :id")
    suspend fun existsByFirestoreId(id: String?): Boolean
}