package com.sam.talkdraft.onboarding

import androidx.compose.animation.core.EaseIn
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
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState
import com.sam.talkdraft.onboarding.composables.LocalAndCloudAIMarker
import com.sam.talkdraft.onboarding.composables.OnBoardingScenes
import com.sam.talkdraft.onboarding.composables.OnBoardingScreenTopBar
import com.sam.talkdraft.onboarding.composables.OnboardingIndicator
import com.sam.talkdraft.onboarding.composables.WaveFormDraw
import com.sam.talkdraft.onboarding.models.OnboardingEvents
import com.sam.talkdraft.onboarding.models.OnboardingScene
import com.sam.talkdraft.onboarding.models.OnboardingScreenState
import kotlinx.coroutines.launch

@Composable
internal fun OnBoardingScreen(
    state: OnboardingScreenState,
    onEvent: (OnboardingEvents) -> Unit,
    onNavigateToModelDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val snackBarHostState = LocalSnackBarState.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val pager = rememberPagerState { OnboardingScene.entries.size }
    val scope = rememberCoroutineScope()

    val isFirstPage by remember(pager) {
        derivedStateOf { pager.currentPage != 0 }
    }

    val isLastPage by remember(pager) {
        derivedStateOf { pager.currentPage + 1 != pager.pageCount }
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
                onSkipFullTour = { onEvent(OnboardingEvents.OnSkipOnboarding(currentScene)) },
                onPreviousScreen = {
                    scope.launch {
                        val current = pager.currentPage
                        if (current > 0) pager.animateScrollToPage(current - 1)
                    }
                },
                showPrevious = isFirstPage,
                showSkipTourButton = isLastPage,
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
                onChangePage = { page ->
                    val animation = tween<Float>(durationMillis = 120, easing = EaseIn)
                    scope.launch {
                        pager.animateScrollToPage(page, animationSpec = animation)
                    }
                },
                tapToChangePageEnable = true,
                modifier = Modifier.height(12.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(.6f),
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.BottomCenter,
            ) {
                WaveFormDraw(
                    screen = currentScene,
                    modifier = Modifier.fillMaxSize(),
                )
                LocalAndCloudAIMarker(
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
                    modifier = Modifier.fillMaxSize(),
                ) { idx ->
                    OnBoardingScenes(
                        page = idx,
                        capturedIdeas = state.capturedIdeas,
                        recommendModel = state.recommended,
                        platform = state.platform,
                        onNavigateToModelDownload = onNavigateToModelDownload,
                        onUpdateCaptureIdea = { onEvent(OnboardingEvents.OnAddToCaptureItems(it)) },
                        onOpenAppSettings = { onEvent(OnboardingEvents.RequestOpenAppSettings) },
                        onAction = {
                            val current = pager.currentPage
                            val max = pager.pageCount
                            val animation = tween<Float>(easing = EaseInBack)
                            scope.launch {
                                if (current < max) {
                                    pager.animateScrollToPage(current + 1, animationSpec = animation)
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
