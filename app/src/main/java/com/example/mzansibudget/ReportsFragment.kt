package com.example.mzansibudget

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var llCategoryList: LinearLayout
    private lateinit var tvNoData: TextView
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var rvDetailedExpenses: RecyclerView
    private lateinit var database: AppDatabase

    private var startDate: Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    private var endDate: Calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llCategoryList = view.findViewById(R.id.ll_category_list)
        tvNoData = view.findViewById(R.id.tv_no_data)
        btnStartDate = view.findViewById(R.id.btn_start_date)
        btnEndDate = view.findViewById(R.id.btn_end_date)
        rvDetailedExpenses = view.findViewById(R.id.rv_detailed_expenses)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        rvDetailedExpenses.layoutManager = LinearLayoutManager(requireContext())

        updateDateButtons()

        btnStartDate.setOnClickListener {
            showDatePicker(startDate) {
                updateDateButtons()
                loadReportData()
            }
        }

        btnEndDate.setOnClickListener {
            showDatePicker(endDate) {
                updateDateButtons()
                loadReportData()
            }
        }

        loadReportData()
    }

    private fun updateDateButtons() {
        btnStartDate.text = "From: ${dateFormatter.format(startDate.time)}"
        btnEndDate.text = "To: ${dateFormatter.format(endDate.time)}"
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: () -> Unit) {
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadReportData() {
        val currentUser = sharedPref.getString("currentUser", "") ?: ""
        val startStr = dateFormatter.format(startDate.time)
        val endStr = dateFormatter.format(endDate.time)
        
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val categoryTotals = database.expenseDao().getCategoryTotalsByPeriod(currentUser, startStr, endStr)
            val detailedExpenses = database.expenseDao().getExpensesByPeriod(currentUser, startStr, endStr)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                llCategoryList.removeAllViews()
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                if (categoryTotals.isEmpty()) {
                    tvNoData.visibility = View.VISIBLE
                } else {
                    tvNoData.visibility = View.GONE
                    categoryTotals.forEach { total ->
                        val itemView = layoutInflater.inflate(R.layout.item_category_summary, llCategoryList, false)
                        val tvCategory = itemView.findViewById<TextView>(R.id.tv_category_name)
                        val tvAmount = itemView.findViewById<TextView>(R.id.tv_category_amount)

                        tvCategory.text = total.category
                        tvAmount.text = currencyFormat.format(total.total)

                        llCategoryList.addView(itemView)
                    }
                }

                rvDetailedExpenses.adapter = ExpenseAdapter(detailedExpenses) { expense ->
                    if (expense.receiptImage != null) {
                        showImageDialog(expense.receiptImage)
                    } else {
                        Toast.makeText(requireContext(), "${expense.description}: ${currencyFormat.format(expense.amount)}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showImageDialog(imageBytes: ByteArray) {
        val dialog = android.app.Dialog(requireContext())
        val imageView = ImageView(requireContext())
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        imageView.setImageBitmap(bitmap)
        dialog.setContentView(imageView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }
}