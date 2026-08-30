package com.sam.talkdraft.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.common.platform.IPlatformTargetProvider
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.IRecommendedModelProvider
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import com.sam.talkdraft.onboarding.models.OnboardingEvents
import com.sam.talkdraft.onboarding.models.OnboardingScreenState
import com.sam.talkdraft.onboarding.util.IAppSettingsProvider
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel


@KoinViewModel
internal class OnBoardingViewmodel(
    private val appSettings: IAppSettingsProvider,
    private val recommendationProvider: IRecommendedModelProvider,
    private val transcriptionModelRepo: ITranscriptionModelsRepo,
    private val analytics: IAnalyticsProvider,
    private val appTargetProvider: IPlatformTargetProvider,
) : ViewModel() {

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    private val _recommendedModel = MutableStateFlow<TranscriptionModel?>(null)
    private val _captureIdeas = MutableStateFlow(listOf(CaptureIdeaOption.BRAIN_STORMING))

    val screenSate = combine(
        _recommendedModel,
        _captureIdeas,
    ) { model, captureIdea ->
        OnboardingScreenState(
            recommended = model,
            capturedIdeas = captureIdea.toImmutableSet(),
            platform = appTargetProvider.target(),
        )
    }.onStart {
        readAndObserveRecommendedModel()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000),
        initialValue = OnboardingScreenState(platform = appTargetProvider.target()),
    )


    fun onEvent(event: OnboardingEvents) {
        when (event) {
            is OnboardingEvents.OnSkipOnboarding -> analytics.track(
                AnalyticsEvent.OnboardingSkipped,
                buildMap {
                    put("from_screen", event.screen.name)
                    put("screen_index", event.screen.index)
                },
            )

            is OnboardingEvents.SendAnalyticsEvent -> analytics.track(
                AnalyticsEvent.OnboardingScreen,
                buildMap {
                    put("screen", event.screen.name)
                    put("screen_index", event.screen.index)
                    putAll(event.extras)
                },
            )

            OnboardingEvents.OnOnboardingCompleted ->
                analytics.track(AnalyticsEvent.OnboardingCompleted)

            is OnboardingEvents.OnAddToCaptureItems -> {
                _captureIdeas.update { old ->
                    if (event.item in old) old.filter { it != event.item }
                    else old + event.item
                }
            }

            OnboardingEvents.RequestOpenAppSettings -> openAppSettings()
        }
    }

    private fun openAppSettings() = viewModelScope.launch {
        appSettings.openSettings()
    }


    private fun readAndObserveRecommendedModel() = viewModelScope.launch {
        val model = recommendationProvider.recommendedModel().getOrNull()
            ?: return@launch

        transcriptionModelRepo.readModelAsFlow(model.id)
            .onEach { model -> _recommendedModel.update { model } }
            .catch { err -> Logger.w(tag = TAG, throwable = err) { "FAILED TO READ FLOW" } }
            .launchIn(this)
    }

    companion object {
        private const val TAG = "ONBOARDING_VIEWMODEL"
    }
}
