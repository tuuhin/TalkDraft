package com.sam.talkdraft.testing.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan(value = ["com.sam.talkdraft.testing"])
actual class TestPlatformModule
