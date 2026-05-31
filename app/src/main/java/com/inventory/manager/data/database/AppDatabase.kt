package com.inventory.manager.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inventory.manager.data.database.dao.*
import com.inventory.manager.data.database.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Category::class, Staff::class, Device::class, StockRecord::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun staffDao(): StaffDao
    abstract fun deviceDao(): DeviceDao
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "inventory_db")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.categoryDao().insert(Category(name = "主机", icon = "🖥️", sortOrder = 0))
                                    database.categoryDao().insert(Category(name = "显示器", icon = "🖥", sortOrder = 1))
                                    database.categoryDao().insert(Category(name = "鼠标", icon = "🖱️", sortOrder = 2))
                                    database.categoryDao().insert(Category(name = "键盘", icon = "⌨️", sortOrder = 3))
                                    database.categoryDao().insert(Category(name = "耳机", icon = "🎧", sortOrder = 4))
                                    database.categoryDao().insert(Category(name = "摄像头", icon = "📷", sortOrder = 5))
                                    database.categoryDao().insert(Category(name = "打印机", icon = "🖨️", sortOrder = 6))
                                    database.categoryDao().insert(Category(name = "其他", icon = "📦", sortOrder = 7))
                                }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
