package com.djody.cloudstreamsync

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import com.lagradost.cloudstream3.utils.DataStore
import com.lagradost.cloudstream3.utils.DataStore.getDefaultSharedPrefs
import com.lagradost.cloudstream3.utils.DataStore.getSharedPrefs
import com.djody.cloudstreamsync.SyncConfig.syncHistory
import com.djody.cloudstreamsync.SyncConfig.syncPlugins
import com.djody.cloudstreamsync.SyncConfig.syncRepos
import com.djody.cloudstreamsync.SyncConfig.syncSettings

private const val TAG = "SyncImporter"

object DataImporter {

    /**
     * Applies a SyncPayload to this device.
     *
     * Returns an ImportResult describing what was applied and what requires
     * manual action (e.g. plugin reinstallation).
     */
    suspend fun import(context: Context, payload: SyncPayload): ImportResult {
        var keystoreWritten   = 0
        var settingsWritten   = 0
        var reposAdded        = 0
        val pluginsMissing    = mutableListOf<SyncPluginEntry>()

        // --- Datastore ---
        val datastoreEditor = DataStore.editor(context, isEditingAppSettings = false)
        payload.datastore.bool?.forEach      { (k, v) -> datastoreEditor.setKeyRaw(k, v); keystoreWritten++ }
        payload.datastore.int?.forEach       { (k, v) -> datastoreEditor.setKeyRaw(k, v); keystoreWritten++ }
        payload.datastore.string?.forEach    { (k, v) -> datastoreEditor.setKeyRaw(k, v); keystoreWritten++ }
        payload.datastore.float?.forEach     { (k, v) -> datastoreEditor.setKeyRaw(k, v); keystoreWritten++ }
        payload.datastore.long?.forEach      { (k, v) -> datastoreEditor.setKeyRaw(k, v); keystoreWritten++ }
        payload.datastore.stringSet?.forEach { (k, v) -> if (v != null) { datastoreEditor.setKeyRaw(k, v); keystoreWritten++ } }
        datastoreEditor.apply()

        // --- Settings (app default shared prefs) ---
        if (context.syncSettings) {
            val settingsEditor = DataStore.editor(context, isEditingAppSettings = true)
            payload.settings.bool?.forEach      { (k, v) -> settingsEditor.setKeyRaw(k, v); settingsWritten++ }
            payload.settings.int?.forEach       { (k, v) -> settingsEditor.setKeyRaw(k, v); settingsWritten++ }
            payload.settings.string?.forEach    { (k, v) -> settingsEditor.setKeyRaw(k, v); settingsWritten++ }
            payload.settings.float?.forEach     { (k, v) -> settingsEditor.setKeyRaw(k, v); settingsWritten++ }
            payload.settings.long?.forEach      { (k, v) -> settingsEditor.setKeyRaw(k, v); settingsWritten++ }
            payload.settings.stringSet?.forEach { (k, v) -> if (v != null) { settingsEditor.setKeyRaw(k, v); settingsWritten++ } }
            settingsEditor.apply()
        }

        // --- Repositories ---
        if (context.syncRepos) {
            val existingUrls = (context.getKey<Array<RepositoryData>>(
                DataExporter.REPOSITORIES_KEY
            ) ?: emptyArray()).map { it.url }.toSet()

            for (entry in payload.repositories) {
                if (entry.url !in existingUrls) {
                    try {
                        PluginManager.addRepository(
                            context,
                            RepositoryData(entry.iconUrl, entry.name, entry.url)
                        )
                        reposAdded++
                        Log.d(TAG, "Added repo: ${entry.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add repo ${entry.name}: ${e.message}")
                    }
                }
            }
        }

        // --- Plugins ---
        // We record which plugins are missing on this device but do NOT auto-install.
        // Auto-install requires Activity context and user confirmation (Fase 5).
        if (context.syncPlugins) {
            val installedNames = (context.getKey<Array<com.lagradost.cloudstream3.plugins.PluginData>>(
                DataExporter.PLUGINS_KEY
            ) ?: emptyArray()).map { it.internalName }.toSet()

            for (plugin in payload.installedPlugins) {
                if (plugin.internalName !in installedNames) {
                    pluginsMissing.add(plugin)
                    Log.d(TAG, "Plugin not installed on this device: ${plugin.internalName}")
                }
            }
        }

        return ImportResult(
            datastoreKeysWritten = keystoreWritten,
            settingsKeysWritten  = settingsWritten,
            reposAdded           = reposAdded,
            pluginsMissing       = pluginsMissing,
        )
    }

    /**
     * Result of an import operation.
     * [pluginsMissing] lists plugins present in the payload but not on this device.
     * The caller (UI layer) is responsible for showing these to the user.
     */
    data class ImportResult(
        val datastoreKeysWritten: Int,
        val settingsKeysWritten:  Int,
        val reposAdded:           Int,
        val pluginsMissing:       List<SyncPluginEntry>,
    ) {
        val hasMissingPlugins get() = pluginsMissing.isNotEmpty()
    }
}
