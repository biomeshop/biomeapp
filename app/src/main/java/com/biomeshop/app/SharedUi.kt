package com.biomeshop.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.biomeshop.app.data.Availability
import com.biomeshop.app.data.BiomeItem
import com.biomeshop.app.ui.theme.AccentGold
import com.biomeshop.app.ui.theme.NightDeep
import com.biomeshop.app.ui.theme.StatusLive
import com.biomeshop.app.ui.theme.StatusSold

@Composable
internal fun FullScreenSurface(
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
internal fun RouteTopBar(
    title: String,
    onClose: () -> Unit,
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
        TextButton(onClick = onClose) {
            Text("Close")
        }
    }
}

@Composable
internal fun OfflinePill(text: String) {
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
internal fun SkeletonImage(
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

    Box(modifier = modifier.background(brush))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TypeChipRow(types: List<String>) {
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
internal fun RemoteBiomeImage(
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
internal fun PriceText(price: String) {
    Text(
        text = price,
        style = MaterialTheme.typography.titleLarge,
        color = AccentGold,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun StatusChip(status: Availability) {
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
internal fun AssetPlaceholder(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OfflinePill(text = label)
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
}

@Composable
internal fun DetailActions(
    item: BiomeItem,
    onOpenPanorama: (BiomeItem) -> Unit,
    onOpenGallery: (BiomeItem) -> Unit,
) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = { onOpenPanorama(item) }) {
            Text("Open 360")
        }
        OutlinedButton(onClick = { onOpenGallery(item) }) {
            Text("Open gallery")
        }
    }
}

@Composable
internal fun rememberConnectivityState(): Boolean {
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

internal fun assetStatusLabel(
    isOnline: Boolean,
    hasLocalAsset: Boolean,
    hasRemoteAsset: Boolean,
    fallbackMissingLabel: String,
): String = when {
    hasLocalAsset -> ""
    !isOnline -> "Currently offline"
    !hasRemoteAsset -> fallbackMissingLabel
    else -> "Loading content"
}
