package com.example.model

import com.example.color.RgbColor

data class ReferencePoint(
    val ph: Float,
    val rgb: RgbColor,
    val label: String = ""
)

data class PhChart(
    val id: String,
    val name: String,
    val description: String,
    val minPh: Float,
    val maxPh: Float,
    val points: List<ReferencePoint>,
    val isCustom: Boolean = false
) {
    fun toRgbMap(): Map<Float, RgbColor> = points.associate { it.ph to it.rgb }
}

object BuiltInCharts {
    /**
     * Exact reference chart from the Python prototype script in the user's artifact.
     */
    val PROTOTYPE_WIDE = PhChart(
        id = "prototype_wide",
        name = "Prototype Reference (4.0–10.0)",
        description = "Calibrated key from the original prototype algorithm for standard laboratory strips.",
        minPh = 4.0f,
        maxPh = 10.0f,
        points = listOf(
            ReferencePoint(4.0f, RgbColor(237, 28, 36), "Red / Strong Acid"),
            ReferencePoint(5.0f, RgbColor(241, 101, 34), "Red-Orange"),
            ReferencePoint(6.0f, RgbColor(250, 166, 26), "Orange"),
            ReferencePoint(6.5f, RgbColor(255, 210, 60), "Yellow-Orange"),
            ReferencePoint(7.0f, RgbColor(215, 216, 60), "Yellow-Green / Neutral"),
            ReferencePoint(7.5f, RgbColor(150, 200, 80), "Light Green"),
            ReferencePoint(8.0f, RgbColor(80, 175, 100), "Green-Teal"),
            ReferencePoint(9.0f, RgbColor(30, 140, 140), "Teal / Weak Base"),
            ReferencePoint(10.0f, RgbColor(25, 90, 160), "Deep Blue / Strong Base")
        )
    )

    /**
     * Standard universal 1-14 full range litmus indicator.
     */
    val UNIVERSAL_FULL = PhChart(
        id = "universal_full",
        name = "Universal Litmus (1.0–14.0)",
        description = "Comprehensive 1–14 full spectrum indicator for general chemical solutions.",
        minPh = 1.0f,
        maxPh = 14.0f,
        points = listOf(
            ReferencePoint(1.0f, RgbColor(228, 26, 28), "Battery Acid"),
            ReferencePoint(2.0f, RgbColor(238, 55, 34), "Lemon Juice"),
            ReferencePoint(3.0f, RgbColor(245, 95, 30), "Vinegar"),
            ReferencePoint(4.0f, RgbColor(251, 140, 35), "Tomato Juice"),
            ReferencePoint(5.0f, RgbColor(255, 195, 45), "Black Coffee"),
            ReferencePoint(6.0f, RgbColor(245, 228, 55), "Milk / Rain"),
            ReferencePoint(7.0f, RgbColor(120, 205, 75), "Pure Water (Neutral)"),
            ReferencePoint(8.0f, RgbColor(45, 185, 120), "Seawater / Pool"),
            ReferencePoint(9.0f, RgbColor(30, 160, 180), "Baking Soda"),
            ReferencePoint(10.0f, RgbColor(35, 125, 210), "Antacid Tablet"),
            ReferencePoint(11.0f, RgbColor(50, 85, 195), "Household Ammonia"),
            ReferencePoint(12.0f, RgbColor(80, 55, 175), "Soapy Water"),
            ReferencePoint(13.0f, RgbColor(105, 40, 150), "Bleach"),
            ReferencePoint(14.0f, RgbColor(120, 25, 125), "Drain Cleaner")
        )
    )

    /**
     * Drinking water, aquariums, and swimming pool test strips.
     */
    val WATER_POOL = PhChart(
        id = "water_pool",
        name = "Water & Pool Range (6.2–8.4)",
        description = "High precision narrow band for potable tap water, koi ponds, aquariums, and pools.",
        minPh = 6.2f,
        maxPh = 8.4f,
        points = listOf(
            ReferencePoint(6.2f, RgbColor(255, 230, 80), "Acidic Water"),
            ReferencePoint(6.8f, RgbColor(225, 215, 68), "Soft Tap"),
            ReferencePoint(7.2f, RgbColor(165, 212, 85), "Ideal Pool / Tank"),
            ReferencePoint(7.6f, RgbColor(95, 198, 115), "Ideal Drinking"),
            ReferencePoint(8.0f, RgbColor(50, 168, 145), "Hard Water"),
            ReferencePoint(8.4f, RgbColor(35, 135, 180), "Alkaline Water")
        )
    )

    /**
     * Soil & agricultural testing.
     */
    val SOIL_GARDEN = PhChart(
        id = "soil_garden",
        name = "Soil & Garden (4.5–8.5)",
        description = "Optimized for soil testing kits, hydroponics, compost, and plant care.",
        minPh = 4.5f,
        maxPh = 8.5f,
        points = listOf(
            ReferencePoint(4.5f, RgbColor(245, 110, 35), "Acidic (Blueberries)"),
            ReferencePoint(5.5f, RgbColor(255, 198, 50), "Moderate Acid (Potatoes)"),
            ReferencePoint(6.0f, RgbColor(235, 220, 65), "Slightly Acid (Tomatoes)"),
            ReferencePoint(6.5f, RgbColor(185, 215, 75), "Near Neutral (Vegetables)"),
            ReferencePoint(7.0f, RgbColor(130, 205, 85), "Neutral"),
            ReferencePoint(7.5f, RgbColor(85, 190, 110), "Slightly Alkaline"),
            ReferencePoint(8.5f, RgbColor(35, 140, 165), "Alkaline Soil (Chalky)")
        )
    )

    val ALL = listOf(PROTOTYPE_WIDE, UNIVERSAL_FULL, WATER_POOL, SOIL_GARDEN)
}
