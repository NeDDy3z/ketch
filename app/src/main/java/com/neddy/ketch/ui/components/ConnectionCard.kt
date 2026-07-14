package com.neddy.ketch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun vehicleIcon(vehicleType: String): ImageVector = when (vehicleType.uppercase()) {
    "HEAVY_RAIL", "COMMUTER_TRAIN", "HIGH_SPEED_TRAIN", "LONG_DISTANCE_TRAIN", "RAIL" ->
        Icons.Filled.DirectionsRailway
    "SUBWAY", "METRO_RAIL" -> Icons.Filled.Subway
    "TRAM", "LIGHT_RAIL" -> Icons.Filled.Tram
    else -> Icons.Filled.DirectionsBus
}

@Composable
fun ConnectionCard(
    title: String,
    connection: TransitConnection,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${connection.travelDuration.toMinutes()} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            connection.legs.forEach { leg ->
                LegRow(leg = leg, zone = zone)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Arrives ${timeFormatter.format(connection.arrivalTime.atZone(zone))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (connection.transfers == 0) {
                        "Direct"
                    } else {
                        "${connection.transfers} transfer" +
                            if (connection.transfers > 1) "s" else ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegRow(leg: TransitLeg, zone: ZoneId) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = vehicleIcon(leg.vehicleType),
            contentDescription = leg.vehicleType,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "${leg.departureStop} (${leg.lineCode})",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${timeFormatter.format(leg.departureTime.atZone(zone))} " +
                    "to ${leg.arrivalStop} " +
                    timeFormatter.format(leg.arrivalTime.atZone(zone)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}
