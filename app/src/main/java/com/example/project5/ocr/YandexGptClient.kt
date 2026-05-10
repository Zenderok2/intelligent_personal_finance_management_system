package com.example.project5.ocr

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YandexGptClient {

    private const val TAG = "GPT_DEBUG"

    private const val ENDPOINT =
        "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun makeGptRequest(
        requestBodyJson: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {


        if (apiKey.isBlank()) {
            Log.e(TAG, "❌ API KEY ПУСТОЙ")
            throw IllegalArgumentException("GPT API KEY пуст")
        }

        Log.d(TAG, "📤 REQUEST JSON:\n$requestBodyJson")

        val body = requestBodyJson
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Api-Key $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->

                val raw = response.body?.string()

                Log.d(TAG, "📥 RESPONSE CODE: ${response.code}")
                Log.d(TAG, "📥 RAW RESPONSE:\n$raw")

                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ HTTP ERROR ${response.code}")
                    throw Exception("GPT API error ${response.code}:\n$raw")
                }

                if (raw.isNullOrBlank()) {
                    Log.e(TAG, "❌ ПУСТОЙ ОТВЕТ")
                    throw Exception("GPT вернул пустой ответ")
                }

                val root = JSONObject(raw)

                if (root.has("error")) {
                    val err = root.getJSONObject("error")
                        .optString("message", "Unknown error")

                    Log.e(TAG, "❌ GPT ERROR: $err")
                    throw Exception("GPT error: $err")
                }

                return@withContext raw
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 EXCEPTION: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun generateText(
        prompt: String,
        apiKey: String,
        folderId: String
    ): String = withContext(Dispatchers.IO) {

        Log.d(TAG, "📂 FOLDER ID = $folderId")
        Log.d(TAG, "🧠 PROMPT = $prompt")

        if (folderId.isBlank()) {
            Log.e(TAG, "❌ FOLDER ID ПУСТОЙ")
            throw IllegalArgumentException("GPT FOLDER ID пуст")
        }

        val requestJson = JSONObject().apply {
            put("modelUri", "gpt://$folderId/yandexgpt-lite/latest")

            put("completionOptions", JSONObject().apply {
                put("stream", false)
                put("temperature", 0.6)
                put("maxTokens", 200)
            })

            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("text", "Ты финансовый ассистент.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("text", prompt)
                })
            })
        }

        val raw = makeGptRequest(requestJson.toString(), apiKey)

        val root = JSONObject(raw)

        // Парсинг
        val result = root
            .getJSONObject("result")
            .getJSONArray("alternatives")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("text")

        Log.d(TAG, "✅ PARSED RESULT:\n$result")

        return@withContext result
    }
}