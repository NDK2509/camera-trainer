package com.example.cameratrainer.domain.model

/**
 * Domain model representing a real-world photo used for composition training.
 */
data class Photo(
    val id: String,
    val url: String,            // High-resolution image URL or local asset path
    val author: String,         // Photographer's name
    val description: String,    // Short description of the original photo
    val metadata: PhotoMetadata // Metadata including target composition guidelines and POIs
)
