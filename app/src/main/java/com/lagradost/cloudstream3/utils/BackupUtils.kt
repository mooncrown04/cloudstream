package com.lagradost.cloudstream3.utils

import android.content.ActivityNotFoundException
import android.content.Intent

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.plugins.PLUGINS_KEY
import com.lagradost.cloudstream3.plugins.PLUGINS_KEY_LOCAL
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Companion.ANILIST_CACHED_LIST
import com.lagradost.cloudstream3.syncproviders.providers.MALApi.Companion.MAL_CACHED_LIST
import com.lagradost.cloudstream3.syncproviders.providers.KitsuApi.Companion.KITSU_CACHED_LIST
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.DataStore.getDefaultSharedPrefs
import com.lagradost.cloudstream3.utils.DataStore.getSharedPrefs
import com.lagradost.cloudstream3.utils.UIHelper.checkWrite
import com.lagradost.cloudstream3.utils.UIHelper.requestRW
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.setupStream
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager.QUEUE_KEY
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.KEY_DOWNLOAD_INFO
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.KEY_RESUME_IN_QUEUE
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.KEY_RESUME_PACKAGES
import com.lagradost.safefile.MediaFileContentType
import com.lagradost.safefile.SafeFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUtils {

    /**
     * No sensitive or breaking data in the backup
     */
    private val nonTransferableKeys = listOf(
        ANILIST_CACHED_LIST,
        MAL_CACHED_LIST,
        KITSU_CACHED_LIST,

        // The plugins themselves are not backed up
        PLUGINS_KEY,
        PLUGINS_KEY_LOCAL,

        AccountManager.ACCOUNT_TOKEN,
        AccountManager.ACCOUNT_IDS,

        "biometric_key",
        "nginx_user",

        // No access rights after restore data from backup
        "download_path_key",
        "download_path_key_visual",
        "backup_path_key",
        "backup_dir_path_key",

        "anilist_token",
        "anilist_user",
        "mal_user",
        "mal_token",
        "mal_refresh_token",
        "mal_unixtime",
        "open_subtitles_user",
        "subdl_user",
        "simkl_token",

        DOWNLOAD_EPISODE_CACHE_BACKUP,
        DOWNLOAD_EPISODE_CACHE,

        KEY_DOWNLOAD_INFO,
        KEY_RESUME_IN_QUEUE,
        KEY_RESUME_PACKAGES,
        QUEUE_KEY,

        "auto_download_plugins_key2"
    )

    private fun String.isTransferable(): Boolean {
        return !nonTransferableKeys.any { this.contains(it) }
    }

    private var restoreFileSelectorOpenDoc: ActivityResultLauncher<Array<String>>? = null
    private var restoreFileSelectorGetContent: ActivityResultLauncher<String>? = null

    @Serializable
    data class BackupVars(
        @JsonProperty("_Bool") @SerialName("_Bool") val bool: Map<String, Boolean>?,
        @JsonProperty("_Int") @SerialName("_Int") val int: Map<String, Int>?,
        @JsonProperty("_String") @SerialName("_String") val string: Map<String, String>?,
        @JsonProperty("_Float") @SerialName("_Float") val float: Map<String, Float>?,
        @JsonProperty("_Long") @SerialName("_Long") val long: Map<String, Long>?,
        @JsonProperty("_StringSet") @SerialName("_StringSet") val stringSet: Map<String, Set<String>?>?,
    )

    @Serializable
    data class BackupFile(
        @JsonProperty("datastore") @SerialName("datastore") val datastore: BackupVars,
        @JsonProperty("settings") @SerialName("settings") val settings: BackupVars,
    )

    @Suppress("UNCHECKED_CAST")
    private fun getBackup(context: Context): BackupFile {
        val allData = context.getSharedPrefs().all.filter { it.key.isTransferable() }
        val allSettings = context.getDefaultSharedPrefs().all.filter { it.key.isTransferable() }

        val allDataSorted = BackupVars(
            allData.filter { it.value is Boolean } as? Map<String, Boolean>,
            allData.filter { it.value is Int } as? Map<String, Int>,
            allData.filter { it.value is String } as? Map<String, String>,
            allData.filter { it.value is Float } as? Map<String, Float>,
            allData.filter { it.value is Long } as? Map<String, Long>,
            allData.filter { it.value as? Set<String> != null } as? Map<String, Set<String>>,
        )

        val allSettingsSorted = BackupVars(
            allSettings.filter { it.value is Boolean } as? Map<String, Boolean>,
            allSettings.filter { it.value is Int } as? Map<String, Int>,
            allSettings.filter { it.value is String } as? Map<String, String>,
            allSettings.filter { it.value is Float } as? Map<String, Float>,
            allSettings.filter { it.value is Long } as? Map<String, Long>,
            allSettings.filter { it.value as? Set<String> != null } as? Map<String, Set<String>>,
        )

        return BackupFile(
            allDataSorted,
            allSettingsSorted,
        )
    }

    @WorkerThread
    fun restore(
        context: Context?,
        backupFile: BackupFile,
        restoreSettings: Boolean,
        restoreDataStore: Boolean,
    ) {
        if (context == null) return
        if (restoreSettings) {
            context.restoreMap(backupFile.settings.bool, true)
            context.restoreMap(backupFile.settings.int, true)
            context.restoreMap(backupFile.settings.string, true)
            context.restoreMap(backupFile.settings.float, true)
            context.restoreMap(backupFile.settings.long, true)
            context.restoreMap(backupFile.settings.stringSet, true)
        }

        if (restoreDataStore) {
            context.restoreMap(backupFile.datastore.bool)
            context.restoreMap(backupFile.datastore.int)
            context.restoreMap(backupFile.datastore.string)
            context.restoreMap(backupFile.datastore.float)
            context.restoreMap(backupFile.datastore.long)
            context.restoreMap(backupFile.datastore.stringSet)
        }

        for(api in AccountManager.syncApis) {
            api.requireLibraryRefresh = true
        }
    }

    fun restoreFromUri(activity: Activity, uri: Uri) {
        ioSafe {
            try {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val text = activity.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: return@ioSafe

                val restoredValue = parseJson<BackupFile>(text)

                restore(
                    activity,
                    restoredValue,
                    restoreSettings = true,
                    restoreDataStore = true,
                )
                activity.runOnUiThread { activity.recreate() }
            } catch (e: Exception) {
                logError(e)
                main {
                    showToast(
                        activity.getString(R.string.restore_failed_format).format(e.toString())
                    )
                }
            }
        }
    }

    fun backup(context: Context?) = ioSafe {
        if (context == null) return@ioSafe
        var fileStream: OutputStream? = null
        var printStream: PrintWriter? = null

        try {
            if (!context.checkWrite()) {
                showToast(R.string.backup_failed, Toast.LENGTH_LONG)
                context.getActivity()?.requestRW()
                return@ioSafe
            }

            val date = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault()).format(Date(currentTimeMillis()))
            val displayName = "CS3_Backup_${date}"
            val backupFile = getBackup(context)
            
            // DÜZELTME: Uzantı varsayılan json olarak gönderiliyor
            val stream = setupBackupStream(context, displayName, "json")

            fileStream = stream.openNew()
            printStream = PrintWriter(fileStream)
            printStream.print(backupFile.toJson())
            showToast(R.string.backup_success, Toast.LENGTH_LONG)
        } catch (e: Exception) {
            logError(e)
            try {
                showToast(
                    txt(R.string.backup_failed_error_format, e.toString()),
                    Toast.LENGTH_LONG,
                )
            } catch (e: Exception) {
                logError(e)
            }
        } finally {
            printStream?.closeQuietly()
            fileStream?.closeQuietly()
        }
    }

    @Throws(IOException::class)
    private fun setupBackupStream(context: Context, name: String, ext: String = "json"): DownloadObjects.StreamData {
        return setupStream(
            baseFile = getCurrentBackupDir(context).first ?: getDefaultBackupDir(context)
            ?: throw IOException("Bad config"),
            name,
            folder = null,
            extension = ext,
            tryResume = false,
        )
    }

    fun FragmentActivity.setUpBackup() {
        try {
            restoreFileSelectorOpenDoc =
                registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri != null) restoreFromUri(this, uri)
                }

            restoreFileSelectorGetContent =
                registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) restoreFromUri(this, uri)
                }
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun Activity.restorePrompt() {
        runOnUiThread {
            try {
                restoreFileSelectorOpenDoc?.launch(arrayOf("*/*"))
            } catch (e: ActivityNotFoundException) {
                try {
                    restoreFileSelectorGetContent?.launch("*/*")
                } catch (e2: ActivityNotFoundException) {
                    showToast("Cihazda dosya seçebilecek bir dosya yöneticisi bulunamadı.")
                    logError(e2)
                }
            } catch (e: Exception) {
                showToast(e.message)
                logError(e)
            }
        }
    }

    private fun <T> Context.restoreMap(
        map: Map<String, T>?,
        isEditingAppSettings: Boolean = false,
    ) {
        val editor = DataStore.editor(this, isEditingAppSettings)
        map?.forEach {
            if (it.key.isTransferable()) {
                editor.setKeyRaw(it.key, it.value)
            }
        }
        editor.apply()
    }

    fun getDefaultBackupDir(context: Context): SafeFile? {
        return SafeFile.fromMedia(context, MediaFileContentType.Downloads)
    }

    fun getCurrentBackupDir(context: Context): Pair<SafeFile?, String?> {
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
        val basePathSetting = settingsManager.getString(context.getString(R.string.backup_path_key), null)
        return baseBackupPathToFile(context, basePathSetting) to basePathSetting
    }

    private fun baseBackupPathToFile(context: Context, path: String?): SafeFile? {
        return when {
            path.isNullOrBlank() -> getDefaultBackupDir(context)
            path.startsWith("content://") -> SafeFile.fromUri(context, path.toUri())
            else -> SafeFile.fromFilePath(context, path)
        }
    }
}
