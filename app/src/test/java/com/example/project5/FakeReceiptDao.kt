package com.example.project5

import com.example.project5.data.local.ReceiptDao
import com.example.project5.data.model.Receipt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeReceiptDao : ReceiptDao {

    override fun getReceiptsByUser(userId: String): Flow<List<Receipt>> {
        return flowOf(emptyList())
    }

    override suspend fun getReceiptById(id: Long): Receipt? {
        TODO("Not yet implemented")
    }

    override suspend fun insert(receipt: Receipt): Long = 1

    override suspend fun update(receipt: Receipt) {}
    override suspend fun delete(receipt: Receipt) {
        TODO("Not yet implemented")
    }

    override fun getAllReceipts(): Flow<List<Receipt>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllByUser(userId: String) {}

    override suspend fun getTotalBetween(
        userId: String,
        start: Long,
        end: Long
    ): Double? = 0.0

    override suspend fun existsByFirestoreId(id: String?): Boolean = false
}