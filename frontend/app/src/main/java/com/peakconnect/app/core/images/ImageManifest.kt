package com.peakconnect.app.core.images

import kotlinx.serialization.Serializable

@Serializable
data class ImageManifest(
    val version: Int = 1,
    val baseUrl: String,
    val items: List<ImageManifestItem> = emptyList(),
    val defaults: Map<String, String> = emptyMap()
)

@Serializable
data class ImageManifestItem(
    val location: String,
    val activity: String,
    val image: String
)
