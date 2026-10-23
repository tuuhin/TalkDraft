package com.sam.talkdraft.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.designsystem.utils.SharedElementTransitionKeys
import com.sam.talkdraft.designsystem.utils.sharedBoundsWrapper
import com.sam.talkdraft.presentation.settings.events.SettingsScreenEvents

@Composable
internal fun SettingsScreen(
    onEvent: (SettingsScreenEvents) -> Unit,
    modifier: Modifier = Modifier,
) {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        modifier = Modifier.sharedBoundsWrapper(key = SharedElementTransitionKeys.HOME_NESTED_NAV_SCREEN_TITLE),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { scPadding ->
        Column(
            modifier = Modifier.padding(
                start = scPadding.calculateStartPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                end = scPadding.calculateEndPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                top = scPadding.calculateTopPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
                bottom = scPadding.calculateBottomPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
            ),
        ) {
            Button(onClick = { onEvent(SettingsScreenEvents.ShowToast) }) {
                Text("Hi")
            }
        }
    }
}
