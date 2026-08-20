package com.sam.talkdraft.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.talkdraft.auth.IUserAuthManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App(
	modifier: Modifier = Modifier,
	background: Color = MaterialTheme.colorScheme.background
) {

	val userManager = koinInject<IUserAuthManager>()
	val scope = rememberCoroutineScope()

	val userFlow by userManager.userFlow.collectAsStateWithLifecycle(null)

	Surface(
		modifier = modifier.fillMaxSize(),
		color = background
	) {
		Column(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text("CURRENT USER: $userFlow")

			Button(onClick = { scope.launch { userManager.signInWithOAuth() } }) {
				Text(text = "Click to Sign in ")
			}

			Button(onClick = { scope.launch { userManager.signOut() } }) {
				Text(text = "Click to sign out")
			}
		}
	}
}