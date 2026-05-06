package com.example.mzansibudget

import android.content.Context
import androidx.room.*

@Database(entities = [User::class, Category::class, Expense::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mzansi-budget-db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface UserDao {
    @Insert suspend fun insertUser(user: User)
    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun login(username: String, password: String): User?
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?
    @Update suspend fun updateUser(user: User)
}

@Dao
interface CategoryDao {
    @Insert suspend fun insertCategory(category: Category)
    @Query("SELECT * FROM categories WHERE username = :username ORDER BY name ASC")
    suspend fun getCategoriesByUser(username: String): List<Category>
    @Update suspend fun updateCategory(category: Category)
    @Delete suspend fun deleteCategory(category: Category)
}

@Dao
interface ExpenseDao {
    @Insert suspend fun insertExpense(expense: Expense)
    @Query("SELECT * FROM expenses WHERE username = :username ORDER BY date DESC, startTime DESC")
    suspend fun getExpensesByUser(username: String): List<Expense>
    @Query("SELECT * FROM expenses WHERE username = :username AND date BETWEEN :startDate AND :endDate ORDER BY date DESC, startTime DESC")
    suspend fun getExpensesByPeriod(username: String, startDate: String, endDate: String): List<Expense>
    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE username = :username AND date BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getCategoryTotalsByPeriod(username: String, startDate: String, endDate: String): List<CategoryTotal>
    @Delete suspend fun deleteExpense(expense: Expense)
}

data class CategoryTotal(
    val category: String,
    val total: Double
)