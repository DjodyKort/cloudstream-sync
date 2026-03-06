package com.djody.cloudstreamsync

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.plugins.PluginData
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getSharedPrefs
import com.lagradost.cloudstream3.utils.DataStore.getDefaultSharedPrefs
import com.djody.cloudstreamsync.SyncConfig.deviceName
import com.djody.cloudstreamsync.SyncConfig.syncSettings
import com.djody.cloudstreamsync.SyncConfig.syncRepos
import com.djody.cloudstreamsync.SyncConfig.syncPlugins
import com.djody.cloudstreamsync.SyncConfig.syncHistory
import java.time.Instant

// Keys whose string value is confirmed from CloudStream source (master, 2026-03-06).
// isTransferable() in BackupUtils uses String.contains() — we replicate that here.
// See _docs/cloudstream-internals.md §7 for the full annotated list.
private val NON_TRANSFERABLE_SUBSTRINGS = listOf(
    // Sync provider caches
    "anilist_cached_list",
    "mal_cached_list",
    "kitsu_cached_list",
    // Plugin metadata — we handle these separately
    "PLUGINS_KEY",
    // Auth tokens / credentials
    "auth_tokens",          // AccountManager.ACCOUNT_TOKEN
    "auth_ids",             // AccountManager.ACCOUNT_IDS
    "biometric_key",
    "nginx_user",
    "anilist_token",
    "anilist_user",
    "mal_user",
    "mal_token",
    "mal_refresh_token",
    "mal_unixtime",
    "open_subtitles_user",
    "subdl_user",
    "simkl_token",
    // Device-specific paths
    "download_path_key",
    "backup_path_key",
    "backup_dir_path_key",
    // Downloads
    "BACKUP_download_episode_cache",
    "download_episode_cache",
    "download_info",            // KEY_DOWNLOAD_INFO
    "download_resume_queue_key",// KEY_RESUME_IN_QUEUE
    "download_resume_2",        // KEY_RESUME_PACKAGES
    "download_queue_key",       // QUEUE_KEY
    // Our own config keys — never sync these
    "cloudstream_sync/",
)

// History-related key patterns — excluded when syncHistory is false.
private val HISTORY_SUBSTRINGS = listOf(
    "video_pos_dur",
    "video_watch_state",
    "result_watch_state",
    "result_watch_state_data",
    "result_subscribed_state_data",
    "result_favorites_state_data",
    "result_resume_watching",
    "result_episode",
    "result_season",
    "result_dub",
)

private fun String.isTransferable(includeHistory: Boolean): Boolean {
    if (NON_TRANSFERABLE_SUBSTRINGS.any { this.contains(it) }) return false
    if (!includeHistory && HISTORY_SUBSTRINGS.any { this.contains(it) }) return false
    return true
}

@Suppress("UNCHECKED_CAST")
object DataExporter {

    fun export(context: Context, config: SyncConfig = SyncConfig): SyncPayload {
        val includeHistory = context.syncHistory

        // --- Datastore (rebuild_preference) ---
        val datastoreAll = context.getSharedPrefs().all
            .filter { it.key.isTransferable(includeHistory) }

        val datastore = SyncVars(
            bool      = datastoreAll.filterValues { it is Boolean } as? Map<String, Boolean>,
            int       = datastoreAll.filterValues { it is Int }     as? Map<String, Int>,
            string    = datastoreAll.filterValues { it is String }  as? Map<String, String>,
            float     = datastoreAll.filterValues { it is Float }   as? Map<String, Float>,
            long      = datastoreAll.filterValues { it is Long }    as? Map<String, Long>,
            stringSet = datastoreAll.filterValues { it as? Set<String> != null } as? Map<String, Set<String>>,
        )

        // --- Settings (default shared prefs) ---
        val settingsAll = if (context.syncSettings) {
            context.getDefaultSharedPrefs().all
                .filter { it.key.isTransferable(includeHistory) }
        } else emptyMap()

        val settings = SyncVars(
            bool      = settingsAll.filterValues { it is Boolean } as? Map<String, Boolean>,
            int       = settingsAll.filterValues { it is Int }     as? Map<String, Int>,
            string    = settingsAll.filterValues { it is String }  as? Map<String, String>,
            float     = settingsAll.filterValues { it is Float }   as? Map<String, Float>,
            long      = settingsAll.filterValues { it is Long }    as? Map<String, Long>,
            stringSet = settingsAll.filterValues { it as? Set<String> != null } as? Map<String, Set<String>>,
        )

        // --- Repositories ---
        val repositories = if (context.syncRepos) {
            (context.getKey<Array<RepositoryData>>(REPOSITORIES_KEY) ?: emptyArray())
                .map { SyncRepositoryEntry(it.iconUrl, it.name, it.url) }
        } else emptyList()

        // --- Installed plugins (online only, url must be non-null) ---
        val plugins = if (context.syncPlugins) {
            (context.getKey<Array<PluginData>>(PLUGINS_KEY) ?: emptyArray())
                .filter { it.isOnline && it.url != null }
                .map { SyncPluginEntry(it.internalName, it.url!!, it.version) }
        } else emptyList()

        return SyncPayload(
            deviceId         = SyncConfig.getOrCreateDeviceId(context),
            deviceName       = context.deviceName,
            timestamp        = Instant.now().toString(),
            datastore        = datastore,
            settings         = settings,
            repositories     = repositories,
            installedPlugins = plugins,
        )
    }

    companion object {
        const val PLUGINS_KEY     = "PLUGINS_KEY"
        const val REPOSITORIES_KEY = "REPOSITORIES_KEY"
    }
}
