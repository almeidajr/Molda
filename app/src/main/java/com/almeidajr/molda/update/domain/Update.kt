package com.almeidajr.molda.update.domain

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersionOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** A GitHub release of Molda, as the update check reads it. */
data class Release(
    val tag: String,
    val isDraft: Boolean,
    val isPrerelease: Boolean,
    /** The release's page on GitHub. */
    val page: String,
    val assets: List<ReleaseAsset>,
)

/** A file attached to a [Release]; [digest] is GitHub's `sha256:…`, absent on older assets. */
@Serializable
data class ReleaseAsset(val name: String, val downloadUrl: String, val digest: String?)

/** An update: a beta build or release newer than the installed version, and its APK. */
@Serializable
data class Update(
    /** The SemVer version on offer, without the tag's `v`. */
    val version: String,
    val page: String,
    val apk: ReleaseAsset,
)

/** The outcome of checking for an [Update] on the installed version's channel. */
sealed interface UpdateCheck {
    data class Available(val update: Update) : UpdateCheck

    data object UpToDate : UpdateCheck

    /** Offline, or GitHub failed: never shown as an error. */
    data object Failed : UpdateCheck

    /** A debug or local build, which never checks. */
    data object NotChecked : UpdateCheck
}

/** The state of an [Update]'s download, which the system download service runs. */
sealed interface UpdateDownload {
    data object NotStarted : UpdateDownload

    /** Downloading, [progress] done from 0 to 1; null while the size is unknown. */
    data class Downloading(val progress: Float?) : UpdateDownload

    /** Downloaded, its APK matching the digest GitHub publishes: ready to install. */
    data object Downloaded : UpdateDownload

    /**
     * The download failed, or its APK had no digest or didn't match it; the file is gone and the
     * download can start again.
     */
    data object VerificationFailed : UpdateDownload
}

/** The updates on offer to the installed version; the only seam the UI depends on. */
interface UpdateRepository {
    /** Checks GitHub for an update, unless the installed version [checksForUpdates] is false. */
    suspend fun check(): UpdateCheck

    /**
     * The state of [update]'s download, found by its APK's file name (even after process death)
     * and followed while collected.
     */
    fun download(update: Update): Flow<UpdateDownload>

    /** Starts downloading [update]'s APK, replacing an earlier or failed download of it. */
    suspend fun startDownload(update: Update)

    /**
     * Installs [update]'s downloaded APK through the system installer, which asks the person to
     * confirm and restarts Molda.
     */
    suspend fun install(update: Update)
}

/** The `versionName` of local builds (app/build.gradle.kts). */
private const val LOCAL_VERSION_NAME = "0.0.0-dev"

/** Whether [versionName] is a CI build's SemVer version, the only builds that check. */
fun checksForUpdates(versionName: String): Boolean =
    versionName != LOCAL_VERSION_NAME && versionName.toVersionOrNull() != null

/**
 * The update offered to [installedVersionName] among [releases]: the highest by SemVer precedence
 * that is newer, published, carries a `molda-*.apk`, and is on the channel (a pre-release only
 * when the installed version is one); null when none is.
 */
fun chooseUpdate(installedVersionName: String, releases: List<Release>): Update? {
    val installed = installedVersionName.toVersionOrNull() ?: return null
    return releases
        .mapNotNull { it.offer() }
        .filter { it.version > installed && (installed.isPreRelease || !it.isPreRelease) }
        .maxByOrNull { it.version }
        ?.update
}

private class Offer(val version: Version, val isPreRelease: Boolean, val update: Update)

/** The published [Release] as an offer, or null when its tag isn't SemVer or it has no APK. */
private fun Release.offer(): Offer? {
    val version = tag.removePrefix("v").toVersionOrNull()
    val apk = assets.find { it.name.startsWith("molda-") && it.name.endsWith(".apk") }
    return if (isDraft || version == null || apk == null) {
        null
    } else {
        // Either GitHub's flag or the tag keeps a pre-release off the release channel.
        Offer(version, version.isPreRelease || isPrerelease, Update(version.toString(), page, apk))
    }
}
