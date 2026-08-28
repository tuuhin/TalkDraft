package com.sam.talkdraft.recorder.di

import com.sam.talkdraft.common.di.CommonModule
import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import com.sam.talkdraft.recorder.utils.FakeRecordPermissionChecker
import com.sam.talkdraft.testing.di.TestPlatformModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module(
    includes = [
        CommonModule::class,
        TestPlatformModule::class,
        RecorderModule::class,
    ],
)
@ComponentScan("com.sam.talkdraft.recorder")
internal class TestRecorderModule {

    @Singleton
    fun testRecordPermissionChecker(): IRecordPermissionChecker = FakeRecordPermissionChecker()
}

