package com.neddy.ketch.ui.navigation

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val WATCHER_EDIT = "watcher_edit?watcherId={watcherId}"

    fun watcherEdit(watcherId: Long? = null): String =
        "watcher_edit?watcherId=${watcherId ?: -1L}"
}
