package com.example.mzansibudget

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etNewCategory: EditText
    private lateinit var btnAddCategory: Button
    private lateinit var sharedPref: SharedPreferences
    private lateinit var database: AppDatabase
    private var categoryList = mutableListOf<Category>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_categories)
        etNewCategory = view.findViewById(R.id.et_new_category)
        btnAddCategory = view.findViewById(R.id.btn_add_category)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadCategories()

        btnAddCategory.setOnClickListener {
            val newCategoryName = etNewCategory.text.toString()
            if (newCategoryName.isNotEmpty()) {
                val currentUser = sharedPref.getString("currentUser", "") ?: ""
                val category = Category(username = currentUser, name = newCategoryName)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    database.categoryDao().insertCategory(category)
                    loadCategories()
                }
                etNewCategory.text.clear()
            } else {
                Toast.makeText(requireContext(), "Enter category name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCategories() {
        val currentUser = sharedPref.getString("currentUser", "") ?: ""
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val categories = database.categoryDao().getCategoriesByUser(currentUser)
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                categoryList.clear()
                categoryList.addAll(categories)
                val adapter = CategoryAdapter(
                    categoryList,
                    { category -> showEditDialog(category) },
                    { category -> deleteCategory(category) }
                )
                recyclerView.adapter = adapter
            }
        }
    }

    private fun showEditDialog(category: Category) {
        val input = EditText(requireContext())
        input.setText(category.name)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Category")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    val updatedCategory = category.copy(name = newName)
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        database.categoryDao().updateCategory(updatedCategory)
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    database.categoryDao().deleteCategory(category)
                    loadCategories()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}