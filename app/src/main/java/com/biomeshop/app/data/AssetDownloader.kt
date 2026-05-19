package com.biomeshop.app.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BiomeAssetRepository(
    context: Context,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) {
    private val cacheRoot = File(context.filesDir, "biome-cache").apply { mkdirs() }
    private val biomesRoot = File(cacheRoot, "biomes").apply { mkdirs() }

    suspend fun inspectCatalog(catalog: BiomeCatalogData): Map<String, CachedBiomeAssets> = withContext(Dispatchers.IO) {
        catalog.items.associate { item ->
            item.id to readCacheState(item)
        }
    }

    suspend fun inspectBiome(item: BiomeItem): CachedBiomeAssets = withContext(Dispatchers.IO) {
        readCacheState(item)
    }

    suspend fun clearDownloadedBiomes() = withContext(Dispatchers.IO) {
        if (biomesRoot.exists()) {
            biomesRoot.deleteRecursively()
        }
        biomesRoot.mkdirs()
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        if (cacheRoot.exists()) {
            cacheRoot.deleteRecursively()
        }
        cacheRoot.mkdirs()
        biomesRoot.mkdirs()
    }

    suspend fun prepareBiomeAssets(biome: BiomeItem): BiomeAssetPrepareResult = withContext(Dispatchers.IO) {
        val existing = readCacheState(biome)
        if (existing.isCurrent && existing.hasAllRequiredFiles) {
            return@withContext BiomeAssetPrepareResult(
                cache = existing,
                message = "Offline files are already current for ${biome.name}.",
            )
        }

        val directory = biomeDirectory(biome.id).apply { mkdirs() }
        return@withContext runCatching {
            downloadBiomeAssets(biome, directory)
            val refreshed = readCacheState(biome)
            BiomeAssetPrepareResult(
                cache = refreshed,
                message = if (existing.hasAnyLocalFiles) {
                    "Updated offline files for ${biome.name}."
                } else {
                    "Downloaded offline files for ${biome.name}."
                },
            )
        }.getOrElse { error ->
            if (existing.hasAnyLocalFiles) {
                BiomeAssetPrepareResult(
                    cache = existing,
                    message = "Live download failed, so the app kept older downloaded files for ${biome.name}: ${error.message}",
                )
            } else {
                BiomeAssetPrepareResult(
                    cache = existing,
                    message = "This biome is not downloaded yet and live download failed: ${error.message}",
                )
            }
        }
    }

    fun previewModel(item: BiomeItem, cache: CachedBiomeAssets?): String {
        return cache?.previewFile?.takeIf { it.exists() }?.let { Uri.fromFile(it).toString() } ?: item.previewImageUrl
    }

    fun panoramaUrl(item: BiomeItem, cache: CachedBiomeAssets?): String {
        val local = cache?.panoramaFile?.takeIf { it.exists() }?.let { file ->
            val relative = file.relativeTo(cacheRoot).invariantSeparatorsPath
            "https://appassets.androidplatform.net/cache/$relative"
        }
        return local ?: item.panoramaImageUrl
    }

    fun galleryModels(item: BiomeItem, cache: CachedBiomeAssets?): List<String> {
        val localFiles = cache?.galleryFiles?.filter { it.exists() }.orEmpty()
        return if (localFiles.isNotEmpty()) {
            localFiles.map { Uri.fromFile(it).toString() }
        } else {
            item.galleryImages.map { it.url }
        }
    }

    private fun readCacheState(item: BiomeItem): CachedBiomeAssets {
        val directory = biomeDirectory(item.id)
        val manifest = readManifest(item.id)
        val previewFile = manifest?.fileByKind("preview")?.let(directory::resolve)?.takeIf(File::exists)
        val panoramaFile = manifest?.fileByKind("panorama")?.let(directory::resolve)?.takeIf(File::exists)
        val galleryFiles = manifest?.downloadedFiles
            ?.filter { it.kind == "gallery" }
            ?.mapNotNull { entry -> directory.resolve(entry.relativePath).takeIf(File::exists) }
            .orEmpty()
            .sortedBy { it.name }

        val expectedGalleryCount = item.galleryImages.size
        val hasRequired = previewFile != null &&
            panoramaFile != null &&
            galleryFiles.size >= expectedGalleryCount

        return CachedBiomeAssets(
            biomeId = item.id,
            cachedRevision = manifest?.revision.orEmpty(),
            previewFile = previewFile,
            panoramaFile = panoramaFile,
            galleryFiles = galleryFiles,
            hasAnyLocalFiles = previewFile != null || panoramaFile != null || galleryFiles.isNotEmpty(),
            hasAllRequiredFiles = hasRequired,
            isCurrent = manifest?.revision == item.revision && hasRequired,
        )
    }

    private fun downloadBiomeAssets(item: BiomeItem, directory: File) {
        val previewPath = downloadToFile(item.previewImageUrl, File(directory, "preview${extensionFor(item.previewImageUrl)}"))
        val panoramaPath = downloadToFile(item.panoramaImageUrl, File(directory, "panorama${extensionFor(item.panoramaImageUrl)}"))

        val galleryDirectory = File(directory, "gallery").apply { mkdirs() }
        val galleryEntries = item.galleryImages.map { image ->
            val target = File(galleryDirectory, image.filename.ifBlank { "image${extensionFor(image.url)}" })
            val file = downloadToFile(image.url, target)
            DownloadedFileEntry(kind = "gallery", relativePath = file.relativeTo(directory).invariantSeparatorsPath)
        }

        val manifest = BiomeAssetManifest(
            id = item.id,
            revision = item.revision,
            downloadedFiles = buildList {
                add(DownloadedFileEntry(kind = "preview", relativePath = previewPath.relativeTo(directory).invariantSeparatorsPath))
                add(DownloadedFileEntry(kind = "panorama", relativePath = panoramaPath.relativeTo(directory).invariantSeparatorsPath))
                addAll(galleryEntries)
            },
            downloadedAtEpochMs = System.currentTimeMillis(),
        )

        manifestFile(item.id).writeText(gson.toJson(manifest))
    }

    private fun downloadToFile(url: String, target: File): File {
        if (url.isBlank()) throw IOException("Asset URL was blank")
        target.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Asset request failed with HTTP ${response.code} for $url")
            }
            val bytes = response.body?.bytes() ?: throw IOException("Empty asset body for $url")
            target.writeBytes(bytes)
        }
        return target
    }

    private fun extensionFor(url: String): String {
        val segment = Uri.parse(url).lastPathSegment.orEmpty()
        val extension = segment.substringAfterLast('.', "")
        return if (extension.isBlank()) ".bin" else ".${extension.lowercase()}"
    }

    private fun biomeDirectory(biomeId: String): File = File(biomesRoot, biomeId)

    private fun manifestFile(biomeId: String): File = File(biomeDirectory(biomeId), "manifest.json")

    private fun readManifest(biomeId: String): BiomeAssetManifest? {
        val file = manifestFile(biomeId)
        if (!file.exists()) return null
        return runCatching { gson.fromJson(file.readText(), BiomeAssetManifest::class.java) }.getOrNull()
    }
}

data class CachedBiomeAssets(
    val biomeId: String,
    val cachedRevision: String,
    val previewFile: File?,
    val panoramaFile: File?,
    val galleryFiles: List<File>,
    val hasAnyLocalFiles: Boolean,
    val hasAllRequiredFiles: Boolean,
    val isCurrent: Boolean,
)

data class BiomeAssetPrepareResult(
    val cache: CachedBiomeAssets,
    val message: String?,
)

data class BiomeAssetManifest(
    val id: String,
    val revision: String,
    val downloadedFiles: List<DownloadedFileEntry>,
    val downloadedAtEpochMs: Long,
) {
    fun fileByKind(kind: String): String? = downloadedFiles.firstOrNull { it.kind == kind }?.relativePath
}

data class DownloadedFileEntry(
    val kind: String,
    val relativePath: String,
)
