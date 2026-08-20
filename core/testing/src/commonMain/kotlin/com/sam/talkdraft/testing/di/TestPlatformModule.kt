package com.sam.talkdraft.testing.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.sam.talkdraft.testing")
expect class TestPlatformModule

