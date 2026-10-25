package com.almeidajr.molda.update.domain

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
