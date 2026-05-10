package com.example.project5

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.project5.data.local.AppDatabase
import com.example.project5.data.repository.ReceiptRepository
import com.example.project5.data.model.Receipt
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReceiptRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ReceiptRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repo = ReceiptRepository(
            db.receiptDao(),
            db.budgetDao(),
            "testFolder",
            "testApiKey",
            context
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Тест: сохранение и получение
    @Test
    fun testSaveAndGet() = runBlocking {

        val receipt = Receipt(
            userId = "user1",
            total = 300.0,
            date = System.currentTimeMillis()
        )

        repo.saveReceipt(receipt)

        val list = repo.getAll()

        assertTrue(list.isNotEmpty())
    }

    // Тест: сумма
    @Test
    fun testTotalForPeriod() = runBlocking {

        val now = System.currentTimeMillis()

        repo.saveReceipt(Receipt(userId = "user1", total = 100.0, date = now))
        repo.saveReceipt(Receipt(userId = "user1", total = 50.0, date = now))

        val total = repo.getTotalForPeriod(now - 1000, now + 1000)

        assertTrue(total >= 150.0)
    }
}