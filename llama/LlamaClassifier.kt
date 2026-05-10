package com.example.project5.llama

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class LlamaClassifier(private val context: Context) {
    
    private var python: Python? = null
    private var llamaWrapper: com.chaquo.python.PyObject? = null
    private var isInitialized = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Инициализируем Python
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            python = Python.getInstance()
            
            // Копируем модель из assets
            val modelPath = copyModelFromAssets()
            
            // Импортируем и инициализируем наш Python модуль
            val module = python?.getModule("llama_wrapper")
            llamaWrapper = module?.callAttr("LlamaWrapper", modelPath)
            
            // Загружаем модель
            val success = llamaWrapper?.callAttr("load_model")?.toBoolean() ?: false
            isInitialized = success
            
            return@withContext success
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    suspend fun classifyProducts(productNames: List<String>): List<ProductClassification> = 
        withContext(Dispatchers.IO) {
            if (!isInitialized) {
                throw IllegalStateException("Модель не инициализирована")
            }
            
            try {
                val result = llamaWrapper?.callAttr("classify_products", productNames)
                return@withContext parsePythonResult(result, productNames)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }
        }
    
    private fun copyModelFromAssets(): String {
        val modelsDir = context.filesDir.resolve("models")
        modelsDir.mkdirs()
        
        val modelFile = modelsDir.resolve("phi-3-mini-4k-instruct-q4_k_m.gguf")
        
        if (!modelFile.exists()) {
            context.assets.open("models/phi-3-mini-4k-instruct-q4_k_m.gguf").use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        return modelFile.absolutePath
    }
    
    private fun parsePythonResult(result: com.chaquo.python.PyObject?, originalNames: List<String>): List<ProductClassification> {
        return try {
            if (result == null) return emptyList()
            
            val classifications = mutableListOf<ProductClassification>()
            for (i in 0 until result.length()) {
                val item = result[i]
                classifications.add(
                    ProductClassification(
                        original_name = item["original_name"].toString(),
                        category = item["category"].toString()
                    )
                )
            }
            classifications
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun isReady(): Boolean = isInitialized
}

data class ProductClassification(
    val original_name: String,
    val category: String
)