package com.peakconnect.app.core.images

object ImageUrlResolver {

    private var activeManifest: ImageManifest? = null

    fun setActiveManifest(manifest: ImageManifest?) {
        activeManifest = manifest
    }

    fun getActiveManifest(): ImageManifest? = activeManifest

    fun slugify(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("[^a-z0-9]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    fun resolve(title: String, location: String): String? {
        val manifest = activeManifest ?: return null
        return resolve(title, location, manifest)
    }

    fun resolve(title: String, location: String, manifest: ImageManifest): String? {
        if (title.isBlank()) return null

        val locSlug = slugify(location)
        val titleSlug = slugify(title)

        // 1) exact match on location + activity slug
        val exactMatch = manifest.items.firstOrNull {
            it.location == locSlug && it.activity == titleSlug
        }
        if (exactMatch != null && exactMatch.image.isNotBlank()) {
            return buildFullUrl(manifest.baseUrl, exactMatch.image)
        }

        // 2) match where activity slug is contained in title slug or vice versa within same location
        val containsMatch = manifest.items.firstOrNull {
            it.location == locSlug && (titleSlug.contains(it.activity) || it.activity.contains(titleSlug))
        }
        if (containsMatch != null && containsMatch.image.isNotBlank()) {
            return buildFullUrl(manifest.baseUrl, containsMatch.image)
        }

        // 3) default by keyword found in the title (raft, trek, camp, hik)
        val defaultImage = when {
            titleSlug.contains("raft") -> manifest.defaults["rafting"]
            titleSlug.contains("trek") -> manifest.defaults["trekking"]
            titleSlug.contains("camp") -> manifest.defaults["camping"]
            titleSlug.contains("hik") -> manifest.defaults["hiking"]
            else -> null
        }

        if (!defaultImage.isNullOrBlank()) {
            return buildFullUrl(manifest.baseUrl, defaultImage)
        }

        // 4) null
        return null
    }

    private fun buildFullUrl(baseUrl: String, relativePath: String): String {
        val cleanBase = baseUrl.trimEnd('/') + "/"
        val cleanPath = relativePath.trimStart('/')
        return cleanBase + cleanPath
    }
}
