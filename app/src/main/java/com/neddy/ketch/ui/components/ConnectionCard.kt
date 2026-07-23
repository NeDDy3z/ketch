package com.neddy.ketch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neddy.ketch.domain.model.TransitConnection
import com.neddy.ketch.domain.model.TransitLeg
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Aligns digits into equal-width columns so times line up. */
private val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

/** Fixed width of a time column in the horizontal journey timeline. */
private val TIME_COLUMN_WIDTH = 52.dp

private fun formatTime(instant: Instant, zone: ZoneId): String =
    timeFormatter.format(instant.atZone(zone))

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
    titleIcon: ImageVector? = null,
    subtitle: String? = null,
) {
    val zone = ZoneId.systemDefault()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            WatcherCardHeader(
                title = title,
                subtitle = subtitle,
                titleIcon = titleIcon,
                trailingContent = {
                    DurationPill(minutes = connection.travelDuration.toMinutes())
                },
            )
            if (connection.legs.size <= 2) {
                HorizontalTimeline(legs = connection.legs, zone = zone)
            } else {
                VerticalTimeline(legs = connection.legs, zone = zone)
            }
            CardFooter(connection = connection, zone = zone)
        }
    }
}

/**
 * Shared header of the home watcher cards: icon tile, title with optional
 * subtitle and an optional trailing element such as the duration pill.
 */
@Composable
fun WatcherCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (titleIcon != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = titleIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) trailingContent()
    }
}

@Composable
private fun DurationPill(minutes: Long) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = "$minutes min",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = TabularNumbers,
            )
        }
    }
}

/**
 * Timeline for direct and one-transfer connections: times and rail segments
 * on one row, stop names lined up underneath.
 */
@Composable
private fun HorizontalTimeline(legs: List<TransitLeg>, zone: ZoneId) {
    val first = legs.first()
    val last = legs.last()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimelineTime(
                instant = first.departureTime,
                zone = zone,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RailCell(leg = first, modifier = Modifier.weight(1f))
            if (legs.size == 2) {
                TimelineTime(
                    instant = legs[1].departureTime,
                    zone = zone,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RailCell(leg = legs[1], modifier = Modifier.weight(1f))
            }
            TimelineTime(
                instant = last.arrivalTime,
                zone = zone,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TimelineStop(name = first.departureStop, textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.weight(1f))
            if (legs.size == 2) {
                TimelineStop(name = legs[1].departureStop, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.weight(1f))
            }
            TimelineStop(name = last.arrivalStop, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun TimelineTime(
    instant: Instant,
    zone: ZoneId,
    textAlign: TextAlign,
    color: Color,
) {
    Text(
        text = formatTime(instant, zone),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = textAlign,
        style = TabularNumbers,
        maxLines = 1,
        modifier = Modifier.width(TIME_COLUMN_WIDTH),
    )
}

@Composable
private fun TimelineStop(name: String, textAlign: TextAlign) {
    Text(
        text = name,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(TIME_COLUMN_WIDTH),
    )
}

/** A rail segment with the leg's line-code chip punched out of its middle. */
@Composable
private fun RailCell(leg: TransitLeg, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        LineChip(leg = leg)
    }
}

@Composable
private fun LineChip(leg: TransitLeg, large: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = if (large) {
                Modifier.padding(start = 8.dp, end = 11.dp, top = 3.dp, bottom = 3.dp)
            } else {
                Modifier.padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vehicleIcon(leg.vehicleType),
                contentDescription = leg.vehicleType,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (large) 14.dp else 13.dp),
            )
            Text(
                text = leg.lineCode,
                fontSize = if (large) 11.sp else 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

/**
 * Timeline for connections with three or more legs: a vertical rail with a
 * node per stop and a line-code chip per leg between them.
 */
@Composable
private fun VerticalTimeline(legs: List<TransitLeg>, zone: ZoneId) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 94.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            TimelineNodeRow(
                time = formatTime(legs.first().departureTime, zone),
                stop = legs.first().departureStop,
                isFinal = false,
            )
            legs.forEach { leg ->
                LegChipRow(leg = leg)
                TimelineNodeRow(
                    time = formatTime(leg.arrivalTime, zone),
                    stop = leg.arrivalStop,
                    isFinal = leg === legs.last(),
                )
            }
        }
    }
}

@Composable
private fun TimelineNodeRow(time: String, stop: String, isFinal: Boolean) {
    val accent = if (isFinal) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val tickColor = if (isFinal) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val dotColor = if (isFinal) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.width(80.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = time,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                style = TabularNumbers,
                maxLines = 1,
            )
            Text(
                text = stop,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Tick leading into the rail at x = 95dp, node dot centered on it
        // with a card-colored ring so the rail reads as passing behind.
        Box(modifier = Modifier.size(width = 22.dp, height = 14.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 3.dp)
                    .size(width = 12.dp, height = 2.dp)
                    .background(tickColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 8.dp)
                    .size(13.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(dotColor, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun LegChipRow(leg: TransitLeg) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(96.dp))
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        LineChip(leg = leg, large = true)
    }
}

@Composable
private fun CardFooter(connection: TransitConnection, zone: ZoneId) {
    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier.padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterItem(
                icon = Icons.Filled.Schedule,
                text = "Arrives ${formatTime(connection.arrivalTime, zone)}",
            )
            if (connection.transfers == 0) {
                FooterItem(icon = Icons.AutoMirrored.Filled.TrendingFlat, text = "Direct")
            } else {
                FooterItem(
                    icon = Icons.Filled.SyncAlt,
                    text = "${connection.transfers} transfer" +
                        if (connection.transfers > 1) "s" else "",
                )
            }
        }
    }
}

@Composable
private fun FooterItem(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = TabularNumbers,
        )
    }
}
