package com.example.cameratrainer.domain.usecase

import com.example.cameratrainer.domain.model.CompositionRule
import com.example.cameratrainer.domain.model.CropRect
import com.example.cameratrainer.domain.model.PhotoMetadata
import com.example.cameratrainer.domain.model.PointOfInterest
import com.example.cameratrainer.domain.model.ScoreResult
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Use Case to evaluate the photo composition based on the Viewfinder's CropRect.
 * Computes geometric distances from points of interest to composition rules.
 */
class EvaluateCompositionUseCase {

    operator fun invoke(cropRect: CropRect, metadata: PhotoMetadata): ScoreResult {
        val detailedFeedback = mutableListOf<String>()
        var baseScore = 100f
        
        // 1. Check if important Points of Interest (POIs) are cropped out
        val activePois = mutableListOf<Pair<PointOfInterest, Pair<Float, Float>>>()
        var totalPoiWeight = 0f
        
        for (poi in metadata.pointsOfInterest) {
            totalPoiWeight += poi.weight
            // Verify if the POI is inside the crop bounding box
            if (poi.x in cropRect.left..cropRect.right && poi.y in cropRect.top..cropRect.bottom) {
                // Calculate normalized POI coordinates (u, v) in the viewfinder [0.0, 1.0]
                val u = (poi.x - cropRect.left) / cropRect.width
                val v = (poi.y - cropRect.top) / cropRect.height
                activePois.add(poi to (u to v))
            } else {
                // Apply penalty if a key subject is cropped out completely
                val penalty = 30f * poi.weight
                baseScore -= penalty
                detailedFeedback.add("Subject \"${poi.label}\" was cut out of the frame (-${penalty.toInt()} pts).")
            }
        }
        
        // Return 0 if there are no subjects/points of interest inside the viewfinder
        if (activePois.isEmpty()) {
            return ScoreResult(
                score = 0,
                feedback = "Empty viewfinder!",
                detailedFeedback = listOf("No subjects or points of interest were found in your viewfinder. Move or zoom out to find a subject!")
            )
        }
        
        // 2. Perform detailed composition checking according to the targetRule
        val compositionScore = when (metadata.targetRule) {
            CompositionRule.RULE_OF_THIRDS -> {
                evaluateRuleOfThirds(activePois, detailedFeedback)
            }
            CompositionRule.GOLDEN_RATIO -> {
                evaluateGoldenRatio(activePois, detailedFeedback)
            }
            CompositionRule.SYMMETRY -> {
                evaluateSymmetry(activePois, detailedFeedback)
            }
            CompositionRule.HORIZON -> {
                evaluateHorizon(activePois, detailedFeedback)
            }
        }
        
        // 3. Compute the final composite score
        val finalScore = (baseScore * (compositionScore / 100f)).coerceIn(0f, 100f).toInt()
        
        // General feedback summary based on the final score
        val feedbackSummary = when {
            finalScore >= 90 -> "Excellent composition! Very professional framing."
            finalScore >= 75 -> "Quite good! Elements are laid out relatively harmoniously."
            finalScore >= 50 -> "Decent. You should adjust the framing to emphasize the main subject."
            else -> "Poor composition. Try applying the ${metadata.targetRule.displayName} rule."
        }
        
        return ScoreResult(
            score = finalScore,
            feedback = feedbackSummary,
            detailedFeedback = detailedFeedback
        )
    }

    /**
     * Rule of Thirds:
     * Maximizes score when subjects align with the 4 intersection points: (1/3, 1/3), (2/3, 1/3), (1/3, 2/3), (2/3, 2/3)
     */
    private fun evaluateRuleOfThirds(
        pois: List<Pair<PointOfInterest, Pair<Float, Float>>>,
        feedback: MutableList<String>
    ): Float {
        val lines = listOf(1f / 3f, 2f / 3f)
        var sumScore = 0f
        var totalWeight = 0f

        for ((poi, uv) in pois) {
            val (u, v) = uv
            var minDistance = Float.MAX_VALUE
            
            // Find distance to the nearest intersection point
            for (gx in lines) {
                for (gy in lines) {
                    val dist = sqrt((u - gx) * (u - gx) + (v - gy) * (v - gy))
                    if (dist < minDistance) {
                        minDistance = dist
                    }
                }
            }

            // Max distance threshold to receive points
            val maxDistance = 0.35f
            val poiScore = if (minDistance < maxDistance) {
                (1f - (minDistance / maxDistance)) * 100f
            } else {
                0f
            }
            
            sumScore += poiScore * poi.weight
            totalWeight += poi.weight

            if (poiScore >= 85f) {
                feedback.add("Subject \"${poi.label}\" is placed perfectly at a Rule of Thirds intersection (+${poiScore.toInt()} pts).")
            } else if (poiScore >= 50f) {
                feedback.add("Subject \"${poi.label}\" is close to a 1/3 intersection but not fully optimal (+${poiScore.toInt()} pts).")
            } else {
                feedback.add("Subject \"${poi.label}\" is too far from Rule of Thirds gold points (0 pts). Try positioning it closer to intersections.")
            }
        }

        return if (totalWeight > 0) sumScore / totalWeight else 0f
    }

