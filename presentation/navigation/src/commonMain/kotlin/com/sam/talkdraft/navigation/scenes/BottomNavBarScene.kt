package com.sam.talkdraft.navigation.scenes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.sam.talkdraft.designsystem.utils.Dimensions
import com.sam.talkdraft.navigation.scenes.BottomSheetSceneStrategy.Companion.bottomSheet
import kotlinx.coroutines.launch

/** An [OverlayScene] that renders an [entry] within a [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val modalBottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    private val isSkipPartiallyExpanded: Boolean = false,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)
    private var sheetState: SheetState? = null

    override val content: @Composable (() -> Unit) = {

        val scope = rememberCoroutineScope()

        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = buildSet {
                addAll(listOf(SheetValue.Hidden, SheetValue.Expanded))
                if (!isSkipPartiallyExpanded) add(SheetValue.PartiallyExpanded)
            },
        )
        this@BottomSheetScene.sheetState = sheetState

        val scaleAnimation by animateFloatAsState(
            targetValue = when (sheetState.targetValue) {
                SheetValue.Hidden -> 0.4f
                else -> 0f
            },
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "sheetScale",
        )

        LaunchedEffect(Unit) {
            if (isSkipPartiallyExpanded) sheetState.show()
            else sheetState.partialExpand()
        }

        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { onBack() }
            },
            sheetState = sheetState,
            properties = modalBottomSheetProperties,
            modifier = Modifier.graphicsLayer {
                scaleX = 1 - scaleAnimation
                scaleY = 1f - scaleAnimation * 0.15f
                transformOrigin = TransformOrigin(.5f, 1f)
            },
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = Dimensions.MODAL_BOTTOM_SHEET_CONTENT_PADDING)
                    .padding(bottom = Dimensions.SCAFFOLD_VERTICAL_PADDING),
                contentAlignment = Alignment.Center,
            ) {
                entry.Content()
            }
        }
    }

    override suspend fun onRemove() {
        sheetState?.hide()
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    @Suppress("UNCHECKED_CAST")
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val sheetProperties = lastEntry?.metadata?.get(SheetKey)
        val isSkipPartiallyExpanded = lastEntry?.metadata?.get(SheetIsPartiallyExpanded) ?: false

        if (sheetProperties != null) {
            val scene = BottomSheetScene(
                key = lastEntry.contentKey as T,
                previousEntries = entries.dropLast(1),
                overlaidEntries = entries.dropLast(1),
                entry = lastEntry,
                isSkipPartiallyExpanded = isSkipPartiallyExpanded,
                modalBottomSheetProperties = sheetProperties,
                onBack = onBack,
            )
            return scene
        }
        return null
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [ModalBottomSheet].
         *
         * @param properties properties that should be passed to the containing
         * [ModalBottomSheet].
         */
        @OptIn(ExperimentalMaterial3Api::class)
        fun bottomSheet(
            isSkipPartiallyExpanded: Boolean = false,
            properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
        ): Map<String, Any> = metadata {
            put(SheetKey, properties)
            put(SheetIsPartiallyExpanded, isSkipPartiallyExpanded)
        }

        data object SheetKey : NavMetadataKey<ModalBottomSheetProperties>
        data object SheetIsPartiallyExpanded : NavMetadataKey<Boolean>
    }
}
