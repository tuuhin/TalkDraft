package com.sam.talkdraft.onboarding

import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.common.model.PlatformTarget
import com.sam.talkdraft.designsystem.annotations.PreviewAppTheme
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState
import com.sam.talkdraft.onboarding.composables.OnBoardingScreenTopBar
import com.sam.talkdraft.onboarding.composables.OnBoardingScreens
import com.sam.talkdraft.onboarding.composables.OnboardingIndicator
import com.sam.talkdraft.onboarding.composables.ProcessingMarkers
import com.sam.talkdraft.onboarding.composables.WaveFormDraw
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import com.sam.talkdraft.onboarding.models.OnboardingEvents
import com.sam.talkdraft.onboarding.models.OnboardingScene
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@Composable
internal fun OnBoardingScreen(
    onEvent: (OnboardingEvents) -> Unit,
    modifier: Modifier = Modifier,
    platform: PlatformTarget = PlatformTarget.UNKNOWN,
    capturedIdeas: ImmutableSet<CaptureIdeaOption> = persistentSetOf<CaptureIdeaOption>(),
) {

    val snackBarHostState = LocalSnackBarState.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val pager = rememberPagerState { OnboardingScene.entries.size }
    val scope = rememberCoroutineScope()

    val canShowPrev by remember(pager) {
        derivedStateOf { pager.currentPage != 0 }
    }

    val currentScene by remember(pager.currentPage) {
        derivedStateOf {
            OnboardingScene.entries.find { it.index == pager.currentPage }
                ?: OnboardingScene.WELCOME_SCREEN
        }
    }

    Scaffold(
        topBar = {
            OnBoardingScreenTopBar(
                onSkipFullTour = { onEvent(OnboardingEvents.OnSkipOnboarding) },
                onPreviousScreen = {
                    scope.launch {
                        val current = pager.currentPage
                        if (current > 0) pager.animateScrollToPage(current - 1)
                    }
                },
                showPrevious = canShowPrev,
                scrollBehaviour = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
                    bottom = padding.calculateBottomPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // indicator
            OnboardingIndicator(
                pageCount = pager.pageCount,
                currentPage = pager.currentPage,
                modifier = Modifier.height(12.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(.8f),
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.BottomCenter,
            ) {
                WaveFormDraw(
                    screen = currentScene,
                    modifier = Modifier.fillMaxSize(),
                )
                ProcessingMarkers(
                    show = currentScene == OnboardingScene.LOCAL_FIRST_AND_PRIVACY,
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                )
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = true,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pager,
                        snapPositionalThreshold = .2f,
                        snapAnimationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                    ),
                ) { idx ->
                    OnBoardingScreens(
                        page = idx,
                        capturedIdeas = capturedIdeas,
                        onUpdateCaptureIdea = { onEvent(OnboardingEvents.OnAddToCaptureItems(it)) },
                        onOpenAppSettings = { onEvent(OnboardingEvents.RequestOpenAppSettings) },
                        platform = platform,
                        onAction = {
                            scope.launch {
                                val current = pager.currentPage
                                val max = pager.pageCount
                                if (current < max) {
                                    val animtion = tween<Float>(easing = EaseInBack)
                                    pager.animateScrollToPage(current + 1, animationSpec = animtion)
                                }
                            }
                        },
                        contentPadding = PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                            end = padding.calculateEndPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}


@Preview
@Composable
@PreviewWrapper(PreviewAppTheme::class)
private fun OnBoardingScreenPreview() {
    OnBoardingScreen(onEvent = {})
}
