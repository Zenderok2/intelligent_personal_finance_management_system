package com.example.project5.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.example.project5.R
import com.example.project5.databinding.ActivityStatisticsBinding
import com.example.project5.domain.statistics.CategoryStatistic
import com.example.project5.utils.RepositoryProvider
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.project5.data.model.Receipt
import kotlin.coroutines.cancellation.CancellationException

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val repository by lazy { RepositoryProvider.getReceiptRepository() }
    private val budgetManager by lazy { RepositoryProvider.getBudgetManager() }

    private var pieChartAnimated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPieChart()

        lifecycleScope.launch {
            try {
                repository.syncFromCloud()

                repository.getAllReceipts().collect { receipts ->
                    updateStatistics(receipts)
                }

            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Log.e("StatisticsActivity", "Ошибка загрузки данных", e)
                Toast.makeText(this@StatisticsActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnClearData.setOnClickListener {
            lifecycleScope.launch {
                try {
                    repository.clearAllReceipts()
                    updateStatistics(emptyList())
                } catch (_: CancellationException) {
                } catch (e: Exception) {
                    Log.e("StatisticsActivity", "Ошибка очистки данных", e)
                    Toast.makeText(this@StatisticsActivity, "Ошибка очистки данных", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initPieChart() = with(binding.pieChart) {

        description.isEnabled = false
        setUsePercentValues(true)

        isRotationEnabled = true
        isHighlightPerTapEnabled = true
        setTouchEnabled(true)

        // Инерция
        dragDecelerationFrictionCoef = 0.92f

        isDrawHoleEnabled = true
        holeRadius = 55f
        setHoleColor(Color.WHITE)

        setTransparentCircleColor(Color.WHITE)
        setTransparentCircleAlpha(115)
        transparentCircleRadius = 60f

        setDrawEntryLabels(false)
        legend.isEnabled = false

        // обработка клика по сектору
        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {

                val entry = e as? PieEntry ?: return

                Toast.makeText(
                    this@StatisticsActivity,
                    "${entry.label}: ${entry.value.toInt()} ₽",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected() {}
        })
    }

    private fun updateStatistics(receipts: List<Receipt>) {

        val categoryTotals = receipts
            .flatMap { it.items }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.price } }

        val totalAmount = categoryTotals.values.sum()

        binding.tvReceiptCount.text =
            getString(R.string.receipt_count, receipts.size)

        binding.tvTotalAmount.text =
            getString(R.string.total_amount_rub, totalAmount)

        if (categoryTotals.isEmpty() || totalAmount <= 0) {
            binding.pieChart.clear()
            binding.pieChart.centerText = getString(R.string.no_data)
            binding.categoryList.text = getString(R.string.stats_unavailable)
            return
        }

        val stats = buildStatistics(categoryTotals, totalAmount)

        lifecycleScope.launch {
            checkBudgets(categoryTotals)
        }

        drawPie(stats)
        displayCategoryList(stats)

        binding.pieChart.centerText = getString(R.string.expenses)
    }

    private fun buildStatistics(
        categoryTotals: Map<String, Double>,
        totalAmount: Double
    ): List<CategoryStatistic> {

        val colors = intArrayOf(
            "#FF6B6B".toColorInt(),
            "#4ECDC4".toColorInt(),
            "#FFD93D".toColorInt(),
            "#1A535C".toColorInt(),
            "#FF9F1C".toColorInt(),
            "#6A4C93".toColorInt(),
            "#00BBF9".toColorInt(),
            "#F15BB5".toColorInt()
        )

        return categoryTotals.entries.mapIndexed { index, (category, amount) ->
            CategoryStatistic(
                category = category,
                amount = amount,
                percentage = (amount / totalAmount) * 100,
                color = colors[index % colors.size]
            )
        }
    }

    private suspend fun checkBudgets(categoryTotals: Map<String, Double>) {
        val warnings = mutableListOf<String>()

        for ((category, amount) in categoryTotals) {
            val budget = withContext(Dispatchers.IO) {
                repository.getBudgetForCategory(category)
            } ?: continue

            when {
                budgetManager.isOverLimit(amount, budget.limit) -> {
                    warnings.add("⚠ Перерасход: $category")
                }
                budgetManager.isNearLimit(amount, budget.limit) -> {
                    warnings.add("⚡ Почти лимит: $category")
                }
            }
        }

        if (warnings.isNotEmpty()) {
            Toast.makeText(
                this@StatisticsActivity,
                warnings.joinToString("\n"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun drawPie(stats: List<CategoryStatistic>) {
        val entries = stats.map {
            PieEntry(it.amount.toFloat(), it.category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = stats.map { it.color }
            valueTextSize = 13f
            valueTextColor = Color.BLACK

            // проценты
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "%.1f%%".format(value)
                }
            })
        }

        binding.pieChart.data = pieData
        binding.pieChart.invalidate()

        if (!pieChartAnimated) {
            binding.pieChart.animateY(1200, Easing.EaseInOutCubic)
            pieChartAnimated = true
        }
    }

    private fun displayCategoryList(stats: List<CategoryStatistic>) {
        val text = buildString {
            append("📊 Детализация по категориям:\n\n")

            stats.forEach { stat ->
                append("• ${stat.category}: ")
                append("%.2f ₽".format(stat.amount))
                append("  (%.1f%%)\n".format(stat.percentage))
            }
        }

        binding.categoryList.text = text
    }
}