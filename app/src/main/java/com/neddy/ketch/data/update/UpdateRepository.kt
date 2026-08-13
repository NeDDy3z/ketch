package com.neddy.ketch.data.update

import com.neddy.ketch.BuildConfig
import com.neddy.ketch.data.settings.SettingsRepository
import com.neddy.ketch.domain.AppVersion

/** A release newer than the running build. */
data class AppUpdate(
    val version: String,
    val title: String,
    val notes: String,
    /** Where "Update" goes: the APK asset when there is one, else the release page. */
    val downloadUrl: String,
    val releaseUrl: String,
)

/**
 * Checks the project's GitHub releases for a newer build. Ketch is sideloaded,
 * so there is no store to do this; the app watches its own release page and
 * hands the user off to the APK.
 */
class UpdateRepository(
    private val api: GitHubApiService,
    private val settingsRepository: SettingsRepository,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /**
     * The prompt-worthy update, or null. Respects the user's answers: switched
     * off entirely by "Don't remind me again", held back until the snooze
     * expires by "Later", and throttled so a relaunch does not re-query.
     */
    suspend fun updateToPrompt(): AppUpdate? {
        val settings = settingsRepository.current()
        if (!settings.updateChecksEnabled) return null
        if (now() < settings.updateSnoozedUntil) return null
        if (now() - settings.lastUpdateCheckAt < CHECK_INTERVAL_MS) return null
        return check()
    }

    /**
     * Queries GitHub and returns the newer release, or null when this build is
     * current. Network and parsing failures are swallowed: an update check is
     * never the reason the user sees an error.
     */
    suspend fun check(): AppUpdate? {
        settingsRepository.setLastUpdateCheckAt(now())
        val release = runCatching { api.latestRelease() }.getOrNull() ?: return null
        if (release.draft || release.prerelease) return null
        val tag = release.tagName ?: return null
        if (!AppVersion.isNewer(tag, currentVersion)) return null

        val apk = release.assets.firstOrNull { it.name?.endsWith(".apk", true) == true }
        val releaseUrl = release.htmlUrl ?: "$RELEASES_URL/tag/$tag"
        return AppUpdate(
            version = tag.removePrefix("v"),
            title = release.name?.takeIf { it.isNotBlank() } ?: tag,
            notes = release.body.orEmpty().trim(),
            downloadUrl = apk?.browserDownloadUrl ?: releaseUrl,
            releaseUrl = releaseUrl,
        )
    }

    /** "Later": stay quiet for a day, then ask again. */
    suspend fun snooze() {
        settingsRepository.setUpdateSnoozedUntil(now() + SNOOZE_MS)
    }

    /** "Don't remind me again": no more checks until Settings turns them back on. */
    suspend fun disableChecks() {
        settingsRepository.setUpdateChecksEnabled(false)
    }

    companion object {
        const val RELEASES_URL = "https://github.com/NeDDy3z/ketch/releases"
        const val LATEST_RELEASE_URL = "$RELEASES_URL/latest"
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val SNOOZE_MS = 24 * 60 * 60 * 1000L
    }
}
