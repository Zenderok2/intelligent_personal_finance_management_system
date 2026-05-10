package com.example.project5.data.cloud

import android.util.Log
import com.example.project5.data.model.Receipt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreManager {

    private val db = FirebaseFirestore.getInstance()

    suspend fun saveReceipt(userId: String, receipt: Receipt): String {
        val doc = db.collection("users")
            .document(userId)
            .collection("receipts")
            .document()

        doc.set(receipt).await()

        return doc.id

    }

    fun deleteReceipt(userId: String, firestoreId: String) {
        db.collection("users")
            .document(userId)
            .collection("receipts")
            .document(firestoreId)
            .delete()
    }

    suspend fun getReceipts(userId: String): List<Receipt> {
        return try {

            val result = db.collection("users")
                .document(userId)
                .collection("receipts")
                .get()
                .await()

            Log.d("FIRESTORE", "Docs from cloud: ${result.size()}")

            result.mapNotNull { doc ->
                try {
                    doc.toObject(Receipt::class.java)
                        ?.copy(firestoreId = doc.id)
                } catch (e: Exception) {
                    Log.e("FIRESTORE", "Mapping error: ${e.message}")
                    null
                }
            }

        } catch (e: Exception) {
            Log.e("FIRESTORE", "Load error: ${e.message}")
            emptyList()
        }
    }
}