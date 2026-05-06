package com.example.mzansibudget

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

class RegisterFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etUsername = view.findViewById(R.id.et_username)
        etPassword = view.findViewById(R.id.et_password)
        etConfirmPassword = view.findViewById(R.id.et_confirm_password)
        btnRegister = view.findViewById(R.id.btn_register)
        tvLogin = view.findViewById(R.id.tv_login)

        database = AppDatabase.getDatabase(requireContext())

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (password == confirmPassword) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val existingUser = database.userDao().getUserByUsername(username)
                        withContext(Dispatchers.Main) {
                            if (existingUser == null) {
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    database.userDao().insertUser(User(username = username, password = password))
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "Registration Successful! Please Login.", Toast.LENGTH_SHORT).show()
                                        (activity as MainActivity).loadFragment(LoginFragment())
                                    }
                                }
                            } else {
                                Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvLogin.setOnClickListener {
            (activity as MainActivity).loadFragment(LoginFragment())
        }
    }
}