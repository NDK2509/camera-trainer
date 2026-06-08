package com.example.cameratrainer.domain.model

/**
 * Suggested composition guidelines for photography training.
 */
enum class CompositionRule(val displayName: String) {
    RULE_OF_THIRDS("Rule of Thirds"),
    GOLDEN_RATIO("Golden Ratio"),
    SYMMETRY("Symmetry"),
    HORIZON("Horizon Line")
}

/**
 * Metadata containing the original dimensions and points of interest of the photo.
 */
data class PhotoMetadata(
    val width: Int,                           // Original width of the photo (pixels)
    val height: Int,                          // Original height of the photo (pixels)
    val targetRule: CompositionRule,          // Recommended optimal composition rule for this photo
    val pointsOfInterest: List<PointOfInterest> // List of golden points and subjects in the original photo
)
