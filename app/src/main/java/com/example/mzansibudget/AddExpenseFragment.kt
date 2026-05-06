package com.example.mzansibudget

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

class AddExpenseFragment : Fragment() {

    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSelectStartTime: Button
    private lateinit var tvStartTime: TextView
    private lateinit var btnSelectEndTime: Button
    private lateinit var tvEndTime: TextView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnUploadPhoto: Button
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var btnSaveExpense: Button
    private lateinit var sharedPref: SharedPreferences
    private lateinit var database: AppDatabase

    private var categoryList = mutableListOf<String>()
    private var selectedCategory = ""
    private var selectedDate = "" // Stored as yyyy-MM-dd
    private var selectedStartTime = ""
    private var selectedEndTime = ""
    private var capturedImageUri: Uri? = null
    private var imageBytes: ByteArray? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            capturedImageUri?.let { uri ->
                processAndDisplayImage(uri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            processAndDisplayImage(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etAmount = view.findViewById(R.id.et_amount)
        etDescription = view.findViewById(R.id.et_description)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        btnSelectStartTime = view.findViewById(R.id.btn_select_start_time)
        tvStartTime = view.findViewById(R.id.tv_start_time)
        btnSelectEndTime = view.findViewById(R.id.btn_select_end_time)
        tvEndTime = view.findViewById(R.id.tv_end_time)
        btnTakePhoto = view.findViewById(R.id.btn_take_photo)
        btnUploadPhoto = view.findViewById(R.id.btn_upload_photo)
        ivReceiptPreview = view.findViewById(R.id.iv_receipt_preview)
        btnSaveExpense = view.findViewById(R.id.btn_save_expense)

        sharedPref = requireActivity().getSharedPreferences("MzansiBudgetPrefs", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(requireContext())

        loadCategories()

        btnSelectDate.setOnClickListener { showDatePicker() }
        btnSelectStartTime.setOnClickListener { showTimePicker(true) }
        btnSelectEndTime.setOnClickListener { showTimePicker(false) }
        btnTakePhoto.setOnClickListener { checkCameraPermission() }
        btnUploadPhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun processAndDisplayImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            ivReceiptPreview.setImageBitmap(bitmap)
            ivReceiptPreview.visibility = View.VISIBLE

            val stream = ByteArrayOutputStream()
            // Compress image to ensure it's not too large for the database
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            imageBytes = stream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCategories() {
        val currentUser = sharedPref.getString("currentUser", "") ?: ""
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val categories = database.categoryDao().getCategoriesByUser(currentUser)
            val categoryNames = categories.map { it.name }.toMutableList()
            if (categoryNames.isEmpty()) {
                categoryNames.addAll(listOf("Transport", "Groceries", "Airtime/Data", "Entertainment", "Savings"))
            }
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                categoryList = categoryNames
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedCategory = categoryList[position]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(requireContext(),
            { _, year, month, day ->
                selectedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                tvSelectedDate.text = "Date: $selectedDate"
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val timePicker = android.app.TimePickerDialog(requireContext(),
            { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                if (isStartTime) {
                    selectedStartTime = time
                    tvStartTime.text = "Start Time: $time"
                } else {
                    selectedEndTime = time
                    tvEndTime.text = "End Time: $time"
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePicker.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Receipt_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.DESCRIPTION, "Expense Receipt")
        }
        
        capturedImageUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        takePictureLauncher.launch(intent)
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString()
        val description = etDescription.text.toString()

        if (amountStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill amount and description", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate.isEmpty() || selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) {
            Toast.makeText(requireContext(), "Please select date, start time, and end time", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val currentUser = sharedPref.getString("currentUser", "") ?: ""

        val expense = Expense(
            username = currentUser,
            amount = amount,
            description = description,
            category = selectedCategory,
            date = selectedDate,
            startTime = selectedStartTime,
            endTime = selectedEndTime,
            receiptImage = imageBytes
        )

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            database.expenseDao().insertExpense(expense)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Expense Added Successfully", Toast.LENGTH_SHORT).show()
                (activity as MainActivity).loadFragment(DashboardFragment())
            }
        }
    }
}