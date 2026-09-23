package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.color.ConfidenceLevel
import com.example.color.PhEstimateResult

@Composable
fun ColorSwatchComparison(
    result: PhEstimateResult,
    chartName: String,
    modifier: Modifier = Modifier
) {
    val bestMatch = result.closestMatches.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("color_swatch_comparison"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Chart & Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Color Match Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                ConfidenceBadge(confidence = result.confidence)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Swatch Side-by-Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Sampled Pad Color
                SwatchColumn(
                    modifier = Modifier.weight(1f),
                    title = "Sampled Pad",
                    subtitle = "From photo",
                    color = result.sampledRgb.toComposeColor(),
                    rgbText = "R ${result.sampledRgb.r}  G ${result.sampledRgb.g}  B ${result.sampledRgb.b}",
                    labText = "L* ${result.sampledLab.l.toInt()}  a* ${result.sampledLab.a.toInt()}  b* ${result.sampledLab.b.toInt()}"
                )

                // Right: Best Reference Chart Point
                if (bestMatch != null) {
                    SwatchColumn(
                        modifier = Modifier.weight(1f),
                        title = "Chart Key",
                        subtitle = "pH ${bestMatch.ph}",
                        color = bestMatch.refRgb.toComposeColor(),
                        rgbText = "R ${bestMatch.refRgb.r}  G ${bestMatch.refRgb.g}  B ${bestMatch.refRgb.b}",
                        labText = "ΔE = ${result.bestMatchDeltaE}",
                        isHighlight = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delta E Perceptual distance explanation
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val deltaE = result.bestMatchDeltaE
                    val (icon, tint, desc) = when {
                        deltaE < 5.0f -> Triple(
                            Icons.Default.CheckCircle,
                            Color(0xFF43A047),
                            "Excellent match (ΔE $deltaE < 5.0): Perceptually imperceptible color difference."
                        )
                        deltaE < 12.0f -> Triple(
                            Icons.Default.CheckCircle,
                            Color(0xFF00ACC1),
                            "Good match (ΔE $deltaE): Closely aligned with reference standard."
                        )
                        deltaE < 20.0f -> Triple(
                            Icons.Default.Info,
                            Color(0xFFFB8C00),
                            "Moderate drift (ΔE $deltaE): Slightly affected by ambient lighting."
                        )
                        else -> Triple(
                            Icons.Default.Warning,
                            Color(0xFFE53935),
                            "Noticeable difference (ΔE $deltaE): Ensure proper lighting or calibrate chart."
                        )
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SwatchColumn(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    color: Color,
    rgbText: String,
    labText: String,
    isHighlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Color block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = rgbText,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = labText,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConfidenceBadge(confidence: ConfidenceLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (confidence) {
        ConfidenceLevel.HIGH -> Color(0xFF1B5E20) to Color(0xFFC8E6C9)
        ConfidenceLevel.MEDIUM -> Color(0xFFE65100) to Color(0xFFFFE0B2)
        ConfidenceLevel.LOW -> Color(0xFFB71C1C) to Color(0xFFFFCDD2)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = confidence.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
