package com.biomeshop.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BiomeCatalogRepository(
    context: Context,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) {
    private val cacheRoot = File(context.filesDir, "biome-cache").apply { mkdirs() }
    private val versionCacheFile = File(cacheRoot, "biome-version.json")
    private val catalogCacheFile = File(cacheRoot, "biome.json")

    suspend fun loadCatalog(forceRefresh: Boolean = false): CatalogLoadResult = withContext(Dispatchers.IO) {
        val localVersion = readLocalVersion()
        val localCatalog = readLocalCatalog()

        val remoteVersionResult = runCatching { fetchVersion() }
        val remoteVersion = remoteVersionResult.getOrNull()

        if (!forceRefresh && remoteVersion != null && localCatalog != null) {
            val unchanged = remoteVersion.contentHash.isNotBlank() &&
                remoteVersion.contentHash == localVersion?.contentHash
            if (unchanged) {
                writeVersion(remoteVersion)
                return@withContext CatalogLoadResult(
                    catalog = localCatalog.copy(dataSourceLabel = "Cached catalog from home repo"),
                    version = remoteVersion,
                    usedFallback = false,
                    message = "Catalog is up to date. Using cached metadata from the last successful sync.",
                )
            }
        }

        if (remoteVersion != null) {
            val remoteCatalog = runCatching { fetchCatalog() }
            remoteCatalog.onSuccess { catalog ->
                writeVersion(remoteVersion)
                writeCatalog(catalog)
                return@withContext CatalogLoadResult(
                    catalog = catalog,
                    version = remoteVersion,
                    usedFallback = false,
                    message = if (localVersion?.contentHash == remoteVersion.contentHash) {
                        "Live catalog refreshed from the home repo."
                    } else {
                        "New home repo content detected and cached locally."
                    },
                )
            }
        }

        if (localCatalog != null) {
            val fallbackMessage = remoteVersionResult.exceptionOrNull()?.message
                ?: "The app could not refresh live data."
            return@withContext CatalogLoadResult(
                catalog = localCatalog.copy(dataSourceLabel = "Cached catalog from home repo"),
                version = localVersion,
                usedFallback = false,
                message = "Using cached metadata because live sync failed: $fallbackMessage",
            )
        }

        CatalogLoadResult(
            catalog = BiomeCatalogDefaults.data,
            version = localVersion,
            usedFallback = true,
            message = "Using bundled fallback because no cached metadata is available yet.",
        )
    }

    suspend fun clearMetadataCache() = withContext(Dispatchers.IO) {
        if (versionCacheFile.exists()) {
            versionCacheFile.delete()
        }
        if (catalogCacheFile.exists()) {
            catalogCacheFile.delete()
        }
    }

    suspend fun loadItem(itemId: String, forceRefresh: Boolean = false): BiomeItem? {
        return loadCatalog(forceRefresh = forceRefresh).catalog.items.firstOrNull { it.id == itemId }
    }

    private fun fetchVersion(): BiomeVersionData {
        val request = Request.Builder()
            .url(BiomeCatalogDefaults.remoteVersionUrl)
            .build()
        val body = execute(request)
        val remote = gson.fromJson(body, RemoteVersionDto::class.java)
            ?: throw IOException("Version payload could not be parsed")

        return BiomeVersionData(
            schemaVersion = remote.schemaVersion,
            sourceOfTruth = remote.sourceOfTruth.orEmpty(),
            siteBaseUrl = remote.siteBaseUrl.orEmpty().ifBlank { BiomeCatalogDefaults.siteBaseUrl },
            contentHash = remote.contentHash.orEmpty(),
            biomeCount = remote.biomeCount,
            biomes = remote.biomes.map {
                BiomeVersionEntry(
                    id = it.id.orEmpty(),
                    revision = it.revision.orEmpty(),
                    status = it.status.orEmpty(),
                    featured = it.featured,
                )
            },
        )
    }

    private fun fetchCatalog(): BiomeCatalogData {
        val request = Request.Builder()
            .url(BiomeCatalogDefaults.remoteCatalogUrl)
            .build()
        val body = execute(request)
        val remote = gson.fromJson(body, RemoteCatalogDto::class.java)
            ?: throw IOException("Catalog payload could not be parsed")

        val siteUrl = remote.siteBaseUrl.orEmpty().ifBlank { BiomeCatalogDefaults.siteBaseUrl }
        val heroImageUrl = "${siteUrl}assets/biomeshop-hero.png"

        return BiomeCatalogData(
            siteUrl = siteUrl,
            sourceOfTruth = remote.sourceOfTruth.orEmpty().ifBlank { "biomeinfos.js" },
            ownerName = BiomeCatalogDefaults.ownerName,
            heroImageUrl = heroImageUrl,
            dataSourceLabel = "Live catalog from home repo",
            biomeOptions = remote.biomeTypeOptions.mapNotNull { option ->
                val value = option.getOrNull(0)?.trim().orEmpty()
                val label = option.getOrNull(1)?.trim().orEmpty()
                if (value.isBlank()) null else FilterOption(value = value, label = label.ifBlank { value })
            }.ifEmpty { BiomeCatalogDefaults.data.biomeOptions },
            mainPanorama = remote.mainBiomeShopPanorama?.toPanoramaSpec(),
            items = remote.biomes.map { it.toBiomeItem() },
        )
    }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Request failed with HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                throw IOException("Response body was empty")
            }

            return body
        }
    }

    private fun readLocalVersion(): BiomeVersionData? = readJson(versionCacheFile, BiomeVersionData::class.java)

    private fun readLocalCatalog(): BiomeCatalogData? = readJson(catalogCacheFile, BiomeCatalogData::class.java)

    private fun <T> readJson(file: File, type: Class<T>): T? {
        if (!file.exists()) return null
        return runCatching {
            gson.fromJson(file.readText(), type)
        }.getOrNull()
    }

    private fun writeVersion(version: BiomeVersionData) {
        versionCacheFile.writeText(gson.toJson(version))
    }

    private fun writeCatalog(catalog: BiomeCatalogData) {
        catalogCacheFile.writeText(gson.toJson(catalog))
    }
}

