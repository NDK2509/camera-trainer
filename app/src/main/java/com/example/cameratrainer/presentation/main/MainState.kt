package com.example.cameratrainer.presentation.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.cameratrainer.domain.model.CompositionRule
import com.example.cameratrainer.domain.model.Photo
import com.example.cameratrainer.domain.model.ScoreResult

/**
 * Class representing the UI state of the Camera Trainer screen.
 * Contains variables for Gestures (Scale, Offset) and guideline configs.
 */
data class MainState(
    val isLoading: Boolean = true,
    val activePhoto: Photo? = null,
    
    // Gesture state of the background photo
    val scale: Float = 1.0f,
    val offset: Offset = Offset.Zero,
    
    // Composition guideline configurations
    val showGrid: Boolean = true,
    val selectedGridRule: CompositionRule = CompositionRule.RULE_OF_THIRDS,
    
    // Measured dimensions of the canvas and viewfinder (measured dynamically at runtime)
    val canvasSize: Size = Size.Zero,
    val viewfinderSize: Size = Size.Zero,
    val viewfinderScale: Float = 0.8f, // Viewfinder scale multiplier (0.4f to 1.0f)
    
    // Settings state
    val isSettingsOpen: Boolean = false,
    val apiKey: String = "",
    
    // Scoring evaluation state
    val isEvaluating: Boolean = false,
    val scoreResult: ScoreResult? = null
)
