package com.biomeshop.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.biomeshop.app.data.Availability
import com.biomeshop.app.data.BiomeAssetPrepareResult
import com.biomeshop.app.data.BiomeAssetRepository
import com.biomeshop.app.data.BiomeCatalogData
import com.biomeshop.app.data.BiomeCatalogDefaults
import com.biomeshop.app.data.BiomeCatalogRepository
import com.biomeshop.app.data.BiomeItem
import com.biomeshop.app.data.CachedBiomeAssets
import com.biomeshop.app.data.FilterOption
import com.biomeshop.app.data.PanoramaSpec
import com.biomeshop.app.data.PriceOrder
import com.biomeshop.app.ui.theme.AccentCyan
import com.biomeshop.app.ui.theme.AccentGold
import com.biomeshop.app.ui.theme.AccentPink
import com.biomeshop.app.ui.theme.BiomeShopTheme
import com.biomeshop.app.ui.theme.Night
import com.biomeshop.app.ui.theme.NightDeep
import com.biomeshop.app.ui.theme.StatusLive
import com.biomeshop.app.ui.theme.StatusSold
import com.biomeshop.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

private val statusOptions = listOf(
    FilterOption("all", "All status"),
    FilterOption("available", "Available"),
    FilterOption("sold", "Sold"),
)

private val priceOptions = listOf(
    FilterOption("default", "Default order"),
    FilterOption("low-high", "Low to high"),
    FilterOption("high-low", "High to low"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiomeShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Night,
                ) {
                    BiomeShopApp()
                }
            }
        }
    }
}

private data class CatalogScreenState(
    val catalog: BiomeCatalogData = BiomeCatalogDefaults.data,
    val isLoading: Boolean = true,
    val message: String? = null,
)

private data class GalleryViewerState(
    val item: BiomeItem,
    val images: List<String>,
    val message: String?,
)

