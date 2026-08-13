package com.neddy.ketch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.ketch.BuildConfig
import com.neddy.ketch.appContainer
import com.neddy.ketch.data.settings.ColorPalette
import com.neddy.ketch.data.settings.RefreshScope
import com.neddy.ketch.data.settings.SettingsRepository
import com.neddy.ketch.data.update.UpdateRepository
import com.neddy.ketch.domain.WalkAdjustment
import com.neddy.ketch.ui.components.SkeletonBox
import com.neddy.ketch.ui.theme.description
import com.neddy.ketch.ui.theme.displayName
import com.neddy.ketch.ui.theme.paletteSwatch
import java.time.DayOfWeek
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenHelp: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(context.appContainer) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateCheck by viewModel.updateCheck.collectAsStateWithLifecycle()
    var paletteSheetOpen by remember { mutableStateOf(false) }
    var windowDialogOpen by remember { mutableStateOf(false) }
    var radiusDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 26.sp,
                )
            }
        },
    ) { padding ->
        val current = settings
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsGroup(title = "Appearance") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    PaletteRow(
                        palette = current.palette,
                        onClick = { paletteSheetOpen = true },
                    )
                }
                Text(
                    text = "Ketch is dark-only — the palette sets its tones.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            SettingsGroup(title = "Journey") {
                SliderCard(
                    title = "Walk faster than the map thinks",
                    value = current.walkReductionPercent.toFloat(),
                    valueLabel = if (current.walkReductionPercent == 0) {
                        "Off"
                    } else {
                        "-${current.walkReductionPercent}%"
                    },
                    valueRange = 0f..WalkAdjustment.MAX_PERCENT.toFloat(),
                    steps = 9,
                    onValueChange = { viewModel.setWalkReductionPercent(it.toInt()) },
                    description = "Takes this much off the calculated walking time, " +
                        "so connections you can still make are not written off. " +
                        "Costs a second lookup per watcher.",
                )
                SliderCard(
                    title = "Driving above",
                    value = current.carSpeedThresholdKmh.toFloat(),
                    valueLabel = "${current.carSpeedThresholdKmh} km/h",
                    valueRange = SettingsRepository.MIN_CAR_SPEED_KMH.toFloat()..
                        SettingsRepository.MAX_CAR_SPEED_KMH.toFloat(),
                    steps = 7,
                    onValueChange = { viewModel.setCarSpeedThresholdKmh(it.toInt()) },
                    description = "Leave this fast and Ketch assumes you took the car, " +
                        "routing from a watcher's car start point instead of your door.",
                )
            }

            SettingsGroup(title = "Gestures") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Double-tap opens in Google Maps",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Double-tap a connection on Home to launch it as a " +
                                "route in Google Maps.",
                            fontSize = 12.sp,
                            lineHeight = 17.4.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = current.doubleTapOpensMaps,
                        onCheckedChange = viewModel::setDoubleTapOpensMaps,
                    )
                }
                Text(
                    text = "Tap a watcher to edit it, long-press to open its details.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            SettingsGroup(title = "Refresh") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    RadioRow(
                        selected = current.refreshScope == RefreshScope.ALL,
                        title = "Refresh all watchers",
                        onClick = { viewModel.setRefreshScope(RefreshScope.ALL) },
                        tintWhenSelected = true,
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    RadioRow(
                        selected = current.refreshScope == RefreshScope.ACTIVE,
                        title = "Only active watchers",
                        onClick = { viewModel.setRefreshScope(RefreshScope.ACTIVE) },
                        tintWhenSelected = true,
                    )
                }
            }

            SettingsGroup(title = "Transit data") {
                var apiKeyText by remember(current.apiKey) { mutableStateOf(current.apiKey) }
                var apiKeyVisible by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "API key",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        BasicTextField(
                            value = apiKeyText,
                            onValueChange = {
                                apiKeyText = it
                                viewModel.setApiKey(it)
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            visualTransformation = if (apiKeyVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector = if (apiKeyVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (apiKeyVisible) {
                                "Hide API key"
                            } else {
                                "Show API key"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Text(
                    text = "Used for connection lookups. Stored only on this device.",
                    fontSize = 12.sp,
                    lineHeight = 17.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            SettingsGroup(title = "Defaults for new watchers") {
                SubLabel("Active days")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in current.watcherDefaults.activeDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                )
                                .clickable { viewModel.toggleDefaultDay(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.getDisplayName(
                                    java.time.format.TextStyle.NARROW,
                                    Locale.getDefault(),
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }

                // Two compact value cards rather than three sliders: the window
                // reads as one range, and each opens its own picker.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ValueCard(
                        overline = "Window",
                        value = "${formatMinutes(current.watcherDefaults.windowStartMinutes)}–" +
                            formatMinutes(current.watcherDefaults.windowEndMinutes),
                        onClick = { windowDialogOpen = true },
                        modifier = Modifier.weight(1f),
                    )
                    ValueCard(
                        overline = "Radius",
                        value = "${current.watcherDefaults.triggerRadiusMeters} m",
                        onClick = { radiusDialogOpen = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SettingsGroup(title = "Updates") {
                UpdatesCard(
                    enabled = current.updateChecksEnabled,
                    checkState = updateCheck,
                    onEnabledChange = viewModel::setUpdateChecksEnabled,
                    onCheckNow = viewModel::checkForUpdate,
                )
            }

            SettingsGroup(title = "Support") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onOpenHelp)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Help & feedback",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "FAQ, troubleshooting, report an issue",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ketch v${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/NeDDy3z/ketch")
                    },
                )
            }
        }

        // A sheet rather than a page, so the change stays visible behind it.
        if (paletteSheetOpen) {
            PaletteSheet(
                selected = current.palette,
                onSelect = viewModel::setPalette,
                onDismiss = { paletteSheetOpen = false },
            )
        }

        if (windowDialogOpen) {
            WindowDialog(
                startMinutes = current.watcherDefaults.windowStartMinutes,
                endMinutes = current.watcherDefaults.windowEndMinutes,
                onConfirm = { start, end ->
                    viewModel.setDefaultWindowStart(start)
                    viewModel.setDefaultWindowEnd(end)
                    windowDialogOpen = false
                },
                onDismiss = { windowDialogOpen = false },
            )
        }

        if (radiusDialogOpen) {
            RadiusDialog(
                meters = current.watcherDefaults.triggerRadiusMeters,
                onConfirm = {
                    viewModel.setDefaultRadius(it)
                    radiusDialogOpen = false
                },
                onDismiss = { radiusDialogOpen = false },
            )
        }
    }
}

/**
 * The Appearance entry point: the active palette's name, its subtitle and the
 * three tones that carry the UI, previewed as overlapping swatches.
 */
@Composable
private fun PaletteRow(palette: ColorPalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = "Color palette", fontSize = 15.sp)
            Text(
                text = "${palette.displayName} · ${palette.description}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PaletteSwatches(palette = palette, size = 22.dp)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * One list, wallpaper first: Material You leads as the shipping default and the
 * seven fixed seeds follow. Tapping a row re-tints everything live — the sheet
 * stays open so the change is visible behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaletteSheet(
    selected: ColorPalette,
    onSelect: (ColorPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Color palette",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = "Applies instantly · dark tones only",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ColorPalette.entries.forEachIndexed { index, palette ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    PaletteSheetRow(
                        palette = palette,
                        selected = palette == selected,
                        onClick = { onSelect(palette) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSheetRow(
    palette: ColorPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaletteSwatches(palette = palette, size = 26.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = palette.displayName,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(
                text = palette.description,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (selected) {
                Icons.Filled.RadioButtonChecked
            } else {
                Icons.Filled.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Each palette previewed by primary, tertiary and its card surface — the three
 * tones that actually carry the UI — rather than an abstract swatch. Wallpaper
 * gets a conic sweep of the live dynamic tones instead.
 */
@Composable
private fun PaletteSwatches(palette: ColorPalette, size: Dp) {
    val scheme = MaterialTheme.colorScheme
    val overlap = size / 3
    if (palette == ColorPalette.WALLPAPER) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            scheme.primary,
                            scheme.tertiary,
                            scheme.secondary,
                            scheme.primary,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Wallpaper,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(size / 2),
            )
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(-overlap)) {
        paletteSwatch(palette, scheme).forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        // The surface tone needs an edge or it vanishes into the row.
                        if (index == 2) {
                            Modifier.border(1.dp, scheme.outlineVariant, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        content()
    }
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun RadioRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    tintWhenSelected: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (tintWhenSelected && selected) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = if (subtitle == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        Icon(
            imageVector = if (selected) {
                Icons.Filled.RadioButtonChecked
            } else {
                Icons.Filled.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(22.dp),
        )
        if (subtitle == null) {
            Text(text = title, fontSize = 15.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 17.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Ketch is sideloaded, so this card is the whole update story: the automatic
 * watch on the release page, a manual check, and the way out to the download
 * when there is something newer.
 */
@Composable
private fun UpdatesCard(
    enabled: Boolean,
    checkState: UpdateCheckState,
    onEnabledChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Watch for new releases",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Checks the GitHub release page a few times a day and " +
                        "offers the new APK. Turn back on here after " +
                        "“Don't remind me”.",
                    fontSize = 12.sp,
                    lineHeight = 17.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "This build",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when (checkState) {
                        UpdateCheckState.Idle ->
                            "Ketch ${BuildConfig.VERSION_NAME}"
                        UpdateCheckState.Checking -> "Checking…"
                        UpdateCheckState.UpToDate ->
                            "Ketch ${BuildConfig.VERSION_NAME} · up to date"
                        is UpdateCheckState.Available ->
                            "Ketch ${checkState.update.version} available"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (checkState is UpdateCheckState.Available) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            if (checkState is UpdateCheckState.Available) {
                TextButton(
                    onClick = { uriHandler.openUri(checkState.update.downloadUrl) },
                ) {
                    Text(text = "Get it", fontWeight = FontWeight.SemiBold)
                }
            }
            TextButton(
                onClick = onCheckNow,
                enabled = checkState != UpdateCheckState.Checking,
            ) {
                Text(text = "Check now")
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(UpdateRepository.LATEST_RELEASE_URL) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.NewReleases,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Latest release on GitHub",
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A setting that is a number on a scale: the value reads out next to its title
 * and the explanation sits under the slider, so the card says what moving it
 * actually does.
 */
@Composable
private fun SliderCard(
    title: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                text = valueLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 17.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "HH:mm" for a minute-of-day. */
private fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

/** A compact labelled value that opens its own picker. */
@Composable
private fun ValueCard(
    overline: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = overline,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

/** From/To time inputs for the default active window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WindowDialog(
    startMinutes: Int,
    endMinutes: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = rememberTimePickerState(
        initialHour = startMinutes / 60,
        initialMinute = startMinutes % 60,
        is24Hour = true,
    )
    val end = rememberTimePickerState(
        initialHour = endMinutes / 60,
        initialMinute = endMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default window") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SubLabel("From")
                TimeInput(state = start)
                SubLabel("To")
                TimeInput(state = end)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        start.hour * 60 + start.minute,
                        end.hour * 60 + end.minute,
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Slider for the default leave radius, with a live readout. */
@Composable
private fun RadiusDialog(
    meters: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(meters.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default leave radius") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${value.toInt()} m",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 100f..1000f,
                    steps = 8,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "100 m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "1000 m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.toInt()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
