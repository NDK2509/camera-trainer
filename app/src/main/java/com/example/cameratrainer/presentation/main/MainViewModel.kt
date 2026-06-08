package com.example.cameratrainer.presentation.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameratrainer.domain.model.CompositionRule
import com.example.cameratrainer.domain.model.CropRect
import com.example.cameratrainer.domain.model.Photo
import com.example.cameratrainer.domain.model.PhotoMetadata
import com.example.cameratrainer.domain.model.PointOfInterest
import com.example.cameratrainer.domain.model.ScoreResult
import com.example.cameratrainer.domain.usecase.EvaluateCompositionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the logic of the camera composition screen.
 * Handles coordinate translation from screen space to normalized photo space.
 */
class MainViewModel(
    private val evaluateCompositionUseCase: EvaluateCompositionUseCase = EvaluateCompositionUseCase()
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    // Mock photo list for composition training challenges
    private val photoRepositoryList = listOf(
        Photo(
            id = "1",
            url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80",
            author = "Sean Oulashin",
            description = "Stunning red sunrise sky over a wild sandy beach.",
            metadata = PhotoMetadata(
                width = 1600,
                height = 1000,
                targetRule = CompositionRule.HORIZON,
                pointsOfInterest = listOf(
                    PointOfInterest(label = "Sunrise", x = 0.67f, y = 0.35f, weight = 1.0f),
                    PointOfInterest(label = "Sea Horizon", x = 0.50f, y = 0.66f, weight = 0.8f)
                )
            )
        ),
        Photo(
            id = "2",
            url = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1000&q=80",
            author = "Christopher Campbell",
            description = "Detailed portrait close-up showcasing a girl with a captivating gaze.",
            metadata = PhotoMetadata(
                width = 1000,
                height = 1250,
                targetRule = CompositionRule.SYMMETRY,
                pointsOfInterest = listOf(
                    PointOfInterest(label = "Subject Face", x = 0.50f, y = 0.45f, weight = 1.0f),
                    PointOfInterest(label = "Eyes", x = 0.50f, y = 0.40f, weight = 0.6f)
                )
            )
        ),
        Photo(
            id = "3",
            url = "https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?auto=format&fit=crop&w=1600&q=80",
            author = "Quino Al",
            description = "A solitary lighthouse standing strong on a rocky shore with waves crashing.",
            metadata = PhotoMetadata(
                width = 1600,
                height = 1066,
                targetRule = CompositionRule.RULE_OF_THIRDS,
                pointsOfInterest = listOf(
                    PointOfInterest(label = "Lighthouse Top", x = 0.33f, y = 0.45f, weight = 1.0f),
                    PointOfInterest(label = "Rocky Reef", x = 0.33f, y = 0.75f, weight = 0.5f)
                )
            )
        )
    )
    
    private var photoIndex = 0

    init {
        loadPhoto()
    }

    private fun loadPhoto() {
        _state.update { 
            it.copy(
                isLoading = false,
                activePhoto = photoRepositoryList[photoIndex],
                scale = 1.0f,
                offset = Offset.Zero,
                scoreResult = null
            )
        }
    }

    /**
     * Dispatches user interactions received from the View.
     */
    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.OnPan -> handlePan(event.dragAmount)
            is MainUiEvent.OnZoom -> handleZoom(event.scaleFactor)
            is MainUiEvent.OnToggleGrid -> {
                _state.update { it.copy(showGrid = event.visible) }
            }
            is MainUiEvent.OnSelectGridRule -> {
                _state.update { it.copy(selectedGridRule = event.rule) }
            }
            is MainUiEvent.OnCanvasSizeMeasured -> {
                _state.update { it.copy(canvasSize = event.size) }
            }
            is MainUiEvent.OnViewfinderSizeMeasured -> {
                _state.update { it.copy(viewfinderSize = event.size) }
            }
            is MainUiEvent.OnCapturePressed -> evaluateComposition()
            is MainUiEvent.OnDismissResultDialog -> {
                _state.update { it.copy(scoreResult = null) }
            }
            is MainUiEvent.OnNextPhotoPressed -> {
                photoIndex = (photoIndex + 1) % photoRepositoryList.size
                loadPhoto()
            }
        }
    }

    /**
     * Handles pan gesture. Restricts translation boundaries to keep the photo in sight.
     */
    private fun handlePan(dragAmount: Offset) {
        val currentScale = _state.value.scale
        val currentOffset = _state.value.offset
        
        // Only allow pan when zoomed in (Scale > 1.0). Limits offsets based on scaling.
        val maxOffset = 500f * currentScale
        val newX = (currentOffset.x + dragAmount.x).coerceIn(-maxOffset, maxOffset)
        val newY = (currentOffset.y + dragAmount.y).coerceIn(-maxOffset, maxOffset)
        
        _state.update { it.copy(offset = Offset(newX, newY)) }
    }

    /**
     * Handles pinch-to-zoom. Limits zoom from 1.0x to 4.0x.
     */
    private fun handleZoom(scaleFactor: Float) {
        val newScale = (_state.value.scale * scaleFactor).coerceIn(1.0f, 4.0f)
        _state.update { 
            it.copy(
                scale = newScale,
                // Adjust translation to keep scale anchor point smooth
                offset = it.offset * (newScale / it.scale)
            ) 
        }
    }

    /**
     * Core mapping math: Calculates the CropRect and runs grading.
     */
    private fun evaluateComposition() {
        val stateVal = _state.value
        val photo = stateVal.activePhoto ?: return
        val canvasSize = stateVal.canvasSize
        val viewfinderSize = stateVal.viewfinderSize

        if (canvasSize.width == 0f || canvasSize.height == 0f || 
            viewfinderSize.width == 0f || viewfinderSize.height == 0f) {
            return
        }

        _state.update { it.copy(isEvaluating = true) }

        viewModelScope.launch {
            // Simulate AI engine score evaluation delay of 800ms
            delay(800)

            // 1. Compute fitted size of original photo inside screen canvas boundaries (Fit mode)
            val imgW = photo.metadata.width.toFloat()
            val imgH = photo.metadata.height.toFloat()
            val imgRatio = imgW / imgH
            val canvasRatio = canvasSize.width / canvasSize.height

            val baseW: Float
            val baseH: Float
            if (imgRatio > canvasRatio) {
                baseW = canvasSize.width
                baseH = canvasSize.width / imgRatio
            } else {
                baseH = canvasSize.height
                baseW = canvasSize.height * imgRatio
            }

            // 2. Compute screen dimensions of photo after scale updates
            val screenW = baseW * stateVal.scale
            val screenH = baseH * stateVal.scale

            // 3. Screen coordinates of photo's Top-Left corner
            val imgLeft = (canvasSize.width / 2f) - (screenW / 2f) + stateVal.offset.x
            val imgTop = (canvasSize.height / 2f) - (screenH / 2f) + stateVal.offset.y

            // 4. Center-aligned viewfinder coordinates on screen
            val vfLeft = (canvasSize.width / 2f) - (viewfinderSize.width / 2f)
            val vfTop = (canvasSize.height / 2f) - (viewfinderSize.height / 2f)
            val vfRight = vfLeft + viewfinderSize.width
            val vfBottom = vfTop + viewfinderSize.height

            // 5. Project screen viewfinder bounds back to photo's normalized coordinates [0.0, 1.0]
            val normLeft = ((vfLeft - imgLeft) / screenW).coerceIn(0f, 1f)
            val normTop = ((vfTop - imgTop) / screenH).coerceIn(0f, 1f)
            val normRight = ((vfRight - imgLeft) / screenW).coerceIn(0f, 1f)
            val normBottom = ((vfBottom - imgTop) / screenH).coerceIn(0f, 1f)

            // 6. Check validation overlap bounds
            if (normLeft >= normRight || normTop >= normBottom) {
                // Viewfinder is completely off-image boundaries
                _state.update {
                    it.copy(
                        isEvaluating = false,
                        scoreResult = ScoreResult(
                            score = 0,
                            feedback = "Out of Bounds!",
                            detailedFeedback = listOf("Your viewfinder is completely off the photo boundaries. Drag the photo back to center.")
                        )
                    )
                }
                return@launch
            }

            val cropRect = CropRect(
                left = normLeft,
                top = normTop,
                right = normRight,
                bottom = normBottom
            )

            // 7. Delegate grading to use case
            val result = evaluateCompositionUseCase(cropRect, photo.metadata)

            _state.update {
                it.copy(
                    isEvaluating = false,
                    scoreResult = result
                )
            }
        }
    }
}
