package com.biomeshop.app.data

enum class Availability {
    Available,
    Sold,
}

enum class PriceOrder {
    Default,
    LowToHigh,
    HighToLow,
}

data class FilterOption(
    val value: String,
    val label: String,
)

data class PanoramaSpec(
    val imageUrl: String,
    val fullWidth: Int,
    val fullHeight: Int,
    val croppedWidth: Int,
    val croppedHeight: Int,
    val croppedX: Int,
    val croppedY: Int,
)

data class GalleryImage(
    val filename: String,
    val path: String = "",
    val url: String,
)

data class BiomeItem(
    val id: String,
    val types: List<String>,
    val name: String,
    val priceLabel: String,
    val priceValue: Double,
    val status: Availability,
    val description: String,
    val previewImageUrl: String,
    val featured: Boolean = false,
    val panorama: PanoramaSpec? = null,
    val panoramaImageUrl: String = previewImageUrl,
    val galleryFolderUrl: String = "",
    val galleryImages: List<GalleryImage> = emptyList(),
    val revision: String = "",
)

data class BiomeCatalogData(
    val siteUrl: String,
    val sourceOfTruth: String,
    val ownerName: String,
    val heroImageUrl: String,
    val dataSourceLabel: String,
    val biomeOptions: List<FilterOption>,
    val mainPanorama: PanoramaSpec?,
    val items: List<BiomeItem>,
)

data class BiomeVersionData(
    val schemaVersion: Int = 1,
    val sourceOfTruth: String = "",
    val siteBaseUrl: String = BiomeCatalogDefaults.siteBaseUrl,
    val contentHash: String = "",
    val biomeCount: Int = 0,
    val biomes: List<BiomeVersionEntry> = emptyList(),
)

data class BiomeVersionEntry(
    val id: String,
    val revision: String,
    val status: String,
    val featured: Boolean,
)

object BiomeCatalogDefaults {
    const val siteBaseUrl = "https://biomeshop.github.io/home/"
    const val remoteVersionUrl = "${siteBaseUrl}data/biome-version.json"
    const val remoteCatalogUrl = "${siteBaseUrl}data/biome.json"
    const val ownerName = "orcMaster"

    private val defaultBiomeOptions = listOf(
        FilterOption("all", "All biomes"),
        FilterOption("hybrid", "Hybrid"),
        FilterOption("mushroom", "Mushroom"),
        FilterOption("jungle", "Jungle"),
        FilterOption("mountain", "Mountain"),
        FilterOption("badlands", "Badlands"),
        FilterOption("desert", "Desert"),
        FilterOption("cherry", "Cherry"),
        FilterOption("pale", "Pale"),
        FilterOption("plains", "Plains"),
        FilterOption("mangrove", "Mangrove"),
    )

    private fun panorama(imageId: Int): PanoramaSpec = PanoramaSpec(
        imageUrl = "${siteBaseUrl}assets/panorama_$imageId.png",
        fullWidth = 6985,
        fullHeight = 3493,
        croppedWidth = 6985,
        croppedHeight = 2580,
        croppedX = 0,
        croppedY = 457,
    )

