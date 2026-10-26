package com.almeidajr.molda.di

import android.content.Context
import com.almeidajr.molda.update.data.GITHUB_API_URL
import com.almeidajr.molda.update.data.GitHubReleases
import com.almeidajr.molda.update.data.GitHubReleasesApi
import com.almeidajr.molda.update.data.gitHubReleasesApi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * The app's one HTTP client and Molda's GitHub Releases read through it, apart from the
 * repositories so tests can replace either.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun callFactory(): Call.Factory = OkHttpClient()

    // Lazy, so OkHttp is built only once a check runs, never for a local build.
    @Provides
    @Singleton
    fun releasesApi(callFactory: Lazy<Call.Factory>): GitHubReleasesApi =
        gitHubReleasesApi(GITHUB_API_URL) { callFactory.get().newCall(it) }

    @Provides
    @Singleton
    fun releases(@ApplicationContext context: Context, api: GitHubReleasesApi): GitHubReleases =
        GitHubReleases(
            api,
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
        )
}
