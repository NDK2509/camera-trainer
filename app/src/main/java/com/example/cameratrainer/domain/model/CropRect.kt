package com.example.cameratrainer.domain.model

/**
 * Normalized crop boundary in the range [0.0, 1.0] relative to the original image dimensions.
 * Decouples the domain/business logic from device-specific screen pixels.
 */
data class CropRect(
    val left: Float,   // Left boundary (0.0 to 1.0)
    val top: Float,    // Top boundary (0.0 to 1.0)
    val right: Float,  // Right boundary (0.0 to 1.0)
    val bottom: Float  // Bottom boundary (0.0 to 1.0)
) {
    init {
        require(left in 0f..1f) { "left must be in [0, 1], was: $left" }
        require(top in 0f..1f) { "top must be in [0, 1], was: $top" }
        require(right in 0f..1f) { "right must be in [0, 1], was: $right" }
        require(bottom in 0f..1f) { "bottom must be in [0, 1], was: $bottom" }
        require(left < right) { "left ($left) must be less than right ($right)" }
        require(top < bottom) { "top ($top) must be less than bottom ($bottom)" }
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
