package com.sam.talkdraft.app.di

import com.sam.talkdraft.analytics.di.AnalyticsModule
import com.sam.talkdraft.auth.di.AuthModule
import com.sam.talkdraft.background_jobs.di.BackgroundJobsModule
import com.sam.talkdraft.common.di.CommonModule
import com.sam.talkdraft.connectivity.ConnectivityModule
import com.sam.talkdraft.crashlytics.di.CrashlyticsModule
import com.sam.talkdraft.database.di.DBModule
import com.sam.talkdraft.datastore.di.DataStoreModule
import com.sam.talkdraft.permissions.di.PermissionsModule
import com.sam.talkdraft.platform_capability.di.PlatformCapabilityModule
import com.sam.talkdraft.player.di.PlayerModule
import com.sam.talkdraft.recorder.di.RecorderModule
import com.sam.talkdraft.remote_config.di.RemoteConfigModule
import com.sam.talkdraft.supabase.di.SupabaseModule
import kotlin.native.HiddenFromObjC
import org.koin.core.annotation.Module

@Module(
    includes = [
        CommonModule::class,
        AuthModule::class,
        AnalyticsModule::class,
        RemoteConfigModule::class,
        CrashlyticsModule::class,
        SupabaseModule::class,
        DBModule::class,
        ConnectivityModule::class,
        PlatformCapabilityModule::class,
        DataStoreModule::class,
        RecorderModule::class,
        PlayerModule::class,
        BackgroundJobsModule::class,
        PermissionsModule::class,
    ],
)
@HiddenFromObjC
internal class CoreModule
