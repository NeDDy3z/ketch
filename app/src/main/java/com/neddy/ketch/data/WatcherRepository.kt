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

    suspend fun save(watcher: Watcher): Long = dao.insert(watcher.toEntity())

    suspend fun delete(watcher: Watcher) = dao.delete(watcher.toEntity())

    suspend fun markTriggered(id: Long, timestamp: Long) = dao.markTriggered(id, timestamp)
}
