package com.biomeshop.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.biomeshop.app.data.BiomeAssetRepository
import com.biomeshop.app.data.BiomeItem
import com.biomeshop.app.data.PanoramaSpec
import com.biomeshop.app.ui.theme.BiomeShopTheme
import com.biomeshop.app.ui.theme.Night
import com.biomeshop.app.ui.theme.TextMuted
import java.io.File
import java.io.FileInputStream
import android.net.Uri
import android.webkit.MimeTypeMap

class PanoramaActivity : ComponentActivity() {
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
                    PanoramaRoute(
                        item = item,
                        onClose = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, item: BiomeItem): Intent =
            Intent(context, PanoramaActivity::class.java)
                .putBiomeItem(item)
    }
}

@Composable
private fun PanoramaRoute(
    item: BiomeItem,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val assetRepository = remember(context) { BiomeAssetRepository(context.applicationContext) }
    val isOnline = rememberConnectivityState()

    var panoramaSpec by remember { mutableStateOf(item.panorama) }
    var panoramaUrl by remember { mutableStateOf(item.panoramaImageUrl) }
    var hasLocalPanorama by remember { mutableStateOf(false) }
    var hasRemotePanorama by remember { mutableStateOf(item.panorama != null && item.panoramaImageUrl.isNotBlank()) }
    var isSyncing by remember { mutableStateOf(true) }

    LaunchedEffect(item.id, item.revision, isOnline) {
        val initialCache = assetRepository.inspectBiome(item)
        panoramaSpec = item.panorama
        panoramaUrl = assetRepository.panoramaUrl(item, initialCache)
        hasLocalPanorama = initialCache.panoramaFile != null
        hasRemotePanorama = item.panorama != null && item.panoramaImageUrl.isNotBlank()

        if (item.panorama == null || (!isOnline && !hasLocalPanorama)) {
            isSyncing = false
            return@LaunchedEffect
        }

        val prepared = assetRepository.prepareBiomeAssets(item)
        panoramaUrl = assetRepository.panoramaUrl(item, prepared.cache)
        hasLocalPanorama = prepared.cache.panoramaFile != null
        isSyncing = false
    }

    val canRenderPanorama = panoramaSpec != null && (hasLocalPanorama || (isOnline && panoramaUrl.isNotBlank()))
    val placeholderLabel = assetStatusLabel(
        isOnline = isOnline,
        hasLocalAsset = hasLocalPanorama,
        hasRemoteAsset = hasRemotePanorama,
        fallbackMissingLabel = "360 file unavailable",
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

            when {
                canRenderPanorama && panoramaSpec != null -> {
                    if (!isOnline && hasLocalPanorama) {
                        item {
                            OfflinePill(text = "Currently offline")
                        }
                    }
                    if (isSyncing) {
                        item {
                            Text(
                                text = "Syncing 360 files in the background.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted,
                            )
                        }
                    }
                    item {
                        PanoramaWebView(
                            title = item.name,
                            panorama = panoramaSpec!!,
                            panoramaUrl = panoramaUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(520.dp),
                        )
                    }
                }
                else -> {
                    item {
                        AssetPlaceholder(
                            label = placeholderLabel.ifBlank { "Loading 360 view" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(520.dp),
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PanoramaWebView(
    title: String,
    panorama: PanoramaSpec,
    panoramaUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/cache/", InternalStoragePathHandler(File(context.filesDir, "biome-cache")))
            .build()
    }

    val pageUrl = remember(title, panorama, panoramaUrl) {
        Uri.Builder()
            .scheme("https")
            .authority("appassets.androidplatform.net")
            .appendPath("assets")
            .appendPath("panorama_viewer.html")
            .appendQueryParameter("title", title)
            .appendQueryParameter("imageUrl", panoramaUrl)
            .appendQueryParameter("fullWidth", panorama.fullWidth.toString())
            .appendQueryParameter("fullHeight", panorama.fullHeight.toString())
            .appendQueryParameter("croppedWidth", panorama.croppedWidth.toString())
            .appendQueryParameter("croppedHeight", panorama.croppedHeight.toString())
            .appendQueryParameter("croppedX", panorama.croppedX.toString())
            .appendQueryParameter("croppedY", panorama.croppedY.toString())
            .build()
            .toString()
    }

    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                }
                loadUrl(pageUrl)
            }
        },
        update = { webView ->
            if (webView.url != pageUrl) {
                webView.loadUrl(pageUrl)
            }
        },
    )
}

private class InternalStoragePathHandler(
    private val root: File,
) : WebViewAssetLoader.PathHandler {
    override fun handle(path: String): WebResourceResponse? {
        val relativePath = path.removePrefix("/")
        val target = File(root, relativePath)
        if (!target.exists() || !target.isFile) return null

        val extension = target.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        return WebResourceResponse(
            mimeType,
            null,
            FileInputStream(target),
        )
    }
}