@Composable
private fun BiomeShopApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { BiomeCatalogRepository(context.applicationContext) }
    val assetRepository = remember(context) { BiomeAssetRepository(context.applicationContext) }

    var screenState by remember { mutableStateOf(CatalogScreenState()) }
    var biomeAssets by remember { mutableStateOf<Map<String, CachedBiomeAssets>>(emptyMap()) }
    var biomeFilter by rememberSaveable { mutableStateOf("all") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var priceOrder by rememberSaveable { mutableStateOf(PriceOrder.Default) }
    var previewItem by remember { mutableStateOf<BiomeItem?>(null) }
    var previewSyncMessage by remember { mutableStateOf<String?>(null) }
    var isPreviewSyncing by remember { mutableStateOf(false) }
    var galleryViewer by remember { mutableStateOf<GalleryViewerState?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(repository, assetRepository, refreshTick) {
        screenState = screenState.copy(isLoading = true, message = if (refreshTick == 0) screenState.message else "Refreshing live catalog...")
        val result = repository.loadCatalog(forceRefresh = refreshTick > 0)
        val caches = assetRepository.inspectCatalog(result.catalog)
        biomeAssets = caches
        screenState = CatalogScreenState(
            catalog = result.catalog,
            isLoading = false,
            message = result.message,
        )
    }

    LaunchedEffect(previewItem?.id, previewItem?.revision) {
        val item = previewItem ?: return@LaunchedEffect
        isPreviewSyncing = true
        val result = assetRepository.prepareBiomeAssets(item)
        biomeAssets = biomeAssets + (item.id to result.cache)
        previewSyncMessage = result.message
        isPreviewSyncing = false
    }

    val catalog = screenState.catalog
    val featured = remember(catalog) {
        catalog.items.firstOrNull { it.featured }
            ?: catalog.items.firstOrNull()
            ?: BiomeCatalogDefaults.data.items.first()
    }
    val visibleItems = remember(catalog, biomeFilter, statusFilter, priceOrder) {
        filterAndSortBiomes(
            catalog = catalog,
            biomeFilter = biomeFilter,
            statusFilter = statusFilter,
            priceOrder = priceOrder,
        )
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun prepareBiome(
        item: BiomeItem,
        onPrepared: (BiomeAssetPrepareResult) -> Unit,
    ) {
        scope.launch {
            val result = assetRepository.prepareBiomeAssets(item)
            biomeAssets = biomeAssets + (item.id to result.cache)
            result.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            onPrepared(result)
        }
    }

    fun openPanorama(item: BiomeItem) {
        val panorama = item.panorama
        if (panorama == null) {
            Toast.makeText(context, "No panorama available for this biome yet.", Toast.LENGTH_SHORT).show()
            return
        }

        prepareBiome(item) { result ->
            val resolved = panorama.copy(imageUrl = assetRepository.panoramaUrl(item, result.cache))
            context.startActivity(PanoramaActivity.intent(context, item.name, resolved))
        }
    }

    fun openGallery(item: BiomeItem) {
        prepareBiome(item) { result ->
            val images = assetRepository.galleryModels(item, result.cache)
            if (images.isEmpty()) {
                Toast.makeText(context, "No gallery files are available for this biome yet.", Toast.LENGTH_SHORT).show()
            } else {
                galleryViewer = GalleryViewerState(
                    item = item,
                    images = images,
                    message = result.message,
                )
            }
        }
    }

    fun refreshCatalog() {
        refreshTick += 1
    }

    Scaffold(containerColor = Night) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroCard(
                    catalog = catalog,
                    isLoading = screenState.isLoading,
                    message = screenState.message,
                    onOpenUrl = ::openUrl,
                    onOpenPanorama = { openPanorama(featured) },
                    onRefreshCatalog = ::refreshCatalog,
                )
            }
            item {
                FeaturedCard(
                    item = featured,
                    imageModel = assetRepository.previewModel(featured, biomeAssets[featured.id]),
                    onPreview = {
                        previewSyncMessage = null
                        previewItem = it
                    },
                    onOpenGallery = ::openGallery,
                    onOpenPanorama = ::openPanorama,
                )
            }
            item {
                FilterSection(
                    biomeOptions = catalog.biomeOptions,
                    biomeFilter = biomeFilter,
                    statusFilter = statusFilter,
                    priceOrder = priceOrder,
                    onBiomeChange = { biomeFilter = it },
                    onStatusChange = { statusFilter = it },
                    onPriceChange = { priceOrder = it },
                    onReset = {
                        biomeFilter = "all"
                        statusFilter = "all"
                        priceOrder = PriceOrder.Default
                    },
                )
            }
            item {
                Text(
                    text = "${visibleItems.size} biomes",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentGold,
                )
            }
            items(visibleItems, key = { it.id }) { item ->
                InventoryCard(
                    item = item,
                    imageModel = assetRepository.previewModel(item, biomeAssets[item.id]),
                    cache = biomeAssets[item.id],
                    onPreview = {
                        previewSyncMessage = null
                        previewItem = it
                    },
                    onOpenGallery = ::openGallery,
                    onOpenPanorama = ::openPanorama,
                )
            }
        }
    }

    previewItem?.let { item ->
        val cache = biomeAssets[item.id]
        PreviewDialog(
            item = item,
            imageModel = assetRepository.previewModel(item, cache),
            cache = cache,
            isSyncing = isPreviewSyncing,
            syncMessage = previewSyncMessage,
            onDismiss = {
                previewItem = null
                previewSyncMessage = null
            },
            onOpenGallery = { openGallery(item) },
            onOpenPanorama = { openPanorama(item) },
        )
    }

    galleryViewer?.let { viewer ->
        GalleryDialog(
            title = viewer.item.name,
            images = viewer.images,
            message = viewer.message,
            onDismiss = { galleryViewer = null },
        )
    }
}

