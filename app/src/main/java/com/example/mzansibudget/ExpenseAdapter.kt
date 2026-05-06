package com.example.mzansibudget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val expenseList: List<Expense>,
    private val onItemClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val ivCameraIcon: ImageView = itemView.findViewById(R.id.iv_camera_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenseList[position]
        holder.tvAmount.text = "R${"%.2f".format(expense.amount)}"
        holder.tvDescription.text = expense.description
        holder.tvCategory.text = expense.category
        holder.tvDate.text = expense.date
        
        if (expense.receiptImage != null) {
            holder.ivCameraIcon.visibility = View.VISIBLE
        } else {
            holder.ivCameraIcon.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener { onItemClick(expense) }
    }

    override fun getItemCount() = expenseList.size
}