package com.almeidajr.molda.update.data

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Environment
import androidx.core.net.toUri
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateDownload
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** An update's APK on the device: its download and its install. */
internal interface UpdateApks {
    fun download(update: Update): Flow<UpdateDownload>

    suspend fun startDownload(update: Update)

    suspend fun install(update: Update)
}

/**
 * True when [apk]'s SHA-256 matches [digest] (GitHub's `sha256:…`); otherwise deletes [apk], so
 * an APK without a digest, or a corrupt or incomplete one, is never installed.
 */
internal fun verifyApk(apk: File, digest: String?): Boolean {
    val isVerified = apk.exists() && digest == "sha256:${apk.sha256()}"
    if (!isVerified) apk.delete()
    return isVerified
}

private fun File.sha256(): String {
    val sha256 = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        generateSequence { input.read(buffer) }
            .takeWhile { it >= 0 }
            .forEach { sha256.update(buffer, 0, it) }
    }
    return sha256.digest().toHexString()
}

private val PollInterval = 500.milliseconds

/**
 * Downloads through the system `DownloadManager` into the app's external files, the APK's file name
 * identifying the download, and installs through a `PackageInstaller` session.
 */
internal class SystemUpdateApks(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UpdateApks {
    private val downloads = checkNotNull(context.getSystemService(DownloadManager::class.java))

    // DownloadManager has no change callback; polling while About shows the download is enough.
    override fun download(update: Update): Flow<UpdateDownload> = flow {
        while (true) {
            emit(rows(update).lastOrNull())
            delay(PollInterval)
        }
    }
        .distinctUntilChanged()
        .map { it.toUpdateDownload(update) }
        .flowOn(ioDispatcher)

    override suspend fun startDownload(update: Update) = withContext(ioDispatcher) {
        rows(update).forEach { downloads.remove(it.id) }
        apkFile(update).delete()
        downloads.enqueue(
            DownloadManager.Request(update.apk.downloadUrl.toUri())
                .setTitle(update.apk.name)
                .setMimeType(APK_MIME_TYPE)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    update.apk.name,
                ),
        )
        Unit
    }

    override suspend fun install(update: Update) = withContext(ioDispatcher) {
        val apk = apkFile(update)
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                .apply { setAppPackageName(context.packageName) },
        )
        installer.openSession(sessionId).use { session ->
            try {
                apk.inputStream().use { input ->
                    session.openWrite(apk.name, 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                session.commit(installStatusReceiver(sessionId))
            } catch (_: IOException) {
                // The APK went missing or storage ran out: nothing installs, Instalar stays.
                session.abandon()
            }
        }
    }

    private fun apkFile(update: Update) =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), update.apk.name)

    private fun installStatusReceiver(sessionId: Int): IntentSender = PendingIntent.getBroadcast(
        context,
        sessionId,
        Intent(context, UpdateInstallReceiver::class.java),
        // Mutable: the installer adds the session's status to the intent.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    ).intentSender

    // This app's downloads of the APK, oldest first: the system lists only the app's own.
    private fun rows(update: Update): List<DownloadRow> =
        downloads.query(DownloadManager.Query()).use { cursor ->
            val id = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val title = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
            val status = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val done = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val size = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .filter { it.getString(title) == update.apk.name }
                .map { row ->
                    DownloadRow(
                        id = row.getLong(id),
                        status = row.getInt(status),
                        done = row.getLong(done),
                        size = row.getLong(size),
                    )
                }
                .sortedBy { it.id }
                .toList()
        }

    private fun DownloadRow?.toUpdateDownload(update: Update): UpdateDownload {
        if (this == null) return UpdateDownload.NotStarted
        return when (status) {
            DownloadManager.STATUS_SUCCESSFUL ->
                if (verifyApk(apkFile(update), update.apk.digest)) {
                    UpdateDownload.Downloaded
                } else {
                    UpdateDownload.VerificationFailed
                }

            DownloadManager.STATUS_FAILED -> UpdateDownload.VerificationFailed

            else -> UpdateDownload.Downloading(if (size > 0) done.toFloat() / size else null)
        }
    }

    private data class DownloadRow(val id: Long, val status: Int, val done: Long, val size: Long)

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

/** Shows the system installer's confirmation for a session [SystemUpdateApks] committed. */
internal class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status =
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) return
        // Sent only by the installer through this app's PendingIntent: the receiver isn't exported.
        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
