package com.example.project5.ocr

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object VisionApiClient {

    private const val ENDPOINT =
        "https://vision.api.cloud.yandex.net/vision/v1/batchAnalyze"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun recognize(
        base64: String,
        folderId: String,
        apiKey: String
    ): String {

        if (apiKey.isBlank())
            throw IllegalArgumentException("❗ API KEY пуст - Vision не сможет работать")

        if (folderId.isBlank())
            throw IllegalArgumentException("❗ FolderID пуст - Vision не сможет работать")

        if (base64.isBlank())
            throw IllegalArgumentException("❗ Пустое изображение Base64")

        val requestJson = JSONObject().apply {
            put("folderId", folderId)
            put("analyzeSpecs",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("content", base64)
                            put("features", JSONArray().apply {
                                put(
                                    JSONObject().apply {
                                        put("type", "TEXT_DETECTION")
                                        put("textDetectionConfig", JSONObject().apply {
                                            put("languageCodes", JSONArray(listOf("ru", "en")))
                                            put("model", "page")
                                        })
                                    }
                                )
                            })
                        }
                    )
                }
            )
        }.toString()

        val body = requestJson.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Api-Key $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string()

            if (!response.isSuccessful) {
                throw Exception(
                    "Vision API error ${response.code}:\n$raw"
                )
            }

            if (raw.isNullOrBlank()) {
                throw Exception("Vision API вернул пустой ответ")
            }

            return raw
        }
    }
}
