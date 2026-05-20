package com.biomeshop.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
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
    val listState = rememberLazyListState()
    val density = LocalDensity.current

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
    var isRefreshing by remember { mutableStateOf(false) }
    var pullDistance by remember { mutableStateOf(0f) }

    val refreshThresholdPx = with(density) { 96.dp.toPx() }
    val maxPullPx = with(density) { 168.dp.toPx() }

    LaunchedEffect(repository, assetRepository, refreshTick) {
        screenState = screenState.copy(isLoading = true)
        val result = repository.loadCatalog(forceRefresh = refreshTick > 0)
        biomeAssets = assetRepository.inspectCatalog(result.catalog)
        screenState = CatalogScreenState(
            catalog = result.catalog,
            isLoading = false,
        )
        isRefreshing = false
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
        if (isRefreshing) return
        isRefreshing = true
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

    val pullRefreshConnection = remember(listState, isRefreshing, refreshThresholdPx, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pullDistance > 0f) {
                    val newDistance = (pullDistance + available.y).coerceAtLeast(0f)
                    val consumed = newDistance - pullDistance
                    pullDistance = newDistance
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y > 0 && listState.isAtTop() && !isRefreshing) {
                    val dragAmount = available.y * 0.45f
                    pullDistance = (pullDistance + dragAmount).coerceAtMost(maxPullPx)
                    return Offset(0f, dragAmount)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance >= refreshThresholdPx && !isRefreshing) {
                    refreshCatalog()
                }
                pullDistance = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }

    val pullProgress = (pullDistance / refreshThresholdPx).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = Night) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshConnection),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
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

                PullRefreshIndicator(
                    progress = pullProgress,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                )
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
    val rotatingLines = remember {
        listOf(
            "Browse available Biomeshop biomes offline and pick land that fits your next build.",
            "Choose rare untouched biomes available now for your future build plans.",
            "Purely untouched, untainted, naturally generated biomes ready for your world.",
        )
    }
    var activeLineIndex by remember { mutableStateOf(0) }

    LaunchedEffect(rotatingLines) {
        while (true) {
            delay(5000)
            activeLineIndex = (activeLineIndex + 1) % rotatingLines.size
        }
    }

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
                            .background(Color(0x22140F20)),
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home menu", tint = TextMuted.copy(alpha = 0.76f))
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
                Crossfade(targetState = if (isLoading) -1 else activeLineIndex, label = "heroMessage") { target ->
                    Text(
                        text = if (target == -1) {
                            "Checking for changes from the live biome catalog."
                        } else {
                            rotatingLines[target]
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatBox("Biomes", if (biomeCount >= 10) "10+" else biomeCount.toString(), AccentGold)
                    StatBox("Available", availableCount.toString(), StatusLive)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "OWNER",
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentPink,
                    )
                    Text(
                        text = catalog.ownerName
                            .takeUnless { it.equals("BiomeShop", ignoreCase = true) }
                            ?.ifBlank { "orcMaster" }
                            ?: "orcMaster",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
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
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
) {
    FullScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RouteTopBar(title = "Settings", onBack = onDismiss)
            Text(
                text = "Files and storage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
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
private fun PullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val visible = isRefreshing || progress > 0f
    val containerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "pullRefreshAlpha",
    )
    val visualProgress = if (isRefreshing) 0.22f else progress.coerceIn(0.08f, 0.95f)

    if (!visible) return

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF29243A).copy(alpha = containerAlpha))
            .border(width = 1.dp, color = Color(0xFF4A445F).copy(alpha = containerAlpha), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = visualProgress,
            modifier = Modifier.size(28.dp),
            color = Color(0xFFE4DFF7),
            strokeWidth = 3.dp,
        )
    }
}

private fun LazyListState.isAtTop(): Boolean =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
