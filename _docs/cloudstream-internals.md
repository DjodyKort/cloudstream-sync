# CloudStream Internals — Research Reference

> Verified against CloudStream source at commit: master (2026-03-06)
> All constants and data classes checked against actual source files.

---

## 1. SharedPreferences Files

CloudStream uses two SharedPreferences files:

| File | Access method | Purpose |
|---|---|---|
| `"rebuild_preference"` | `context.getSharedPrefs()` | Primary data store (watch history, bookmarks, plugin data, repo data) |
| System default prefs | `context.getDefaultSharedPrefs()` | App settings (player, UI, language, DNS) |

```kotlin
// DataStore.kt
const val PREFERENCES_NAME = "rebuild_preference"

fun Context.getSharedPrefs(): SharedPreferences =
    getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

fun Context.getDefaultSharedPrefs(): SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(this)
```

The `BackupFile` in BackupUtils.kt maps to:
- `datastore` = `getSharedPrefs()` contents
- `settings`  = `getDefaultSharedPrefs()` contents

---

## 2. Plugin Keys

**Source**: `app/.../plugins/PluginManager.kt`

```kotlin
const val PLUGINS_KEY       = "PLUGINS_KEY"        // online installed plugins
const val PLUGINS_KEY_LOCAL = "PLUGINS_KEY_LOCAL"  // local sideloaded plugins
```

### PluginData — what is stored per plugin

```kotlin
// PluginManager.kt
data class PluginData(
    @JsonProperty("internalName") val internalName: String,
    @JsonProperty("url")          val url: String?,      // download URL — null for local plugins
    @JsonProperty("isOnline")     val isOnline: Boolean,
    @JsonProperty("filePath")     val filePath: String,  // DEVICE-SPECIFIC, do not sync
    @JsonProperty("version")      val version: Int,
)
```

**For sync**: only use `url`, `internalName`, `version`, `isOnline`.
**Never sync** `filePath` — it is device-specific.
**Only sync online plugins** (`isOnline = true`) — local plugins have `url = null`.

Access:
```kotlin
val onlinePlugins: Array<PluginData> = getKey(PLUGINS_KEY) ?: emptyArray()
val localPlugins:  Array<PluginData> = getKey(PLUGINS_KEY_LOCAL) ?: emptyArray()
```

---

## 3. Repository Key

**Source**: `app/.../ui/settings/extensions/ExtensionsViewModel.kt`

```kotlin
const val REPOSITORIES_KEY = "REPOSITORIES_KEY"
```

### RepositoryData — what is stored per repo

```kotlin
// ExtensionsViewModel.kt
data class RepositoryData(
    @JsonProperty("iconUrl") val iconUrl: String?,
    @JsonProperty("name")    val name: String,
    @JsonProperty("url")     val url: String        // URL to the repo's JSON manifest
)
```

Note: `url` points to the JSON manifest file (e.g. a raw GitHub URL). The manifest itself
contains `pluginLists: List<String>` with URLs to individual plugin list JSONs. We do NOT
need to store the manifest contents — just the `RepositoryData` url is enough to re-add a repo.

Access:
```kotlin
val repos: Array<RepositoryData> = getKey(REPOSITORIES_KEY) ?: emptyArray()
```

Re-add on new device:
```kotlin
RepositoryManager.addRepository(repositoryData)
```

---

## 4. Repository Manifest Format (remote JSON)

What CloudStream fetches when you add a repo URL:

```kotlin
// RepositoryManager.kt
data class Repository(
    @JsonProperty("iconUrl")         val iconUrl: String?,
    @JsonProperty("name")            val name: String,
    @JsonProperty("description")     val description: String?,
    @JsonProperty("manifestVersion") val manifestVersion: Int,
    @JsonProperty("pluginLists")     val pluginLists: List<String>  // URLs to plugin list JSONs
)
```

This is NOT what we store — we only store `RepositoryData` (url + name + iconUrl).

---

## 5. SitePlugin — plugin metadata from remote repo

