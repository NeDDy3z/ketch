package com.neddy.ketch.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * The one GitHub endpoint Ketch talks to: the latest published release of its
 * own repository. Unauthenticated, so it is rate limited per IP — which is why
 * the check is throttled rather than run on every launch.
 */
interface GitHubApiService {

    @GET("repos/NeDDy3z/ketch/releases/latest")
    suspend fun latestRelease(
        @Header("Accept") accept: String = "application/vnd.github+json",
    ): GitHubReleaseDto
}

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(
    val name: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
)
