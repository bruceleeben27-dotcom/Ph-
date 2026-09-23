package com.example.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CIE Lab color representation.
 * L*: Lightness (0 to 100)
 * a*: Green (-128) to Red (+127)
 * b*: Blue (-128) to Yellow (+127)
 */
data class LabColor(val l: Float, val a: Float, val b: Float)

data class RgbColor(val r: Int, val g: Int, val b: Int) {
    fun toComposeColor(): Color = Color(r, g, b)
    fun toHex(): String = String.format("#%02X%02X%02X", r, g, b)
}

data class MatchScore(
    val ph: Float,
    val refRgb: RgbColor,
    val refLab: LabColor,
    val deltaE: Float
)

data class PhEstimateResult(
    val sampledRgb: RgbColor,
    val sampledLab: LabColor,
    val bestMatchPh: Float,
    val bestMatchDeltaE: Float,
    val interpolatedPh: Float,
    val closestMatches: List<MatchScore>,
    val confidence: ConfidenceLevel,
    val category: PhCategory
)

enum class ConfidenceLevel(val label: String, val description: String) {
    HIGH("High Confidence", "Color matches reference chart closely (ΔE < 8.0)"),
    MEDIUM("Moderate Confidence", "Good match, slight lighting variance (ΔE 8.0–18.0)"),
    LOW("Low Confidence", "Color drifts from reference chart (ΔE > 18.0). Check lighting/white balance.")
}

enum class PhCategory(val label: String, val colorHex: String) {
    STRONGLY_ACIDIC("Strongly Acidic (0–3.0)", "#E53935"),
    WEAKLY_ACIDIC("Weakly Acidic (3.1–6.4)", "#FB8C00"),
    NEUTRAL("Neutral (6.5–7.5)", "#43A047"),
    WEAKLY_BASIC("Weakly Basic (7.6–10.0)", "#00ACC1"),
    STRONGLY_BASIC("Strongly Basic (10.1–14.0)", "#5E35B1")
}

object ColorScience {

    /**
     * Converts sRGB (0-255) to CIE L*a*b* using standard D65 illuminant.
     * Follows the exact color science formulation from the prototype.
     */
    fun rgbToLab(rgb: RgbColor): LabColor {
        val rNorm = rgb.r.coerceIn(0, 255) / 255.0f
        val gNorm = rgb.g.coerceIn(0, 255) / 255.0f
        val bNorm = rgb.b.coerceIn(0, 255) / 255.0f

        fun invGamma(c: Float): Float =
            if (c > 0.04045f) ((c + 0.055f) / 1.055f).pow(2.4f) else c / 12.92f

        val rLinear = invGamma(rNorm)
        val gLinear = invGamma(gNorm)
        val bLinear = invGamma(bNorm)

        // sRGB -> XYZ (D65 illuminant)
        val x = (rLinear * 0.4124f + gLinear * 0.3576f + bLinear * 0.1805f) / 0.95047f
        val y = (rLinear * 0.2126f + gLinear * 0.7152f + bLinear * 0.0722f) / 1.00000f
        val z = (rLinear * 0.0193f + gLinear * 0.1192f + bLinear * 0.9505f) / 1.08883f

        fun f(t: Float): Float =
            if (t > 0.008856f) t.pow(1.0f / 3.0f) else (7.787f * t) + (16.0f / 116.0f)

        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val l = (116.0f * fy) - 16.0f
        val a = 500.0f * (fx - fy)
        val b = 200.0f * (fy - fz)

        return LabColor(l, a, b)
    }

    /**
     * Euclidean distance in CIE Lab space (CIE76 ΔE).
     * Approximates human-perceived color difference.
     */
    fun deltaE76(lab1: LabColor, lab2: LabColor): Float {
        val dl = lab1.l - lab2.l
        val da = lab1.a - lab2.a
        val db = lab1.b - lab2.b
        return sqrt(dl * dl + da * da + db * db)
    }

