package com.biomeshop.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biomeshop.app.data.BiomeAssetRepository
import com.biomeshop.app.data.BiomeItem
import com.biomeshop.app.ui.theme.BiomeShopTheme
import com.biomeshop.app.ui.theme.Night
import com.biomeshop.app.ui.theme.TextMuted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BiomeDetailActivity : ComponentActivity() {
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
                    BiomeDetailRoute(
                        item = item,
                        onClose = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, item: BiomeItem): Intent =
            Intent(context, BiomeDetailActivity::class.java)
                .putBiomeItem(item)
    }
}

@Composable
private fun BiomeDetailRoute(
    item: BiomeItem,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val assetRepository = remember(context) { BiomeAssetRepository(context.applicationContext) }

    var imageModel by remember { mutableStateOf(item.previewImageUrl) }
    var isSyncing by remember { mutableStateOf(true) }
    var pendingLaunch by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(item.id, item.revision) {
        val cache = assetRepository.inspectBiome(item)
        imageModel = assetRepository.previewModel(item, cache)
        if (cache.isCurrent) {
            isSyncing = false
            return@LaunchedEffect
        }

        val prepared = assetRepository.prepareBiomeAssets(item)
        imageModel = assetRepository.previewModel(item, prepared.cache)
        isSyncing = false
    }

    fun launchLatest(action: () -> Unit) {
        pendingLaunch?.cancel()
        pendingLaunch = scope.launch {
            delay(150)
            action()
            pendingLaunch = null
        }
    }

    FullScreenSurface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                RouteTopBar(title = item.name, onBack = onClose)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    RemoteBiomeImage(
                        imageModel = imageModel,
                        contentDescription = item.name,
                        height = 320.dp,
                    )
                    if (isSyncing) {
                        SkeletonImage(
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }
                }
            }
            item {
                TypeChipRow(item.types)
            }
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PriceText(item.priceLabel)
                    StatusChip(item.status)
                }
            }
            item {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )
            }
            item {
                DetailActions(
                    item = item,
                    onOpenPanorama = { selected ->
                        launchLatest {
                            context.startActivity(PanoramaActivity.intent(context, selected))
                        }
                    },
                    onOpenGallery = { selected ->
                        launchLatest {
                            context.startActivity(BiomeGalleryActivity.intent(context, selected))
                        }
                    },
                )
            }
        }
    }
}
