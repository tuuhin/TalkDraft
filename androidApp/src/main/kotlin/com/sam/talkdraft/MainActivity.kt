package com.sam.talkdraft

import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sam.talkdraft.app.App
import com.sam.talkdraft.utils.animateOnExit

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // enable edge to edge
        enableEdgeToEdge()
        // on splash complete again enable edge to edge
        splash.animateOnExit(onAnimationEnd = { enableEdgeToEdge() })
        // set activity transitions
        setTransitions()

        setContent {
            App()
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
