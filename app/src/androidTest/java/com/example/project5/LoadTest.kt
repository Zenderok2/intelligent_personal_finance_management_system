package com.example.project5

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.project5.data.local.AppDatabase
import com.example.project5.data.model.Receipt
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class LoadTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testLoadInsert() = runBlocking {

        val dao = db.receiptDao()

        val sizes = listOf(10, 50, 100)

        for (size in sizes) {

            val time = measureTimeMillis {

                repeat(size) {
                    dao.insert(
                        Receipt(
                            userId = "user1",
                            total = 100.0,
                            date = System.currentTimeMillis()
                        )
                    )
                }
            }

            println("SIZE: $size -> TIME: $time ms")
        }
    }
    
}