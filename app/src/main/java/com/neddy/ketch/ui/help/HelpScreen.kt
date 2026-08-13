package com.neddy.ketch.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neddy.ketch.BuildConfig
import com.neddy.ketch.data.update.UpdateRepository

private const val REPO_URL = "https://github.com/NeDDy3z/ketch"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val DISCUSSIONS_URL = "$REPO_URL/discussions"
private const val RELEASES_URL = UpdateRepository.RELEASES_URL
private const val LATEST_RELEASE_URL = UpdateRepository.LATEST_RELEASE_URL

/** One frequently asked question and its answer. */
private data class Faq(val question: String, val answer: String)

private val FAQS = listOf(
    Faq(
        question = "Why do I see “No connection found”?",
        answer = "Usually the trigger window has passed or the radius is too tight. " +
            "Widen the radius to 300 m, or check that the watcher's days include today.",
    ),
    Faq(
        question = "Where do I get an API key?",
        answer = "Ketch looks connections up through the Google Routes and Places APIs. " +
            "Create a key in the Google Cloud console, enable both APIs on it, then paste " +
            "it into Settings → Transit data. It is stored only on this device.",
    ),
    Faq(
        question = "Does Ketch track my location in the background?",
        answer = "Only as a geofence. Ketch registers a circle around each watcher's " +
            "trigger and Android wakes it when you leave — there is no location history, " +
            "and nothing leaves the device except the stops sent to the transit API.",
    ),
    Faq(
        question = "Why is the widget not refreshing?",
        answer = "The widget refreshes on page change and every 15 minutes inside a " +
            "watcher's active window, and is skipped entirely while resting — so it costs " +
            "nothing overnight. Outside every window it keeps the last line and dims it.",
    ),
    Faq(
        question = "What does a long press on a watcher do?",
        answer = "It opens the watcher's details: the connection you would be told to " +
            "catch right now, a quicker one worth waiting for when there is one, and " +
            "the edit and delete actions. A plain tap still opens the editor.",
    ),
    Faq(
        question = "How does the car leg work?",
        answer = "Set the stop where you swap car and transit, and which stretch the car " +
            "covers. Driving out, Ketch looks the connection up from that stop and " +
            "remembers the car is parked there; coming home, it plans the train to the " +
            "waiting car and tells you to drive the rest. Take the bus instead and both " +
            "journeys stay on public transport. Missed a drive? The Car card on the " +
            "watcher's details page has a switch for it.",
    ),
    Faq(
        question = "Ketch says I have missed a connection I can still catch",
        answer = "Routing providers assume a slow walk. Settings → Journey lets you take " +
            "a percentage off the calculated walking time; 10% is the default. It costs " +
            "one extra lookup per watcher, so turn it to 0% to halve the quota.",
    ),
    Faq(
        question = "Why is a watcher shown as resting?",
        answer = "Its active days or time window do not contain the current moment. " +
            "Resting watchers are skipped by refresh so a morning commute never spends " +
            "quota on an evening one; a tap wakes one for a single lookup.",
    ),
)

/**
 * Help &amp; feedback (docs/design_document.md · 07). Answers first, contact
 * second: search and the two getting-started cards sit above the fold, and the
 * outbound GitHub rows come after the FAQ.
 */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(FAQS.first().question) }

    // Search filters the FAQ as you type; only one answer stays expanded.
    val visibleFaqs = remember(query) {
        if (query.isBlank()) {
            FAQS
        } else {
            FAQS.filter {
                it.question.contains(query, ignoreCase = true) ||
                    it.answer.contains(query, ignoreCase = true)
            }
        }
    }

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
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "Help",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 26.sp,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SearchField(query = query, onQueryChange = { query = it })

            if (query.isBlank()) {
                HelpGroup(title = "Getting started") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // The same primary/tertiary container pairing the journey
                        // cards use for departure vs. arrival.
                        StarterCard(
                            icon = Icons.Filled.Explore,
                            title = "How watchers work",
                            meta = "2 min read",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { uriHandler.openUri("$REPO_URL#how-watchers-work") },
                            modifier = Modifier.weight(1f),
                        )
                        StarterCard(
                            icon = Icons.Filled.MyLocation,
                            title = "Triggers & radius",
                            meta = "3 min read",
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = { uriHandler.openUri("$REPO_URL#triggers-and-radius") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HelpGroup(title = "Common questions") {
                if (visibleFaqs.isEmpty()) {
                    Text(
                        text = "No answers match “$query”. Try fewer words, or open an " +
                            "issue below.",
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        visibleFaqs.forEachIndexed { index, faq ->
                            if (index > 0) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            FaqRow(
                                faq = faq,
                                expanded = expanded == faq.question,
                                onToggle = {
                                    expanded = if (expanded == faq.question) {
                                        null
                                    } else {
                                        faq.question
                                    }
                                },
                            )
                        }
                    }
                }
            }

            HelpGroup(title = "Report a problem") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    OutboundRow(
                        icon = Icons.Filled.BugReport,
                        title = "Report an issue on GitHub",
                        subtitle = "Bugs, ideas and wrong departure data — one tracker " +
                            "for all three",
                        onClick = { uriHandler.openUri(ISSUES_URL) },
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    OutboundRow(
                        icon = Icons.Filled.Forum,
                        title = "Open discussions",
                        subtitle = "github.com/NeDDy3z/ketch",
                        onClick = { uriHandler.openUri(DISCUSSIONS_URL) },
                    )
                }
            }

            HelpGroup(title = "Updates") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    OutboundRow(
                        icon = Icons.Filled.NewReleases,
                        title = "Latest release",
                        subtitle = "Release notes and the APK for the newest build",
                        onClick = { uriHandler.openUri(LATEST_RELEASE_URL) },
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    OutboundRow(
                        icon = Icons.Filled.History,
                        title = "All releases",
                        subtitle = "Every version Ketch has shipped",
                        onClick = { uriHandler.openUri(RELEASES_URL) },
                    )
                }
                Text(
                    text = "Ketch watches this page and offers the new APK when there " +
                        "is one. Settings → Updates turns that off or checks now.",
                    fontSize = 12.sp,
                    lineHeight = 17.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            HelpGroup(title = "About") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    OutboundRow(
                        icon = Icons.Filled.Code,
                        title = "View source on GitHub",
                        subtitle = "Ketch is open source — read it, fork it, build it",
                        onClick = { uriHandler.openUri(REPO_URL) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Ketch v${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri(REPO_URL) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search help",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StarterCard(
    icon: ImageVector,
    title: String,
    meta: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = title,
            fontSize = 14.sp,
            lineHeight = 17.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = onContainer,
        )
        Text(
            text = meta,
            fontSize = 11.5.sp,
            color = onContainer.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun FaqRow(faq: Faq, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (expanded) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = faq.question,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = if (expanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = faq.answer,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, end = 26.dp),
            )
        }
    }
}

@Composable
private fun OutboundRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun HelpGroup(title: String, content: @Composable () -> Unit) {
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
