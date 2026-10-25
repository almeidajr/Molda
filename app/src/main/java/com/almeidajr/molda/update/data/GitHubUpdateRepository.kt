package com.almeidajr.molda.update.data

import com.almeidajr.molda.update.domain.Release
import com.almeidajr.molda.update.domain.ReleaseAsset
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import com.almeidajr.molda.update.domain.UpdateRepository
import com.almeidajr.molda.update.domain.checksForUpdates
import com.almeidajr.molda.update.domain.chooseUpdate
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

/** GitHub's REST API, where Molda's releases are published. */
internal const val GITHUB_API_URL = "https://api.github.com/"

/** The repository's releases, read unauthenticated. */
internal interface GitHubReleasesApi {
    // The list, not "releases/latest", which skips pre-releases; newest first, one page is plenty.
    @GET("repos/almeidajr/Molda/releases?per_page=100")
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    suspend fun releases(): List<NetworkRelease>
}

@Serializable
internal data class NetworkRelease(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<NetworkAsset>,
)

@Serializable
internal data class NetworkAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val digest: String? = null,
)

private val NetworkJson = Json { ignoreUnknownKeys = true }

/** The releases API at [baseUrl], calling through [callFactory]. */
internal fun gitHubReleasesApi(baseUrl: String, callFactory: Call.Factory): GitHubReleasesApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .callFactory(callFactory)
        .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubReleasesApi::class.java)

/** Updates for [installedVersionName] from the GitHub Releases, their APKs from [apks]. */
internal class GitHubUpdateRepository(
    private val api: GitHubReleasesApi,
    private val installedVersionName: String,
    private val apks: UpdateApks,
) : UpdateRepository {
    override fun download(update: Update): Flow<UpdateDownload> = apks.download(update)

    override suspend fun startDownload(update: Update) = apks.startDownload(update)

    override suspend fun install(update: Update) = apks.install(update)

    override suspend fun check(): UpdateCheck {
        if (!checksForUpdates(installedVersionName)) return UpdateCheck.NotChecked
        return try {
            chooseUpdate(installedVersionName, api.releases().map { it.toRelease() })
                ?.let(UpdateCheck::Available) ?: UpdateCheck.UpToDate
        } catch (_: IOException) {
            UpdateCheck.Failed
        } catch (_: HttpException) {
            UpdateCheck.Failed
        } catch (_: SerializationException) {
            UpdateCheck.Failed
        }
    }
}

private fun NetworkRelease.toRelease() = Release(
    tag = tagName,
    isDraft = draft,
    isPrerelease = prerelease,
    page = htmlUrl,
    assets = assets.map { ReleaseAsset(it.name, it.browserDownloadUrl, it.digest) },
)
