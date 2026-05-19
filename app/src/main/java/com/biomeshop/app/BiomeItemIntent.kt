package com.biomeshop.app

import android.content.Intent
import com.biomeshop.app.data.BiomeItem
import com.google.gson.Gson

internal const val EXTRA_BIOME_ITEM = "biomeItem"

internal fun Intent.putBiomeItem(item: BiomeItem): Intent =
    putExtra(EXTRA_BIOME_ITEM, Gson().toJson(item))

internal fun Intent.biomeItemOrNull(): BiomeItem? {
    val json = getStringExtra(EXTRA_BIOME_ITEM) ?: return null
    return runCatching { Gson().fromJson(json, BiomeItem::class.java) }.getOrNull()
}
