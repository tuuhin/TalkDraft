package com.sam.talkdraft.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface NavDestinations : NavKey {

    @Serializable
    data object OnBoardingScreen : NavDestinations

    @Serializable
    data object HomeScreen : NavDestinations

    @Serializable
    data class RecommendedDownloadModelScreen(val modelId: Uuid) : NavDestinations

    @Serializable
    data object CaptureFirstRecording : NavDestinations
}
