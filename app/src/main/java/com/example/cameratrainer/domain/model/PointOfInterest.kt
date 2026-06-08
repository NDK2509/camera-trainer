package com.example.cameratrainer.domain.model

/**
 * Represents a key point of interest (POI) in the original photo.
 * The [x] and [y] coordinates are normalized in the range [0.0, 1.0] relative to the original photo dimensions.
 * For example: (0.5, 0.5) is the exact center of the photo.
 */
data class PointOfInterest(
    val label: String,        // Name of the point of interest (e.g., "Model", "Sun", "Lighthouse")
    val x: Float,             // Normalized X coordinate (0.0 to 1.0) from left to right
    val y: Float,             // Normalized Y coordinate (0.0 to 1.0) from top to bottom
    val weight: Float = 1.0f  // Importance weight of this POI (e.g., primary subject = 1.0, secondary = 0.5)
)
