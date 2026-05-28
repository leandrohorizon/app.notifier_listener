package com.example.notificationlistener.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
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
    version = 10,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = AppDatabase.MyAutoMigration::class),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9, spec = AppDatabase.DeleteKeywordQuerySpec::class),
        AutoMigration(from = 9, to = 10, spec = AppDatabase.MuteMigrationSpec::class)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    @RenameColumn(tableName = "saved_filters", fromColumnName = "query", toColumnName = "keyword_query")
    @RenameColumn(tableName = "saved_filters", fromColumnName = "package_filter", toColumnName = "package_names")
    class MyAutoMigration : AutoMigrationSpec

    @DeleteColumn(tableName = "saved_filters", columnName = "keyword_query")
    class DeleteKeywordQuerySpec : AutoMigrationSpec

    @RenameColumn(tableName = "mute_rules", fromColumnName = "text_keyword", toColumnName = "keywords_to_mute")
    @DeleteColumn(tableName = "mute_rules", columnName = "category")
    @DeleteColumn(tableName = "mute_rules", columnName = "channel_id")
    class MuteMigrationSpec : AutoMigrationSpec

    abstract fun notificationDao(): NotificationDao
    abstract fun appFilterDao(): AppFilterDao
    abstract fun keywordDao(): KeywordDao
    abstract fun muteRuleDao(): MuteRuleDao
    abstract fun savedFilterDao(): SavedFilterDao

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
