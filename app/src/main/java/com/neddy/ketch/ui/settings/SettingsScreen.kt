package com.neddy.ketch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neddy.ketch.appContainer
import com.neddy.ketch.data.settings.EditGesture
import com.neddy.ketch.data.settings.RefreshScope
import com.neddy.ketch.data.settings.ThemeMode
import com.neddy.ketch.ui.components.SkeletonBox
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(context.appContainer) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        val current = settings
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CategoryCard(title = "Theme") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = current.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                        ) {
                            Text(
                                mode.name.lowercase()
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                            )
                        }
                    }
                }
            }

            CategoryCard(title = "Editing") {
                Text(
                    text = "How opening a watcher from the home screen works.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    EditGesture.entries.forEachIndexed { index, gesture ->
                        SegmentedButton(
                            selected = current.editGesture == gesture,
                            onClick = { viewModel.setEditGesture(gesture) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = EditGesture.entries.size,
                            ),
                        ) {
                            Text(
                                when (gesture) {
                                    EditGesture.TAP -> "Tap to edit"
                                    EditGesture.HOLD -> "Hold to edit"
                                },
                            )
                        }
                    }
                }
            }

            CategoryCard(title = "Refresh") {
                Text(
                    text = "What pull to refresh on the home screen looks up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RefreshScope.entries.forEachIndexed { index, scope ->
                        SegmentedButton(
                            selected = current.refreshScope == scope,
                            onClick = { viewModel.setRefreshScope(scope) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = RefreshScope.entries.size,
                            ),
                        ) {
                            Text(
                                when (scope) {
                                    RefreshScope.ALL -> "All"
                                    RefreshScope.ACTIVE -> "Active only"
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "Active only skips watchers outside their day and time " +
                        "window, so a 12:00–14:00 watcher refreshes at 12:30 but not " +
                        "at 15:00.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CategoryCard(title = "Google Maps Platform API key") {
                var apiKeyText by remember(current.apiKey) { mutableStateOf(current.apiKey) }
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        viewModel.setApiKey(it)
                    },
                    label = { Text("API key") },
                    supportingText = {
                        Text(
                            "Used for the Routes and Places APIs. Can also be provided " +
                                "at build time via GOOGLE_MAPS_API_KEY in local.properties.",
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CategoryCard(title = "New watcher defaults") {
                Text("Active days", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in current.watcherDefaults.activeDays,
                            onClick = { viewModel.toggleDefaultDay(day) },
                            label = {
                                Text(
                                    text = day.getDisplayName(
                                        TextStyle.NARROW,
                                        Locale.getDefault(),
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Text(
                    text = "Time window: %02d:%02d to %02d:%02d".format(
                        current.watcherDefaults.windowStartMinutes / 60,
                        current.watcherDefaults.windowStartMinutes % 60,
                        current.watcherDefaults.windowEndMinutes / 60,
                        current.watcherDefaults.windowEndMinutes % 60,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Window start", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = current.watcherDefaults.windowStartMinutes.toFloat(),
                    onValueChange = { viewModel.setDefaultWindowStart(it.toInt() / 15 * 15) },
                    valueRange = 0f..(24f * 60f - 15f),
                )
                Text("Window end", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = current.watcherDefaults.windowEndMinutes.toFloat(),
                    onValueChange = { viewModel.setDefaultWindowEnd(it.toInt() / 15 * 15) },
                    valueRange = 0f..(24f * 60f - 15f),
                )

                Text(
                    text = "Leave radius: ${current.watcherDefaults.triggerRadiusMeters} m",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = current.watcherDefaults.triggerRadiusMeters.toFloat(),
                    onValueChange = { viewModel.setDefaultRadius(it.toInt()) },
                    valueRange = 100f..1000f,
                    steps = 8,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val uriHandler = LocalUriHandler.current
            Text(
                text = "Erik Vaněk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://github.com/NeDDy3z/ketch") }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}
