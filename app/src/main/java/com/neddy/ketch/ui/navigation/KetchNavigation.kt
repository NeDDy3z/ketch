package com.neddy.ketch.ui.navigation

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val WATCHER_EDIT = "watcher_edit?watcherId={watcherId}"
    const val WATCHER_DETAIL = "watcher_detail/{watcherId}"

    fun watcherEdit(watcherId: Long? = null): String =
        "watcher_edit?watcherId=${watcherId ?: -1L}"

    fun watcherDetail(watcherId: Long): String = "watcher_detail/$watcherId"
}
