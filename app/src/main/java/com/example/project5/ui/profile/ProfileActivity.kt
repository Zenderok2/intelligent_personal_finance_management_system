package com.example.project5.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.project5.R
import com.example.project5.data.cloud.FirebaseAuthManager
import com.example.project5.databinding.ActivityProfileBinding
import com.example.project5.domain.rating.RatingCalculator
import com.example.project5.ui.auth.LoginActivity
import com.example.project5.utils.Constants
import com.example.project5.utils.RepositoryProvider
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val authManager by lazy { FirebaseAuthManager() }

    private val repository by lazy { RepositoryProvider.getReceiptRepository() }

    private val ratingCalculator by lazy {
        RatingCalculator(
            repository,
            Constants.YANDEX_API_KEY,
            Constants.YANDEX_FOLDER_ID
        )
    }

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        restoreRatingIfExists()
    }

    private fun setupListeners() {

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogout.setOnClickListener {
            authManager.logout()
            navigateToLogin()
        }

        binding.btnGetRating.setOnClickListener {
            calculateRating()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun restoreRatingIfExists() {
        val savedRating = prefs.getInt(KEY_RATING, NO_RATING)

        if (savedRating != NO_RATING) {
            showRating(savedRating)
            binding.btnGetRating.visibility = View.GONE
        }
    }

    private fun calculateRating() {
        lifecycleScope.launch {

            setLoadingState(true)

            try {
                val rating = ratingCalculator.calculateRatingWithAI()

                prefs.edit {
                    putInt(KEY_RATING, rating)
                }

                showRating(rating)
                binding.btnGetRating.visibility = View.GONE

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Rating error", e)

                Toast.makeText(
                    this@ProfileActivity,
                    "Ошибка при расчёте рейтинга",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnGetRating.isEnabled = !isLoading
        binding.btnGetRating.text =
            if (isLoading) "Считаем..." else getString(R.string.get_rating)
    }

    private fun showRating(value: Int) {

        binding.tvRatingValue.text = getString(R.string.rating_format, value)
        binding.progressRating.progress = value

        val (colorRes, description) = when {
            value >= 80 -> R.color.green to "Отличная финансовая дисциплина"
            value >= 50 -> R.color.yellow to "Нормальный уровень"
            else -> R.color.red to "Стоит пересмотреть расходы"
        }

        val color = ContextCompat.getColor(this, colorRes)

        binding.tvRatingValue.setTextColor(color)
        binding.progressRating.progressTintList = ColorStateList.valueOf(color)
        binding.tvRatingDescription.text = description
    }

    companion object {
        private const val PREFS_NAME = "rating_prefs"
        private const val KEY_RATING = "rating"
        private const val NO_RATING = -1
    }
}