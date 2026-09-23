package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhSpectrumBar(
    currentPh: Float,
    minPh: Float = 0.0f,
    maxPh: Float = 14.0f,
    modifier: Modifier = Modifier
) {
    val animatedPh by animateFloatAsState(
        targetValue = currentPh.coerceIn(minPh, maxPh),
        animationSpec = spring(dampingRatio = 0.8f),
        label = "ph_needle_anim"
    )

    val spectrumColors = listOf(
        Color(0xFFE53935), // pH 0-1 Red
        Color(0xFFF4511E), // pH 2-3 Orange-Red
        Color(0xFFFB8C00), // pH 4-5 Orange
        Color(0xFFFDD835), // pH 6 Yellow
        Color(0xFF43A047), // pH 7 Green (Neutral)
        Color(0xFF00ACC1), // pH 8-9 Cyan-Teal
        Color(0xFF1E88E5), // pH 10-11 Blue
        Color(0xFF5E35B1), // pH 12-13 Violet
        Color(0xFF4A148C)  // pH 14 Purple
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "pH $minPh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "7.0 (Neutral)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "pH $maxPh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("ph_spectrum_bar")
        ) {
            // Gradient spectrum bar
            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp).align(Alignment.Center)) {
                val corner = 8.dp.toPx()
                drawRoundRect(
                    brush = Brush.horizontalGradient(spectrumColors),
                    size = size,
                    cornerRadius = CornerRadius(corner, corner)
                )

                // Neutral marker line at pH 7.0 if in range
                if (7.0f in minPh..maxPh) {
                    val neutralFraction = (7.0f - minPh) / (maxPh - minPh)
                    val neutralX = size.width * neutralFraction
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(neutralX, 0f),
                        end = Offset(neutralX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Needle indicator for current pH
            Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
                val range = (maxPh - minPh).coerceAtLeast(0.1f)
                val fraction = ((animatedPh - minPh) / range).coerceIn(0f, 1f)
                val needleX = (size.width * fraction).coerceIn(8.dp.toPx(), size.width - 8.dp.toPx())

                // Downward triangle pointer on top
                val topPath = Path().apply {
                    moveTo(needleX - 6.dp.toPx(), 0f)
                    lineTo(needleX + 6.dp.toPx(), 0f)
                    lineTo(needleX, 8.dp.toPx())
                    close()
                }
                drawPath(topPath, color = Color.White)

                // Upward triangle pointer on bottom
                val bottomPath = Path().apply {
                    moveTo(needleX - 6.dp.toPx(), size.height)
                    lineTo(needleX + 6.dp.toPx(), size.height)
                    lineTo(needleX, size.height - 8.dp.toPx())
                    close()
                }
                drawPath(bottomPath, color = Color.White)

                // Vertical needle line
                drawLine(
                    color = Color.White,
                    start = Offset(needleX, 0f),
                    end = Offset(needleX, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◄ Acidic",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE53935)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Alkaline ►",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF5E35B1)
            )
        }
    }
}
