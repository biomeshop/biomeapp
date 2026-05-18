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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.biomeshop.app.data.AssetDownloader
import com.biomeshop.app.data.Availability
import com.biomeshop.app.data.BiomeCatalogData
import com.biomeshop.app.data.BiomeCatalogDefaults
import com.biomeshop.app.data.BiomeCatalogRepository
import com.biomeshop.app.data.BiomeItem
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

@Composable
private fun BiomeShopApp() {
    val context = LocalContext.current
    val repository = remember { BiomeCatalogRepository() }
    val downloader = remember(context) { AssetDownloader(context.applicationContext) }

    var screenState by remember {
        mutableStateOf(CatalogScreenState())
    }
    var biomeFilter by rememberSaveable { mutableStateOf("all") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var priceOrder by rememberSaveable { mutableStateOf(PriceOrder.Default) }
    var previewItem by remember { mutableStateOf<BiomeItem?>(null) }

    LaunchedEffect(repository) {
        val result = repository.loadCatalog()
        screenState = CatalogScreenState(
            catalog = result.catalog,
            isLoading = false,
            message = result.message ?: "Live catalog loaded from GitHub.",
        )
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

    fun openPanorama(title: String, panorama: PanoramaSpec?) {
        if (panorama == null) {
            Toast.makeText(context, "No panorama available for this biome yet.", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(PanoramaActivity.intent(context, title, panorama))
    }

    fun queueDownloads() {
        val count = downloader.enqueueCatalogAssets(catalog)
        Toast.makeText(context, "Queued $count assets for download.", Toast.LENGTH_LONG).show()
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
                    onOpenPanorama = ::openPanorama,
                    onDownloadAll = ::queueDownloads,
                )
            }
            item {
                FeaturedCard(
                    item = featured,
                    onPreview = { previewItem = it },
                    onOpenGallery = { openUrl(catalog.galleryUrl(it.id)) },
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
                    onPreview = { previewItem = it },
                    onOpenGallery = { openUrl(catalog.galleryUrl(it.id)) },
                    onOpenPanorama = ::openPanorama,
                )
            }
        }
    }

    previewItem?.let { item ->
        PreviewDialog(
            item = item,
            onDismiss = { previewItem = null },
            onOpenBrowser = { openUrl(catalog.galleryUrl(item.id)) },
            onOpenPanorama = { openPanorama(item.name, item.panorama) },
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
    onOpenPanorama: (String, PanoramaSpec?) -> Unit,
    onDownloadAll: () -> Unit,
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
                    text = "Live catalog, panorama launch, and download support are wired in. Visit /pw biomeshop in-game.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )

                AssistChip(
                    onClick = { },
                    label = { Text(if (isLoading) "Loading live data..." else catalog.dataSourceLabel) },
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
                    listOf("Natural Generated", "Remote JSON", "360 Panorama", "Download Assets").forEach { pill ->
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
                    StatBox("Owner", catalog.ownerName)
                    StatBox("Biomes", catalog.items.size.toString())
                    StatBox("Mode", "360")
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
                    OutlinedButton(onClick = { onOpenPanorama("BiomeShop Main Panorama", catalog.mainPanorama) }) {
                        Text("Open 360")
                    }
                    OutlinedButton(onClick = onDownloadAll) {
                        Text("Download assets")
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
    onPreview: (BiomeItem) -> Unit,
    onOpenGallery: (BiomeItem) -> Unit,
    onOpenPanorama: (String, PanoramaSpec?) -> Unit,
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
            RemoteBiomeImage(item = item, height = 220.dp)
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
                    Text("Preview")
                }
                OutlinedButton(onClick = { onOpenPanorama(item.name, item.panorama) }) {
                    Text("360 View")
                }
                OutlinedButton(onClick = { onOpenGallery(item) }) {
                    Icon(Icons.Default.Language, contentDescription = null)
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
    onPreview: (BiomeItem) -> Unit,
    onOpenGallery: (BiomeItem) -> Unit,
    onOpenPanorama: (String, PanoramaSpec?) -> Unit,
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
            RemoteBiomeImage(item = item, height = 190.dp)
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
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { onPreview(item) }) {
                    Text("Preview")
                }
                OutlinedButton(onClick = { onOpenPanorama(item.name, item.panorama) }) {
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
private fun RemoteBiomeImage(item: BiomeItem, height: Dp) {
    AsyncImage(
        model = item.imageUrl,
        contentDescription = item.name,
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
    onDismiss: () -> Unit,
    onOpenBrowser: () -> Unit,
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
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    text = "This screen uses the live catalog image. You can jump into the 360 panorama or open the original gallery page from here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = onOpenPanorama) {
                        Text("Open 360")
                    }
                    OutlinedButton(onClick = onOpenBrowser) {
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
