package com.example.cameratrainer.presentation.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameratrainer.R
import com.example.cameratrainer.domain.model.CompositionRule
import com.example.cameratrainer.domain.model.CropRect
import com.example.cameratrainer.domain.model.Photo
import com.example.cameratrainer.domain.model.PhotoMetadata
import com.example.cameratrainer.domain.model.PointOfInterest
import com.example.cameratrainer.domain.model.ScoreResult
import com.example.cameratrainer.domain.usecase.EvaluateCompositionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainViewModel(
    private val evaluateCompositionUseCase: EvaluateCompositionUseCase = EvaluateCompositionUseCase()
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val photoRepositoryList = listOf(
        Photo(
            id = "1",
            url = R.drawable.banner_2,
            author = "Local Asset",
            description = "Practice framing and scaling with this local banner photo.",
            metadata = PhotoMetadata(
                width = 500,
                height = 500,
                targetRule = CompositionRule.RULE_OF_THIRDS,
                pointsOfInterest = listOf(
                    PointOfInterest(label = "Highlight Element", x = 0.33f, y = 0.33f, weight = 1.0f),
                    PointOfInterest(label = "Symmetric Line", x = 0.5f, y = 0.5f, weight = 0.5f)
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
            is MainUiEvent.OnViewfinderScaleChanged -> {
                _state.update { it.copy(viewfinderScale = event.scale.coerceIn(0.4f, 1.0f)) }
            }
            is MainUiEvent.OnLoadSettings -> {
                val prefs = event.context.getSharedPreferences("camera_trainer_prefs", android.content.Context.MODE_PRIVATE)
                val savedKey = prefs.getString("gemini_api_key", "") ?: ""
                _state.update { it.copy(apiKey = savedKey) }
            }
            is MainUiEvent.OnOpenSettings -> {
                _state.update { it.copy(isSettingsOpen = true) }
            }
            is MainUiEvent.OnCloseSettings -> {
                _state.update { it.copy(isSettingsOpen = false) }
            }
            is MainUiEvent.OnSaveApiKey -> {
                val prefs = event.context.getSharedPreferences("camera_trainer_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("gemini_api_key", event.apiKey).apply()
                _state.update { it.copy(apiKey = event.apiKey, isSettingsOpen = false) }
            }
            is MainUiEvent.OnCapturePressed -> evaluateComposition(event.context)
            is MainUiEvent.OnDismissResultDialog -> {
                _state.update { it.copy(scoreResult = null) }
            }
            is MainUiEvent.OnNextPhotoPressed -> {
                photoIndex = (photoIndex + 1) % photoRepositoryList.size
                loadPhoto()
            }
        }
    }

    private fun handlePan(dragAmount: Offset) {
        val currentScale = _state.value.scale
        val currentOffset = _state.value.offset
        val maxOffset = 500f * currentScale
        val newX = (currentOffset.x + dragAmount.x).coerceIn(-maxOffset, maxOffset)
        val newY = (currentOffset.y + dragAmount.y).coerceIn(-maxOffset, maxOffset)
        _state.update { it.copy(offset = Offset(newX, newY)) }
    }

    private fun handleZoom(scaleFactor: Float) {
        val newScale = (_state.value.scale * scaleFactor).coerceIn(1.0f, 4.0f)
        _state.update { 
            it.copy(
                scale = newScale,
                offset = it.offset * (newScale / it.scale)
            ) 
        }
    }

    private fun evaluateComposition(context: android.content.Context) {
        val stateVal = _state.value
        val photo = stateVal.activePhoto ?: return
        val canvasSize = stateVal.canvasSize
        val viewfinderSize = stateVal.viewfinderSize

        if (canvasSize.width == 0f || canvasSize.height == 0f || 
            viewfinderSize.width == 0f || viewfinderSize.height == 0f) {
            return
        }

        _state.update { it.copy(isEvaluating = true) }

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Compute fitted size of original photo inside screen canvas boundaries (ContentScale.Crop mode)
            val imgW = photo.metadata.width.toFloat()
            val imgH = photo.metadata.height.toFloat()
            val imgRatio = imgW / imgH
            val canvasRatio = canvasSize.width / canvasSize.height

            val baseW: Float
            val baseH: Float
            if (imgRatio > canvasRatio) {
                // Fitted height equals canvas height, width overflows (crop)
                baseH = canvasSize.height
                baseW = canvasSize.height * imgRatio
            } else {
                // Fitted width equals canvas width, height overflows (crop)
                baseW = canvasSize.width
                baseH = canvasSize.width / imgRatio
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
            val mathResult = evaluateCompositionUseCase(cropRect, photo.metadata)

            // 8. Gemini API Call (if API Key is configured)
            val aiCritiqueText = if (stateVal.apiKey.isNotEmpty()) {
                try {
                    // Load active photo as Bitmap using Coil
                    val loader = coil.ImageLoader(context)
                    val request = coil.request.ImageRequest.Builder(context)
                        .data(photo.url)
                        .allowHardware(false)
                        .build()
                    val drawable = (loader.execute(request) as? coil.request.SuccessResult)?.drawable
                    val bitmap = drawable?.toBitmap()

                    if (bitmap != null) {
                        val leftPx = (cropRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                        val topPx = (cropRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                        val rightPx = (cropRect.right * bitmap.width).toInt().coerceIn(leftPx + 1, bitmap.width)
                        val bottomPx = (cropRect.bottom * bitmap.height).toInt().coerceIn(topPx + 1, bitmap.height)
                        
                        val croppedBitmap = Bitmap.createBitmap(bitmap, leftPx, topPx, rightPx - leftPx, bottomPx - topPx)
                        val stream = ByteArrayOutputStream()
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                        
                        callGeminiApi(base64Image, stateVal.apiKey)
                    } else {
                        "Failed to load image for AI analysis."
                    }
                } catch (e: Exception) {
                    "Error processing image for Gemini: ${e.localizedMessage}"
                }
            } else {
                "No API Key configured. Please go to Settings to add your Gemini API Key for rich AI analysis and framing recommendations."
            }

            val finalResult = mathResult.copy(aiFeedback = aiCritiqueText)

            _state.update {
                it.copy(
                    isEvaluating = false,
                    scoreResult = finalResult
                )
            }
        }
    }

    private fun callGeminiApi(base64Image: String, apiKey: String): String {
        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val prompt = "Analyze this photo composition crop. Write a detailed critique focusing on:\n" +
                "1. Pros of the framing.\n" +
                "2. Cons / Areas of improvement.\n" +
                "3. Creative ideas to make it more beautiful (e.g. alignment rules, angles, focal elements)."

        val requestJson = org.json.JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("text", prompt)
                        })
                        put(org.json.JSONObject().apply {
                            put("inlineData", org.json.JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        return try {
            java.io.OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val responseJson = org.json.JSONObject(responseText)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")
                text
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                "Failed to generate feedback: HTTP $responseCode\n$errorText"
            }
        } catch (e: Exception) {
            "Failed to connect to Gemini API: ${e.localizedMessage}"
        } finally {
            conn.disconnect()
        }
    }
}
