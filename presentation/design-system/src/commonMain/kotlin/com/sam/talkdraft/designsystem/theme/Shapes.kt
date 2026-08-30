package com.sam.talkdraft.designsystem.theme

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class MorphOverlayClip(
    val morph: Morph,
    val progress: () -> Float,
) : SharedTransitionScope.OverlayClip {

    private val matrix = Matrix()
    private val composePath = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path {
        matrix.reset()
        composePath.reset()

        val max = maxOf(bounds.width, bounds.height)
        morph.toPath(progress(), path = composePath)

        matrix.scale(max, max)
        composePath.transform(matrix)

        val translationOffset = bounds.center + Offset(-max * 0.5f, -max * 0.5f)
        composePath.translate(translationOffset)
        return composePath
    }

}


class MorphShape(
    private val morph: Morph,
    private val progress: () -> Float,
) : Shape {
    private val composePath = Path()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        // Clear previous calculations
        composePath.reset()
        // Convert the AndroidX Graphics Path to a Compose Path
        val path = morph.toPath(progress())

        // Scale and transform the path to match the card's visual bounds
        composePath.addPath(path)
        val matrix = androidx.compose.ui.graphics.Matrix().apply {
            scale(size.width, size.height)
        }
        composePath.transform(matrix)

        return Outline.Generic(composePath)
    }
}