    /**
     * CIEDE2000 color difference formula for advanced perceptual accuracy.
     */
    fun deltaE2000(lab1: LabColor, lab2: LabColor): Float {
        val l1 = lab1.l
        val a1 = lab1.a
        val b1 = lab1.b
        val l2 = lab2.l
        val a2 = lab2.a
        val b2 = lab2.b

        val c1 = hypot(a1, b1)
        val c2 = hypot(a2, b2)
        val cBar = (c1 + c2) / 2.0f

        val g = 0.5f * (1.0f - sqrt(cBar.pow(7) / (cBar.pow(7) + 25.0f.pow(7))))
        val a1Prime = a1 * (1.0f + g)
        val a2Prime = a2 * (1.0f + g)

        val c1Prime = hypot(a1Prime, b1)
        val c2Prime = hypot(a2Prime, b2)

        fun hueAngle(a: Float, b: Float): Float {
            val rad = atan2(b, a)
            var deg = Math.toDegrees(rad.toDouble()).toFloat()
            if (deg < 0) deg += 360.0f
            return deg
        }

        val h1Prime = hueAngle(a1Prime, b1)
        val h2Prime = hueAngle(a2Prime, b2)

        val deltaLPrime = l2 - l1
        val deltaCPrime = c2Prime - c1Prime

        var deltaHPrime = 0.0f
        if (c1Prime * c2Prime != 0.0f) {
            val diff = h2Prime - h1Prime
            deltaHPrime = when {
                abs(diff) <= 180.0f -> diff
                diff > 180.0f -> diff - 360.0f
                else -> diff + 360.0f
            }
        }
        val deltaBigHPrime = 2.0f * sqrt(c1Prime * c2Prime) * sin(Math.toRadians((deltaHPrime / 2.0).toDouble()).toFloat())

        val lBarPrime = (l1 + l2) / 2.0f
        val cBarPrime = (c1Prime + c2Prime) / 2.0f

        var hBarPrime = (h1Prime + h2Prime) / 2.0f
        if (abs(h1Prime - h2Prime) > 180.0f && c1Prime * c2Prime != 0.0f) {
            hBarPrime += if (h1Prime + h2Prime < 360.0f) 180.0f else -180.0f
        }

        val t = 1.0f -
                0.17f * cos(Math.toRadians((hBarPrime - 30.0).toDouble()).toFloat()) +
                0.24f * cos(Math.toRadians((2.0 * hBarPrime).toDouble()).toFloat()) +
                0.32f * cos(Math.toRadians((3.0 * hBarPrime + 6.0).toDouble()).toFloat()) -
                0.20f * cos(Math.toRadians((4.0 * hBarPrime - 63.0).toDouble()).toFloat())

        val sL = 1.0f + ((0.015f * (lBarPrime - 50.0f).pow(2)) / sqrt(20.0f + (lBarPrime - 50.0f).pow(2)))
        val sC = 1.0f + 0.045f * cBarPrime
        val sH = 1.0f + 0.015f * cBarPrime * t

        val deltaTheta = 30.0f * exp(-(((hBarPrime - 275.0f) / 25.0f).pow(2)))
        val rC = 2.0f * sqrt(cBarPrime.pow(7) / (cBarPrime.pow(7) + 25.0f.pow(7)))
        val rT = -sin(Math.toRadians((2.0 * deltaTheta).toDouble()).toFloat()) * rC

        val dL = deltaLPrime / sL
        val dC = deltaCPrime / sC
        val dH = deltaBigHPrime / sH

        return sqrt(dL * dL + dC * dC + dH * dH + rT * dC * dH)
    }

    /**
     * Applies white balance correction to sample RGB using a reference neutral white point.
     * (Phase 3 in prototype scope).
     */
    fun applyWhiteBalance(sample: RgbColor, whiteRef: RgbColor): RgbColor {
        val rScale = if (whiteRef.r > 20) 255.0f / whiteRef.r else 1.0f
        val gScale = if (whiteRef.g > 20) 255.0f / whiteRef.g else 1.0f
        val bScale = if (whiteRef.b > 20) 255.0f / whiteRef.b else 1.0f

        val newR = (sample.r * rScale).toInt().coerceIn(0, 255)
        val newG = (sample.g * gScale).toInt().coerceIn(0, 255)
        val newB = (sample.b * bScale).toInt().coerceIn(0, 255)

        return RgbColor(newR, newG, newB)
    }

