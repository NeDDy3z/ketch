package com.neddy.ketch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WatcherEntity::class], version = 1, exportSchema = false)
abstract class KetchDatabase : RoomDatabase() {

    abstract fun watcherDao(): WatcherDao

    companion object {
        @Volatile
        private var instance: KetchDatabase? = null

        fun get(context: Context): KetchDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KetchDatabase::class.java,
                    "ketch.db",
                ).build().also { instance = it }
            }
    }
}
