package com.example.mystreamapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Log.Logger
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.util.EventLogger
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import com.example.mystreamapplication.databinding.FragmentVideoBinding

class VideoFragment : Fragment(R.layout.fragment_video) {

    private var _binding: FragmentVideoBinding? = null
    private val binding: FragmentVideoBinding
        get() = _binding!!
    private lateinit var player: ExoPlayer

    private val listener: Player.Listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            for (index in 0 until events.size()) {
                println("nannandenden onEvents ${events.get(index)}")
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            println("nannandenden onTimelineChanged $timeline")

        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            println("nannandenden onMediaItemTransition $mediaItem")

        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            println("nannandenden onTracksChanged")

        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            println("nannandenden onMediaMetadataChanged")

        }

        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onPlaylistMetadataChanged(mediaMetadata)
            println("nannandenden onPlaylistMetadataChanged")

        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            println("nannandenden onIsLoadingChanged $isLoading")

        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            super.onAvailableCommandsChanged(availableCommands)
            println("nannandenden onAvailableCommandsChanged $availableCommands")

        }

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
            super.onTrackSelectionParametersChanged(parameters)
            println("nannandenden onTrackSelectionParametersChanged")

        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            val state = when(playbackState) {
                STATE_IDLE -> { "idle" }
                STATE_BUFFERING -> {"buffering"}
                STATE_READY -> { "ready" }
                STATE_ENDED -> {
                    VideoAnalytics.reportPlaybackEnded()
                    "ended"
                }
                else -> "invalid state"
            }
            println("nannandenden onPlaybackStateChanged $state")

        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            println("nannandenden onPlayWhenReadyChanged $playWhenReady")

        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            super.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
            println("nannandenden onPlaybackSuppressionReasonChanged")

        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            println("nannandenden onIsPlayingChanged $isPlaying")

        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            println("nannandenden onRepeatModeChanged")

        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled)
            println("nannandenden onShuffleModeEnabledChanged")

        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            println("nannandenden onPlayerError")

        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            super.onPlayerErrorChanged(error)
            println("nannandenden onPlayerErrorChanged")

        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            println("nannandenden onPositionDiscontinuity")

        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            super.onPlaybackParametersChanged(playbackParameters)
            println("nannandenden onPlaybackParametersChanged")

        }

        override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
            super.onSeekBackIncrementChanged(seekBackIncrementMs)
            println("nannandenden onSeekBackIncrementChanged")

        }

        override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
            super.onSeekForwardIncrementChanged(seekForwardIncrementMs)
            println("nannandenden onSeekForwardIncrementChanged")

        }

        override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
            super.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
            println("nannandenden onMaxSeekToPreviousPositionChanged")

        }

        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
            super.onAudioAttributesChanged(audioAttributes)
            println("nannandenden onAudioAttributesChanged")

        }

        override fun onVolumeChanged(volume: Float) {
            super.onVolumeChanged(volume)
            println("nannandenden onVolumeChanged")

        }

        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
            super.onSkipSilenceEnabledChanged(skipSilenceEnabled)
            println("nannandenden onSkipSilenceEnabledChanged")

        }

        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            super.onDeviceInfoChanged(deviceInfo)
            println("nannandenden onDeviceInfoChanged")

        }

        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            super.onDeviceVolumeChanged(volume, muted)
            println("nannandenden onDeviceVolumeChanged")

        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            println("nannandenden onVideoSizeChanged")

        }

        override fun onSurfaceSizeChanged(width: Int, height: Int) {
            super.onSurfaceSizeChanged(width, height)
            println("nannandenden onSurfaceSizeChanged $height $width")

        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            println("nannandenden onRenderedFirstFrame")

        }

        override fun onCues(cueGroup: CueGroup) {
            super.onCues(cueGroup)
            println("nannandenden onCues")

        }
    }

    companion object {
        fun newInstance(): VideoFragment = VideoFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVideoBinding.bind(view)
        startPlay(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.removeListener(listener)
        player.release()
        _binding = null
    }

    private fun startPlay(context: Context) {
        VideoAnalytics.setContentInfo(
            mapOf(
                ConvivaSdkConstants.ASSET_NAME to "Funny Cat",
                ConvivaSdkConstants.VIEWER_ID to "test_viewer_id",
                ConvivaSdkConstants.IS_LIVE to false,
                ConvivaSdkConstants.PLAYER_NAME to "text player name",
                "c3.cm.contentType" to ConvivaSdkConstants.StreamType.LIVE,
                "Custom Business Info" to "custom"
            )
        )

        VideoAnalytics.reportPlaybackRequested()

        // create a player
        player = ExoPlayer.Builder(context).build()
        VideoAnalytics.setPlayer(player)
        // attach to the playerView
        binding.playerView.player = player
        player.addListener(listener)
        player.addAnalyticsListener(EventLogger())

        val mediaItem: MediaItem = getMediaItem()
        println("nannandenden ${mediaItem.mediaMetadata}")

        player.setMediaItem(mediaItem)

        player.prepare()

        player.play()
    }

    private fun getMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri("https://html5demos.com/assets/dizzy.mp4")
            .setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse("https://storage.googleapis.com/exoplayer-test-media-1/mp4/frame-counter-one-hour.mp4")).build())
            .build()
    }


}