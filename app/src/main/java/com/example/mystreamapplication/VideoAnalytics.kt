package com.example.mystreamapplication

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaExperienceAnalytics.ICallback
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics

object VideoAnalytics {

    private lateinit var videoAnalytics: ConvivaVideoAnalytics
    private lateinit var adsAnalytics: ConvivaAdAnalytics

    fun initialize(context: Context) {
        videoAnalytics = ConvivaAnalytics.buildVideoAnalytics(context)
        adsAnalytics = ConvivaAnalytics.buildAdAnalytics(context, videoAnalytics)
    }

    fun reportPlaybackRequested() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackRequested()
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun setAdContentInfo(adsLoader: Any, addTagUrl: String? = null) {
        if (::adsAnalytics.isInitialized) {
            val info: HashMap<String, Any> = hashMapOf(
                ConvivaSdkConstants.AD_PLAYER to ConvivaSdkConstants.AdPlayer.CONTENT.toString()
            )
            if (!addTagUrl.isNullOrEmpty()) {
                info[ConvivaSdkConstants.AD_TAG_URL] = addTagUrl
            }
            adsAnalytics.setAdListener(adsLoader, info)
        } else {
            println("nannandenden ERROR: adsAnalytics not initialized")
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

        if (::adsAnalytics.isInitialized) {
            println("nannandenden release video player")
            adsAnalytics.release()
        } else {
            println("nannandenden ERROR: adsAnalytics not initialized")
        }
    }

    fun reportPlaybackFailed(errorMessage: String) {
        if (::videoAnalytics.isInitialized) {
            println("nannandenden reportPlaybackFailed")
            videoAnalytics.reportPlaybackFailed(errorMessage)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun reportPlaybackMetric(key: String, vararg value: Any) {
        if (::videoAnalytics.isInitialized) {
            println("nannandenden reportPlaybackMetric")
            videoAnalytics.reportPlaybackMetric(key, value)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun setCallback(callback: ICallback) {
        if (::videoAnalytics.isInitialized) {
            println("nannandenden setCallback")
            videoAnalytics.setCallback(callback)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun reportAdBreakStarted(isClient: Boolean = true) {
        if (::adsAnalytics.isInitialized) {
            videoAnalytics.reportAdBreakStarted(
                ConvivaSdkConstants.AdPlayer.CONTENT,
                if (isClient) ConvivaSdkConstants.AdType.CLIENT_SIDE else ConvivaSdkConstants.AdType.SERVER_SIDE
            )
        } else {
            println("nannandenden ERROR: adsAnalytics not initialized")
        }
    }

    fun reportAdBreakEnded() {
        if (::adsAnalytics.isInitialized) {
            videoAnalytics.reportAdBreakEnded()
        } else {
            println("nannandenden ERROR: adsAnalytics not initialized")
        }
    }






}