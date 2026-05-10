package com.example.project5.ui.achievements

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project5.databinding.ActivityAchievementsBinding
import com.example.project5.ui.profile.AchievementAdapter
import com.example.project5.utils.RepositoryProvider
import kotlinx.coroutines.launch

class AchievementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var adapter: AchievementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AchievementAdapter()
        binding.recyclerAchievements.layoutManager = LinearLayoutManager(this)
        binding.recyclerAchievements.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        loadAchievements()
    }

    private fun loadAchievements() {
        lifecycleScope.launch {
            val achievements = RepositoryProvider.getAchievementManager().checkAchievements()
            adapter.submitList(achievements)
        }
    }
}