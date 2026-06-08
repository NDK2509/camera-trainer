package com.example.cameratrainer.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cameratrainer.domain.model.CompositionRule
import com.example.cameratrainer.presentation.component.ScoringResultDialog
import com.example.cameratrainer.presentation.component.ViewfinderOverlay

/**
 * Main screen of the Camera Trainer.
 * Manages rendering the background image, zooming, and panning using gestures.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Minimal Slate dark background
            .onGloballyPositioned { layoutCoordinates ->
                // Measure actual screen canvas dimensions
                val size = layoutCoordinates.size
                viewModel.onEvent(MainUiEvent.OnCanvasSizeMeasured(Size(size.width.toFloat(), size.height.toFloat())))
            }
    ) {
        // 1. BACKGROUND PHOTO - Handles Pan/Zoom gestures via Modifier.graphicsLayer
        if (state.activePhoto != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        // Listen to Zoom & Pan gestures from pointerInput
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.onEvent(MainUiEvent.OnZoom(zoom))
                            viewModel.onEvent(MainUiEvent.OnPan(pan))
                        }
                    }
            ) {
                AsyncImage(
                    model = state.activePhoto!!.url,
                    contentDescription = "Real World Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = state.scale,
                            scaleY = state.scale,
                            translationX = state.offset.x,
                            translationY = state.offset.y
                        )
                )
            }
        }

        // 2. VIEWFINDER BOUNDARY BOX - Used to measure viewfinder sizing dynamically
        // Viewfinder has a fixed 4:3 aspect ratio placed at the center of the screen
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(240.dp)
                .align(Alignment.Center)
                .onGloballyPositioned { layoutCoordinates ->
                    // Measure viewfinder size in pixels to feed the coordinate translation algorithm
                    val size = layoutCoordinates.size
                    viewModel.onEvent(MainUiEvent.OnViewfinderSizeMeasured(Size(size.width.toFloat(), size.height.toFloat())))
                }
        )

        // 3. CANVAS OVERLAY - Draws dark bezel masking and guideline rules
        ViewfinderOverlay(
            viewfinderSize = state.viewfinderSize,
            showGrid = state.showGrid,
            gridRule = state.selectedGridRule,
            modifier = Modifier.fillMaxSize()
        )

        // 4. HEADER UI - Displays photography challenge instructions (Glassmorphism card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FRAMING MISSION",
                            color = Color(0xFFFFB74D), // Light orange accent
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Target: ${state.activePhoto?.metadata?.targetRule?.displayName ?: ""}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = state.activePhoto?.description ?: "Loading Photo...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "By: ${state.activePhoto?.author ?: "Unknown"}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // 5. FOOTER UI - Camera control dials
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Guideline rule selection tab bar
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionRule.values().forEach { rule ->
                    val isSelected = state.selectedGridRule == rule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { viewModel.onEvent(MainUiEvent.OnSelectGridRule(rule)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rule.displayName,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Shutter Button and utility toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle grid view
                IconButton(
                    onClick = { viewModel.onEvent(MainUiEvent.OnToggleGrid(!state.showGrid)) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle Gridlines",
                        tint = Color.White
                    )
                }

                // SHUTTER BUTTON (Capture)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable { viewModel.onEvent(MainUiEvent.OnCapturePressed) }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, CircleShape)
                    )
                }

                // Next challenge button
                IconButton(
                    onClick = { viewModel.onEvent(MainUiEvent.OnNextPhotoPressed) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Next Photo",
                        tint = Color.White
                    )
                }
            }
        }

        // 6. EVALUATION SCREEN - Grading photo in background (Evaluating Overlay)
        AnimatedVisibility(
            visible = state.isEvaluating,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI analyzing composition...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Running Scoring Engine Matrix...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 7. DIALOG DISPLAYING SCORE RESULTS
        if (state.scoreResult != null) {
            ScoringResultDialog(
                scoreResult = state.scoreResult!!,
                onDismiss = { viewModel.onEvent(MainUiEvent.OnDismissResultDialog) },
                onNextPhoto = { viewModel.onEvent(MainUiEvent.OnNextPhotoPressed) }
            )
        }
    }
}
