package com.sam.talkdraft.player

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.ext.tryWithLock
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.player.model.AudioSource
import com.sam.talkdraft.player.model.PlayerPlayBackSpeed
import com.sam.talkdraft.player.model.PlayerPlayItemMetadata
import com.sam.talkdraft.player.model.PlayerPlaybackState
import com.sam.talkdraft.player.model.PlayerTimeline
import com.sam.talkdraft.player.utils.PlayerKVOObserver
import io.ktor.utils.io.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLooper
import platform.AVFoundation.AVPlayerStatusFailed
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.defaultRate
import platform.AVFoundation.duration
import platform.AVFoundation.isMuted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setDefaultRate
import platform.AVFoundation.setMuted
import platform.AVFoundation.setRate
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSURL
import platform.Foundation.addObserver
import platform.Foundation.removeObserver

private const val TAG = "IOS_AUDIO_PLAYER"

@OptIn(ExperimentalForeignApi::class)
@Factory(binds = [IAudioPlayer::class])
internal actual class PlatformAudioPlayerImpl(
    val player: AVQueuePlayer,
    val dispatchers: IPlatformCoroutineDispatchers,
) : IAudioPlayer {

    private var _looper: AVPlayerLooper? = null
    private var _playItem: AVPlayerItem? = null
    private val _lock = Mutex()

    actual override val playerState: Flow<PlayerPlayItemMetadata>
        get() = callbackFlow {

            launch {
                val metadata = readMetadata()
                trySend(metadata)
            }

            val observers = PlayerKVOObserver {
                val metadata = readMetadata()
                trySend(metadata)
            }

            val keyPaths = listOf("timeControlStatus", "rate", "muted", "currentItem.status")

            keyPaths.forEach { keyPath ->
                player.addObserver(
                    observer = observers,
                    forKeyPath = keyPath,
                    options = NSKeyValueObservingOptionNew,
                    context = null,
                )
                Logger.i(tag = TAG) { "ADDING OBSERVER FOR KEY :$keyPath" }
            }

            awaitClose {
                keyPaths.forEach { keyPath ->
                    Logger.i(tag = TAG) { "REMOVING OBSERVER FOR KEY :$keyPath" }
                    player.removeObserver(observer = observers, forKeyPath = keyPath)
                }
            }
        }


    actual override val timeline: Flow<PlayerTimeline>
        get() = callbackFlow {
            val block: (CValue<CMTime>) -> Unit = { time: CValue<CMTime> ->
                val currentItem = player.currentItem
                val positionSeconds = CMTimeGetSeconds(time)
                val currentPosition = if (positionSeconds.isFinite() && positionSeconds >= 0) positionSeconds else 0.0

                val durationTime = currentItem?.duration
                val rawDurationSeconds = durationTime?.let { CMTimeGetSeconds(it) } ?: 0.0
                val totalDuration =
                    if (rawDurationSeconds.isFinite() && rawDurationSeconds > 0) rawDurationSeconds else 0.0

                val playerTimeline = PlayerTimeline(
                    current = currentPosition.seconds,
                    total = totalDuration.seconds,
                )
                trySend(playerTimeline)
            }

            val interval = CMTimeMakeWithSeconds(seconds = .1, preferredTimescale = 1000)
            val observer = player.addPeriodicTimeObserverForInterval(interval, queue = null, usingBlock = block)
            Logger.i(tag = TAG) { "ADDING A PERIODIC TIME OBSERVER FOR INTERVAL 100 MILLISECONDS" }

            awaitClose {
                Logger.i(tag = TAG) { "REMOVING TIMED OBSERVER" }
                player.removeTimeObserver(observer)
            }
        }

    actual override val errorFlow: Flow<Throwable>
        get() = callbackFlow {

            val observer = PlayerKVOObserver {
                val currentItem = player.currentItem ?: return@PlayerKVOObserver
                val allGood = currentItem.status != AVPlayerStatusFailed
                if (allGood) return@PlayerKVOObserver
                val nsError = currentItem.error
                val errorMessage = nsError?.localizedDescription ?: "Unknown AVPlayerItem playback error"
                val errorCode = nsError?.code ?: -1
                trySend(IllegalStateException("AVPlayer Error ($errorCode): $errorMessage"))
            }

            player.addObserver(
                observer = observer,
                forKeyPath = "currentItem.status",
                options = NSKeyValueObservingOptionNew,
                context = null,
            )

            awaitClose {
                player.removeObserver(observer, forKeyPath = "currentItem.status")
            }
        }


    actual override suspend fun prepare(source: AudioSource): Result<Boolean> = withContext(dispatchers.main) {
        try {
            val nsUrl = NSURL.URLWithString(source.source)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL: ${source.source}"))

            _looper?.disableLooping()
            _looper = null

            val playerItem = AVPlayerItem(nsUrl)
            player.replaceCurrentItemWithPlayerItem(playerItem)
            _playItem = playerItem

            Result.success(true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG) { "UNABLE TO PREPARE PLAYER" }
            Result.failure(e)
        }
    }

    actual override suspend fun play() = withContext(dispatchers.main) {
        _lock.withLock(this) {
            try {
                player.play()
                Logger.i(tag = TAG) { "PLAYER PLAYING" }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.w(tag = TAG, throwable = e) { "CANNOT PLAY THE PLAYER TRY AGAIN" }
            }
        }
    }

    actual override suspend fun pause() {
        withContext(dispatchers.main) {
            _lock.tryWithLock(this) {
                try {
                    player.pause()
                    Logger.i(tag = TAG) { "PLAYER PAUSED" }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.w(tag = TAG, throwable = e) { "CANNOT PAUSE THE PLAYER TRY AGAIN" }
                }
            }
        }
    }

    actual override suspend fun stop() {
        withContext(dispatchers.main) {
            _lock.tryWithLock(this) {
                try {
                    player.pause()
                    val zeroTime = cValue<CMTime> {
                        value = kCMTimeZero.value
                        timescale = kCMTimeZero.timescale
                        flags = kCMTimeZero.flags
                        epoch = kCMTimeZero.epoch
                    }
                    player.seekToTime(zeroTime)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.w(tag = TAG, throwable = e) { "CANNOT STOP THE PLAYER TRY AGAIN" }
                }
            }
        }
    }

    actual override suspend fun seekBy(delta: Duration, rewind: Boolean) = withContext(dispatchers.main) {
        val currentItem = player.currentItem ?: run {
            Logger.w(tag = TAG) { "Cannot seek: currentItem is null" }
            return@withContext
        }

        val currentTimeSeconds = CMTimeGetSeconds(player.currentTime())
        val totalDurationSeconds = CMTimeGetSeconds(currentItem.duration)

        val deltaSeconds = delta.inWholeMilliseconds / 1000.0
        val offset = if (rewind) -deltaSeconds else deltaSeconds
        val targetTimeSeconds = if (totalDurationSeconds.isFinite() && totalDurationSeconds > 0) {
            (currentTimeSeconds + offset).coerceIn(0.0, totalDurationSeconds)
        } else (currentTimeSeconds + offset).coerceAtLeast(0.0)

        val targetCMTime = CMTimeMakeWithSeconds(targetTimeSeconds, preferredTimescale = 1000)
        player.seekToTime(targetCMTime)
        Logger.d(tag = TAG) { "SEEK-ED BY ${if (rewind) "-" else "+"}$delta. NEW TIME: $targetTimeSeconds" }
    }

    actual override suspend fun onMuteDevice() = withContext(dispatchers.main) {
        val isMuted = player.isMuted()
        Logger.d(tag = TAG) { "PLAYER MUTED STATE :$isMuted" }
        player.setMuted(!isMuted)
        Logger.d(tag = TAG) { "PLAYER MUTED STATE UPDATED :${player.isMuted()}" }
    }

    actual override suspend fun setPlayBackSpeed(playBackSpeed: PlayerPlayBackSpeed) = withContext(dispatchers.main) {
        Logger.d(tag = TAG) { "PLAYER RATE  :${player.defaultRate()}" }
        player.setDefaultRate(playBackSpeed.speed)
        if (player.rate != 0.0f) {
            player.setRate(playBackSpeed.speed)
        }
        Logger.d(tag = TAG) { "PLAYER RATE UPDATED :${player.defaultRate()}" }
    }

    actual override suspend fun setPlayLooping(loop: Boolean) = withContext(dispatchers.main) {
        val item = _playItem ?: run {
            Logger.w(tag = TAG) { "NO PLAYER ITEM IS SET TO WORK WITH" }
            return@withContext
        }
        if (loop && _looper == null) {
            _looper = AVPlayerLooper.playerLooperWithPlayer(player, templateItem = item)
            Logger.d(tag = TAG) { "LOOPING ENABLED" }
        } else {
            _looper?.disableLooping()
            _looper = null
            player.removeAllItems()
            player.insertItem(item, afterItem = null)
            Logger.d(tag = TAG) { "LOOPING DISABLED" }
        }
    }

    actual override suspend fun release() = withContext(dispatchers.main) {
        try {
            player.pause()
            _looper?.disableLooping()
            _looper = null
            player.removeAllItems()
            _playItem = null
            Logger.i(tag = TAG) { "PLAYER RELEASED AND CLEANED UP" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "ERROR RELEASING PLAYER" }
        }
    }

    private fun readMetadata(): PlayerPlayItemMetadata {
        var state: PlayerPlaybackState? = null
        if (player.currentItem == null || player.status == AVPlayerStatusFailed)
            state = PlayerPlaybackState.IDLE

        state = when (player.timeControlStatus) {
            AVPlayerTimeControlStatusPlaying -> PlayerPlaybackState.PLAYER_READY
            AVPlayerTimeControlStatusPaused -> PlayerPlaybackState.PLAYER_READY
            AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> PlayerPlaybackState.BUFFERING
            else -> state ?: PlayerPlaybackState.IDLE
        }

        val isPlaying = player.timeControlStatus == AVPlayerTimeControlStatusPlaying
        val speed = PlayerPlayBackSpeed.fromFloat(player.rate) ?: PlayerPlayBackSpeed.Normal
        val isMuted = player.isMuted()
        val isRepeating = _looper != null

        return PlayerPlayItemMetadata(
            playerState = state,
            playBackSpeed = speed,
            isRepeating = isRepeating,
            isMuted = isMuted,
            isPlaying = isPlaying,
        )
    }
}
