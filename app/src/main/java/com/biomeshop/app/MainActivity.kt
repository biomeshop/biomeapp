package com.biomeshop.app

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.biomeshop.app.data.Availability
import com.biomeshop.app.data.BiomeAssetRepository
import com.biomeshop.app.data.BiomeCatalogData
import com.biomeshop.app.data.BiomeCatalogDefaults
import com.biomeshop.app.data.BiomeCatalogRepository
import com.biomeshop.app.data.BiomeItem
import com.biomeshop.app.data.CachedBiomeAssets
import com.biomeshop.app.data.FilterOption
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
)

private data class ConnectivityBanner(
    val text: String,
    val online: Boolean,
)

private enum class MenuScreen {
    QuickMenu,
    Settings,
}

@Composable
private fun BiomeShopApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { BiomeCatalogRepository(context.applicationContext) }
    val assetRepository = remember(context) { BiomeAssetRepository(context.applicationContext) }
    val isOnline = rememberConnectivityState()

    var screenState by remember { mutableStateOf(CatalogScreenState()) }
    var biomeAssets by remember { mutableStateOf<Map<String, CachedBiomeAssets>>(emptyMap()) }
    var biomeFilter by rememberSaveable { mutableStateOf("all") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var priceOrder by rememberSaveable { mutableStateOf(PriceOrder.Default) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var activeMenuScreen by remember { mutableStateOf<MenuScreen?>(null) }
    var banner by remember { mutableStateOf<ConnectivityBanner?>(null) }
    var knownConnectivity by remember { mutableStateOf<Boolean?>(null) }
    var pendingNavigation by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(repository, assetRepository, refreshTick) {
        screenState = screenState.copy(isLoading = true)
        val result = repository.loadCatalog(forceRefresh = refreshTick > 0)
        biomeAssets = assetRepository.inspectCatalog(result.catalog)
        screenState = CatalogScreenState(
            catalog = result.catalog,
            isLoading = false,
        )
    }

    LaunchedEffect(isOnline) {
        if (knownConnectivity == null) {
            knownConnectivity = isOnline
            return@LaunchedEffect
        }
        if (knownConnectivity != isOnline) {
            knownConnectivity = isOnline
            banner = ConnectivityBanner(
                text = if (isOnline) "Now online" else "Currently offline",
                online = isOnline,
            )
            delay(3000)
            banner = null
        }
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
    val availableCount = remember(catalog) {
        catalog.items.count { it.status == Availability.Available }
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun launchLatestNavigation(action: () -> Unit) {
        pendingNavigation?.cancel()
        pendingNavigation = scope.launch {
            delay(150)
            action()
            pendingNavigation = null
        }
    }

    fun openDetails(item: BiomeItem) {
        launchLatestNavigation {
            context.startActivity(BiomeDetailActivity.intent(context, item))
        }
    }

    fun openPanorama(item: BiomeItem) {
        launchLatestNavigation {
            context.startActivity(PanoramaActivity.intent(context, item))
        }
    }

    fun openGallery(item: BiomeItem) {
        launchLatestNavigation {
            context.startActivity(BiomeGalleryActivity.intent(context, item))
        }
    }

    fun refreshCatalog() {
        refreshTick += 1
        activeMenuScreen = null
    }

    fun clearDownloadedImages() {
        scope.launch {
            assetRepository.clearDownloadedBiomes()
            biomeAssets = assetRepository.inspectCatalog(screenState.catalog)
            Toast.makeText(context, "Downloaded biome images cleared.", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllData() {
        scope.launch {
            assetRepository.clearAllCache()
            repository.clearMetadataCache()
            biomeAssets = emptyMap()
            refreshTick += 1
            activeMenuScreen = null
            Toast.makeText(context, "Catalog data and downloaded images cleared.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        biomeCount = catalog.items.size,
                        availableCount = availableCount,
                        onOpenUrl = ::openUrl,
                        onOpenMenu = { activeMenuScreen = MenuScreen.QuickMenu },
                    )
                }
                item {
                    FeaturedCard(
                        item = featured,
                        imageModel = assetRepository.previewModel(featured, biomeAssets[featured.id]),
                        onPreview = ::openDetails,
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
                        onPreview = ::openDetails,
                        onOpenGallery = ::openGallery,
                        onOpenPanorama = ::openPanorama,
                    )
                }
            }
        }

        ConnectivityStatusBanner(
            banner = banner,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
        )
        when (activeMenuScreen) {
            MenuScreen.QuickMenu -> {
                QuickMenuOverlay(
                    onDismiss = { activeMenuScreen = null },
                    onOpenSettings = { activeMenuScreen = MenuScreen.Settings },
                )
            }
            MenuScreen.Settings -> {
                SettingsScreen(
                    onDismiss = { activeMenuScreen = null },
                    onRefreshCatalog = ::refreshCatalog,
                    onClearCache = ::clearDownloadedImages,
                    onClearData = ::clearAllData,
                )
            }
            null -> Unit
        }
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
    biomeCount: Int,
    availableCount: Int,
    onOpenUrl: (String) -> Unit,
    onOpenMenu: () -> Unit,
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
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x44140F20)),
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home menu", tint = AccentGold)
                    }
                }

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
                    text = if (isLoading) {
                        "Checking for changes from the live biome catalog."
                    } else {
                        "Browse, inspect, and download biome details with a smoother offline-first flow."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatBox("Biomes", if (biomeCount >= 10) "10+" else biomeCount.toString(), AccentGold)
                    StatBox("Available", availableCount.toString(), StatusLive)
                }

                Button(onClick = { onOpenUrl(catalog.siteUrl) }) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open site")
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatBox(label: String, value: String, accent: Color) {
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
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent)
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
private fun ConnectivityStatusBanner(
    banner: ConnectivityBanner?,
    modifier: Modifier = Modifier,
) {
    val visible = banner != null
    val offsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (visible) 0.dp else (-72).dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "bannerOffset",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "bannerAlpha",
    )

    if (banner != null || alpha > 0.01f) {
        Row(
            modifier = modifier
                .offset(y = offsetY)
                .clip(RoundedCornerShape(22.dp))
                .background(if (banner?.online == true) StatusLive.copy(alpha = 0.22f) else StatusSold.copy(alpha = 0.22f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (banner?.online == true) StatusLive else StatusSold,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = banner?.text.orEmpty(),
                color = if (banner?.online == true) StatusLive else StatusSold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.alpha(alpha),
            )
        }
    }
}

@Composable
private fun PreviewScreen(
    item: BiomeItem,
    imageModel: String,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPanorama: () -> Unit,
) {
    FullScreenModal(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ModalTopBar(title = item.name, onDismiss = onDismiss)
            Box {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
                if (isSyncing) {
                    SkeletonImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                }
            }
            TypeChipRow(item.types)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriceText(item.priceLabel)
                StatusChip(item.status)
            }
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
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
                OutlinedButton(onClick = onOpenGallery) {
                    Text("Open gallery")
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(
    title: String,
    images: List<String>,
    showOfflinePlaceholder: Boolean,
    isOnline: Boolean,
    onDismiss: () -> Unit,
) {
    FullScreenModal(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModalTopBar(title = title, onDismiss = onDismiss)
            if (showOfflinePlaceholder) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OfflinePill(text = "Currently offline")
                    Spacer(modifier = Modifier.height(18.dp))
                    SkeletonImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SkeletonImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
            } else {
                if (!isOnline) {
                    OfflinePill(text = "Currently offline")
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(images) { image ->
                        AsyncImage(
                            model = image,
                            contentDescription = title,
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
}

@Composable
private fun QuickMenuOverlay(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings")
        }
    }
}

@Composable
private fun SettingsScreen(
    onDismiss: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
) {
    FullScreenModal(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ModalTopBar(title = "Settings", onDismiss = onDismiss)
            Text(
                text = "Refresh checks the home repo for changes. It does not force every biome image to download again.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
            )
            Button(onClick = onRefreshCatalog, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh catalog")
            }
            OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                Text("Clear cache")
            }
            OutlinedButton(onClick = onClearData, modifier = Modifier.fillMaxWidth()) {
                Text("Clear data")
            }
        }
    }
}

@Composable
private fun FullScreenModal(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightDeep),
    ) {
        content()
    }
}

@Composable
private fun ModalTopBar(
    title: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}

@Composable
private fun OfflinePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(StatusSold.copy(alpha = 0.24f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = StatusSold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SkeletonImage(
    modifier: Modifier = Modifier,
) {
    val shimmer = rememberInfiniteTransition(label = "skeleton")
    val progress by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonProgress",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1D1830),
            Color(0xFF2A2340),
            Color(0xFF1D1830),
        ),
        start = androidx.compose.ui.geometry.Offset(progress * 900f - 450f, 0f),
        end = androidx.compose.ui.geometry.Offset(progress * 900f, 450f),
    )

    Box(
        modifier = modifier.background(brush),
    )
}

@Composable
private fun rememberConnectivityState(): Boolean {
    val context = LocalContext.current
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var online by remember { mutableStateOf(connectivityManager.isOnline()) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = true
            }

            override fun onLost(network: Network) {
                online = connectivityManager.isOnline()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    return online
}

private fun ConnectivityManager.isOnline(): Boolean {
    val network = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
