package com.example.mzansibudget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val amount: Double,
    val description: String,
    val category: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val receiptImage: ByteArray? = null
)