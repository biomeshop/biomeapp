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

data class BiomeItem(
    val id: String,
    val types: List<String>,
    val name: String,
    val priceLabel: String,
    val priceValue: Double,
    val status: Availability,
    val description: String,
    val imageUrl: String,
    val featured: Boolean = false,
    val panorama: PanoramaSpec? = null,
)

data class BiomeCatalogData(
    val siteUrl: String,
    val ownerName: String,
    val heroImageUrl: String,
    val galleryBaseUrl: String,
    val dataSourceLabel: String,
    val biomeOptions: List<FilterOption>,
    val mainPanorama: PanoramaSpec?,
    val items: List<BiomeItem>,
) {
    fun galleryUrl(itemId: String): String = "$galleryBaseUrl$itemId"
}

object BiomeCatalogDefaults {
    const val remoteCatalogUrl = "https://raw.githubusercontent.com/biomeshop/biomeapp/main/app-data/biomeshop-data.json"

    private fun panorama(imageId: Int): PanoramaSpec = PanoramaSpec(
        imageUrl = "https://biomeshop.github.io/home/assets/panorama_$imageId.png",
        fullWidth = 6985,
        fullHeight = 3493,
        croppedWidth = 6985,
        croppedHeight = 2580,
        croppedX = 0,
        croppedY = 457,
    )

    val data = BiomeCatalogData(
        siteUrl = "https://biomeshop.github.io/home/",
        ownerName = "orcMaster",
        heroImageUrl = "https://biomeshop.github.io/home/assets/biomeshop-hero.png",
        galleryBaseUrl = "https://biomeshop.github.io/home/images/?id=",
        dataSourceLabel = "Bundled fallback catalog",
        biomeOptions = listOf(
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
        ),
        mainPanorama = panorama(0),
        items = listOf(
            BiomeItem("1", listOf("mushroom"), "Medium Mushroom Island", "$7m", 7.0, Availability.Sold, "A well-sized island, perfect for starting your base.", "https://biomeshop.github.io/home/assets/panorama_1.png", panorama = panorama(1)),
            BiomeItem("2", listOf("mushroom"), "Medium Mushroom Island", "$8m", 8.0, Availability.Available, "A mushroom shaped like a coiled serpent, waiting for you.", "https://biomeshop.github.io/home/assets/panorama_2.png", panorama = panorama(2)),
            BiomeItem("3", listOf("mushroom"), "Large Mushroom Island", "$19m", 19.0, Availability.Available, "Do not underestimate its massive scale.", "https://biomeshop.github.io/home/assets/panorama_3.png", panorama = panorama(3)),
            BiomeItem("4", listOf("mushroom"), "Super Large Mushroom Island", "$23m", 23.0, Availability.Available, "Oh godfather, what a size-could this be the largest?", "https://biomeshop.github.io/home/assets/panorama_4.png", panorama = panorama(4)),
            BiomeItem("5", listOf("mushroom"), "Smallest Mushroom Island", "$12m", 12.0, Availability.Available, "A rare natural generation, seen only once in a lifetime.", "https://biomeshop.github.io/home/assets/panorama_5.png", panorama = panorama(5)),
            BiomeItem("6", listOf("mushroom"), "Mushroom Behind Spawn", "$25m", 25.0, Availability.Available, "The Mother of All Mushrooms rises before you.", "https://biomeshop.github.io/home/assets/panorama_6.png", featured = true, panorama = panorama(6)),
            BiomeItem("7", listOf("hybrid", "jungle", "plains"), "Biome Blend Isle", "$4m", 4.0, Availability.Available, "Born in a sacred and unusual way-claim me.", "https://biomeshop.github.io/home/assets/panorama_7.png", panorama = panorama(7)),
            BiomeItem("8", listOf("badlands"), "Crimson Heart Badlands", "$3m", 3.0, Availability.Sold, "I stand at the center of temptation itself.", "https://biomeshop.github.io/home/assets/panorama_8.png", panorama = panorama(8)),
            BiomeItem("9", listOf("mountain"), "Icewraith Hollow", "$4m", 4.0, Availability.Available, "The peak of defense-build your fortress upon me.", "https://biomeshop.github.io/home/assets/panorama_9.png", panorama = panorama(9)),
            BiomeItem("10", listOf("mountain"), "Frostbound Crown", "$4m", 4.0, Availability.Sold, "Once a king, now a cursed crown-shaped mountain.", "https://biomeshop.github.io/home/assets/panorama_10.png", panorama = panorama(10)),
            BiomeItem("11", listOf("mushroom"), "Small Mushroom Island", "$2.5m", 2.5, Availability.Sold, "Small, adorable, and waiting to be claimed by one.", "https://biomeshop.github.io/home/assets/visual-soon.png"),
            BiomeItem("12", listOf("mushroom"), "Medium Mushroom Island", "$9.5m", 9.5, Availability.Sold, "Hmmmmmmmmmmmmmmmmmm...", "https://biomeshop.github.io/home/assets/panorama_12.png", panorama = panorama(12)),
            BiomeItem("13", listOf("cherry"), "Large Natural Cherry Grove", "$7.9m", 7.9, Availability.Sold, "I was the greatest of them all - legendary tier.", "https://biomeshop.github.io/home/assets/panorama_13.png", panorama = panorama(13)),
            BiomeItem("14", listOf("hybrid", "mangrove", "jungle"), "Large Natural Mangrove Grove", "$9.5m", 9.5, Availability.Available, "The witches' bane-forge your cavern within me.", "https://biomeshop.github.io/home/assets/panorama_14.png", panorama = panorama(14)),
            BiomeItem("15", listOf("pale"), "Large Mother Pale Biome", "$3.4m", 3.4, Availability.Sold, "The holy land of death, only the worthy may claim me.", "https://biomeshop.github.io/home/assets/panorama_15.png", panorama = panorama(15)),
            BiomeItem("16", listOf("pale"), "Circular Hollow Pale Biome", "$4.8m", 4.8, Availability.Sold, "I am the child of death itself.", "https://biomeshop.github.io/home/assets/panorama_16.png", panorama = panorama(16)),
            BiomeItem("17", listOf("hybrid", "mangrove", "jungle"), "Largest Mangrove Near Border", "$8.6m", 8.6, Availability.Available, "Come to the far west, my child.", "https://biomeshop.github.io/home/assets/panorama_17.png", panorama = panorama(17)),
            BiomeItem("18", listOf("hybrid", "jungle", "desert", "plains"), "Cool Donut Island", "$2m", 2.0, Availability.Sold, "I was once a donut in a past life.", "https://biomeshop.github.io/home/assets/panorama_18.png", panorama = panorama(18)),
            BiomeItem("19", listOf("jungle"), "Isolated Lonely Jungle", "$4m", 4.0, Availability.Sold, "I am alone... I need friends... *sniff*.", "https://biomeshop.github.io/home/assets/panorama_19.png", panorama = panorama(19)),
            BiomeItem("20", listOf("hybrid", "jungle", "plains"), "Mirror Mounds Islands", "$4m", 4.0, Availability.Available, "Do I look like something beyond legend?", "https://biomeshop.github.io/home/assets/panorama_20.png", panorama = panorama(20)),
            BiomeItem("21", listOf("hybrid", "desert", "badlands"), "Secluded Geode Desert", "$5m", 5.0, Availability.Sold, "Only the one who conquered the sun may claim me.", "https://biomeshop.github.io/home/assets/panorama_21.png", panorama = panorama(21)),
        ),
    )
}
