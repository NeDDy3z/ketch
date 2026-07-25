package com.neddy.ketch.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Storage for widget state: which watchers each widget shows, which page of
 * the connection pager is visible, and the last fetched connection line per
 * watcher. SharedPreferences is intentional here, both readers and writers
 * are synchronous widget plumbing.
 */
/**
 * How a placed widget picks between the palette's dark tones and its light
 * counterpart. Independent of the app — which is dark-only — so a translucent
 * widget can sit lighter on a bright wallpaper.
 */
enum class WidgetTheme { SYSTEM, LIGHT, DARK }

object WidgetPrefs {

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("ketch_widget", Context.MODE_PRIVATE)

    fun setSelectedWatchers(context: Context, appWidgetId: Int, watcherIds: List<Long>) {
        prefs(context).edit {
            putString("watchers_$appWidgetId", watcherIds.joinToString(","))
            // A new selection invalidates whatever page was open.
            putInt("page_$appWidgetId", 0)
        }
    }

    fun selectedWatchers(context: Context, appWidgetId: Int): List<Long> =
        prefs(context).getString("watchers_$appWidgetId", null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

    fun clearWidget(context: Context, appWidgetId: Int) {
        prefs(context).edit {
            remove("watchers_$appWidgetId")
            remove("page_$appWidgetId")
            remove("only_active_$appWidgetId")
            remove("theme_$appWidgetId")
        }
    }

    /**
     * Whether the pager skips resting watchers until their window opens. On by
     * default, so a placed widget costs nothing overnight.
     */
    fun showOnlyActive(context: Context, appWidgetId: Int): Boolean =
        prefs(context).getBoolean("only_active_$appWidgetId", true)

    fun setShowOnlyActive(context: Context, appWidgetId: Int, onlyActive: Boolean) {
        prefs(context).edit {
            putBoolean("only_active_$appWidgetId", onlyActive)
            // The visible page set changes with the filter.
            putInt("page_$appWidgetId", 0)
        }
    }

    fun theme(context: Context, appWidgetId: Int): WidgetTheme =
        prefs(context).getString("theme_$appWidgetId", null)
            ?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SYSTEM

    fun setTheme(context: Context, appWidgetId: Int, theme: WidgetTheme) {
        prefs(context).edit { putString("theme_$appWidgetId", theme.name) }
    }

    /** Index of the connection the widget currently shows. */
    fun page(context: Context, appWidgetId: Int): Int =
        prefs(context).getInt("page_$appWidgetId", 0)

    fun setPage(context: Context, appWidgetId: Int, page: Int) {
        prefs(context).edit { putInt("page_$appWidgetId", page) }
    }

    fun setConnectionLine(context: Context, watcherId: Long, line: String) {
        prefs(context).edit { putString("line_$watcherId", line) }
    }

    fun connectionLine(context: Context, watcherId: Long): String? =
        prefs(context).getString("line_$watcherId", null)

    /**
     * The last journey found for a watcher, flattened so the widget can lay it
     * out as a departure board instead of a paragraph. Glance re-reads this on
     * every render, so it stays a plain string rather than a parsed object.
     */
    fun setJourney(context: Context, watcherId: Long, journey: WidgetJourney?) {
        prefs(context).edit {
            if (journey == null) {
                remove("journey_$watcherId")
            } else {
                putString("journey_$watcherId", journey.serialize())
            }
        }
    }

    fun journey(context: Context, watcherId: Long): WidgetJourney? =
        prefs(context).getString("journey_$watcherId", null)?.let(WidgetJourney::parse)
}
