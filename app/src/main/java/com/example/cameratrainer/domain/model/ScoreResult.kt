package com.example.cameratrainer.domain.model

/**
 * Results of the composition evaluation after the user captures the photo.
 */
data class ScoreResult(
    val score: Int,                        // Score from 0 to 100
    val feedback: String,                  // Brief general feedback (e.g., "Excellent Composition!")
    val detailedFeedback: List<String>,    // Bulleted list of detailed technical feedback
    val aiFeedback: String? = null         // Detailed AI critique from Gemini API
)
