package com.example.project5.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.edit
import com.example.project5.data.cloud.FirebaseAuthManager
import com.example.project5.data.cloud.FirestoreManager
import com.example.project5.data.local.BudgetDao
import com.example.project5.data.local.ReceiptDao
import com.example.project5.data.model.Budget
import com.example.project5.data.model.Receipt
import com.example.project5.ocr.ImageUtils
import com.example.project5.ocr.ReceiptParser
import com.example.project5.ocr.VisionApiClient
import com.example.project5.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

open class ReceiptRepository(
    private val receiptDao: ReceiptDao,
    private val budgetDao: BudgetDao,
    private val folderId: String,
    private val apiKey: String,
    context: Context
) {

    private val authManager by lazy { FirebaseAuthManager() }
    private val firestoreManager by lazy { FirestoreManager() }

    private val appContext = context.applicationContext

    suspend fun processReceiptImage(bitmap: Bitmap): Receipt? {
        val userId = authManager.getUserId() ?: return null

        return try {
            withContext(Dispatchers.IO) {

                val base64 = ImageUtils.toBase64(bitmap)

                val visionResponse = withTimeoutOrNull(15_000) {
                    VisionApiClient.recognize(base64, folderId, apiKey)
                } ?: return@withContext null

                val recognizedText = parseVisionText(visionResponse)
                if (recognizedText.isBlank()) return@withContext null

                val parseResult = withTimeoutOrNull(20_000) {
                    ReceiptParser.parseReceiptWithGPT(
                        recognizedText,
                        apiKey,
                        folderId
                    )
                } ?: return@withContext null

                val receipt = Receipt(
                    userId = userId,
                    total = parseResult.total,
                    date = System.currentTimeMillis(),
                    imagePath = "",
                    items = parseResult.products
                )

                saveReceiptInternal(receipt, userId)
            }

        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Ошибка обработки чека", e)
            null
        }
    }

    private suspend fun saveReceiptInternal(
        receipt: Receipt,
        userId: String
    ): Receipt = withContext(Dispatchers.IO) {

        val receiptWithUser = receipt.copy(userId = userId)

        val localId = receiptDao.insert(receiptWithUser)
        var savedReceipt = receiptWithUser.copy(id = localId)

        try {
            val firestoreId = firestoreManager.saveReceipt(userId, savedReceipt)

            savedReceipt = savedReceipt.copy(firestoreId = firestoreId)
            receiptDao.update(savedReceipt)

        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Cloud save failed", e)
        }

        clearSavedRating()
        savedReceipt
    }

    suspend fun saveReceipt(receipt: Receipt): Receipt {
        val userId = authManager.getUserId()
            ?: throw IllegalStateException("User not logged in")

        return saveReceiptInternal(receipt, userId)
    }

    private fun clearSavedRating() {
        appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(Constants.KEY_RATING)
            }
    }

    fun getAllReceipts(): Flow<List<Receipt>> {
        val userId = authManager.getUserId() ?: return flowOf(emptyList())
        return receiptDao.getReceiptsByUser(userId)
    }

    suspend fun getAll(): List<Receipt> {
        val userId = authManager.getUserId() ?: return emptyList()
        return receiptDao.getReceiptsByUser(userId).first()
    }

    suspend fun getTotalForPeriod(start: Long, end: Long): Double {
        val userId = authManager.getUserId() ?: return 0.0
        return receiptDao.getTotalBetween(userId, start, end) ?: 0.0
    }

    suspend fun getCategoryTotals(): Map<String, Double> {
        val receipts = getAll()

        return receipts
            .flatMap { it.items }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.price } }
    }

    open suspend fun getTotalByCategoryForPeriod(
        category: String,
        start: Long,
        end: Long
    ): Double {
        val receipts = getAll()
            .filter { it.date in start..end }

        return receipts
            .flatMap { it.items }
            .filter { it.category == category }
            .sumOf { it.price }
    }

    suspend fun clearAllReceipts() {
        val userId = authManager.getUserId() ?: return

        try {
            val receipts = firestoreManager.getReceipts(userId)

            coroutineScope {
                receipts
                    .mapNotNull { it.firestoreId }
                    .map { id ->
                        async {
                            runCatching {
                                firestoreManager.deleteReceipt(userId, id)
                            }.onFailure {
                                Log.e("ReceiptRepository", "Ошибка удаления $id", it)
                            }
                        }
                    }
                    .awaitAll()
            }

        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Ошибка удаления из облака", e)
        }

        receiptDao.deleteAllByUser(userId)
    }

    suspend fun getBudgetForCategory(category: String): Budget? {
        return budgetDao.getBudgetForCategory(category)
    }

    suspend fun syncFromCloud() {
        val userId = authManager.getUserId() ?: return

        try {
            val cloudReceipts = firestoreManager.getReceipts(userId)

            cloudReceipts.forEach { receipt ->
                val exists = receiptDao.existsByFirestoreId(receipt.firestoreId)
                if (!exists) {
                    receiptDao.insert(receipt.copy(userId = userId))
                }
            }

            Log.d("ReceiptRepository", "Sync success for user: $userId")

        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Ошибка синхронизации", e)
        }
    }

    private fun parseVisionText(raw: String): String {
        return try {
            val root = JSONObject(raw)

            val pages = root.optJSONArray("results")
                ?.optJSONObject(0)
                ?.optJSONArray("results")
                ?.optJSONObject(0)
                ?.optJSONObject("textDetection")
                ?.optJSONArray("pages")
                ?: return ""

            val sb = StringBuilder(1024)

            for (p in 0 until pages.length()) {
                val blocks = pages.optJSONObject(p)?.optJSONArray("blocks") ?: continue

                for (b in 0 until blocks.length()) {
                    val lines = blocks.optJSONObject(b)?.optJSONArray("lines") ?: continue

                    for (l in 0 until lines.length()) {
                        val words = lines.optJSONObject(l)?.optJSONArray("words") ?: continue

                        for (w in 0 until words.length()) {
                            val word = words.optJSONObject(w)?.optString("text")
                            if (!word.isNullOrBlank()) {
                                sb.append(word).append(' ')
                            }
                        }
                        sb.append('\n')
                    }
                }
            }

            sb.toString().trim()

        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Ошибка парсинга OCR JSON", e)
            ""
        }
    }
}