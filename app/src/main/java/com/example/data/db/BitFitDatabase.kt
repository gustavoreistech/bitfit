package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.UserSettings
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Database(
    entities = [WorkoutRecord::class, WeightRecord::class, UserSettings::class],
    version = 4,
    exportSchema = false
)
abstract class BitFitDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun weightDao(): WeightDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: BitFitDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_records ADD COLUMN runningSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_records ADD COLUMN walkingSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_records ADD COLUMN routineDetailsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): BitFitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BitFitDatabase::class.java,
                    "bitfit_local.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                ensureDefaultSettingsRow(database)
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                ensureDefaultSettingsRow(database)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun ensureDefaultSettingsRow(database: BitFitDatabase) {
            val existingSettings = database.settingsDao().getSettings().firstOrNull()
            if (existingSettings == null) {
                database.settingsDao().saveSettings(
                    UserSettings(
                        id = 1,
                        userName = "",
                        initialWeightKg = 78.0f,
                        heightMeters = 1.75f,
                        targetWeightKg = 72.0f,
                        targetRunDistanceMeters = 1600,
                        targetRunFrequency = 3,
                        targetCalisDays = 4,
                        activeCalisDaysCsv = "SEG,TER,QUI,SEX",
                        streakDays = 0,
                        isProfileConfigured = false
                    )
                )
            }
        }
    }
}
