package com.example.mzansibudget

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var sharedPref: SharedPreferences
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etUsername = view.findViewById(R.id.et_username)
        etPassword = view.findViewById(R.id.et_password)
        btnLogin = view.findViewById(R.id.btn_login)
        tvRegister = view.findViewById(R.id.tv_register)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val user = database.userDao().login(username, password)
                    withContext(Dispatchers.Main) {
                        if (user != null) {
                            sharedPref.edit().putBoolean("isLoggedIn", true).apply()
                            sharedPref.edit().putString("currentUser", username).apply()
                            Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                            (activity as MainActivity).showBottomNav(true)
                            (activity as MainActivity).loadFragment(DashboardFragment())
                        } else {
                            Toast.makeText(requireContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            (activity as MainActivity).loadFragment(RegisterFragment())
        }
    }
}