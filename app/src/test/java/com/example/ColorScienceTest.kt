package com.example

import com.example.color.ColorScience
import com.example.color.RgbColor
import com.example.model.BuiltInCharts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorScienceTest {

    @Test
    fun testRgbToLabD65() {
        // Pure white (255, 255, 255) in D65 should have L* ~ 100, a* ~ 0, b* ~ 0
        val whiteLab = ColorScience.rgbToLab(RgbColor(255, 255, 255))
        assertEquals(100.0f, whiteLab.l, 0.5f)
        assertEquals(0.0f, whiteLab.a, 0.5f)
        assertEquals(0.0f, whiteLab.b, 0.5f)

        // Pure black (0, 0, 0) should have L* ~ 0
        val blackLab = ColorScience.rgbToLab(RgbColor(0, 0, 0))
        assertEquals(0.0f, blackLab.l, 0.5f)
    }

    @Test
    fun testPrototypeChartExactMatch() {
        val chart = BuiltInCharts.PROTOTYPE_WIDE.toRgbMap()

        // Test exact RGB for pH 7.0: (215, 216, 60)
        val result7 = ColorScience.estimatePh(RgbColor(215, 216, 60), chart)
        assertEquals(7.0f, result7.bestMatchPh, 0.01f)
        assertEquals(0.0f, result7.bestMatchDeltaE, 0.01f)

        // Test exact RGB for pH 4.0: (237, 28, 36)
        val result4 = ColorScience.estimatePh(RgbColor(237, 28, 36), chart)
        assertEquals(4.0f, result4.bestMatchPh, 0.01f)
        assertEquals(0.0f, result4.bestMatchDeltaE, 0.01f)
    }

    @Test
    fun testInterpolationBetweenPoints() {
        val chart = BuiltInCharts.PROTOTYPE_WIDE.toRgbMap()

        // Midpoint between pH 6.0 (250, 166, 26) and pH 6.5 (255, 210, 60)
        val midRgb = RgbColor((250 + 255) / 2, (166 + 210) / 2, (26 + 60) / 2)
        val result = ColorScience.estimatePh(midRgb, chart)

        // Interpolated pH should lie between 6.0 and 6.5
        assertTrue(result.interpolatedPh in 5.9f..6.6f)
    }

    @Test
    fun testWhiteBalanceCorrection() {
        // Under warm incandescent lighting (e.g., white paper looks yellowish (255, 240, 200))
        val warmWhite = RgbColor(255, 240, 200)
        val sample = RgbColor(200, 190, 150)
        val corrected = ColorScience.applyWhiteBalance(sample, warmWhite)

        // Blue channel should be boosted to compensate for warm lighting
        assertTrue(corrected.b > sample.b)
    }
}
