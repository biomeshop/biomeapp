package com.biomeshop.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.biomeshop.app.data.BiomeAssetRepository
import com.biomeshop.app.data.BiomeItem
import com.biomeshop.app.ui.theme.BiomeShopTheme
import com.biomeshop.app.ui.theme.Night
import com.biomeshop.app.ui.theme.TextMuted

class BiomeGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val item = intent.biomeItemOrNull() ?: run {
            finish()
            return
        }

        setContent {
            BiomeShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Night,
                ) {
                    BiomeGalleryRoute(
                        item = item,
                        onClose = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, item: BiomeItem): Intent =
            Intent(context, BiomeGalleryActivity::class.java)
                .putBiomeItem(item)
    }
}

@Composable
private fun BiomeGalleryRoute(
    item: BiomeItem,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val assetRepository = remember(context) { BiomeAssetRepository(context.applicationContext) }
    val isOnline = rememberConnectivityState()

    var images by remember { mutableStateOf(item.galleryImages.map { it.url }) }
    var hasLocalImages by remember { mutableStateOf(false) }
    var hasRemoteImages by remember { mutableStateOf(item.galleryImages.isNotEmpty()) }
    var isSyncing by remember { mutableStateOf(true) }

    LaunchedEffect(item.id, item.revision, isOnline) {
        val cache = assetRepository.inspectBiome(item)
        val initialImages = assetRepository.galleryModels(item, cache)
        images = initialImages
        hasLocalImages = cache.galleryFiles.isNotEmpty()
        hasRemoteImages = item.galleryImages.isNotEmpty()

        if (!isOnline && !hasLocalImages) {
            isSyncing = false
            return@LaunchedEffect
        }

        val prepared = assetRepository.prepareBiomeAssets(item)
        val refreshedImages = assetRepository.galleryModels(item, prepared.cache)
        images = refreshedImages
        hasLocalImages = prepared.cache.galleryFiles.isNotEmpty()
        isSyncing = false
    }

    val showPlaceholder = images.isEmpty()
    val statusLabel = assetStatusLabel(
        isOnline = isOnline,
        hasLocalAsset = hasLocalImages,
        hasRemoteAsset = hasRemoteImages,
        fallbackMissingLabel = "Gallery files unavailable",
    )

    FullScreenSurface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                RouteTopBar(title = item.name, onClose = onClose)
            }
            if (showPlaceholder) {
                item {
                    AssetPlaceholder(
                        label = statusLabel.ifBlank { "Loading gallery" },
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            } else {
                if (!isOnline && hasLocalImages) {
                    item {
                        OfflinePill(text = "Currently offline")
                    }
                }
                if (isSyncing) {
                    item {
                        Text(
                            text = "Syncing gallery files in the background.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted,
                        )
                    }
                }
                items(images) { image ->
                    AsyncImage(
                        model = image,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(22.dp)),
                    )
                }
            }
        }
    }
}
