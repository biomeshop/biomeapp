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

data class BiomeItem(
    val id: String,
    val types: List<String>,
    val name: String,
    val priceLabel: String,
    val priceValue: Double,
    val status: Availability,
    val description: String,
    val imageKey: String,
    val featured: Boolean = false,
)

object BiomeCatalog {
    const val baseUrl = "https://biomeshop.github.io/home"
    const val heroImageUrl = "$baseUrl/assets/biomeshop-hero.png"

    val biomeOptions = listOf(
        "all" to "All biomes",
        "hybrid" to "Hybrid",
        "mushroom" to "Mushroom",
        "jungle" to "Jungle",
        "mountain" to "Mountain",
        "badlands" to "Badlands",
        "desert" to "Desert",
        "cherry" to "Cherry",
        "pale" to "Pale",
        "plains" to "Plains",
        "mangrove" to "Mangrove",
    )

    val items = listOf(
        BiomeItem("1", listOf("mushroom"), "Medium Mushroom Island", "$7m", 7.0, Availability.Sold, "A well-sized island, perfect for starting your base.", "panorama_1.png"),
        BiomeItem("2", listOf("mushroom"), "Medium Mushroom Island", "$8m", 8.0, Availability.Available, "A mushroom shaped like a coiled serpent, waiting for you.", "panorama_2.png"),
        BiomeItem("3", listOf("mushroom"), "Large Mushroom Island", "$19m", 19.0, Availability.Available, "Do not underestimate its massive scale.", "panorama_3.png"),
        BiomeItem("4", listOf("mushroom"), "Super Large Mushroom Island", "$23m", 23.0, Availability.Available, "Oh godfather, what a size-could this be the largest?", "panorama_4.png"),
        BiomeItem("5", listOf("mushroom"), "Smallest Mushroom Island", "$12m", 12.0, Availability.Available, "A rare natural generation, seen only once in a lifetime.", "panorama_5.png"),
        BiomeItem("6", listOf("mushroom"), "Mushroom Behind Spawn", "$25m", 25.0, Availability.Available, "The Mother of All Mushrooms rises before you.", "panorama_6.png", featured = true),
        BiomeItem("7", listOf("hybrid", "jungle", "plains"), "Biome Blend Isle", "$4m", 4.0, Availability.Available, "Born in a sacred and unusual way-claim me.", "panorama_7.png"),
        BiomeItem("8", listOf("badlands"), "Crimson Heart Badlands", "$3m", 3.0, Availability.Sold, "I stand at the center of temptation itself.", "panorama_8.png"),
        BiomeItem("9", listOf("mountain"), "Icewraith Hollow", "$4m", 4.0, Availability.Available, "The peak of defense-build your fortress upon me.", "panorama_9.png"),
        BiomeItem("10", listOf("mountain"), "Frostbound Crown", "$4m", 4.0, Availability.Sold, "Once a king, now a cursed crown-shaped mountain.", "panorama_10.png"),
        BiomeItem("11", listOf("mushroom"), "Small Mushroom Island", "$2.5m", 2.5, Availability.Sold, "Small, adorable, and waiting to be claimed by one.", "visual-soon.png"),
        BiomeItem("12", listOf("mushroom"), "Medium Mushroom Island", "$9.5m", 9.5, Availability.Sold, "Hmmmmmmmmmmmmmmmmmm...", "panorama_12.png"),
        BiomeItem("13", listOf("cherry"), "Large Natural Cherry Grove", "$7.9m", 7.9, Availability.Sold, "I was the greatest of them all - legendary tier.", "panorama_13.png"),
        BiomeItem("14", listOf("hybrid", "mangrove", "jungle"), "Large Natural Mangrove Grove", "$9.5m", 9.5, Availability.Available, "The witches' bane-forge your cavern within me.", "panorama_14.png"),
        BiomeItem("15", listOf("pale"), "Large Mother Pale Biome", "$3.4m", 3.4, Availability.Sold, "The holy land of death, only the worthy may claim me.", "panorama_15.png"),
        BiomeItem("16", listOf("pale"), "Circular Hollow Pale Biome", "$4.8m", 4.8, Availability.Sold, "I am the child of death itself.", "panorama_16.png"),
        BiomeItem("17", listOf("hybrid", "mangrove", "jungle"), "Largest Mangrove Near Border", "$8.6m", 8.6, Availability.Available, "Come to the far west, my child.", "panorama_17.png"),
        BiomeItem("18", listOf("hybrid", "jungle", "desert", "plains"), "Cool Donut Island", "$2m", 2.0, Availability.Sold, "I was once a donut in a past life.", "panorama_18.png"),
        BiomeItem("19", listOf("jungle"), "Isolated Lonely Jungle", "$4m", 4.0, Availability.Sold, "I am alone... I need friends... *sniff*.", "panorama_19.png"),
        BiomeItem("20", listOf("hybrid", "jungle", "plains"), "Mirror Mounds Islands", "$4m", 4.0, Availability.Available, "Do I look like something beyond legend?", "panorama_20.png"),
        BiomeItem("21", listOf("hybrid", "desert", "badlands"), "Secluded Geode Desert", "$5m", 5.0, Availability.Sold, "Only the one who conquered the sun may claim me.", "panorama_21.png"),
    )

    fun imageUrl(imageKey: String): String = "$baseUrl/assets/$imageKey"
    fun galleryUrl(itemId: String): String = "$baseUrl/images/?id=$itemId"
}
