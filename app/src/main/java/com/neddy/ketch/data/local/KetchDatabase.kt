package com.neddy.ketch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WatcherEntity::class], version = 4, exportSchema = false)
abstract class KetchDatabase : RoomDatabase() {

    abstract fun watcherDao(): WatcherDao

    companion object {
        @Volatile
        private var instance: KetchDatabase? = null

        /**
         * Adds the connection preference and home ordering columns without
         * dropping existing watchers.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchers ADD COLUMN preferredVehicle TEXT")
                db.execSQL("ALTER TABLE watchers ADD COLUMN maxTravelDeltaMinutes INTEGER")
                db.execSQL(
                    "ALTER TABLE watchers ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /** Adds the optional car start point, leaving existing watchers on foot. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watchers ADD COLUMN carStartName TEXT")
                db.execSQL("ALTER TABLE watchers ADD COLUMN carStartLatitude REAL")
                db.execSQL("ALTER TABLE watchers ADD COLUMN carStartLongitude REAL")
            }
        }

        fun get(context: Context): KetchDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KetchDatabase::class.java,
                    "ketch.db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // Safety net for any version gap without an explicit path.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
