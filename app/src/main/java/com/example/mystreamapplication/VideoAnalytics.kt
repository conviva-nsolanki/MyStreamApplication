package com.example.mystreamapplication

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
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

    fun setContentInfo(contentInfo: Map<String, Any>) {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.setContentInfo(contentInfo)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun reportPlaybackEnded() {
        if (::videoAnalytics.isInitialized) {
            println("nannandenden reportPlaybackEnded")
            videoAnalytics.reportPlaybackEnded()
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun setPlayer(player: ExoPlayer) {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.setPlayer(player)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun release() {
        if (::videoAnalytics.isInitialized) {
            println("nannandenden release video player")
            videoAnalytics.release()
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }


}