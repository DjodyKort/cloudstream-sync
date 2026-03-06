package com.djody.cloudstreamsync

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

private const val TAG = "SyncPlugin"

@CloudstreamPlugin
class SyncPlugin : Plugin() {

    override fun load(context: Context) {
        Log.d(TAG, "CloudStream Sync loaded")

        openSettings = { ctx ->
            // TODO Fase 4: replace with SyncSettingsFragment via FragmentManager
            android.widget.Toast.makeText(
                ctx,
                "CloudStream Sync — settings coming in Fase 4",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // TODO Fase 3: SyncScheduler.schedule(context) — WorkManager periodic sync
    }
}
