package com.example.cameratrainer.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.cameratrainer.domain.model.CompositionRule

/**
 * Custom Canvas to draw the Viewfinder, outer bezel mask,
 * professional camera L-shaped corner indicators,
 * and dashed composition gridlines.
 */
@Composable
fun ViewfinderOverlay(
    viewfinderSize: Size,
    showGrid: Boolean,
    gridRule: CompositionRule,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height
        
        val vfW = viewfinderSize.width
        val vfH = viewfinderSize.height
        
        // 1. Center the viewfinder on the canvas
        val left = (canvasW - vfW) / 2f
        val top = (canvasH - vfH) / 2f
        val right = left + vfW
        val bottom = top + vfH

        // 2. Draw outer dimmed mask using EvenOdd fillType to clear the middle
        val maskPath = Path().apply {
            addRect(Rect(0f, 0f, canvasW, canvasH))
            addRect(Rect(left, top, right, bottom))
            fillType = PathFillType.EvenOdd
        }
        drawPath(path = maskPath, color = Color.Black.copy(alpha = 0.7f))

        // 3. Draw a thin white border around the viewfinder frame
        drawRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(left, top),
            size = Size(vfW, vfH),
            style = Stroke(width = 1f)
        )

        // 4. Draw 4 thick corner indicators for a professional camera feel
        val cornerLength = 24f
        val cornerThickness = 4f
        val cornerColor = Color.White

        // Top-Left Corner
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth = cornerThickness)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth = cornerThickness)

        // Top-Right Corner
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth = cornerThickness)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth = cornerThickness)

        // Bottom-Left Corner
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth = cornerThickness)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth = cornerThickness)

        // Bottom-Right Corner
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth = cornerThickness)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth = cornerThickness)

        // 5. Draw dashed guidelines based on active composition rules
        if (showGrid) {
            val intervals = when (gridRule) {
                CompositionRule.RULE_OF_THIRDS -> listOf(1f / 3f, 2f / 3f)
                CompositionRule.GOLDEN_RATIO -> listOf(0.382f, 0.618f)
                CompositionRule.HORIZON -> listOf(1f / 3f, 2f / 3f) // Horizon uses horizontal 1/3 splits
                CompositionRule.SYMMETRY -> listOf(0.5f) // Symmetry splits down the middle
            }
            
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            val gridColor = Color.White.copy(alpha = 0.6f)
            val gridThickness = 2f

            // Draw horizontal lines
            for (ratio in intervals) {
                val y = top + (vfH * ratio)
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = gridThickness,
                    pathEffect = dashEffect
                )
            }

            // Draw vertical lines (Horizon only splits horizontally)
            if (gridRule != CompositionRule.HORIZON) {
                for (ratio in intervals) {
                    val x = left + (vfW * ratio)
                    drawLine(
                        color = gridColor,
                        start = Offset(x, top),
                        end = Offset(x, bottom),
                        strokeWidth = gridThickness,
                        pathEffect = dashEffect
                    )
                }
            }
        }
    }
}
