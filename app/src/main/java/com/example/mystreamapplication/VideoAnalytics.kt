package com.example.mystreamapplication

import android.content.Context
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaVideoAnalytics

object VideoAnalytics {

    private lateinit var videoAnalytics: ConvivaVideoAnalytics

    fun initialize(context: Context) {
        videoAnalytics = ConvivaAnalytics.buildVideoAnalytics(context)
    }

    fun reportPlaybackRequested() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackRequested()
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun setContentInfo(contentInfo: Map<String, String>) {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.setContentInfo(contentInfo)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun reportPlaybackEnded() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackEnded()
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }


}