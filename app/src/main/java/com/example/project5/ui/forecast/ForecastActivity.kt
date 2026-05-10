package com.example.project5.ui.forecast

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project5.R
import com.example.project5.databinding.ActivityForecastBinding
import com.example.project5.utils.MoneyUtils
import com.example.project5.utils.RepositoryProvider
import kotlinx.coroutines.launch


class ForecastActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForecastBinding

    private val forecastManager by lazy {
        RepositoryProvider.getForecastManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadForecast()
    }

    private fun loadForecast() {
        lifecycleScope.launch {

            val prediction = forecastManager.predictNextMonthSpending()

            binding.tvForecast.text = getString(
                R.string.forecast_result,
                MoneyUtils.format(prediction)
            )

            val ai = forecastManager.predictWithAI()
            binding.tvForecastAi.text = ai
        }
    }
}