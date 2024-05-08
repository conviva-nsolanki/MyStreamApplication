package com.example.mystreamapplication

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaExperienceAnalytics.ICallback
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.google.ads.interactivemedia.v3.api.Ad
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdPodInfo

object VideoAnalytics {

    private lateinit var videoAnalytics: ConvivaVideoAnalytics
    private lateinit var adsAnalytics: ConvivaAdAnalytics

    private var mPodBreakPostion: String? = null
    private var mPodIndex = 0

    fun initialize(context: Context) {
        videoAnalytics = ConvivaAnalytics.buildVideoAnalytics(context)
    }

    fun initAdsSession(context: Context) {
        if (::videoAnalytics.isInitialized) {
            adsAnalytics = ConvivaAnalytics.buildAdAnalytics(context, videoAnalytics)
            val adTagInfo = hashMapOf<String, Any>()
            adTagInfo[ConvivaSdkConstants.IS_LIVE] = false
            adTagInfo["c3.ad.adManagerVersion"] = "3.31.0"
            adsAnalytics.setAdInfo(adTagInfo)
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun reportPlaybackRequested() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackRequested()
        } else {
            println("nannandenden ERROR: videoAnalytics not initialized")
        }
    }

    fun setAdContentInfo(addTagUrl: String? = null, isClient: Boolean = true) {
        if (::adsAnalytics.isInitialized) {
            val info = hashMapOf<String, Any>()
            if (isClient) {
                info[ConvivaSdkConstants.AD_PLAYER] = ConvivaSdkConstants.AdPlayer.SEPARATE.toString()
                if (!addTagUrl.isNullOrEmpty()) {
                    info[ConvivaSdkConstants.AD_TAG_URL] = addTagUrl
                }
            }
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

    fun logAdEvent(adEvent: AdEvent, isClient: Boolean = true) {
        when(adEvent.type) {
            AdEvent.AdEventType.LOADED -> {
                val metadata: HashMap<String, Any> = getAdsMetadata(adEvent.ad, isClient)
                val playerinfo: MutableMap<String, Any> = java.util.HashMap()
                playerinfo[ConvivaSdkConstants.FRAMEWORK_NAME] = "Google IMA SDK"
                playerinfo[ConvivaSdkConstants.FRAMEWORK_VERSION] = "3.11.2"
                adsAnalytics.reportAdLoaded(metadata)
                adsAnalytics.setAdPlayerInfo(playerinfo)
            }
            AdEvent.AdEventType.STARTED -> {
                val metadata: HashMap<String, Any> = getAdsMetadata(adEvent.ad, isClient)
                adsAnalytics.reportAdStarted(metadata)
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, adEvent.ad.vastMediaBitrate)
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, adEvent.ad.vastMediaWidth, adEvent.ad.vastMediaHeight)
            }
            AdEvent.AdEventType.SKIPPED -> {
                adsAnalytics.reportAdSkipped()
            }
            AdEvent.AdEventType.AD_PROGRESS -> {
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, adEvent.ad.vastMediaBitrate)
            }
            AdEvent.AdEventType.PAUSED -> {
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED)
            }
            AdEvent.AdEventType.AD_BUFFERING -> {
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
            }
            AdEvent.AdEventType.RESUMED -> {
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
                adsAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, adEvent.ad.vastMediaBitrate)
            }
            AdEvent.AdEventType.ALL_ADS_COMPLETED -> {

            }
            AdEvent.AdEventType.COMPLETED -> {
                adsAnalytics.reportAdEnded()
            }
            AdEvent.AdEventType.CONTENT_PAUSE_REQUESTED -> {
                if (isClient) {
                    val podInfo: AdPodInfo = adEvent.ad.adPodInfo
                    mPodBreakPostion =
                        if (podInfo.podIndex == 0) "PREROLL" else if (podInfo.podIndex == -1) "POSTROLL" else "MIDROLL"
                    mPodIndex++
                    val podStartAttributes: MutableMap<String, Any> = java.util.HashMap()
                    podStartAttributes[ConvivaSdkConstants.POD_DURATION] =
                        podInfo.maxDuration.toString()
                    podStartAttributes[ConvivaSdkConstants.POD_POSITION] = mPodBreakPostion!!
                    podStartAttributes[ConvivaSdkConstants.POD_INDEX] = mPodIndex.toString()
                    videoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.SEPARATE, ConvivaSdkConstants.AdType.CLIENT_SIDE, podStartAttributes)
                }
            }
            AdEvent.AdEventType.CONTENT_RESUME_REQUESTED -> {
                if (isClient) {
                    videoAnalytics.reportAdBreakEnded()
                }
            }
            AdEvent.AdEventType.LOG -> {

            }
            AdEvent.AdEventType.AD_BREAK_STARTED -> {
                if (isClient.not()) {
                    videoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.SERVER_SIDE)
                }
            }
            AdEvent.AdEventType.AD_BREAK_ENDED -> {
                if (isClient.not()) {
                    videoAnalytics.reportAdBreakEnded()
                }
            }
            else -> {}
        }
    }

    private fun getAdsMetadata(ad: Ad, isClient: Boolean): java.util.HashMap<String, Any> {
        val podInfo = ad.adPodInfo
        val podPosition =
            if (podInfo.podIndex == 0) ConvivaSdkConstants.AdPosition.PREROLL.toString() else if (podInfo.podIndex == -1) ConvivaSdkConstants.AdPosition.POSTROLL.toString() else ConvivaSdkConstants.AdPosition.MIDROLL.toString()
        val tags = hashMapOf<String, Any>()
        tags["c3.ad.technology"] = if (isClient) "Client Side" else "Server Side"
        tags["c3.ad.id"] = ad.adId
        tags["c3.ad.system"] = ad.adSystem
        tags["c3.ad.advertiser"] = ad.advertiserName
        tags["c3.ad.creativeId"] = ad.creativeId
        tags["c3.ad.description"] = ad.description
        tags["c3.ad.sequence"] = ad.adPodInfo.adPosition.toString()
        tags["c3.ad.position"] = podPosition
        tags["c3.ad.isSlate"] = false
        tags["c3.ad.mediaApiFramework"] = "NA"
        tags["c3.ad.adManagerName"] = "Google IMA SDK"
        tags["c3.ad.adManagerVersion"] = "3.11.2"
        val contentInfo: Map<String, Any> = videoAnalytics.metadataInfo
        tags["c3.ad.videoAssetName"] = contentInfo[ConvivaSdkConstants.ASSET_NAME].toString()
        tags[ConvivaSdkConstants.VIEWER_ID] = contentInfo[ConvivaSdkConstants.VIEWER_ID].toString()
        if (ad.universalAdIds.isNotEmpty()) {
            var adIdValue = "NA"
            var adIdRegistry = "NA"
            for (univAdId in ad.universalAdIds) {
                if (null != univAdId && univAdId.adIdValue.isNotEmpty() && univAdId.adIdValue != "unknown" && univAdId.adIdRegistry.isNotEmpty() && univAdId.adIdRegistry != "unknown"
                ) {
                    adIdValue = univAdId.adIdValue
                    adIdRegistry = univAdId.adIdRegistry
                    break
                }
            }
            tags["c3.ad.univAdIdVal"] = adIdValue
            tags["c3.ad.univAdIdReg"] = adIdRegistry
        }
        // First wrapper info

        // First wrapper info
        val firstAdSystem: String
        val firstAdId: String
        val firstCreativeId: String
        if (ad.adWrapperIds.isNotEmpty()) {
            val len = ad.adWrapperIds.size
            firstAdSystem = ad.adWrapperSystems[len - 1]
            firstAdId = ad.adWrapperIds[len - 1]
            firstCreativeId = ad.adWrapperCreativeIds[len - 1]
        } else {
            firstAdSystem = ad.adSystem
            firstAdId = ad.adId
            firstCreativeId = ad.creativeId
        }
        tags["c3.ad.firstAdSystem"] = firstAdSystem
        tags["c3.ad.firstAdId"] = firstAdId
        tags["c3.ad.firstCreativeId"] = firstCreativeId

        tags[ConvivaSdkConstants.ASSET_NAME] = ad.title
        tags[ConvivaSdkConstants.STREAM_URL] = "adtag_url"
        tags[ConvivaSdkConstants.IS_LIVE] = "false"
        tags[ConvivaSdkConstants.PLAYER_NAME] = "app name"
        tags[ConvivaSdkConstants.ENCODED_FRAMERATE] = "30"
        tags[ConvivaSdkConstants.DURATION] = ad.duration.toString()
        return tags
    }

    fun logAdError(adErrorEvent: AdErrorEvent) {
        val error = adErrorEvent.error
        val type = error.errorType
        val code = error.errorCode
        val errorMessage =
            "Code: " + code.toString() + "; Type: " + type.toString() + "; Message: " + error.message
        if (type == AdError.AdErrorType.PLAY) {
            adsAnalytics.reportAdError(errorMessage, ConvivaSdkConstants.ErrorSeverity.FATAL)
            adsAnalytics.reportAdEnded()
        } else {

            // no ad object available so no ad metadata
            val errorAdMetadata: MutableMap<String, Any> = java.util.HashMap()
            errorAdMetadata[ConvivaSdkConstants.ASSET_NAME] = "Ad Failed"
            handleAdError(errorAdMetadata, errorMessage)
        }
    }

    private fun handleAdError(errorAdMetadata: MutableMap<String, Any>, errorMessage: String) {
        adsAnalytics.reportAdError(errorMessage, ConvivaSdkConstants.ErrorSeverity.FATAL)
        adsAnalytics.reportAdEnded()
    }


}