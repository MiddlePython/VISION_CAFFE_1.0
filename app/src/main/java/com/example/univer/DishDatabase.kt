package com.example.univer

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "dishes")
data class DishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    // Используется как цена за 1 грамм для весовых, или как цена за 1 штуку для штучных
    val pricePerGram: Double, 
    // Флаг: true - товар штучный, false - товар весовой (по умолчанию)
    val isPiece: Boolean = false 
)

// СУЩНОСТЬ ДЛЯ ШАБЛОНОВ ТАРЫ
@Entity(tableName = "plates")
data class PlateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weightGrams: Int
)

@Dao
interface DishDao {
    @Query("SELECT * FROM dishes ORDER BY name ASC")
    suspend fun getAllDishes(): List<DishEntity>

    @Query("SELECT DISTINCT category FROM dishes ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDish(dish: DishEntity)

    // МЕТОДЫ ДЛЯ ТАРЫ
    @Query("SELECT * FROM plates ORDER BY name ASC")
    suspend fun getAllPlates(): List<PlateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlate(plate: PlateEntity)
}

// Версия изменена на 3, так как изменилась структура DishEntity
@Database(entities = [DishEntity::class, PlateEntity::class], version = 3, exportSchema = false)
abstract class DishDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao

    companion object {
        @Volatile
        private var INSTANCE: DishDatabase? = null

        fun getDatabase(context: Context): DishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DishDatabase::class.java,
                    "dish_database"
                )
                .fallbackToDestructiveMigration() // Безопасное обновление структуры БД при смене версии
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        val dao = database.dishDao()
                        
                        // Предзаполнение блюд (включая весовые и штучные)
                        val defaultDishes = listOf(
                            DishEntity(name = "Борщ с говядиной", category = "Супы", pricePerGram = 0.45, isPiece = false),
                            DishEntity(name = "Котлета домашняя", category = "Горячее", pricePerGram = 0.85, isPiece = false),
                            DishEntity(name = "Пюре картофельное", category = "Гарниры", pricePerGram = 0.20, isPiece = false),
                            DishEntity(name = "Оливье", category = "Салаты", pricePerGram = 0.50, isPiece = false),
                            
                            // НОВЫЕ ШТУЧНЫЕ ТОВАРЫ
                            DishEntity(name = "Булочка с корицей", category = "Выпечка и напитки", pricePerGram = 65.00, isPiece = true),
                            DishEntity(name = "Сок в ассортименте (0.5л)", category = "Выпечка и напитки", pricePerGram = 90.00, isPiece = true)
                        )
                        defaultDishes.forEach { dao.insertDish(it) }

                        // Предзаполнение шаблонов тары
                        val defaultPlates = listOf(
                            PlateEntity(name = "Глубокая", weightGrams = 220),
                            PlateEntity(name = "Плоская", weightGrams = 180),
                            PlateEntity(name = "Салатник", weightGrams = 150),
                            PlateEntity(name = "Контейнер", weightGrams = 45)
                        )
                        defaultPlates.forEach { dao.insertPlate(it) }
                    }
                }
            }
        }
    }
}
