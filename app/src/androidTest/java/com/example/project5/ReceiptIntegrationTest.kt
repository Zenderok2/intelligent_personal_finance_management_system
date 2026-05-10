package com.example.project5

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.project5.data.local.AppDatabase
import com.example.project5.data.local.ReceiptDao
import com.example.project5.data.model.Receipt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReceiptIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var receiptDao: ReceiptDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        receiptDao = db.receiptDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Тест 1: вставка и получение
    @Test
    fun testInsertAndGetReceipt() = runBlocking {

        val receipt = Receipt(
            userId = "user1",
            total = 100.0,
            date = System.currentTimeMillis()
        )

        receiptDao.insert(receipt)

        val list = receiptDao.getAllReceipts().first()

        assertTrue(list.isNotEmpty())
        assertEquals(100.0, list[0].total, 0.01)
    }

    // Тест 2: удаление
    @Test
    fun testDeleteReceipt() = runBlocking {

        val receipt = Receipt(
            userId = "user1",
            total = 200.0,
            date = System.currentTimeMillis()
        )

        val id = receiptDao.insert(receipt)
        val saved = receipt.copy(id = id)

        receiptDao.delete(saved)

        val list = receiptDao.getAllReceipts().first()

        assertTrue(list.isEmpty())
    }

    // Тест 3: сумма за период
    @Test
    fun testGetTotalBetween() = runBlocking {

        val now = System.currentTimeMillis()

        val r1 = Receipt(userId = "user1", total = 100.0, date = now)
        val r2 = Receipt(userId = "user1", total = 50.0, date = now)

        receiptDao.insert(r1)
        receiptDao.insert(r2)

        val total = receiptDao.getTotalBetween("user1", now - 1000, now + 1000)

        assertEquals(150.0, total ?: 0.0, 0.01)
    }
}