```kotlin
// RepositoryManager.kt
data class SitePlugin(
    @JsonProperty("url")           val url: String,
    @JsonProperty("status")        val status: Int,
    @JsonProperty("version")       val version: Int,
    @JsonProperty("apiVersion")    val apiVersion: Int,
    @JsonProperty("name")          val name: String,
    @JsonProperty("internalName")  val internalName: String,
    @JsonProperty("authors")       val authors: List<String>,
    @JsonProperty("description")   val description: String?,
    @JsonProperty("repositoryUrl") val repositoryUrl: String?,
    @JsonProperty("tvTypes")       val tvTypes: List<String>?,
    @JsonProperty("language")      val language: String?,
    @JsonProperty("iconUrl")       val iconUrl: String?,
    @JsonProperty("fileSize")      val fileSize: Long?,
)
```

---

## 6. DataStoreHelper Keys

**Source**: `app/.../utils/DataStoreHelper.kt`

All per-account keys are prefixed with `{currentAccountIndex}/`.

### Watch History (per account)

```kotlin
const val VIDEO_POS_DUR                   = "video_pos_dur"           // playback position+duration
const val VIDEO_WATCH_STATE               = "video_watch_state"       // Watched/None
const val RESULT_RESUME_WATCHING          = "result_resume_watching_2" // resume data (v2)
const val RESULT_RESUME_WATCHING_OLD      = "result_resume_watching"   // legacy (migrated)
const val RESULT_RESUME_WATCHING_HAS_MIGRATED = "result_resume_watching_migrated"
const val RESULT_EPISODE                  = "result_episode"           // last viewed episode
const val RESULT_SEASON                   = "result_season"            // last viewed season
const val RESULT_DUB                      = "result_dub"               // dub/sub preference
```

### Library (per account)

```kotlin
const val RESULT_WATCH_STATE              = "result_watch_state"           // Bookmarked/Favorited/Subscribed
const val RESULT_WATCH_STATE_DATA         = "result_watch_state_data"      // BookmarkedData
const val RESULT_SUBSCRIBED_STATE_DATA    = "result_subscribed_state_data" // SubscribedData
const val RESULT_FAVORITES_STATE_DATA     = "result_favorites_state_data"  // FavoritesData
```

### Provider Preferences

```kotlin
const val USER_PINNED_PROVIDERS           = "user_pinned_providers"    // pinned providers list
const val USER_SELECTED_HOMEPAGE_API      = "home_api_used"            // selected homepage API
const val USER_PROVIDER_API               = "user_custom_sites"        // custom override sites
const val KEY_RESULT_SORT                 = "result_sort"              // results sort order

// Used as string literals (no const):
"search_pref_providers"   // active search providers
"search_pref_tags"        // search content type filter
"home_pref_homepage"      // homepage content types
"library_sorting_mode"    // library sort mode
```

### Playback Preferences

```kotlin
"playback_speed"   // Float, default 1.0
"resize_mode"      // Int
```

### Account System

```kotlin
"data_store_helper/account"           // Array<Account>
"data_store_helper/account_key_index" // Int (selected account)
```

---

## 7. BackupUtils — nonTransferableKeys (complete list)

These keys are EXCLUDED from the built-in backup. Our sync plugin also excludes them.

```kotlin
private val nonTransferableKeys = listOf(
    // Sync cache (rebuilds from API)
    ANILIST_CACHED_LIST,
    MAL_CACHED_LIST,
    KITSU_CACHED_LIST,

    // Plugin metadata — we handle these separately
    PLUGINS_KEY,
    PLUGINS_KEY_LOCAL,

    // Auth tokens
    AccountManager.ACCOUNT_TOKEN,
    AccountManager.ACCOUNT_IDS,

    // Security — cannot transfer between devices
    "biometric_key",
    "nginx_user",

    // Device-specific paths — cannot transfer
    "download_path_key",
    "download_path_key_visual",
    "backup_path_key",
    "backup_dir_path_key",

    // Deprecated auth tokens
    "anilist_token",
    "anilist_user",
    "mal_user",
    "mal_token",
    "mal_refresh_token",
    "mal_unixtime",
    "open_subtitles_user",
    "subdl_user",
    "simkl_token",

    // Downloads
    DOWNLOAD_EPISODE_CACHE_BACKUP,
    DOWNLOAD_EPISODE_CACHE,
    KEY_DOWNLOAD_INFO,
    KEY_RESUME_IN_QUEUE,
    KEY_RESUME_PACKAGES,
    QUEUE_KEY
)
```

