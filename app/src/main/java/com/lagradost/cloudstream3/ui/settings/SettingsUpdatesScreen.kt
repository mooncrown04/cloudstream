package com.lagradost.cloudstream3.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.AutoDownloadMode
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.MainActivityScreen
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.BackupUtils
import com.lagradost.cloudstream3.utils.BackupUtils.restorePrompt
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.InAppUpdater.installPreReleaseIfNeeded
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream4.rememberAppSettings
import com.mihon.presentation.settings.Preference
import com.mihon.presentation.settings.SearchableSettings
import com.mihon.presentation.settings.collectAsState
import kotlinx.collections.immutable.persistentListOf

object SettingsUpdatesScreen : SearchableSettings {
    @Composable
    override fun getTitleRes(): String = stringResource(R.string.category_updates)

    @Composable
    override fun getPreferences(): List<Preference> {
        val settings = rememberAppSettings()
        val context = LocalContext.current

        val pathPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                try {
                    // SAF erişim iznini kalıcı yapma
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)

                    // Ayara kaydetme (Uri string formatında)
                    settings.backup.path.set(it.toString())
                } catch (t: Throwable) {
                    logError(t)
                }
            }
        }

        val currentBackupPath by settings.backup.path.collectAsState()
        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            com.lagradost.cloudstream3.ui.settings.logcat.LogcatDialog {
                showDialog = false
            }
        }

        val githubViewModel = MainActivityScreen.githubViewModel()

        return persistentListOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_app_updates),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.check_for_update),
                        subtitle = BuildConfig.VERSION_NAME,
                        icon = painterResource(R.drawable.mobile_arrow_down_24px),
                        onClick = {
                            githubViewModel?.onAction(GithubAction.SearchForUpdate)
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.install_prerelease),
                        icon = painterResource(R.drawable.mobile_code_24px),
                        enabled = BuildConfig.FLAVOR == "stable",
                        onClick = {
                            activity?.installPreReleaseIfNeeded()
                        }
                    ),
                    Preference.PreferenceItem.ListPreference(
                        title = stringResource(R.string.apk_installer_settings),
                        subtitle = stringResource(R.string.apk_installer_settings_des),
                        icon = painterResource(R.drawable.mobile_wrench_24px),
                        entries = integerArrayResource(R.array.apk_installer_values).zip(
                            stringArrayResource(R.array.apk_installer_pref)
                        ).toMap(),
                        preference = settings.updates.apkInstaller
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(R.string.updates_settings),
                        subtitle = stringResource(R.string.updates_settings_des),
                        icon = painterResource(R.drawable.notifications_active_24px),
                        preference = settings.updates.showAppUpdates
                    )
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_backup),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.backup_settings),
                        icon = painterResource(R.drawable.save_as_24px),
                        onClick = {
                            BackupUtils.backup(activity)
                        }
                    ),
                    Preference.PreferenceItem.ListPreference(
                        title = stringResource(R.string.backup_frequency),
                        icon = painterResource(R.drawable.save_clock_24px),
                        entries = integerArrayResource(R.array.periodic_work_values).zip(
                            stringArrayResource(R.array.periodic_work_names)
                        ).toMap(),
                        preference = settings.backup.frequency
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.backup_path_title),
                        subtitle = currentBackupPath.ifEmpty { null } ?: stringResource(R.string.custom),
                        icon = painterResource(R.drawable.folder_24px),
                        onClick = {
                            try {
                                pathPicker.launch(null)
                            } catch (e: Exception) {
                                logError(e)
                            }
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.restore_settings),
                        icon = painterResource(R.drawable.restore_page_24px),
                        onClick = {
                            activity?.restorePrompt()
                        }
                    ),
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_extensions),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        title = stringResource(R.string.automatic_plugin_updates),
                        icon = painterResource(R.drawable.extension_24px),
                        preference = settings.plugins.autoUpdate,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        title = stringResource(R.string.automatic_plugin_download),
                        subtitle = "%s\n" + stringResource(R.string.automatic_plugin_download_summary),
                        icon = painterResource(R.drawable.extention_renew2),
                        entries = AutoDownloadMode.entries.map { it.value }.sorted()
                            .zip(stringArrayResource((R.array.auto_download_plugin))).toMap(),
                        preference = settings.plugins.autoDownload
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.update_plugins),
                        subtitle = stringResource(R.string.update_plugins_manually),
                        icon = painterResource(R.drawable.extention_download),
                        onClick = {
                            ioSafe {
                                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_manuallyReloadAndUpdatePlugins(
                                    activity ?: return@ioSafe
                                )
                            }
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.test_extensions),
                        subtitle = stringResource(R.string.test_extensions_summary),
                        icon = painterResource(R.drawable.baseline_network_ping_24),
                        onClick = {
                            activity?.navigate(R.id.navigation_test_providers)
                        })
                )
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.pref_category_actions),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.show_log_cat),
                        icon = painterResource(R.drawable.article_24px),
                        onClick = {
                            showDialog = true
                        }
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.redo_setup_process),
                        icon = painterResource(R.drawable.construction_24px),
                        onClick = {
                            activity?.navigate(R.id.navigation_setup_language)
                        }
                    ),
                )
            )
        )
    }
}