    /**
     * Golden Ratio:
     * Divide grid lines at 0.382 and 0.618.
     */
    private fun evaluateGoldenRatio(
        pois: List<Pair<PointOfInterest, Pair<Float, Float>>>,
        feedback: MutableList<String>
    ): Float {
        val phiLines = listOf(0.382f, 0.618f)
        var sumScore = 0f
        var totalWeight = 0f

        for ((poi, uv) in pois) {
            val (u, v) = uv
            var minDistance = Float.MAX_VALUE
            
            for (gx in phiLines) {
                for (gy in phiLines) {
                    val dist = sqrt((u - gx) * (u - gx) + (v - gy) * (v - gy))
                    if (dist < minDistance) {
                        minDistance = dist
                    }
                }
            }

            val maxDistance = 0.3f
            val poiScore = if (minDistance < maxDistance) {
                (1f - (minDistance / maxDistance)) * 100f
            } else {
                0f
            }

            sumScore += poiScore * poi.weight
            totalWeight += poi.weight

            if (poiScore >= 85f) {
                feedback.add("Subject \"${poi.label}\" is positioned precisely at a Golden Ratio intersection (+${poiScore.toInt()} pts).")
            } else if (poiScore >= 50f) {
                feedback.add("Subject \"${poi.label}\" is slightly offset from the Golden Ratio intersection (+${poiScore.toInt()} pts).")
            } else {
                feedback.add("Subject \"${poi.label}\" is out of the golden ratio points zone (0 pts).")
            }
        }

        return if (totalWeight > 0) sumScore / totalWeight else 0f
    }

    /**
     * Symmetry/Center composition:
     * Evaluates alignment with the absolute center (0.5, 0.5)
     */
    private fun evaluateSymmetry(
        pois: List<Pair<PointOfInterest, Pair<Float, Float>>>,
        feedback: MutableList<String>
    ): Float {
        var sumScore = 0f
        var totalWeight = 0f

        for ((poi, uv) in pois) {
            val (u, v) = uv
            // Euclidean distance to center point (0.5, 0.5)
            val distToCenter = sqrt((u - 0.5f) * (u - 0.5f) + (v - 0.5f) * (v - 0.5f))
            
            val maxDistance = 0.35f
            val poiScore = if (distToCenter < maxDistance) {
                (1f - (distToCenter / maxDistance)) * 100f
            } else {
                0f
            }

            sumScore += poiScore * poi.weight
            totalWeight += poi.weight

            if (poiScore >= 85f) {
                feedback.add("Subject \"${poi.label}\" is perfectly centered and symmetric (+${poiScore.toInt()} pts).")
            } else if (poiScore >= 50f) {
                feedback.add("Subject \"${poi.label}\" is near the center but not perfectly aligned (+${poiScore.toInt()} pts).")
            } else {
                feedback.add("Subject \"${poi.label}\" is far off the central axis (0 pts). Try placing it in the center.")
            }
        }

        return if (totalWeight > 0) sumScore / totalWeight else 0f
    }

    /**
     * Horizon Line:
     * Horizon should align with the 1/3 horizontal top (0.33) or bottom (0.66) line.
     */
    private fun evaluateHorizon(
        pois: List<Pair<PointOfInterest, Pair<Float, Float>>>,
        feedback: MutableList<String>
    ): Float {
        var sumScore = 0f
        var totalWeight = 0f

        for ((poi, uv) in pois) {
            val (_, v) = uv
            // Calculate minimum offset to v = 0.333 or v = 0.666
            val distToTopThird = abs(v - 0.333f)
            val distToBottomThird = abs(v - 0.666f)
            val minDist = Math.min(distToTopThird, distToBottomThird)

            val maxDistance = 0.15f
            val poiScore = if (minDist < maxDistance) {
                (1f - (minDist / maxDistance)) * 100f
            } else {
                0f
            }

            sumScore += poiScore * poi.weight
            totalWeight += poi.weight

            if (poiScore >= 85f) {
                feedback.add("Horizon line \"${poi.label}\" is beautifully aligned at the 1/3 divider (+${poiScore.toInt()} pts).")
            } else if (poiScore >= 50f) {
                feedback.add("Horizon line \"${poi.label}\" is in a safe region but split ratio could be improved (+${poiScore.toInt()} pts).")
            } else {
                feedback.add("Avoid splitting the photo in half with horizon line \"${poi.label}\" or placing it too close to the borders (0 pts).")
            }
        }

        return if (totalWeight > 0) sumScore / totalWeight else 0f
    }
}
