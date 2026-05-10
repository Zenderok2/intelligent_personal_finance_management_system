package com.example.project5.ui

import com.example.project5.data.cloud.FirebaseAuthManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.project5.R
import com.example.project5.data.model.Receipt
import com.example.project5.databinding.ActivityMainBinding
import com.example.project5.ui.statistics.StatisticsActivity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import com.example.project5.ui.budget.BudgetActivity
import com.example.project5.ui.forecast.ForecastActivity
import com.example.project5.ui.profile.ProfileActivity
import com.example.project5.ui.achievements.AchievementsActivity
import com.example.project5.ui.auth.LoginActivity
import com.example.project5.ui.recommendations.RecommendationActivity
import com.example.project5.utils.clickWithScale
import android.view.View
import com.example.project5.utils.Constants
import com.example.project5.utils.RepositoryProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy {
        RepositoryProvider.getReceiptRepository()
    }

    private var imagePath: String? = null


    // Галерея
    private val galleryPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { loadImageFromUri(it) }
        }

    // Камера
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imagePath != null) {
                val bmp = BitmapFactory.decodeFile(imagePath)
                binding.imageView.setImageBitmap(bmp)
                binding.tvResult.text = getString(R.string.photo_taken)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверка авторизации
        val auth = FirebaseAuthManager()
        if (auth.getUserId() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // UI
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация
        RepositoryProvider.init(applicationContext)

        initButtons()
        checkApiCredentials()

        // Синхронизация
        lifecycleScope.launch {
            try {
                repository.syncFromCloud()
            } catch (e: Exception) {
                showToast(getString(R.string.error_occurred, e.message ?: "Unknown error"))
            }
        }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.menu_home -> {
                    true
                }

                R.id.menu_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    finish()
                    true
                }

                R.id.menu_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    finish()
                    true
                }

                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }

        // Открытие главной вкладки при запуске
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_home
        }
    }

    private fun checkApiCredentials() {
        if (
            Constants.YANDEX_API_KEY.isBlank() ||
            Constants.YANDEX_FOLDER_ID.isBlank()
        ) {
            showToast(getString(R.string.error_occurred, "API ключ или Folder ID не заданы"))
            binding.btnScan.isEnabled = false
        }
    }

    private fun initButtons() {
        binding.btnSelect.setOnClickListener { showImageSourceDialog() }
        binding.btnScan.setOnClickListener { startPipeline() }

        binding.btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        binding.btnGoToBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }

        binding.btnGoToForecast.setOnClickListener {
            startActivity(Intent(this, ForecastActivity::class.java))
        }

        binding.btnGoToProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.btnGoToAchievements.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        binding.btnGoToRecommendations.setOnClickListener {
            startActivity(Intent(this, RecommendationActivity::class.java))
        }
    }

    // навигация
    private fun View.navigateTo(activity: Class<*>) {
        this.clickWithScale {
            context.startActivity(Intent(context, activity))
        }
    }

    /**
     * Диалог выбора источника изображения
     */
    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.select_source)
            .setItems(
                arrayOf(
                    getString(R.string.take_photo),
                    getString(R.string.choose_from_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> galleryPicker.launch("image/*")
                }
            }
            .show()
    }

    /**
     * Запуск камеры и сохранение временного файла
     */
    private fun openCamera() {
        try {
            val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            imagePath = file.absolutePath

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            showToast(getString(R.string.error_occurred, e.message ?: "Camera error"))
        }
    }

    /**
     * Загрузка изображения из URI галереи во временный файл
     */
    private fun loadImageFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)

                if (bmp == null) {
                    showToast(getString(R.string.error_loading_image, "Decode failed"))
                    return
                }

                val file = File(cacheDir, "receipt_${System.currentTimeMillis()}.jpg")

                FileOutputStream(file).use {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }

                imagePath = file.absolutePath
                binding.imageView.setImageBitmap(bmp)
                binding.tvResult.text = getString(R.string.image_loaded)
            }
        } catch (e: Exception) {
            showToast(getString(R.string.error_loading_image, e.message ?: "Unknown error"))
        }
    }

    /**
     * Основной конвейер: OCR → GPT → сохранение
     */
    private fun startPipeline() {
        val path = imagePath
        if (path == null) {
            showToast(getString(R.string.select_receipt_first))
            return
        }

        binding.btnScan.isEnabled = false
        binding.tvResult.text = getString(R.string.recognizing_text)
        showProgress(true)

        lifecycleScope.launch {
            try {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap == null) {
                    showToast("Ошибка загрузки изображения")
                    return@launch
                }

                val receipt = repository.processReceiptImage(bitmap)

                if (receipt == null) {
                    binding.tvResult.text = getString(R.string.ocr_failed)
                    showToast(getString(R.string.ocr_failed))
                    return@launch
                }

                showResult(receipt)

            } catch (e: Exception) {
                binding.tvResult.text =
                    getString(R.string.error_occurred, e.message ?: "Unknown error")
            } finally {
                showProgress(false)
                binding.btnScan.isEnabled = true
            }
        }
    }

    private fun showResult(receipt: Receipt) {
        val displayText = buildString {
            append(getString(R.string.receipt_saved)).append("\n\n")

            append(
                getString(
                    R.string.total_amount,
                    String.format(Locale.US, "%.2f", receipt.total)
                )
            ).append("\n\n")

            append(getString(R.string.products)).append("\n")

            receipt.items.forEach { p ->
                append(
                    getString(
                        R.string.product_item,
                        p.name,
                        String.format(Locale.US, "%.2f", p.price),
                        p.category
                    )
                ).append("\n")
            }
        }

        binding.tvResult.text = displayText
    }

    /**
     * Управление ProgressBar
     */
    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}