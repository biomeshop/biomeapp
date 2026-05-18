package com.biomeshop.app.data

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BiomeCatalogRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) {
    suspend fun loadCatalog(): CatalogLoadResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BiomeCatalogDefaults.remoteCatalogUrl)
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Catalog request failed with ${response.code}")
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    throw IOException("Catalog response was empty")
                }

                gson.fromJson(body, BiomeCatalogData::class.java)
            }
        }.fold(
            onSuccess = { remote ->
                CatalogLoadResult(
                    catalog = remote.copy(
                        dataSourceLabel = if (remote.dataSourceLabel.isBlank()) {
                            "Live catalog from GitHub"
                        } else {
                            remote.dataSourceLabel
                        },
                    ),
                    usedFallback = false,
                    message = null,
                )
            },
            onFailure = { error ->
                CatalogLoadResult(
                    catalog = BiomeCatalogDefaults.data,
                    usedFallback = true,
                    message = "Using bundled fallback because live data could not be loaded: ${error.message}",
                )
            },
        )
    }
}

data class CatalogLoadResult(
    val catalog: BiomeCatalogData,
    val usedFallback: Boolean,
    val message: String?,
)
