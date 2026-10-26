package com.almeidajr.molda.update.data

import com.almeidajr.molda.update.domain.ReleaseAsset
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test

/** The update check against a stand-in for GitHub's REST API. */
class GitHubReleasesTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val server get() = serverRule.server

    @Test
    fun check_withARealShapedReleasesList_offersTheNewestOnTheChannel() = runTest {
        // Given
        server.enqueue(MockResponse(body = RELEASES))

        // When
        val check = releases("0.3.0-beta.1").check()

        // Then
        assertEquals(
            UpdateCheck.Available(
                Update(
                    version = "0.3.0-beta.2",
                    page = "https://github.com/almeidajr/Molda/releases/tag/v0.3.0-beta.2",
                    apk = ReleaseAsset(
                        name = "molda-v0.3.0-beta.2.apk",
                        downloadUrl = "https://github.com/almeidajr/Molda/releases/download/" +
                            "v0.3.0-beta.2/molda-v0.3.0-beta.2.apk",
                        digest = "sha256:9f86d081884c7d659a2feaa0c55ad015" +
                            "a3bf4f1b2b0b822cd15d6c15b0f00a08",
                    ),
                ),
            ),
            check,
        )
        // The list, not GitHub's "latest", which skips pre-releases.
        assertEquals("/repos/almeidajr/Molda/releases", server.takeRequest().url.encodedPath)
    }

    @Test
    fun check_onAnHttpError_fails() = runTest {
        // Given
        server.enqueue(MockResponse(code = 503))

        // When/Then
        assertEquals(UpdateCheck.Failed, releases("0.3.0-beta.1").check())
    }

    @Test
    fun check_withAMalformedBody_fails() = runTest {
        // Given
        server.enqueue(MockResponse(body = """[{"tag_name": "v0.3.0", "assets": """))

        // When/Then
        assertEquals(UpdateCheck.Failed, releases("0.3.0-beta.1").check())
    }

    @Test
    fun check_onALocalBuild_makesNoRequest() = runTest {
        // When
        val check = releases("0.0.0-dev").check()

        // Then
        assertEquals(UpdateCheck.NotChecked, check)
        assertEquals(0, server.requestCount)
    }

    private fun releases(installedVersionName: String) = GitHubReleases(
        gitHubReleasesApi(server.url("/").toString(), OkHttpClient()),
        installedVersionName,
    )

    private companion object {
        // Trimmed from GitHub's GET /repos/{owner}/{repo}/releases, unread fields included.
        val RELEASES = """
            [
              {
                "url": "https://api.github.com/repos/almeidajr/Molda/releases/3",
                "html_url": "https://github.com/almeidajr/Molda/releases/tag/v0.4.0-beta.1",
                "id": 3,
                "tag_name": "v0.4.0-beta.1",
                "name": "v0.4.0-beta.1",
                "draft": true,
                "prerelease": true,
                "created_at": "2026-10-20T12:00:00Z",
                "published_at": null,
                "assets": []
              },
              {
                "url": "https://api.github.com/repos/almeidajr/Molda/releases/2",
                "html_url": "https://github.com/almeidajr/Molda/releases/tag/v0.3.0-beta.2",
                "id": 2,
                "tag_name": "v0.3.0-beta.2",
                "name": "v0.3.0-beta.2",
                "draft": false,
                "prerelease": true,
                "created_at": "2026-10-18T12:00:00Z",
                "published_at": "2026-10-18T12:05:00Z",
                "author": {"login": "github-actions[bot]", "id": 41898282},
                "assets": [
                  {
                    "url": "https://api.github.com/repos/almeidajr/Molda/releases/assets/20",
                    "id": 20,
                    "name": "molda-v0.3.0-beta.2.apk",
                    "label": "",
                    "content_type": "application/vnd.android.package-archive",
                    "state": "uploaded",
                    "size": 12345678,
                    "digest": "sha256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                    "download_count": 3,
                    "browser_download_url": "https://github.com/almeidajr/Molda/releases/download/v0.3.0-beta.2/molda-v0.3.0-beta.2.apk"
                  }
                ],
                "body": "Notas"
              },
              {
                "url": "https://api.github.com/repos/almeidajr/Molda/releases/1",
                "html_url": "https://github.com/almeidajr/Molda/releases/tag/v0.3.0-beta.1",
                "id": 1,
                "tag_name": "v0.3.0-beta.1",
                "name": "v0.3.0-beta.1",
                "draft": false,
                "prerelease": true,
                "assets": [
                  {
                    "name": "molda-v0.3.0-beta.1.apk",
                    "digest": null,
                    "browser_download_url": "https://github.com/almeidajr/Molda/releases/download/v0.3.0-beta.1/molda-v0.3.0-beta.1.apk"
                  }
                ]
              }
            ]
        """.trimIndent()
    }
}