Note: `isTransferable()` uses `String.contains()` — it matches SUBSTRINGS, not exact keys.
Be careful with key names that might accidentally match (e.g. "token" matches "anilist_token").

---

## 8. DataStore Public API

**Source**: `app/.../utils/DataStore.kt`

```kotlin
// Write
fun <T : Any> Context.setKey(path: String, value: T)
fun <T : Any> Context.setKey(folder: String, path: String, value: T)

// Read
inline fun <reified T : Any> Context.getKey(path: String): T?
inline fun <reified T : Any> Context.getKey(path: String, defVal: T?): T?
inline fun <reified T : Any> Context.getKey(folder: String, path: String): T?

// Delete
fun Context.removeKey(path: String)
fun Context.removeKey(folder: String, path: String)
fun Context.removeKeys(folder: String): Int

// Query
fun Context.containsKey(path: String): Boolean
fun Context.getKeys(folder: String): List<String>
```

All values are stored as JSON strings via Jackson. Supported types: Boolean, Int, String, Float, Long, Set<String>, and any Jackson-serializable data class.

---

## 9. BackupVars / BackupFile Structure

```kotlin
data class BackupVars(
    @JsonProperty("_Bool")      val bool:      Map<String, Boolean>?,
    @JsonProperty("_Int")       val int:       Map<String, Int>?,
    @JsonProperty("_String")    val string:    Map<String, String>?,
    @JsonProperty("_Float")     val float:     Map<String, Float>?,
    @JsonProperty("_Long")      val long:      Map<String, Long>?,
    @JsonProperty("_StringSet") val stringSet: Map<String, Set<String>?>?,
)

data class BackupFile(
    @JsonProperty("datastore") val datastore: BackupVars,
    @JsonProperty("settings")  val settings:  BackupVars
)
```

Our `SyncPayload` extends this:

```kotlin
data class SyncPayload(
    val version: Int,
    val deviceId: String,
    val timestamp: String,      // ISO 8601
    val deviceName: String,
    val datastore: BackupVars,
    val settings: BackupVars,
    val repositories: List<RepositoryData>,
    val installedPlugins: List<SyncPluginEntry>   // filtered PluginData (online only, no filePath)
)

data class SyncPluginEntry(
    val internalName: String,
    val url: String,
    val version: Int
)
```

---

## 10. Plugin Registration

```kotlin
// Plugin.kt
abstract class Plugin {
    var resources: Resources? = null
    var openSettings: ((context: Context) -> Unit)? = null

    @Throws(Throwable::class)
    open fun load(context: Context) {}

    fun registerVideoClickAction(element: VideoClickAction)
}
```

Plugin class must be annotated:
```kotlin
@CloudstreamPlugin
class SyncPlugin : Plugin() { ... }
```

Settings UI approach (chosen): Fragment via FragmentManager
```kotlin
openSettings = { context ->
    val activity = context as? FragmentActivity
    activity?.supportFragmentManager
        ?.beginTransaction()
        ?.replace(android.R.id.content, SyncSettingsFragment())
        ?.addToBackStack(null)
        ?.commit()
}
```

---

## 11. Source File Locations (verified)

```
app/src/main/java/com/lagradost/cloudstream3/
  plugins/
    Plugin.kt                              — Plugin base class
    PluginManager.kt                       — PLUGINS_KEY, PLUGINS_KEY_LOCAL, PluginData
    RepositoryManager.kt                   — Repository, SitePlugin, RepositoryManager
  ui/settings/extensions/
    ExtensionsViewModel.kt                 — REPOSITORIES_KEY, RepositoryData
    ExtensionsFragment.kt                  — repo management UI
  utils/
    DataStore.kt                           — PREFERENCES_NAME, getKey/setKey API
    DataStoreHelper.kt                     — all user data keys
    BackupUtils.kt                         — BackupFile, BackupVars, nonTransferableKeys
    AppContextUtils.kt                     — getRepositories(), loadRepository()
```
