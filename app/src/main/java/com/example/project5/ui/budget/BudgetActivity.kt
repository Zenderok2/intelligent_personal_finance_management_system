package com.example.project5.ui.budget

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project5.data.model.Budget
import com.example.project5.databinding.ActivityBudgetBinding
import com.example.project5.domain.budget.BudgetManager
import com.example.project5.utils.Constants
import com.example.project5.utils.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var budgetManager: BudgetManager
    private lateinit var adapter: BudgetAdapter

    private var toastShown = false // защита от спама

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        budgetManager = RepositoryProvider.getBudgetManager()

        setupCategorySpinner()
        setupRecycler()

        loadBudgetStatuses()

        binding.btnSaveBudget.setOnClickListener {
            saveBudget()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupCategorySpinner() {
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Constants.DEFAULT_CATEGORIES
        )
        binding.spinnerCategory.adapter = spinnerAdapter
    }

    private fun setupRecycler() {
        adapter = BudgetAdapter()

        binding.recyclerBudgets.layoutManager = LinearLayoutManager(this)
        binding.recyclerBudgets.adapter = adapter
    }

    private fun loadBudgetStatuses() {
        lifecycleScope.launch {
            try {
                val statuses = withContext(Dispatchers.IO) {
                    budgetManager.getAllBudgetStatuses()
                }

                adapter.submitList(statuses)

                if (!toastShown) {
                    val exceeded = statuses.filter { it.isExceeded }

                    if (exceeded.isNotEmpty()) {
                        val message = exceeded.joinToString("\n") {
                            "Превышен бюджет: ${it.budget.category}"
                        }

                        showToast(message)
                        toastShown = true
                    }
                }

            } catch (e: Exception) {
                Log.e("BudgetActivity", "Ошибка загрузки бюджетов", e)
                showToast("Ошибка загрузки данных")
            }
        }
    }

    private fun saveBudget() {
        val category = binding.spinnerCategory.selectedItem.toString()
        val limitText = binding.etLimit.text.toString()

        if (limitText.isBlank()) {
            showToast("Введите лимит")
            return
        }

        val limit = limitText.toDoubleOrNull()

        if (limit == null || limit <= 0) {
            showToast("Некорректный лимит")
            return
        }

        lifecycleScope.launch {
            try {
                val budget = Budget(
                    category = category,
                    limit = limit,
                    period = "monthly",
                    startDate = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    budgetManager.addOrUpdateBudget(budget)
                }

                showToast("Лимит сохранён")

                binding.etLimit.text?.clear()

                loadBudgetStatuses()

            } catch (e: Exception) {
                Log.e("BudgetActivity", "Ошибка сохранения бюджета", e)
                showToast("Ошибка сохранения")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}