package com.example.mystreamapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.conviva.api.ConvivaConstants
import com.conviva.playerinterface.CVMediaExoPlayerListener
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
import java.lang.reflect.Field

object VideoAnalytics: ICallback {

    private lateinit var videoAnalytics: ConvivaVideoAnalytics
    private lateinit var adsAnalytics: ConvivaAdAnalytics

    private var mPodBreakPostion: String? = null
    private var mPodIndex = 0
    private var mPrevPlaybackState = ConvivaSdkConstants.PlayerState.UNKNOWN
    private var _mDuration = -1
    private var isAudioDisabled = false
    private var mAudioBitrate = -1
    private var mAvgVideoBitrate = -1
    private var mAvgAudioBitrate = -1
    private var mPeakBitrate = -1
    private var mAvgBitrate = -1
    private var mFrameRate = -1f
    private var mVideoBitrate = -1
    private var mPlayer: ExoPlayer? = null
    private var mainHandler: Handler? = null
    private val lock = Any()
    private var pht: Long = -1
    private var bufferLength = -1

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
            println("conviva ERROR: videoAnalytics not initialized")
        }
    }

    fun reportPlaybackRequested() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackRequested()
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
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
            println("conviva ERROR: adsAnalytics not initialized")
        }
    }

    fun setContentInfo(contentInfo: Map<String, Any>) {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.setContentInfo(contentInfo)
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
        }
    }
    @OptIn(UnstableApi::class)
    fun setPlayer(player: ExoPlayer) {
        mPlayer = player
        createHandler()
        if (::videoAnalytics.isInitialized) {
            setCallback(this)
            val fwField: Field = MediaLibraryInfo::class.java.getDeclaredField("VERSION")
            val version: String = fwField.get(null)?.toString()?: "N/A"
            val playerInfo = hashMapOf<String, Any>(
                ConvivaSdkConstants.FRAMEWORK_VERSION to version,
                ConvivaSdkConstants.FRAMEWORK_NAME to "ExoPlayer"
            )
            videoAnalytics.setPlayerInfo(playerInfo)
            player.addAnalyticsListener(
                object : AnalyticsListener {
                    override fun onPlaybackStateChanged(
                        eventTime: AnalyticsListener.EventTime,
                        state: Int
                    ) {
                        if (state == Player.STATE_ENDED) {
                            videoAnalytics.reportPlaybackEnded()
                        }
                        println("conviva onPlaybackStateChanged ${eventTime.currentPlaybackPositionMs} $state")
                        getMetrics()
                        parsePlayerState(player.playWhenReady, state)
                    }

                    override fun onPlayWhenReadyChanged(
                        eventTime: AnalyticsListener.EventTime,
                        playWhenReady: Boolean,
                        reason: Int
                    ) {
                        println("conviva onPlayWhenReadyChanged reasong: $reason")
                        var state = 0
                        if (Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST == reason) {
                            state = Player.STATE_READY
                        } else if (Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM == reason) {
                            state = Player.STATE_ENDED
                        }
                        parsePlayerState(playWhenReady, state)
                    }

                    override fun onIsPlayingChanged(
                        eventTime: AnalyticsListener.EventTime,
                        isPlaying: Boolean
                    ) {
                        println("conviva onIsPlayingChanged: isPlaying: $isPlaying")
                        mPlayer?.let { plyer ->
                            val state: Int = plyer.playbackState
                            getMetrics()
                            parsePlayerState(plyer.playWhenReady, state)
                        }
                    }

                    override fun onPlayerError(
                        eventTime: AnalyticsListener.EventTime,
                        error: PlaybackException
                    ) {
                        val errorMsg: String
                        val cause = error.cause
                        errorMsg = if (cause is DecoderInitializationException) {
                            CVMediaExoPlayerListener.DECODER_INIT_ERROR
                        } else {
                            CVMediaExoPlayerListener.RENDERER_INIT_ERROR
                        }
                        println("conviva onPlayerError errorMsg: $errorMsg")
                        getMetrics()
                        setPlayerState(ConvivaSdkConstants.PlayerState.STOPPED)
                        sendPlayerError(errorMsg, ConvivaConstants.ErrorSeverity.FATAL)
                    }

                    /**
                     * Called before a frame is rendered for the first time since setting the surface,
                     * and each time there's a change in the size or pixel aspect ratio of the video being rendered.
                     */
                    override fun onVideoSizeChanged(
                        eventTime: AnalyticsListener.EventTime,
                        videoSize: VideoSize
                    ) {
                        println("conviva onVideoSizeChanged")
                        getMetrics()
                        setVideoResolution(videoSize.width, videoSize.height)
                    }
                    // Called when the downstream format sent to the renderers changed.
                    override fun onDownstreamFormatChanged(
                        eventTime: AnalyticsListener.EventTime,
                        mediaLoadData: MediaLoadData
                    ) {
                        println("conviva onDownstreamFormatChanged")
                        computeAndReportBitrate(mediaLoadData)
                        computeAndReportAvgBitrate(mediaLoadData)
                        reportFrameRate(mediaLoadData)
                    }
                    // Called when a position discontinuity occurred.
                    override fun onPositionDiscontinuity(
                        eventTime: AnalyticsListener.EventTime,
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        println("conviva onPositionDiscontinuity")
                        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                            // Get the metric in event callback before triggering event
                            getMetrics()
                            setPlayerSeekEnd()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
                        println("conviva onSeekStarted")
                        getMetrics()
                        setPlayerSeekStarted()
                    }

                    // Called when a media source completed loading data.
                    override fun onLoadCompleted(
                        eventTime: AnalyticsListener.EventTime,
                        loadEventInfo: LoadEventInfo,
                        mediaLoadData: MediaLoadData
                    ) {
                        println("conviva onLoadCompleted")
                        if (mediaLoadData.trackFormat != null) {

                            //As onLoadCompleted is called whenever a segment is downloaded, so it can provide info
                            // when segment is downloaded, but we need info when segment is played by the player.
                            // Hence, better to use the info when we can't get it from onDownstreamFormatChanged.
                            // That's why we are checking if mPeakBitrate is not set in the session
                            checkAndUpdateAudioState(mPlayer!!)
                            if (-1 == mPeakBitrate && mediaLoadData.trackFormat!!.peakBitrate >= 0) {
                                computeAndReportBitrate(mediaLoadData)
                            }

                            if (-1 == mAvgBitrate && mediaLoadData.trackFormat!!.averageBitrate >= 0) {
                                computeAndReportAvgBitrate(mediaLoadData)
                            }

                            reportFrameRate(mediaLoadData)
                        }

                    }
                    // Called when the timeline changed.
                    override fun onTimelineChanged(
                        eventTime: AnalyticsListener.EventTime,
                        reason: Int
                    ) {
                        println("conviva onTimelineChanged: $reason")
                        val durationInMillis: Long = eventTime.timeline.getWindow(
                            player.currentMediaItemIndex,
                            Timeline.Window()
                        ).durationMs
                        if (_mDuration.toLong() != durationInMillis && durationInMillis > 0) {
                            setDuration((durationInMillis / 1000).toInt())
                        }
                    }

                    override fun onAudioCodecError(
                        eventTime: AnalyticsListener.EventTime,
                        audioCodecError: java.lang.Exception
                    ) {
                        println("conviva onAudioCodecError")
                        sendPlayerError(
                            audioCodecError.localizedMessage,
                            ConvivaConstants.ErrorSeverity.FATAL
                        )
                    }

                    override fun onVideoCodecError(
                        eventTime: AnalyticsListener.EventTime,
                        videoCodecError: java.lang.Exception
                    ) {
                        println("conviva onVideoCodecError")
                        sendPlayerError(
                            videoCodecError.localizedMessage,
                            ConvivaConstants.ErrorSeverity.FATAL
                        )
                    }

                    override fun onAudioSinkError(
                        eventTime: AnalyticsListener.EventTime,
                        audioSinkError: java.lang.Exception
                    ) {
                        println("conviva onAudioSinkError")
                        sendPlayerError(
                            audioSinkError.localizedMessage,
                            ConvivaConstants.ErrorSeverity.WARNING
                        )
                    }

                    override fun onTracksChanged(
                        eventTime: AnalyticsListener.EventTime,
                        tracks: Tracks
                    ) {
                        println("conviva onTracksChanged")
                        super.onTracksChanged(eventTime, tracks)
                        checkAndUpdateAudioState(player)
                    }

                    override fun onDroppedVideoFrames(
                        eventTime: AnalyticsListener.EventTime,
                        droppedFrames: Int,
                        elapsedMs: Long
                    ) {
                        println("conviva onDroppedVideoFrames")
                        if (droppedFrames > 0) setDroppedFrameCount(droppedFrames)
                    }

                    override fun onTrackSelectionParametersChanged(
                        eventTime: AnalyticsListener.EventTime,
                        trackSelectionParameters: TrackSelectionParameters
                    ) {
                        super.onTrackSelectionParametersChanged(eventTime, trackSelectionParameters)
                        println("conviva onTrackSelectionParametersChanged")
                        val disabledTrackTypes = trackSelectionParameters.disabledTrackTypes
                        isAudioDisabled = disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
                        if (isAudioDisabled) {
                            isAudioDisabled = true
                            mAudioBitrate = -1
                            mAvgAudioBitrate = -1
                            //Reporting Bitrate and Avg Bitrate
                            if (mVideoBitrate > -1) {
                                setPlayerBitrateKbps(mVideoBitrate / 1000, false)
                                mPeakBitrate = mVideoBitrate
                            }
                            if (mAvgVideoBitrate > -1) {
                                setPlayerBitrateKbps(mAvgVideoBitrate / 1000, true)
                                mAvgBitrate = mAvgVideoBitrate
                            }
                        }
                    }

                }
            )
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
        }
    }

    private fun createHandler() {
        //As per the exoplayer docs, player apis must be called from the Looper where its object is created.
        mainHandler = if (mPlayer != null) {
            Handler(mPlayer!!.applicationLooper)
        } else if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper())
        } else {
            Handler()
        }
    }

    private fun setPlayerSeekStarted() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED)
        }
    }

    private fun setPlayerSeekEnd() {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED)
        }
    }

    private fun setVideoResolution(width: Int, height: Int) {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.RESOLUTION,
                width,
                height
            )
        }
    }

    fun getMetrics() {
        try {
            //pht and bl should be fetched from the main thread as per exoplayer v.2.9.3
            //CSR-3307
            if (mPlayer != null) {
                pht = mPlayer!!.currentPosition
                bufferLength =
                    (mPlayer!!.bufferedPosition - mPlayer!!.currentPosition).toInt()
                println("conviva getMetrics currentPosition: $pht bufferLength: $bufferLength")
            }
        } catch (e: Exception) {
            //Log("Exception occurred " + e.getMessage(), SystemSettings.LogLevel.DEBUG);
            e.printStackTrace()
        }
    }

    private fun setDroppedFrameCount(droppedFrameCount: Int) {
        if (::videoAnalytics.isInitialized && droppedFrameCount > 0) {
            videoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.DROPPED_FRAMES_COUNT,
                droppedFrameCount
            )
        }
    }

    private fun setEncodedFrameRate(frameRate: Int) {
        if (::videoAnalytics.isInitialized && frameRate >= 0) {
            videoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.ENCODED_FRAMERATE,
                frameRate
            )
        }
    }

    @OptIn(UnstableApi::class)
    private fun reportFrameRate(mediaLoadData: MediaLoadData?) {
        if (mediaLoadData?.trackFormat == null) return
        val frameRate = mediaLoadData.trackFormat!!.frameRate.toInt()
        if (frameRate >= 0 && mFrameRate != frameRate.toFloat()) {
            setEncodedFrameRate(frameRate)
            mFrameRate = frameRate.toFloat()
        }
    }

    @OptIn(UnstableApi::class)
    private fun computeAndReportAvgBitrate(mediaLoadData: MediaLoadData?) {
        if (mediaLoadData?.trackFormat == null) return
        val bitrate = mediaLoadData.trackFormat!!.averageBitrate
        if (bitrate != -1) {
            if (mediaLoadData.trackType == C.TRACK_TYPE_DEFAULT) {    // Hls content - No Audio content
                mAvgVideoBitrate = mediaLoadData.trackFormat!!.averageBitrate
                mAvgAudioBitrate = 0
            } else if (mediaLoadData.trackType == C.TRACK_TYPE_AUDIO) {
                mAvgAudioBitrate = mediaLoadData.trackFormat!!.averageBitrate
            } else if (mediaLoadData.trackType == C.TRACK_TYPE_VIDEO) {
                mAvgVideoBitrate = mediaLoadData.trackFormat!!.averageBitrate
            }
            // DE-5310: For SS and Dash contents Audio + Video bitrates are overall bitrate
            // For HLS Demuxed contents as well the TRACK_TYPE_DEFAULT which is limitation of Exo Player
            if (mAvgAudioBitrate >= 0 && mAvgVideoBitrate >= 0) {
                // Get the metric in event callback before triggering event
                setPlayerBitrateKbps((mAvgAudioBitrate + mAvgVideoBitrate) / 1000, true)
                mAvgBitrate = mAvgAudioBitrate + mAvgVideoBitrate
            } /* When we have only video bitrate available we are reporting it
             */ else if (mAvgVideoBitrate >= 0 && isAudioDisabled) {
                // Get the metric in event callback before triggering event
                setPlayerBitrateKbps(mAvgVideoBitrate / 1000, true)
                mAvgBitrate = mAvgVideoBitrate
            }
        }
    }


    private fun checkAndUpdateAudioState(player: ExoPlayer) {
        val trackSelectionParameters: TrackSelectionParameters =
            player.trackSelectionParameters
        val disabledTrackTypes = trackSelectionParameters.disabledTrackTypes
        isAudioDisabled = disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
    }

    private fun parsePlayerState(playWhenReady: Boolean, playbackState: Int) {
        println("conviva parsePlayerState: $playbackState")
        when (playbackState) {
            Player.STATE_BUFFERING -> setPlayerState(ConvivaSdkConstants.PlayerState.BUFFERING)
            Player.STATE_ENDED -> setPlayerState(ConvivaSdkConstants.PlayerState.STOPPED)
            Player.STATE_READY -> if (playWhenReady) {
                //DE-8423 Sometimes player state gets reported event when play head is not moving but
                //Player is in READY state. To avoid false play states check if playback is
                //really happening.
                if (mPlayer?.isPlaying == true) {
                    setPlayerState(ConvivaSdkConstants.PlayerState.PLAYING)
                } else if (mPlayer?.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
                    setPlayerState(ConvivaSdkConstants.PlayerState.PAUSED)
                } else {
                    setPlayerState(ConvivaSdkConstants.PlayerState.BUFFERING)
                }

                //content length is available only after preparing state
                val dur: Int = (mPlayer?.duration?.toInt() ?: 0) / 1000
                if (_mDuration != dur && dur > 0) {
                    setDuration((mPlayer?.duration?.toInt()?:0) / 1000)
                    _mDuration = dur
                }
            } else {
                setPlayerState(ConvivaSdkConstants.PlayerState.PAUSED)
            }

            else -> {}
        }
    }

    private fun setDuration(duration: Int) {
        if (::videoAnalytics.isInitialized && duration > 0) {
            val info: MutableMap<String, Any> = java.util.HashMap()
            info[ConvivaSdkConstants.DURATION] = duration
            videoAnalytics.setContentInfo(info)
        }
    }

    private fun setPlayerState(playerState: ConvivaSdkConstants.PlayerState) {
        if (playerState == mPrevPlaybackState) return
        if (::videoAnalytics.isInitialized) {
            println("conviva reportPlaybackMetric PLAYER_STATE: $playerState")
            when (playerState) {
                ConvivaSdkConstants.PlayerState.BUFFERING -> videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.BUFFERING
                )

                ConvivaSdkConstants.PlayerState.STOPPED -> videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.STOPPED
                )

                ConvivaSdkConstants.PlayerState.PLAYING -> videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PLAYING
                )

                ConvivaSdkConstants.PlayerState.PAUSED -> videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PAUSED
                )

                else -> {}
            }
            mPrevPlaybackState = playerState
        }
    }

    private fun sendPlayerError(errorMsg: String?, severity: ConvivaConstants.ErrorSeverity) {
        if (::videoAnalytics.isInitialized) {
            println("conviva sendPlayerError")
            if (ConvivaConstants.ErrorSeverity.FATAL == severity) {
                videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.STOPPED
                )
                videoAnalytics.reportPlaybackError(
                    errorMsg,
                    ConvivaSdkConstants.ErrorSeverity.FATAL
                )
            } else videoAnalytics.reportPlaybackError(
                errorMsg,
                ConvivaSdkConstants.ErrorSeverity.WARNING
            )
        }
    }

    private fun setPlayerBitrateKbps(bitrate: Int, isAvgBitrate: Boolean) {
        if (::videoAnalytics.isInitialized && bitrate >= 0) {
            if (!isAvgBitrate) {
                videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.BITRATE,
                    bitrate, true
                )
            } else {
                videoAnalytics.reportPlaybackMetric(
                    ConvivaSdkConstants.PLAYBACK.AVG_BITRATE,
                    bitrate, true
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun computeAndReportBitrate(mediaLoadData: MediaLoadData?) {
        if (mediaLoadData?.trackFormat == null) return
        val bitrate = mediaLoadData.trackFormat!!.peakBitrate
        if (bitrate != -1) {
            if (mediaLoadData.trackType == C.TRACK_TYPE_DEFAULT) {    // Hls content - No Audio content
                mVideoBitrate = mediaLoadData.trackFormat!!.peakBitrate
                mAudioBitrate = 0
            } else if (mediaLoadData.trackType == C.TRACK_TYPE_AUDIO) {
                mAudioBitrate = mediaLoadData.trackFormat!!.peakBitrate
            } else if (mediaLoadData.trackType == C.TRACK_TYPE_VIDEO) {
                mVideoBitrate = mediaLoadData.trackFormat!!.peakBitrate
            }
            // DE-5310: For SS and Dash contents Audio + Video bitrates are overall bitrate
            // For HLS Demuxed contents as well the TRACK_TYPE_DEFAULT which is limitation of Exo Player
            if (mAudioBitrate >= 0 && mVideoBitrate >= 0) {
                // Get the metric in event callback before triggering event
                setPlayerBitrateKbps((mAudioBitrate + mVideoBitrate) / 1000, false)
                mPeakBitrate = mAudioBitrate + mVideoBitrate
            } /* When we have only video bitrate available we are reporting it
             */ else if (mVideoBitrate >= 0 && isAudioDisabled) {
                // Get the metric in event callback before triggering event
                setPlayerBitrateKbps(mVideoBitrate / 1000, false)
                mPeakBitrate = mVideoBitrate
            }
        }
    }

    fun release() {
        if (::videoAnalytics.isInitialized) {
            println("conviva release video player")
            videoAnalytics.release()
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
        }

        if (::adsAnalytics.isInitialized) {
            println("conviva release video player")
            adsAnalytics.release()
        } else {
            println("conviva ERROR: adsAnalytics not initialized")
        }
    }

    fun reportPlaybackFailed(errorMessage: String) {
        if (::videoAnalytics.isInitialized) {
            println("conviva reportPlaybackFailed")
            videoAnalytics.reportPlaybackFailed(errorMessage)
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
        }
    }

    fun reportPlaybackMetric(key: String, vararg value: Any) {
        if (::videoAnalytics.isInitialized) {
            println("conviva reportPlaybackMetric")
            videoAnalytics.reportPlaybackMetric(key, value)
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
        }
    }

    fun setCallback(callback: ICallback) {
        if (::videoAnalytics.isInitialized) {
            println("conviva setCallback")
            videoAnalytics.setCallback(callback)
        } else {
            println("conviva ERROR: videoAnalytics not initialized")
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

    override fun update() {
        println("conviva update")
        updateMetrics()
    }

    private fun updateMetrics() {
        if (mPlayer != null && mainHandler != null) {
            mainHandler?.post(Runnable {
                synchronized(lock) {
                    getMetrics()
                    updatedMetrics(pht, bufferLength)
                }
            })
        }
    }

    private fun updatedMetrics(pht: Long, bufferLength: Int) {
        if (::videoAnalytics.isInitialized) {
            videoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.PLAY_HEAD_TIME,
                pht
            )
            videoAnalytics.reportPlaybackMetric(
                ConvivaSdkConstants.PLAYBACK.BUFFER_LENGTH,
                if (bufferLength >= 0) bufferLength else -1
            )
        }
    }

    override fun update(p0: String?) {
        println("conviva update: $p0")
        if (!p0.isNullOrEmpty()) {
            if (ConvivaSdkConstants.PLAYBACK.CDN_IP == p0) {
                getCDNServerIP()
            }
        }
    }

    private fun getCDNServerIP() {
        // TODO
    }


}