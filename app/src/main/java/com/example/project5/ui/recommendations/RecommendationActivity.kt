package com.example.project5.ui.recommendations

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project5.databinding.ActivityRecommendationBinding
import com.example.project5.utils.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RecommendationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecommendationBinding
    private val engine by lazy { RepositoryProvider.getRecommendationEngine() }

    private lateinit var adapter: RecommendationAdapter

    private var isLoaded = false // защита от повторной загрузки

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecommendationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()

        if (!isLoaded) {
            loadRecommendations()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecycler() {
        adapter = RecommendationAdapter()
        binding.recyclerRecommendations.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecommendations.adapter = adapter
    }

    private fun loadRecommendations() {
        lifecycleScope.launch {
            try {
                showLoading(true)

                val recommendationsDeferred = async(Dispatchers.IO) {
                    engine.generateRecommendations()
                }

                val aiDeferred = async(Dispatchers.IO) {
                    engine.generateAIRecommendations()
                }

                val recommendations = recommendationsDeferred.await()
                val aiText = aiDeferred.await()

                adapter.submitList(recommendations)

                binding.tvAiAdvice.text = if (recommendations.isEmpty()) {
                    "Недостаточно данных для рекомендаций"
                } else {
                    aiText
                }

                isLoaded = true

            } catch (e: Exception) {
                Log.e("RecommendationActivity", "Ошибка загрузки рекомендаций", e)
                showToast("Ошибка загрузки рекомендаций")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerRecommendations.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}