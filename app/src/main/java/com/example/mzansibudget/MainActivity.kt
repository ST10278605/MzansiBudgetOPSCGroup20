package com.example.mzansibudget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)
        checkLoginState()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_add -> loadFragment(AddExpenseFragment())
                R.id.nav_categories -> loadFragment(CategoriesFragment())
                R.id.nav_reports -> loadFragment(ReportsFragment())
            }
            true
        }
    }

    private fun checkLoginState() {
        val sharedPref = getSharedPreferences("MzansiBudgetPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            loadFragment(DashboardFragment())
            bottomNav.visibility = BottomNavigationView.VISIBLE
        } else {
            loadFragment(LoginFragment())
            bottomNav.visibility = BottomNavigationView.GONE
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun showBottomNav(show: Boolean) {
        bottomNav.visibility = if (show) BottomNavigationView.VISIBLE else BottomNavigationView.GONE
    }
}