package com.sam.talkdraft.testing.di

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan(value = ["com.sam.talkdraft.testing"])
actual class TestPlatformModule {

    @Singleton
    fun provideTestContext(): Context {
        return InstrumentationRegistry.getInstrumentation().context
    }
}
