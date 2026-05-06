package com.example.mzansibudget

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var btnSaveGoals: Button
    private lateinit var pbBudget: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnLogout: Button
    private lateinit var sharedPref: SharedPreferences
    private lateinit var database: AppDatabase
    private var expenseList = mutableListOf<Expense>()
    private var currentUserObj: User? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalSpent = view.findViewById(R.id.tv_total_spent)
        tvStatus = view.findViewById(R.id.tv_status)
        etMinGoal = view.findViewById(R.id.et_min_goal)
        etMaxGoal = view.findViewById(R.id.et_max_goal)
        btnSaveGoals = view.findViewById(R.id.btn_save_goals)
        pbBudget = view.findViewById(R.id.pb_budget)
        recyclerView = view.findViewById(R.id.recycler_view_expenses)
        btnLogout = view.findViewById(R.id.btn_logout)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        btnSaveGoals.setOnClickListener {
            saveGoals()
        }

        btnLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            (activity as MainActivity).showBottomNav(false)
            (activity as MainActivity).loadFragment(LoginFragment())
        }

        loadUserData()
    }

    private fun loadUserData() {
        val username = sharedPref.getString("currentUser", "") ?: ""
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            currentUserObj = database.userDao().getUserByUsername(username)
            withContext(Dispatchers.Main) {
                currentUserObj?.let {
                    etMinGoal.setText(it.minMonthlyGoal.toString())
                    etMaxGoal.setText(it.maxMonthlyGoal.toString())
                    loadDashboardData()
                }
            }
        }
    }

    private fun saveGoals() {
        val min = etMinGoal.text.toString().toDoubleOrNull() ?: 0.0
        val max = etMaxGoal.text.toString().toDoubleOrNull() ?: 0.0
        
        currentUserObj?.let { user ->
            val updatedUser = user.copy(minMonthlyGoal = min, maxMonthlyGoal = max)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                database.userDao().updateUser(updatedUser)
                currentUserObj = updatedUser
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Goals saved", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadDashboardData() {
        val currentUser = sharedPref.getString("currentUser", "") ?: ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val expenses = database.expenseDao().getExpensesByUser(currentUser)
            val totalSpent = expenses.sumOf { it.amount }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                tvTotalSpent.text = "Total Spent: ${currencyFormat.format(totalSpent)}"
                
                currentUserObj?.let { user ->
                    when {
                        totalSpent < user.minMonthlyGoal -> {
                            tvStatus.text = "Status: Below Minimum Goal"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                        }
                        totalSpent <= user.maxMonthlyGoal -> {
                            tvStatus.text = "Status: Within Target Range"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                        }
                        else -> {
                            tvStatus.text = "Status: Above Maximum Goal"
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                        }
                    }
                    
                    if (user.maxMonthlyGoal > 0) {
                        pbBudget.max = user.maxMonthlyGoal.toInt()
                        pbBudget.progress = totalSpent.toInt()
                    } else {
                        pbBudget.progress = 0
                    }
                }

                expenseList.clear()
                expenseList.addAll(expenses)
                recyclerView.adapter = ExpenseAdapter(expenseList) { expense ->
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