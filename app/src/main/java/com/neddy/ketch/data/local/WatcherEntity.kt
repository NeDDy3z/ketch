package com.neddy.ketch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.TriggerType
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek

@Entity(tableName = "watchers")
data class WatcherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val originName: String,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationName: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val triggerType: String,
    val triggerLatitude: Double,
    val triggerLongitude: Double,
    val triggerRadiusMeters: Int,
    /** Comma separated ISO day numbers, Monday = 1 through Sunday = 7. */
    val activeDays: String,
    val windowStartMinutes: Int,
    val windowEndMinutes: Int,
    val notificationsEnabled: Boolean,
    val maxTransfers: Int?,
    val maxTravelMinutes: Int?,
    val enabled: Boolean,
    val lastTriggeredAt: Long?,
)

fun WatcherEntity.toDomain(): Watcher = Watcher(
    id = id,
    name = name,
    origin = StopPlace(originName, originLatitude, originLongitude),
    destination = StopPlace(destinationName, destinationLatitude, destinationLongitude),
    triggerType = TriggerType.valueOf(triggerType),
    triggerLatitude = triggerLatitude,
    triggerLongitude = triggerLongitude,
    triggerRadiusMeters = triggerRadiusMeters,
    activeDays = activeDays.split(',')
        .filter { it.isNotBlank() }
        .map { DayOfWeek.of(it.trim().toInt()) }
        .toSet(),
    windowStartMinutes = windowStartMinutes,
    windowEndMinutes = windowEndMinutes,
    notificationsEnabled = notificationsEnabled,
    maxTransfers = maxTransfers,
    maxTravelMinutes = maxTravelMinutes,
    enabled = enabled,
    lastTriggeredAt = lastTriggeredAt,
)

fun Watcher.toEntity(): WatcherEntity = WatcherEntity(
    id = id,
    name = name,
    originName = origin.name,
    originLatitude = origin.latitude,
    originLongitude = origin.longitude,
    destinationName = destination.name,
    destinationLatitude = destination.latitude,
    destinationLongitude = destination.longitude,
    triggerType = triggerType.name,
    triggerLatitude = triggerLatitude,
    triggerLongitude = triggerLongitude,
    triggerRadiusMeters = triggerRadiusMeters,
    activeDays = activeDays.map { it.value }.sorted().joinToString(","),
    windowStartMinutes = windowStartMinutes,
    windowEndMinutes = windowEndMinutes,
    notificationsEnabled = notificationsEnabled,
    maxTransfers = maxTransfers,
    maxTravelMinutes = maxTravelMinutes,
    enabled = enabled,
    lastTriggeredAt = lastTriggeredAt,
)
