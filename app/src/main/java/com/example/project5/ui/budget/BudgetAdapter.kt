package com.example.project5.ui.budget

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.project5.databinding.ItemBudgetBinding
import com.example.project5.domain.budget.BudgetStatus
import com.example.project5.utils.MoneyUtils
import android.graphics.Color

class BudgetAdapter(
    private val onItemClick: (BudgetStatus) -> Unit = {}
) : ListAdapter<BudgetStatus, BudgetAdapter.ViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemBudgetBinding,
        private val onItemClick: (BudgetStatus) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(status: BudgetStatus) {

            val budget = status.budget
            val percent = status.percentage.toInt().coerceIn(0, 100)

            binding.tvCategory.text = budget.category

            binding.tvLimit.text =
                "${MoneyUtils.format(status.spent)} / ${MoneyUtils.format(budget.limit)}"

            // Прогресс
            binding.progressBudget.progress = percent

            // Цвет
            val color = when {
                status.isExceeded -> Color.RED
                status.isNearLimit -> Color.YELLOW
                else -> Color.GREEN
            }

            binding.progressBudget.progressTintList =
                ColorStateList.valueOf(color)

            binding.root.setOnClickListener { onItemClick(status) }
        }
    }
}

class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetStatus>() {
    override fun areItemsTheSame(oldItem: BudgetStatus, newItem: BudgetStatus): Boolean {
        return oldItem.budget.id == newItem.budget.id
    }

    override fun areContentsTheSame(oldItem: BudgetStatus, newItem: BudgetStatus): Boolean {
        return oldItem == newItem
    }
}