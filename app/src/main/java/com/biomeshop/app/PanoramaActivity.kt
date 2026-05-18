package com.biomeshop.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.biomeshop.app.data.PanoramaSpec
import java.io.File
import java.io.FileInputStream

class PanoramaActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val panoramaUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: return finish()

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/cache/", InternalStoragePathHandler(File(filesDir, "biome-cache")))
            .build()

        val webView = WebView(this).apply {
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
        }

        setContentView(webView)

        val uri = Uri.Builder()
            .scheme("https")
            .authority("appassets.androidplatform.net")
            .appendPath("assets")
            .appendPath("panorama_viewer.html")
            .appendQueryParameter("title", intent.getStringExtra(EXTRA_TITLE).orEmpty())
            .appendQueryParameter("imageUrl", panoramaUrl)
            .appendQueryParameter("fullWidth", intent.getIntExtra(EXTRA_FULL_WIDTH, 0).toString())
            .appendQueryParameter("fullHeight", intent.getIntExtra(EXTRA_FULL_HEIGHT, 0).toString())
            .appendQueryParameter("croppedWidth", intent.getIntExtra(EXTRA_CROPPED_WIDTH, 0).toString())
            .appendQueryParameter("croppedHeight", intent.getIntExtra(EXTRA_CROPPED_HEIGHT, 0).toString())
            .appendQueryParameter("croppedX", intent.getIntExtra(EXTRA_CROPPED_X, 0).toString())
            .appendQueryParameter("croppedY", intent.getIntExtra(EXTRA_CROPPED_Y, 0).toString())
            .build()

        webView.loadUrl(uri.toString())
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_IMAGE_URL = "imageUrl"
        private const val EXTRA_FULL_WIDTH = "fullWidth"
        private const val EXTRA_FULL_HEIGHT = "fullHeight"
        private const val EXTRA_CROPPED_WIDTH = "croppedWidth"
        private const val EXTRA_CROPPED_HEIGHT = "croppedHeight"
        private const val EXTRA_CROPPED_X = "croppedX"
        private const val EXTRA_CROPPED_Y = "croppedY"

        fun intent(
            context: Context,
            title: String,
            panorama: PanoramaSpec,
        ): Intent = Intent(context, PanoramaActivity::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_IMAGE_URL, panorama.imageUrl)
            putExtra(EXTRA_FULL_WIDTH, panorama.fullWidth)
            putExtra(EXTRA_FULL_HEIGHT, panorama.fullHeight)
            putExtra(EXTRA_CROPPED_WIDTH, panorama.croppedWidth)
            putExtra(EXTRA_CROPPED_HEIGHT, panorama.croppedHeight)
            putExtra(EXTRA_CROPPED_X, panorama.croppedX)
            putExtra(EXTRA_CROPPED_Y, panorama.croppedY)
        }
    }
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
