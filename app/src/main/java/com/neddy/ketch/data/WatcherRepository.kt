package com.neddy.ketch.data

import com.neddy.ketch.data.local.WatcherDao
import com.neddy.ketch.data.local.toDomain
import com.neddy.ketch.data.local.toEntity
import com.neddy.ketch.domain.model.Watcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatcherRepository(private val dao: WatcherDao) {

    fun observeWatchers(): Flow<List<Watcher>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getWatchers(): List<Watcher> = dao.getAll().map { it.toDomain() }

    suspend fun getWatcher(id: Long): Watcher? = dao.getById(id)?.toDomain()

    suspend fun save(watcher: Watcher): Long {
        // A brand new watcher goes to the end of the home ordering.
        val toSave = if (watcher.id == 0L) {
            watcher.copy(sortOrder = dao.maxSortOrder() + 1)
        } else {
            watcher
        }
        return dao.insert(toSave.toEntity())
    }

    suspend fun delete(watcher: Watcher) = dao.delete(watcher.toEntity())

    /** Persists [orderedIds] as the new home ordering, first id shown first. */
    suspend fun reorder(orderedIds: List<Long>) = dao.applyOrder(orderedIds)

    suspend fun markTriggered(id: Long, timestamp: Long) = dao.markTriggered(id, timestamp)
}
