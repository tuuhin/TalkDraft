package com.sam.talkdraft

import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.sam.talkdraft.app.App
import com.sam.talkdraft.feature_onboarding.IOnboardingPreferences
import com.sam.talkdraft.permissions.controller.IPermissionController
import com.sam.talkdraft.remote_config.IRemoteConfigProvider
import com.sam.talkdraft.utils.animateOnExit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val remoteConfig by inject<IRemoteConfigProvider>()
    private val onboardingProvider by inject<IOnboardingPreferences>()
    private val permissionController by inject<IPermissionController>()

    private var _isRemoteConfigLoaded by mutableStateOf(false)
    private var _isOnboardingValueChecked by mutableStateOf(false)
    private var _showOnboardingScreen by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {

        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // enable edge to edge
        enableEdgeToEdge()
        // on splash complete again enable edge to edge
        splash.animateOnExit(onAnimationEnd = { enableEdgeToEdge() })
        splash.setKeepOnScreenCondition { !_isRemoteConfigLoaded || !_isOnboardingValueChecked }

        // set activity transitions
        setTransitions()
        // bind permission controller
        permissionController.bindToActivity(this)

        // set fields
        lifecycleScope.launch { remoteConfig.loadFlags() }
            .invokeOnCompletion { _isRemoteConfigLoaded = true }

        lifecycleScope.launch { _showOnboardingScreen = onboardingProvider.showOnboarding.first() }
            .invokeOnCompletion { _isOnboardingValueChecked = true }

        setContent {
            App(showOnboarding = _showOnboardingScreen)
        }
    }

    @Suppress("DEPRECATION")
    private fun setTransitions() {
        // allow activity transitions
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        // set transitions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.activity_enter_transistion,
                R.anim.activity_exit_transition,
            )
        } else {
            overridePendingTransition(
                R.anim.activity_exit_transition,
                R.anim.activity_exit_transition,
            )
        }
    }
}
