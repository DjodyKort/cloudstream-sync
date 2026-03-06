package com.djody.cloudstreamsync

import com.fasterxml.jackson.annotation.JsonProperty

// Mirror of CloudStream's BackupVars — same JSON structure for compatibility.
data class SyncVars(
    @JsonProperty("_Bool")      val bool:      Map<String, Boolean>?,
    @JsonProperty("_Int")       val int:       Map<String, Int>?,
    @JsonProperty("_String")    val string:    Map<String, String>?,
    @JsonProperty("_Float")     val float:     Map<String, Float>?,
    @JsonProperty("_Long")      val long:      Map<String, Long>?,
    @JsonProperty("_StringSet") val stringSet: Map<String, Set<String>?>?,
)

// Minimal plugin entry for sync — filePath is device-specific and intentionally omitted.
data class SyncPluginEntry(
    @JsonProperty("internalName") val internalName: String,
    @JsonProperty("url")          val url: String,
    @JsonProperty("version")      val version: Int,
)

// Mirror of CloudStream's RepositoryData.
data class SyncRepositoryEntry(
    @JsonProperty("iconUrl") val iconUrl: String?,
    @JsonProperty("name")    val name: String,
    @JsonProperty("url")     val url: String,
)

data class SyncPayload(
    @JsonProperty("version")          val version: Int = PAYLOAD_VERSION,
    @JsonProperty("deviceId")         val deviceId: String,
    @JsonProperty("deviceName")       val deviceName: String,
    @JsonProperty("timestamp")        val timestamp: String,          // ISO 8601
    @JsonProperty("datastore")        val datastore: SyncVars,
    @JsonProperty("settings")         val settings: SyncVars,
    @JsonProperty("repositories")     val repositories: List<SyncRepositoryEntry>,
    @JsonProperty("installedPlugins") val installedPlugins: List<SyncPluginEntry>,
) {
    companion object {
        // Bump when SyncPayload structure changes incompatibly.
        const val PAYLOAD_VERSION = 1
    }
}
