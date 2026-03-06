package com.djody.cloudstreamsync

import android.content.Context
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SyncPlugin : Plugin() {
    override fun load(context: Context) {
        // TODO: initialize sync backend, schedule WorkManager sync

        openSettings = { ctx ->
            // TODO: replace with SyncSettingsFragment via FragmentManager
            Toast.makeText(ctx, "CloudStream Sync - Settings coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
