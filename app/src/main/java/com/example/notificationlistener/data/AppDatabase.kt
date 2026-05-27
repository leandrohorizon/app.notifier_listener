package com.example.notificationlistener.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NotificationEntity::class, 
        AppFilterEntity::class, 
        KeywordEntity::class, 
        MuteRuleEntity::class,
        SavedFilterEntity::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appFilterDao(): AppFilterDao
    abstract fun keywordDao(): KeywordDao
    abstract fun muteRuleDao(): MuteRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_database"
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val database = getDatabase(context)
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultKeywords = listOf("token", "chave", "senha", "2fa", "verification", "código", "otp", "code", "pix")
                            defaultKeywords.forEach { word ->
                                database.keywordDao().insert(KeywordEntity(word))
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