private fun filterAndSortBiomes(
    catalog: BiomeCatalogData,
    biomeFilter: String,
    statusFilter: String,
    priceOrder: PriceOrder,
): List<BiomeItem> {
    val filtered = catalog.items.filter { item ->
        val biomeMatches = biomeFilter == "all" || item.types.contains(biomeFilter)
        val statusMatches = when (statusFilter) {
            "available" -> item.status == Availability.Available
            "sold" -> item.status == Availability.Sold
            else -> true
        }
        biomeMatches && statusMatches
    }

    return when (priceOrder) {
        PriceOrder.Default -> filtered
        PriceOrder.LowToHigh -> filtered.sortedBy { it.priceValue }
        PriceOrder.HighToLow -> filtered.sortedByDescending { it.priceValue }
    }
}

@Composable
private fun HeroCard(
    catalog: BiomeCatalogData,
    isLoading: Boolean,
    message: String?,
    onOpenUrl: (String) -> Unit,
    onOpenPanorama: () -> Unit,
    onRefreshCatalog: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(NightDeep, Night),
                    ),
                ),
        ) {
            AsyncImage(
                model = catalog.heroImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.38f,
                modifier = Modifier.matchParentSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "NUMBER ONE PREMIUM SHOP OF BIOMES",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentPink,
                )
                Text(
                    text = "BiomeShop",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "This app now checks the tiny version file first, caches biome metadata locally, and downloads each biome's files only when that biome is opened.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )

                AssistChip(
                    onClick = { },
                    label = { Text(if (isLoading) "Checking home repo..." else catalog.dataSourceLabel) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0x33201A38),
                        labelColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )

                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold,
                    )
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Home JSON contract", "Revision-aware cache", "Offline gallery", "Lazy biome downloads").forEach { pill ->
                        AssistChip(
                            onClick = { },
                            label = { Text(pill) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0x33201A38),
                                labelColor = MaterialTheme.colorScheme.onBackground,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatBox("Source", catalog.sourceOfTruth)
                    StatBox("Biomes", catalog.items.size.toString())
                    StatBox("Mode", "Offline ready")
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = { onOpenUrl(catalog.siteUrl) }) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open site")
                    }
                    OutlinedButton(onClick = onOpenPanorama) {
                        Text("Open featured 360")
                    }
                    OutlinedButton(onClick = onRefreshCatalog) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh catalog")
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatBox(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x55201836)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.weight(1f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelLarge, color = AccentPink)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = AccentGold)
        }
    }
}

