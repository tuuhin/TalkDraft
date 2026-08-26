package com.sam.talkdraft.player.utils

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import co.touchlab.kermit.Logger
import com.sam.talkdraft.player.model.PlayerTimeline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PLAYER_TRACK_DATA_FLOW"

internal fun Player.computePlayerTrackData(
    delayDuration: Duration = 100.milliseconds,
): Flow<PlayerTimeline> = callbackFlow {

    // first emission
    launch {
        try {
            val trackData = this@computePlayerTrackData.toTrackData()
            Logger.d(tag = TAG) { "CURRENT TRACK DATA :$trackData" }
            send(trackData)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "CANNOT EMIT INITIAL TRACK DATA" }
        }
    }

    var job: Job? = null
    fun startPeriodicUpdates() {
        job?.cancel()
        job = launch {
            try {
                while (isActive) {
                    val trackData = toTrackData()
                    if (trackData.allPositiveAndFinite) trySend(trackData)
                    delay(delayDuration)
                }
            } catch (_: CancellationException) {
                Logger.d(tag = TAG) { "ADVERTISING COROUTINE CANCELLED" }
            } catch (e: Exception) {
                Logger.e(tag = TAG, throwable = e) { "ERROR IN TRACK UPDATE" }
            }
        }
    }

    val listener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                launch {
                    try {
                        val trackData = this@computePlayerTrackData.toTrackData()
                        send(trackData)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Logger.e(tag = TAG, throwable = e) { "CANNOT SEND TRACK DATA PLAYER STATE CHANGE" }
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason != Player.DISCONTINUITY_REASON_SEEK) return
            launch {
                try {
                    val newPosDuration = newPosition.positionMs.milliseconds
                    val trackData = this@computePlayerTrackData.toTrackData()
                        .copy(current = newPosDuration)
                    send(trackData)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.e(tag = TAG, throwable = e) { "CANNOT SEND TRACK DATA ON SEEK POSITION CHANGE" }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                Logger.d(tag = TAG) { "STARTING PERIODIC UPDATES" }
                startPeriodicUpdates()
            } else {
                Logger.d(tag = TAG) { "STOPPING PERIODIC UPDATES" }
                job?.cancel()
                job = null
                launch {
                    try {
                        val track = this@computePlayerTrackData.toTrackData()
                        send(track)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.e(tag = TAG, throwable = e) { "CANNOT SEND TRACK DATA" }
                    }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason in arrayOf(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
            ) {
                launch {
                    try {
                        val trackData = this@computePlayerTrackData.toTrackData()
                        send(trackData)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Logger.e(tag = TAG, throwable = e) { "CANNOT SEND TRAC DATA MEDIA ITEM CHANGED" }
                    }
                }
            }
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                val itemDuration = mediaItem?.mediaMetadata?.durationMs?.milliseconds ?: return
                Logger.d(tag = TAG) { "PLAYER TIMELINE CHANGED" }
                launch {
                    val trackData = PlayerTimeline(0.seconds, itemDuration)
                    send(trackData)
                }
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
                launch {
                    try {
                        val window = Timeline.Window()
                        timeline.getWindow(0, window)
                        val duration = window.durationMs.takeIf { it != C.TIME_UNSET } ?: return@launch
                        val trackData = PlayerTimeline(0.seconds, duration.milliseconds)
                        send(trackData)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Logger.e(tag = TAG, throwable = e) { "Error sending timeline update" }
                    }
                }
            }
        }
    }

    Logger.d(tag = TAG) { "LISTENER ADDED" }
    addListener(listener)

    if (isPlaying) {
        Logger.d(tag = TAG) { "PLAYER IS ALREADY PLAYING" }
        startPeriodicUpdates()
    }

    awaitClose {
        job?.cancel()
        Logger.i(tag = TAG) { "REMOVING LISTENER" }
        removeListener(listener)
    }
}
    .filter { it.allPositiveAndFinite }
    .distinctUntilChanged { old, new -> old.current == new.current }


private suspend fun Player.toTrackData(): PlayerTimeline = withContext(Dispatchers.Main.immediate) {
    readTrackData()
}

private fun Player.readTrackData(): PlayerTimeline {
    val currentPos = currentPosition.takeIf { it != C.TIME_UNSET }?.milliseconds ?: Duration.ZERO
    val durationMs = duration.takeIf { it != C.TIME_UNSET }?.milliseconds ?: Duration.ZERO
    return PlayerTimeline(current = currentPos, total = durationMs)
}
