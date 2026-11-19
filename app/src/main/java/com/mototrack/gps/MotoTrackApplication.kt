package com.mototrack.gps

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

/**
 * MotoTrackApplication initializes global configurations such as osmdroid cache paths.
 */
class MotoTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set up OSMDroid tile cache in app internal storage for zero disk permissions needed
        val basePath = File(cacheDir, "osmdroid")
        if (!basePath.exists()) {
            basePath.mkdirs()
        }
        val tilePath = File(basePath, "tiles")
        if (!tilePath.exists()) {
            tilePath.mkdirs()
        }

        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = tilePath
        Configuration.getInstance().userAgentValue = packageName
    }
}
