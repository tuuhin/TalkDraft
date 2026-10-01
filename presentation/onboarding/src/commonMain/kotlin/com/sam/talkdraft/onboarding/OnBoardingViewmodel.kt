package com.sam.talkdraft.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.common.platform.IPlatformTargetProvider
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import com.sam.talkdraft.model_manager.domain.repository.IRecommendedModelProvider
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import com.sam.talkdraft.navigation.NavDestinations
import com.sam.talkdraft.navigation.navigator.AppNavigator
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import com.sam.talkdraft.onboarding.models.OnboardingEvents
import com.sam.talkdraft.onboarding.models.OnboardingScene
import com.sam.talkdraft.onboarding.models.OnboardingScreenState
import com.sam.talkdraft.permissions.IAppSettingsProvider
import com.sam.talkdraft.permissions.IPermissionsRequester
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val appPermissions: IPermissionsRequester,
    private val recommendationProvider: IRecommendedModelProvider,
    private val transcriptionModelRepo: ITranscriptionModelsRepo,
    private val analytics: IAnalyticsProvider,
    private val appTargetProvider: IPlatformTargetProvider,
    private val savedState: SavedStateHandle,
    private val navigator: AppNavigator,
) : ViewModel() {

    private val _recommendedModel = MutableStateFlow<TranscriptionModel?>(null)
    private val _captureIdeas = MutableStateFlow(CaptureIdeaOption.entries.toList())
    private val _permissionsState = MutableStateFlow<Map<Permissions, PermissionState>>(emptyMap())

    val initialScene: StateFlow<OnboardingScene>
        field = MutableStateFlow<OnboardingScene>(OnboardingScene.WELCOME_SCREEN)

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    val screenSate = combine(
        _recommendedModel,
        _captureIdeas,
        _permissionsState,
    ) { model, captureIdea, permissions ->
        OnboardingScreenState(
            recommended = model,
            capturedIdeas = captureIdea.toImmutableSet(),
            platform = appTargetProvider.target(),
            permissionsState = permissions.toImmutableMap(),
        )
    }.onStart {
        readAndSetInitialPagerPage()
        loadPermissionsState()
        readAndObserveRecommendedModel()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000),
        initialValue = OnboardingScreenState(platform = appTargetProvider.target()),
    )


    fun onEvent(event: OnboardingEvents) {
        when (event) {
            is OnboardingEvents.OnSkipOnboarding -> onSkipOnboarding(event.screen)
            is OnboardingEvents.SendAnalyticsEvent -> onIncomingSceneAnalyticsEvent(event.screen, event.extras)

            OnboardingEvents.OnOnboardingCompleted -> viewModelScope.launch {
                navigator.updateBackStack(listOf(NavDestinations.HomeScreen))
                analytics.track(AnalyticsEvent.OnboardingCompleted)
            }

            is OnboardingEvents.OnAddToCaptureItems -> updateIdeas(event.item)
            OnboardingEvents.RequestOpenAppSettings -> openAppSettings()
            OnboardingEvents.RequestPermissions -> requestPermissions()
            OnboardingEvents.OnNavigateToModelDownloader -> viewModelScope.launch {
                val model = _recommendedModel.value ?: return@launch
                navigator.navigateTo(NavDestinations.RecommendedDownloadModelScreen(model.id))
            }

            OnboardingEvents.OnNavigateToRecorder -> viewModelScope.launch {
                navigator.navigateTo(NavDestinations.CaptureFirstRecording)
            }
        }
    }

    private fun updateIdeas(idea: CaptureIdeaOption) {
        _captureIdeas.update { old ->
            if (idea in old) old.filter { it != idea }
            else old + idea
        }
    }

    private fun requestPermissions() = viewModelScope.launch {
        val result = appPermissions.requestPermissions(Permissions.entries)
        val currentState = _permissionsState.value.toMutableMap()

        result.forEach { (permission, isGranted) ->
            if (currentState.containsKey(permission))
                currentState[permission] = isGranted
        }
        _permissionsState.update { currentState }
    }

    private fun openAppSettings() = viewModelScope.launch {
        appSettings.openSettings()
    }

    private fun loadPermissionsState() = viewModelScope.launch {
        val permissions = buildMap {
            Permissions.entries.forEach {
                put(it, appPermissions.checkPermissionStatus(it))
            }
        }
        _permissionsState.update { permissions }
    }


    private fun readAndObserveRecommendedModel() = viewModelScope.launch {
        val model = recommendationProvider.recommendedModel(TranscriptionType.STREAMING).getOrNull()
            ?: return@launch

        transcriptionModelRepo.readModelAsFlow(model.id)
            .onEach { model -> _recommendedModel.update { model } }
            .catch { err -> Logger.w(tag = TAG, throwable = err) { "FAILED TO READ FLOW" } }
            .launchIn(this)
    }

    private fun readAndSetInitialPagerPage() {
        val pageIdx = savedState.get<Int>(PAGER_INITIAL_PAGE_IDX) ?: 0
        val page = OnboardingScene.entries.find { it.index == pageIdx }
            ?: OnboardingScene.WELCOME_SCREEN
        initialScene.update { page }
    }

    private fun onIncomingSceneAnalyticsEvent(scene: OnboardingScene, extras: Map<String, Any>) {
        savedState[PAGER_INITIAL_PAGE_IDX] = scene.index
        analytics.track(
            AnalyticsEvent.OnboardingScreen,
            buildMap {
                put("screen", scene.name)
                put("screen_index", scene.index)
                putAll(extras)
            },
        )
    }

    private fun onSkipOnboarding(screen: OnboardingScene) = viewModelScope.launch {
        analytics.track(
            AnalyticsEvent.OnboardingSkipped,
            buildMap {
                put("from_screen", screen.name)
                put("screen_index", screen.index)
            },
        )
        navigator.updateBackStack(listOf(NavDestinations.HomeScreen))
    }

    companion object {
        private const val TAG = "ONBOARDING_VIEWMODEL"
        private const val PAGER_INITIAL_PAGE_IDX = "PAGER_PAGE_INDEX"
    }
}
