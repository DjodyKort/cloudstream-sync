package com.djody.cloudstreamsync

import android.content.Context
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.setKey
import java.util.UUID

enum class BackendType { GITHUB_GIST, WEBDAV, HTTP }

private const val CONFIG_PREFIX       = "cloudstream_sync/"
private const val KEY_DEVICE_ID       = "${CONFIG_PREFIX}device_id"
private const val KEY_DEVICE_NAME     = "${CONFIG_PREFIX}device_name"
private const val KEY_BACKEND_TYPE    = "${CONFIG_PREFIX}backend_type"
private const val KEY_GITHUB_TOKEN    = "${CONFIG_PREFIX}github_token"
private const val KEY_GIST_ID         = "${CONFIG_PREFIX}gist_id"
private const val KEY_WEBDAV_URL      = "${CONFIG_PREFIX}webdav_url"
private const val KEY_WEBDAV_USER     = "${CONFIG_PREFIX}webdav_user"
private const val KEY_WEBDAV_PASSWORD = "${CONFIG_PREFIX}webdav_password"
private const val KEY_HTTP_URL        = "${CONFIG_PREFIX}http_url"
private const val KEY_HTTP_TOKEN      = "${CONFIG_PREFIX}http_token"
private const val KEY_SYNC_INTERVAL_H = "${CONFIG_PREFIX}sync_interval_hours"
private const val KEY_SYNC_SETTINGS   = "${CONFIG_PREFIX}sync_settings"
private const val KEY_SYNC_REPOS      = "${CONFIG_PREFIX}sync_repos"
private const val KEY_SYNC_PLUGINS    = "${CONFIG_PREFIX}sync_plugins"
private const val KEY_SYNC_HISTORY    = "${CONFIG_PREFIX}sync_history"
private const val KEY_LAST_SYNC_MS    = "${CONFIG_PREFIX}last_sync_ms"

object SyncConfig {

    // Returns existing device ID or generates a new one (persisted).
    fun getOrCreateDeviceId(context: Context): String {
        val existing = context.getKey<String>(KEY_DEVICE_ID)
        if (existing != null) return existing
        val new = UUID.randomUUID().toString()
        context.setKey(KEY_DEVICE_ID, new)
        return new
    }

    var Context.deviceName: String
        get() = getKey(KEY_DEVICE_NAME) ?: android.os.Build.MODEL
        set(v) = setKey(KEY_DEVICE_NAME, v)

    var Context.backendType: BackendType
        get() = BackendType.valueOf(getKey(KEY_BACKEND_TYPE) ?: BackendType.GITHUB_GIST.name)
        set(v) = setKey(KEY_BACKEND_TYPE, v.name)

    // GitHub Gist backend
    var Context.githubToken: String?
        get() = getKey(KEY_GITHUB_TOKEN)
        set(v) = setKey(KEY_GITHUB_TOKEN, v ?: "")

    var Context.gistId: String?
        get() = getKey<String>(KEY_GIST_ID)?.ifBlank { null }
        set(v) = setKey(KEY_GIST_ID, v ?: "")

    // WebDAV backend
    var Context.webdavUrl: String?
        get() = getKey(KEY_WEBDAV_URL)
        set(v) = setKey(KEY_WEBDAV_URL, v ?: "")

    var Context.webdavUser: String?
        get() = getKey(KEY_WEBDAV_USER)
        set(v) = setKey(KEY_WEBDAV_USER, v ?: "")

    var Context.webdavPassword: String?
        get() = getKey(KEY_WEBDAV_PASSWORD)
        set(v) = setKey(KEY_WEBDAV_PASSWORD, v ?: "")

    // Generic HTTP backend
    var Context.httpUrl: String?
        get() = getKey(KEY_HTTP_URL)
        set(v) = setKey(KEY_HTTP_URL, v ?: "")

    var Context.httpToken: String?
        get() = getKey(KEY_HTTP_TOKEN)
        set(v) = setKey(KEY_HTTP_TOKEN, v ?: "")

    // Sync behaviour
    var Context.syncIntervalHours: Int
        get() = getKey(KEY_SYNC_INTERVAL_H) ?: 6
        set(v) = setKey(KEY_SYNC_INTERVAL_H, v)

    var Context.syncSettings: Boolean
        get() = getKey(KEY_SYNC_SETTINGS) ?: true
        set(v) = setKey(KEY_SYNC_SETTINGS, v)

    var Context.syncRepos: Boolean
        get() = getKey(KEY_SYNC_REPOS) ?: true
        set(v) = setKey(KEY_SYNC_REPOS, v)

    var Context.syncPlugins: Boolean
        get() = getKey(KEY_SYNC_PLUGINS) ?: true
        set(v) = setKey(KEY_SYNC_PLUGINS, v)

    var Context.syncHistory: Boolean
        get() = getKey(KEY_SYNC_HISTORY) ?: true
        set(v) = setKey(KEY_SYNC_HISTORY, v)

    var Context.lastSyncMs: Long
        get() = getKey(KEY_LAST_SYNC_MS) ?: 0L
        set(v) = setKey(KEY_LAST_SYNC_MS, v)

    fun Context.isConfigured(): Boolean = when (backendType) {
        BackendType.GITHUB_GIST -> !githubToken.isNullOrBlank()
        BackendType.WEBDAV      -> !webdavUrl.isNullOrBlank() && !webdavUser.isNullOrBlank()
        BackendType.HTTP        -> !httpUrl.isNullOrBlank()
    }
}
