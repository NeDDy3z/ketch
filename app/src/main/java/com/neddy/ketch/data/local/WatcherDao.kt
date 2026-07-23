package com.neddy.ketch.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatcherDao {

    @Query("SELECT * FROM watchers ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<WatcherEntity>>

    @Query("SELECT * FROM watchers ORDER BY sortOrder, name")
    suspend fun getAll(): List<WatcherEntity>

    @Query("SELECT * FROM watchers WHERE id = :id")
    suspend fun getById(id: Long): WatcherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watcher: WatcherEntity): Long

    @Update
    suspend fun update(watcher: WatcherEntity)

    @Delete
    suspend fun delete(watcher: WatcherEntity)

    @Query("UPDATE watchers SET lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun markTriggered(id: Long, timestamp: Long)

    @Query("UPDATE watchers SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)

    /**
     * Writes the whole ordering atomically so overlapping reorder commits
     * cannot interleave into an inconsistent order.
     */
    @Transaction
    suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM watchers")
    suspend fun maxSortOrder(): Int
}
