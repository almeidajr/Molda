package com.almeidajr.molda.update.domain

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersionOrNull

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