@Composable
private fun FeaturedCard(
    item: BiomeItem,
    imageModel: String,
    onPreview: (BiomeItem) -> Unit,
    onOpenGallery: (BiomeItem) -> Unit,
    onOpenPanorama: (BiomeItem) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161121)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "FEATURED BIOME",
                style = MaterialTheme.typography.labelLarge,
                color = AccentGold,
            )
            RemoteBiomeImage(imageModel = imageModel, contentDescription = item.name, height = 220.dp)
            TypeChipRow(item.types)
            Text(text = item.name, style = MaterialTheme.typography.headlineMedium)
            Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriceText(item.priceLabel)
                StatusChip(item.status)
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { onPreview(item) }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Details")
                }
                OutlinedButton(onClick = { onOpenPanorama(item) }) {
                    Text("360 View")
                }
                OutlinedButton(onClick = { onOpenGallery(item) }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    biomeOptions: List<FilterOption>,
    biomeFilter: String,
    statusFilter: String,
    priceOrder: PriceOrder,
    onBiomeChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPriceChange: (PriceOrder) -> Unit,
    onReset: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140F20)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.FilterAlt, contentDescription = null, tint = AccentCyan)
                Text("Biome Filters", style = MaterialTheme.typography.titleLarge)
            }

            DropdownSelector(
                label = "Biome",
                selectedLabel = biomeOptions.firstOrNull { it.value == biomeFilter }?.label ?: "All biomes",
                options = biomeOptions,
                onSelected = onBiomeChange,
            )
            DropdownSelector(
                label = "Status",
                selectedLabel = statusOptions.firstOrNull { it.value == statusFilter }?.label ?: "All status",
                options = statusOptions,
                onSelected = onStatusChange,
            )
            DropdownSelector(
                label = "Price",
                selectedLabel = when (priceOrder) {
                    PriceOrder.Default -> "Default order"
                    PriceOrder.LowToHigh -> "Low to high"
                    PriceOrder.HighToLow -> "High to low"
                },
                options = priceOptions,
                onSelected = {
                    onPriceChange(
                        when (it) {
                            "low-high" -> PriceOrder.LowToHigh
                            "high-low" -> PriceOrder.HighToLow
                            else -> PriceOrder.Default
                        },
                    )
                },
            )

            TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
                Text("Reset filters")
            }
        }
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    selectedLabel: String,
    options: List<FilterOption>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelLarge, color = AccentPink)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1830)),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(selectedLabel)
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                expanded = false
                                onSelected(option.value)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryCard(
    item: BiomeItem,
    imageModel: String,
    cache: CachedBiomeAssets?,
    onPreview: (BiomeItem) -> Unit,
    onOpenGallery: (BiomeItem) -> Unit,
    onOpenPanorama: (BiomeItem) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.status == Availability.Available) Color(0xFF141022) else Color(0xFF1A1016),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemoteBiomeImage(imageModel = imageModel, contentDescription = item.name, height = 190.dp)
            TypeChipRow(item.types)
            Text(text = item.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriceText(item.priceLabel)
                StatusChip(item.status)
            }
            cache?.takeIf { it.hasAnyLocalFiles }?.let {
                Text(
                    text = if (it.isCurrent) {
                        "Offline files ready"
                    } else {
                        "Older offline files available"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGold,
                )
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { onPreview(item) }) {
                    Text("Details")
                }
                OutlinedButton(onClick = { onOpenPanorama(item) }) {
                    Text("360 View")
                }
                TextButton(onClick = { onOpenGallery(item) }) {
                    Text("Open gallery")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeChipRow(types: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { type ->
            FilterChip(
                selected = true,
                onClick = { },
                enabled = false,
                label = { Text(type.replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@Composable
private fun RemoteBiomeImage(
    imageModel: String,
    contentDescription: String,
    height: Dp,
) {
    AsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp)),
    )
}

@Composable
private fun PriceText(price: String) {
    Text(
        text = price,
        style = MaterialTheme.typography.titleLarge,
        color = AccentGold,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StatusChip(status: Availability) {
    val background = if (status == Availability.Available) StatusLive.copy(alpha = 0.2f) else StatusSold.copy(alpha = 0.2f)
    val foreground = if (status == Availability.Available) StatusLive else StatusSold

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = status.name.uppercase(),
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PreviewDialog(
    item: BiomeItem,
    imageModel: String,
    cache: CachedBiomeAssets?,
    isSyncing: Boolean,
    syncMessage: String?,
    onDismiss: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPanorama: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF120E1D)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    text = "Opening this biome prepares offline files using its revision from the home repo. If a newer revision exists, the app downloads only this biome's files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                if (isSyncing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
                        Text("Checking offline files...", color = AccentGold)
                    }
                }
                cache?.takeIf { it.hasAnyLocalFiles }?.let {
                    Text(
                        text = if (it.isCurrent) "Offline assets match revision ${item.revision}." else "Offline assets are older than revision ${item.revision}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold,
                    )
                }
                syncMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold,
                    )
                }
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = onOpenPanorama) {
                        Text("Open 360")
                    }
                    OutlinedButton(onClick = onOpenGallery) {
                        Text("Open gallery")
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryDialog(
    title: String,
    images: List<String>,
    message: String?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF120E1D)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                message?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AccentGold)
                }
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(images) { image ->
                        AsyncImage(
                            model = image,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
