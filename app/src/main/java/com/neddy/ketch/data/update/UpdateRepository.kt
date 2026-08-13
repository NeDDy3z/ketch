package com.neddy.ketch.data.update

import com.neddy.ketch.BuildConfig
import com.neddy.ketch.data.settings.KnownRelease
import com.neddy.ketch.data.settings.SettingsRepository
import com.neddy.ketch.domain.AppVersion
import com.neddy.ketch.domain.UpdatePolicy
import com.neddy.ketch.domain.UpdatePromptDecision

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

    /** One automatic check per launch, whatever brings the home screen back. */
    @Volatile
    private var checkedThisLaunch = false

    /**
     * The prompt-worthy update, or null. Respects the user's answers: switched
     * off entirely by "Don't remind me again", and held back until the snooze
     * expires by "Later".
     *
     * A release the app has already heard of prompts straight away from the
     * cache, so the answer never depends on the network being up, or on a
     * check having been allowed to run this launch.
     */
    suspend fun updateToPrompt(): AppUpdate? {
        val settings = settingsRepository.current()
        val cached = cachedUpdate()
        val decision = UpdatePolicy.decide(
            checksEnabled = settings.updateChecksEnabled,
            snoozedUntil = settings.updateSnoozedUntil,
            lastCheckAt = settings.lastUpdateCheckAt,
            checkedThisLaunch = checkedThisLaunch,
            knownNewerRelease = cached != null,
            now = now(),
        )
        return when (decision) {
            UpdatePromptDecision.STAY_QUIET -> null
            UpdatePromptDecision.PROMPT_FROM_CACHE -> cached
            UpdatePromptDecision.CHECK -> check()
        }
    }

    /**
     * Queries GitHub and returns the newer release, or null when this build is
     * current. Network and parsing failures are swallowed: an update check is
     * never the reason the user sees an error.
     */
    suspend fun check(): AppUpdate? {
        checkedThisLaunch = true
        settingsRepository.setLastUpdateCheckAt(now())
        val release = runCatching { api.latestRelease() }.getOrNull()
            // Offline, or GitHub said no: fall back on whatever is cached rather
            // than claiming this build is current.
            ?: return cachedUpdate()
        val tag = release.tagName
        if (release.draft || release.prerelease || tag == null) return null

        val apk = release.assets.firstOrNull { it.name?.endsWith(".apk", true) == true }
        val known = KnownRelease(
            tag = tag,
            title = release.name?.takeIf { it.isNotBlank() } ?: tag,
            notes = release.body.orEmpty().trim(),
            downloadUrl = apk?.browserDownloadUrl ?: release.htmlUrl ?: releasePage(tag),
            releaseUrl = release.htmlUrl ?: releasePage(tag),
        )
        // Cached either way: a tag that is not newer clears a stale entry left
        // behind by the version this build has just replaced.
        val isNewer = AppVersion.isNewer(tag, currentVersion)
        settingsRepository.setKnownRelease(known.takeIf { isNewer })
        return known.toUpdate().takeIf { isNewer }
    }

    /** "Later": stay quiet for a day, then ask again. */
    suspend fun snooze() {
        settingsRepository.setUpdateSnoozedUntil(now() + SNOOZE_MS)
    }

    /** "Don't remind me again": no more checks until Settings turns them back on. */
    suspend fun disableChecks() {
        settingsRepository.setUpdateChecksEnabled(false)
    }

    private suspend fun cachedUpdate(): AppUpdate? {
        val known = settingsRepository.knownRelease() ?: return null
        if (!AppVersion.isNewer(known.tag, currentVersion)) {
            settingsRepository.setKnownRelease(null)
            return null
        }
        return known.toUpdate()
    }

    private fun KnownRelease.toUpdate() = AppUpdate(
        version = tag.removePrefix("v"),
        title = title,
        notes = notes,
        downloadUrl = downloadUrl.ifBlank { releasePage(tag) },
        releaseUrl = releaseUrl.ifBlank { releasePage(tag) },
    )

    private fun releasePage(tag: String) = "$RELEASES_URL/tag/$tag"

    companion object {
        const val RELEASES_URL = "https://github.com/NeDDy3z/ketch/releases"
        const val LATEST_RELEASE_URL = "$RELEASES_URL/latest"
        private const val SNOOZE_MS = 24 * 60 * 60 * 1000L
    }
}
