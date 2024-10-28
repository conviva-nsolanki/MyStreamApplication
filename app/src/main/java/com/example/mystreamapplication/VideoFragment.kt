package com.example.mystreamapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import com.conviva.sdk.ConvivaSdkConstants
import com.example.mystreamapplication.VideoAnalytics.TAG
import com.example.mystreamapplication.databinding.FragmentVideoBinding
import com.google.ads.interactivemedia.v3.api.AdEvent


class VideoFragment : Fragment(R.layout.fragment_video) {

    private var _binding: FragmentVideoBinding? = null
    private val binding: FragmentVideoBinding
        get() = _binding!!
    private lateinit var player: ExoPlayer

    private var startAutoPlay: Boolean = true

    private val ADD_TAG_URL = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator="
    private val CSAI_URL = "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv"
    private val SSAI_URL = "ssai://dai.google.com/?contentSourceId=2559737&videoId=tos-dash&format=0&adsId=1"

    private var adsLoader: ImaAdsLoader? = null

    @UnstableApi
    private var serverSideAdsLoader: ImaServerSideAdInsertionMediaSource.AdsLoader? = null

    private val listener: Player.Listener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            val state = when (playbackState) {
                STATE_IDLE -> {
                    "idle"
                }
                STATE_BUFFERING -> {
                    "buffering"
                }
                STATE_READY -> {
                    "ready"
                }
                STATE_ENDED -> {
                    "ended"
                }
                else -> "invalid state"
            }
            Log.d(TAG, "onPlaybackStateChanged $state")

        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.d(TAG, " onPlayerError")

        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            super.onPlayerErrorChanged(error)
            Log.d(TAG, " onPlayerErrorChanged")

        }
    }

    companion object {
        fun newInstance(type: String): VideoFragment {
            val fragment = VideoFragment()
            fragment.arguments = bundleOf(KEY_VIDEO_TYPE to type)
            return fragment
        }

        const val KEY_VIDEO_TYPE = "key_video_type"
        const val VIDEO_NO_ADS = "Play(No Ads)"
        const val VIDEO_CSAI = "Play(CSAI)"
        const val VIDEO_SSAI = "Play(SSAI)"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVideoBinding.bind(view)
        val type = arguments?.getString(KEY_VIDEO_TYPE)
        if (type.isNullOrEmpty()) {
            activity?.supportFragmentManager?.popBackStack()
        }
        startPlay(requireContext(), type)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.removeListener(listener)
        player.release()
        _binding = null
        adsLoader?.setPlayer(null)
        VideoAnalytics.release()
    }

    @OptIn(UnstableApi::class)
    private fun startPlay(context: Context, type: String?) {
        VideoAnalytics.initialize(context)
        VideoAnalytics.setContentInfo(
            mapOf(
                ConvivaSdkConstants.ASSET_NAME to (type?: "No name"),
                ConvivaSdkConstants.VIEWER_ID to "test_viewer_id",
                ConvivaSdkConstants.IS_LIVE to false,
                ConvivaSdkConstants.PLAYER_NAME to "Stream Sample App",
                "custom_business_logic_tag" to "custom",
                "test attribute key" to "default"
            )
        )

        VideoAnalytics.reportPlaybackRequested()

        VideoAnalytics.initAdsSession(context)

        // create a player
        player = getPlayer(context, type, binding.playerView)
        // set the player
        Log.d(TAG, " setPlayer called")
        VideoAnalytics.setPlayer(player)
        adsLoader?.setPlayer(player)
        serverSideAdsLoader?.setPlayer(player)

        val mediaItem: MediaItem = getMediaItem(type)
        player.setMediaItem(mediaItem)
        when(type) {
            VIDEO_CSAI -> {
                VideoAnalytics.setAdContentInfo(CSAI_URL)
            }
            VIDEO_SSAI -> {
                VideoAnalytics.setAdContentInfo(addTagUrl = SSAI_URL, isClient = false)
            }
        }
        player.addListener(listener)
        player.addAnalyticsListener(EventLogger())
        player.playWhenReady = startAutoPlay

        // attach to the playerView
        binding.playerView.player = player

        player.prepare()
    }

    @OptIn(UnstableApi::class)
    private fun getPlayer(context: Context, type: String?, playerView: PlayerView): ExoPlayer {
        val player = ExoPlayer.Builder(context)
        when(type) {
            VIDEO_CSAI -> {
                val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)
                if (adsLoader == null) {
                    adsLoader = ImaAdsLoader
                        .Builder(context)
                        .setAdEventListener { adEvent: AdEvent ->
                            Log.d(TAG, " ${adEvent.type}")
                            VideoAnalytics.logAdEvent(adEvent)
                        }
                        .setAdErrorListener { adErrorEvent ->
                            VideoAnalytics.logAdError(adErrorEvent)
                        }
                        .build()
                }
                val mediaSourceFactory: MediaSource.Factory =
                    DefaultMediaSourceFactory(dataSourceFactory)
                        .setLocalAdInsertionComponents(
                            { unusedAdTagUri: AdsConfiguration? -> adsLoader },
                            playerView
                        )
                player.setMediaSourceFactory(mediaSourceFactory)
            }
            VIDEO_SSAI -> {
                // MediaSource.Factory to load the actual media stream.
                val defaultMediaSourceFactory = DefaultMediaSourceFactory(context)
                if (serverSideAdsLoader == null) {
                    serverSideAdsLoader = ImaServerSideAdInsertionMediaSource.AdsLoader
                        .Builder(context, playerView)
                        .setAdEventListener { adEvent: AdEvent ->
                            Log.d(TAG, " ${adEvent.type}")
                            VideoAnalytics.logAdEvent(adEvent, isClient = false)
                        }
                        .setAdErrorListener { adErrorEvent ->
                            VideoAnalytics.logAdError(adErrorEvent)
                        }
                        .build()
                }
                val adsMediaSourceFactory =
                    ImaServerSideAdInsertionMediaSource.Factory(serverSideAdsLoader!!, defaultMediaSourceFactory)
                defaultMediaSourceFactory.setServerSideAdInsertionMediaSourceFactory(adsMediaSourceFactory)
                player.setMediaSourceFactory(defaultMediaSourceFactory)
            }
        }
        return player.build()
    }

    private fun getMediaItem(type: String?): MediaItem {
        val media = MediaItem.Builder()
        when (type) {
            VIDEO_CSAI -> {
                val addTagUri = Uri.parse(ADD_TAG_URL)
                media.setUri(CSAI_URL)
                    .setAdsConfiguration(AdsConfiguration.Builder(addTagUri).build())
            }

            VIDEO_SSAI -> {
                media.setUri(SSAI_URL)
            }

            else -> {
                media.setUri("https://html5demos.com/assets/dizzy.mp4")
            }
        }
        return media.build()
    }


}