    val data = BiomeCatalogData(
        siteUrl = siteBaseUrl,
        sourceOfTruth = "bundled-fallback",
        ownerName = ownerName,
        heroImageUrl = "${siteBaseUrl}assets/biomeshop-hero.png",
        dataSourceLabel = "Bundled fallback catalog",
        biomeOptions = defaultBiomeOptions,
        mainPanorama = panorama(0),
        items = listOf(
            BiomeItem("1", listOf("mushroom"), "Medium Mushroom Island", "$7m", 7.0, Availability.Sold, "A well-sized island, perfect for starting your base.", "${siteBaseUrl}assets/panorama_1.png", panorama = panorama(1)),
            BiomeItem("2", listOf("mushroom"), "Medium Mushroom Island", "$8m", 8.0, Availability.Available, "A mushroom shaped like a coiled serpent, waiting for you.", "${siteBaseUrl}assets/panorama_2.png", panorama = panorama(2)),
            BiomeItem("3", listOf("mushroom"), "Large Mushroom Island", "$19m", 19.0, Availability.Available, "Do not underestimate its massive scale.", "${siteBaseUrl}assets/panorama_3.png", panorama = panorama(3)),
            BiomeItem("4", listOf("mushroom"), "Super Large Mushroom Island", "$23m", 23.0, Availability.Available, "Oh godfather, what a size-could this be the largest?", "${siteBaseUrl}assets/panorama_4.png", panorama = panorama(4)),
            BiomeItem("5", listOf("mushroom"), "Smallest Mushroom Island", "$12m", 12.0, Availability.Available, "A rare natural generation, seen only once in a lifetime.", "${siteBaseUrl}assets/panorama_5.png", panorama = panorama(5)),
            BiomeItem("6", listOf("mushroom"), "Mushroom Behind Spawn", "$25m", 25.0, Availability.Available, "The Mother of All Mushrooms rises before you.", "${siteBaseUrl}assets/panorama_6.png", featured = true, panorama = panorama(6)),
            BiomeItem("7", listOf("hybrid", "jungle", "plains"), "Biome Blend Isle", "$4m", 4.0, Availability.Available, "Born in a sacred and unusual way-claim me.", "${siteBaseUrl}assets/panorama_7.png", panorama = panorama(7)),
            BiomeItem("8", listOf("badlands"), "Crimson Heart Badlands", "$3m", 3.0, Availability.Sold, "I stand at the center of temptation itself.", "${siteBaseUrl}assets/panorama_8.png", panorama = panorama(8)),
            BiomeItem("9", listOf("mountain"), "Icewraith Hollow", "$4m", 4.0, Availability.Available, "The peak of defense-build your fortress upon me.", "${siteBaseUrl}assets/panorama_9.png", panorama = panorama(9)),
            BiomeItem("10", listOf("mountain"), "Frostbound Crown", "$4m", 4.0, Availability.Sold, "Once a king, now a cursed crown-shaped mountain.", "${siteBaseUrl}assets/panorama_10.png", panorama = panorama(10)),
            BiomeItem("11", listOf("mushroom"), "Small Mushroom Island", "$2.5m", 2.5, Availability.Sold, "Small, adorable, and waiting to be claimed by one.", "${siteBaseUrl}assets/visual-soon.png"),
            BiomeItem("12", listOf("mushroom"), "Medium Mushroom Island", "$9.5m", 9.5, Availability.Sold, "Hmmmmmmmmmmmmmmmmmm...", "${siteBaseUrl}assets/panorama_12.png", panorama = panorama(12)),
            BiomeItem("13", listOf("cherry"), "Large Natural Cherry Grove", "$7.9m", 7.9, Availability.Sold, "I was the greatest of them all - legendary tier.", "${siteBaseUrl}assets/panorama_13.png", panorama = panorama(13)),
            BiomeItem("14", listOf("hybrid", "mangrove", "jungle"), "Large Natural Mangrove Grove", "$9.5m", 9.5, Availability.Available, "The witches' bane-forge your cavern within me.", "${siteBaseUrl}assets/panorama_14.png", panorama = panorama(14)),
            BiomeItem("15", listOf("pale"), "Large Mother Pale Biome", "$3.4m", 3.4, Availability.Sold, "The holy land of death, only the worthy may claim me.", "${siteBaseUrl}assets/panorama_15.png", panorama = panorama(15)),
            BiomeItem("16", listOf("pale"), "Circular Hollow Pale Biome", "$4.8m", 4.8, Availability.Sold, "I am the child of death itself.", "${siteBaseUrl}assets/panorama_16.png", panorama = panorama(16)),
            BiomeItem("17", listOf("hybrid", "mangrove", "jungle"), "Largest Mangrove Near Border", "$8.6m", 8.6, Availability.Available, "Come to the far west, my child.", "${siteBaseUrl}assets/panorama_17.png", panorama = panorama(17)),
            BiomeItem("18", listOf("hybrid", "jungle", "desert", "plains"), "Cool Donut Island", "$2m", 2.0, Availability.Sold, "I was once a donut in a past life.", "${siteBaseUrl}assets/panorama_18.png", panorama = panorama(18)),
            BiomeItem("19", listOf("jungle"), "Isolated Lonely Jungle", "$4m", 4.0, Availability.Sold, "I am alone... I need friends... *sniff*.", "${siteBaseUrl}assets/panorama_19.png", panorama = panorama(19)),
            BiomeItem("20", listOf("hybrid", "jungle", "plains"), "Mirror Mounds Islands", "$4m", 4.0, Availability.Available, "Do I look like something beyond legend?", "${siteBaseUrl}assets/panorama_20.png", panorama = panorama(20)),
            BiomeItem("21", listOf("hybrid", "desert", "badlands"), "Secluded Geode Desert", "$5m", 5.0, Availability.Sold, "Only the one who conquered the sun may claim me.", "${siteBaseUrl}assets/panorama_21.png", panorama = panorama(21)),
        ),
    )
}