data class CatalogLoadResult(
    val catalog: BiomeCatalogData,
    val version: BiomeVersionData?,
    val usedFallback: Boolean,
    val message: String?,
)

private data class RemoteVersionDto(
    @SerializedName("schemaVersion") val schemaVersion: Int = 1,
    @SerializedName("sourceOfTruth") val sourceOfTruth: String? = null,
    @SerializedName("siteBaseUrl") val siteBaseUrl: String? = null,
    @SerializedName("contentHash") val contentHash: String? = null,
    @SerializedName("biomeCount") val biomeCount: Int = 0,
    @SerializedName("biomes") val biomes: List<RemoteVersionBiomeDto> = emptyList(),
)

private data class RemoteVersionBiomeDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("revision") val revision: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("featured") val featured: Boolean = false,
)

private data class RemoteCatalogDto(
    @SerializedName("siteBaseUrl") val siteBaseUrl: String? = null,
    @SerializedName("sourceOfTruth") val sourceOfTruth: String? = null,
    @SerializedName("biomeTypeOptions") val biomeTypeOptions: List<List<String>> = emptyList(),
    @SerializedName("mainBiomeShopPanorama") val mainBiomeShopPanorama: RemotePanoramaContainerDto? = null,
    @SerializedName("biomes") val biomes: List<RemoteBiomeDto> = emptyList(),
)

private data class RemotePanoramaContainerDto(
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("pano") val pano: RemotePanoDto? = null,
) {
    fun toPanoramaSpec(): PanoramaSpec? {
        val url = imageUrl.orEmpty()
        val data = pano ?: return null
        if (url.isBlank()) return null
        return PanoramaSpec(
            imageUrl = url,
            fullWidth = data.fullWidth,
            fullHeight = data.fullHeight,
            croppedWidth = data.croppedWidth,
            croppedHeight = data.croppedHeight,
            croppedX = data.croppedX,
            croppedY = data.croppedY,
        )
    }
}

private data class RemotePanoDto(
    @SerializedName("fullWidth") val fullWidth: Int = 0,
    @SerializedName("fullHeight") val fullHeight: Int = 0,
    @SerializedName("croppedWidth") val croppedWidth: Int = 0,
    @SerializedName("croppedHeight") val croppedHeight: Int = 0,
    @SerializedName("croppedX") val croppedX: Int = 0,
    @SerializedName("croppedY") val croppedY: Int = 0,
)

private data class RemoteBiomeDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("types") val types: List<String> = emptyList(),
    @SerializedName("name") val name: String? = null,
    @SerializedName("priceLabel") val priceLabel: String? = null,
    @SerializedName("priceValue") val priceValue: Double = 0.0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("featured") val featured: Boolean = false,
    @SerializedName("previewImageUrl") val previewImageUrl: String? = null,
    @SerializedName("panoramaImageUrl") val panoramaImageUrl: String? = null,
    @SerializedName("galleryFolderUrl") val galleryFolderUrl: String? = null,
    @SerializedName("galleryImages") val galleryImages: List<RemoteGalleryImageDto> = emptyList(),
    @SerializedName("revision") val revision: String? = null,
    @SerializedName("pano") val pano: RemotePanoDto? = null,
) {
    fun toBiomeItem(): BiomeItem {
        val previewUrl = previewImageUrl.orEmpty()
        val panoramaUrl = panoramaImageUrl.orEmpty().ifBlank { previewUrl }
        return BiomeItem(
            id = id.orEmpty(),
            types = types,
            name = name.orEmpty(),
            priceLabel = priceLabel.orEmpty(),
            priceValue = priceValue,
            status = if (status.equals("Sold", ignoreCase = true)) Availability.Sold else Availability.Available,
            description = description.orEmpty(),
            previewImageUrl = previewUrl,
            featured = featured,
            panorama = pano?.let {
                PanoramaSpec(
                    imageUrl = panoramaUrl,
                    fullWidth = it.fullWidth,
                    fullHeight = it.fullHeight,
                    croppedWidth = it.croppedWidth,
                    croppedHeight = it.croppedHeight,
                    croppedX = it.croppedX,
                    croppedY = it.croppedY,
                )
            },
            panoramaImageUrl = panoramaUrl,
            galleryFolderUrl = galleryFolderUrl.orEmpty(),
            galleryImages = galleryImages.mapNotNull { image ->
                val filename = image.filename.orEmpty()
                val url = image.url.orEmpty()
                if (url.isBlank()) null else GalleryImage(filename = filename, path = image.path.orEmpty(), url = url)
            },
            revision = revision.orEmpty(),
        )
    }
}

private data class RemoteGalleryImageDto(
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("url") val url: String? = null,
)
