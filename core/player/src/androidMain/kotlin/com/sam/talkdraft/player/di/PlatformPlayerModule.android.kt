package com.sam.talkdraft.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@OptIn(UnstableApi::class)
internal actual class PlatformPlayerModule {

    @Singleton
    fun providesPlayerAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
            .build()
    }

    @Singleton
    fun providesMediaExtractorFactory(context: Context): MediaSource.Factory {

        val extractor = DefaultExtractorsFactory().apply {
            //set extractor flags later if there is some problem
            setAmrExtractorFlags(AmrExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
            setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
        }

        return DefaultMediaSourceFactory(context, extractor)
    }


    @Singleton
    fun providesExoPlayer(
        context: Context,
        mediaSource: MediaSource.Factory,
        attributes: AudioAttributes,
    ): Player {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSource)
            .setAudioAttributes(attributes, true)
            .setTrackSelector(DefaultTrackSelector(context))
            .setName("Common_Audio_Player")
            .build()
    }
}
