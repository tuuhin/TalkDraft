package com.sam.talkdraft.app.di

import com.sam.talkdraft.model_manager.di.ModelManagerModule
import org.koin.core.annotation.Module

@Module(includes = [ModelManagerModule::class])
internal class FeatureModule
