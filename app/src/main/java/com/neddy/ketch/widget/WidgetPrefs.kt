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
        }
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
}
