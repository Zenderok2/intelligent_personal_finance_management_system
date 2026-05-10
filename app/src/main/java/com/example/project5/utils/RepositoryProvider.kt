package com.example.project5.utils

import android.annotation.SuppressLint
import android.content.Context
import com.example.project5.data.local.AppDatabase
import com.example.project5.data.repository.ReceiptRepository
import com.example.project5.domain.achievements.AchievementManager
import com.example.project5.domain.budget.BudgetManager
import com.example.project5.domain.forecast.ForecastManager
import com.example.project5.domain.recommendations.RecommendationEngine

@SuppressLint("StaticFieldLeak")
object RepositoryProvider {

    private lateinit var appContext: Context
    private lateinit var db: AppDatabase

    private lateinit var receiptRepo: ReceiptRepository
    private lateinit var budgetManager: BudgetManager
    private lateinit var forecastManager: ForecastManager
    private lateinit var recommendationEngine: RecommendationEngine
    private lateinit var achievementManager: AchievementManager

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        appContext = context.applicationContext
        db = AppDatabase.getInstance(appContext)

        receiptRepo = ReceiptRepository(
            receiptDao = db.receiptDao(),
            budgetDao = db.budgetDao(),
            context = appContext,
            folderId = Constants.YANDEX_FOLDER_ID,
            apiKey = Constants.YANDEX_API_KEY
        )

        budgetManager = BudgetManager(db.budgetDao(), receiptRepo)

        forecastManager = ForecastManager(
            receiptRepo,
            Constants.YANDEX_API_KEY,
            Constants.YANDEX_FOLDER_ID
        )

        recommendationEngine = RecommendationEngine(
            receiptRepo,
            budgetManager,
            Constants.YANDEX_API_KEY,
            Constants.YANDEX_FOLDER_ID
        )

        achievementManager = AchievementManager(receiptRepo)

        isInitialized = true
    }

    private fun checkInit() {
        check(isInitialized) { "RepositoryProvider is not initialized. Call init() first." }
    }

    fun getReceiptRepository(): ReceiptRepository {
        checkInit()
        return receiptRepo
    }

    fun getBudgetManager(): BudgetManager {
        checkInit()
        return budgetManager
    }

    fun getForecastManager(): ForecastManager {
        checkInit()
        return forecastManager
    }

    fun getRecommendationEngine(): RecommendationEngine {
        checkInit()
        return recommendationEngine
    }

    fun getAchievementManager(): AchievementManager {
        checkInit()
        return achievementManager
    }
}