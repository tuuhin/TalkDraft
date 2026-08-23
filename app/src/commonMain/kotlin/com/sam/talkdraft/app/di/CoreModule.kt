package com.sam.talkdraft.app.di

import com.sam.talkdraft.analytics.di.AnalyticsModule
import com.sam.talkdraft.auth.di.AuthModule
import com.sam.talkdraft.common.di.CommonModule
import com.sam.talkdraft.connectivity.ConnectivityModule
import com.sam.talkdraft.crashlytics.di.CrashlyticsModule
import com.sam.talkdraft.database.di.DBModule
import com.sam.talkdraft.platform_capability.di.PlatformCapabilityModule
import com.sam.talkdraft.supabase.di.SupabaseModule
import kotlin.native.HiddenFromObjC
import org.koin.core.annotation.Module

@Module(
    includes = [
        CommonModule::class,
        AuthModule::class,
        AnalyticsModule::class,
        CrashlyticsModule::class,
        SupabaseModule::class,
        DBModule::class,
        ConnectivityModule::class,
        PlatformCapabilityModule::class,
    ],
)
@HiddenFromObjC
internal class CoreModule
