package com.example.cameratrainer.presentation.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.cameratrainer.domain.model.CompositionRule

/**
 * User interactions dispatched from the UI Screen to the ViewModel.
 */
sealed interface MainUiEvent {
    // Gesture interaction events
    data class OnPan(val dragAmount: Offset) : MainUiEvent
    data class OnZoom(val scaleFactor: Float) : MainUiEvent
    
    // Grid settings
    data class OnToggleGrid(val visible: Boolean) : MainUiEvent
    data class OnSelectGridRule(val rule: CompositionRule) : MainUiEvent
    
    // Layout dimension measurements
    data class OnCanvasSizeMeasured(val size: Size) : MainUiEvent
    data class OnViewfinderSizeMeasured(val size: Size) : MainUiEvent
    
    // Button click action events
    object OnCapturePressed : MainUiEvent
    object OnDismissResultDialog : MainUiEvent
    object OnNextPhotoPressed : MainUiEvent
}