    /**
     * Samples the median RGB from a rectangular region of a Bitmap.
     * Using median avoids glare highlights and edge shadows on the reactive strip pad.
     */
    fun sampleRegionMedian(
        bitmap: Bitmap,
        leftNorm: Float,
        topNorm: Float,
        rightNorm: Float,
        bottomNorm: Float
    ): RgbColor {
        val width = bitmap.width
        val height = bitmap.height

        val x0 = (leftNorm.coerceIn(0f, 1f) * width).toInt().coerceIn(0, width - 1)
        val y0 = (topNorm.coerceIn(0f, 1f) * height).toInt().coerceIn(0, height - 1)
        val x1 = (rightNorm.coerceIn(0f, 1f) * width).toInt().coerceIn(x0 + 1, width)
        val y1 = (bottomNorm.coerceIn(0f, 1f) * height).toInt().coerceIn(y0 + 1, height)

        val rValues = ArrayList<Int>()
        val gValues = ArrayList<Int>()
        val bValues = ArrayList<Int>()

        val step = maxOf(1, ((x1 - x0) * (y1 - y0) / 1000).coerceAtLeast(1))

        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                if (count++ % step == 0) {
                    val pixel = bitmap.getPixel(x, y)
                    rValues.add(android.graphics.Color.red(pixel))
                    gValues.add(android.graphics.Color.green(pixel))
                    bValues.add(android.graphics.Color.blue(pixel))
                }
            }
        }

        if (rValues.isEmpty()) {
            return RgbColor(128, 128, 128)
        }

        rValues.sort()
        gValues.sort()
        bValues.sort()

        val mid = rValues.size / 2
        return RgbColor(rValues[mid], gValues[mid], bValues[mid])
    }

    /**
     * Estimates pH from sampled RGB against a reference chart.
     * Performs distance scoring in Lab space and smooth inverse-distance weighted interpolation.
     */
    fun estimatePh(
        sampledRgb: RgbColor,
        referenceChart: Map<Float, RgbColor>,
        useCiede2000: Boolean = false
    ): PhEstimateResult {
        val sampleLab = rgbToLab(sampledRgb)

        val scored = referenceChart.map { (ph, refRgb) ->
            val refLab = rgbToLab(refRgb)
            val dist = if (useCiede2000) deltaE2000(sampleLab, refLab) else deltaE76(sampleLab, refLab)
            MatchScore(ph, refRgb, refLab, dist)
        }.sortedBy { it.deltaE }

        val best = scored.firstOrNull() ?: MatchScore(7.0f, RgbColor(128, 128, 128), sampleLab, 0f)
        val second = scored.getOrNull(1) ?: best

        val interpolated = if (best.deltaE + second.deltaE > 0.0001f) {
            val w1 = second.deltaE / (best.deltaE + second.deltaE)
            val w2 = best.deltaE / (best.deltaE + second.deltaE)
            (best.ph * w1 + second.ph * w2)
        } else {
            best.ph
        }

        val roundedInterpolated = (Math.round(interpolated * 100.0) / 100.0).toFloat()
        val roundedBest = (Math.round(best.ph * 10.0) / 10.0).toFloat()
        val roundedDist = (Math.round(best.deltaE * 100.0) / 100.0).toFloat()

        val confidence = when {
            best.deltaE < 8.0f -> ConfidenceLevel.HIGH
            best.deltaE < 18.0f -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val category = when {
            roundedInterpolated <= 3.0f -> PhCategory.STRONGLY_ACIDIC
            roundedInterpolated < 6.5f -> PhCategory.WEAKLY_ACIDIC
            roundedInterpolated <= 7.5f -> PhCategory.NEUTRAL
            roundedInterpolated <= 10.0f -> PhCategory.WEAKLY_BASIC
            else -> PhCategory.STRONGLY_BASIC
        }

        return PhEstimateResult(
            sampledRgb = sampledRgb,
            sampledLab = sampleLab,
            bestMatchPh = roundedBest,
            bestMatchDeltaE = roundedDist,
            interpolatedPh = roundedInterpolated,
            closestMatches = scored.take(5),
            confidence = confidence,
            category = category
        )
    }
}
