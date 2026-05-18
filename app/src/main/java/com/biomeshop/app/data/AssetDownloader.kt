package com.biomeshop.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment

class AssetDownloader(private val context: Context) {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    fun enqueueCatalogAssets(catalog: BiomeCatalogData): Int {
        val assets = buildList {
            add(DownloadableAsset("hero", catalog.heroImageUrl))
            catalog.mainPanorama?.let { add(DownloadableAsset("main-panorama", it.imageUrl)) }
            catalog.items.forEach { item ->
                add(DownloadableAsset(item.id, item.imageUrl))
                item.panorama?.let { add(DownloadableAsset("${item.id}-panorama", it.imageUrl)) }
            }
        }.distinctBy { it.url }

        assets.forEach { asset ->
            val request = DownloadManager.Request(Uri.parse(asset.url))
                .setTitle(asset.fileName)
                .setDescription("BiomeShop asset download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "BiomeShop/${asset.fileName}",
                )
            } else {
                request.setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    "BiomeShop/${asset.fileName}",
                )
            }

            downloadManager.enqueue(request)
        }

        return assets.size
    }
}

private data class DownloadableAsset(
    val id: String,
    val url: String,
) {
    val fileName: String
        get() {
            val lastSegment = Uri.parse(url).lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
            return lastSegment ?: "$id.bin"
        }
}
