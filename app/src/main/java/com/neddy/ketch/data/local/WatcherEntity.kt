package com.neddy.ketch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neddy.ketch.domain.model.CarLeg
import com.neddy.ketch.domain.model.StopPlace
import com.neddy.ketch.domain.model.VehicleCategory
import com.neddy.ketch.domain.model.Watcher
import java.time.DayOfWeek

@Entity(tableName = "watchers")
data class WatcherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val icon: String,
    val destinationName: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    /**
     * The car swap stop, all three columns set or all three null. The column
     * names are historical, from when this was a plain start point.
     */
    val carStartName: String? = null,
    val carStartLatitude: Double? = null,
    val carStartLongitude: Double? = null,
    /** [CarLeg] name: which stretch of the journey the car covers. */
    val carLegMode: String? = null,
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
    /** [VehicleCategory] name, or null for no preference. */
    val preferredVehicle: String?,
    val maxTravelDeltaMinutes: Int?,
    val enabled: Boolean,
    val sortOrder: Int,
    val lastTriggeredAt: Long?,
)

fun WatcherEntity.toDomain(): Watcher = Watcher(
    id = id,
    name = name,
    icon = icon,
    destination = StopPlace(destinationName, destinationLatitude, destinationLongitude),
    carStop = if (carStartLatitude != null && carStartLongitude != null) {
        StopPlace(carStartName.orEmpty(), carStartLatitude, carStartLongitude)
    } else {
        null
    },
    carLeg = carLegMode
        ?.let { runCatching { CarLeg.valueOf(it) }.getOrNull() }
    // A stop saved before the leg existed was always a drive to it.
        ?: if (carStartLatitude != null) CarLeg.TO_STOP else CarLeg.NONE,
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
    preferredVehicle = preferredVehicle
        ?.let { runCatching { VehicleCategory.valueOf(it) }.getOrNull() },
    maxTravelDeltaMinutes = maxTravelDeltaMinutes,
    enabled = enabled,
    sortOrder = sortOrder,
    lastTriggeredAt = lastTriggeredAt,
)

fun Watcher.toEntity(): WatcherEntity = WatcherEntity(
    id = id,
    name = name,
    icon = icon,
    destinationName = destination.name,
    destinationLatitude = destination.latitude,
    destinationLongitude = destination.longitude,
    carStartName = carStop?.name,
    carStartLatitude = carStop?.latitude,
    carStartLongitude = carStop?.longitude,
    carLegMode = carLeg.name,
    triggerLatitude = triggerLatitude,
    triggerLongitude = triggerLongitude,
    triggerRadiusMeters = triggerRadiusMeters,
    activeDays = activeDays.map { it.value }.sorted().joinToString(","),
    windowStartMinutes = windowStartMinutes,
    windowEndMinutes = windowEndMinutes,
    notificationsEnabled = notificationsEnabled,
    maxTransfers = maxTransfers,
    maxTravelMinutes = maxTravelMinutes,
    preferredVehicle = preferredVehicle?.name,
    maxTravelDeltaMinutes = maxTravelDeltaMinutes,
    enabled = enabled,
    sortOrder = sortOrder,
    lastTriggeredAt = lastTriggeredAt,
)
