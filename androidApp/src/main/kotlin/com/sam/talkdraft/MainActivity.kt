package com.sam.talkdraft

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		setContent {
			Handler(Looper.getMainLooper()).postDelayed({
				throw RuntimeException("Test crash from Measure onboarding")
			}, 2000)
		}
	}
}
