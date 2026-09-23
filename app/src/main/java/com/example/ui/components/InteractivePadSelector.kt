package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.color.RgbColor

data class PadBox(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val sizeRatio: Float = 0.14f // box width & height relative to smaller dimension
) {
    val left: Float get() = (centerX - sizeRatio / 2).coerceIn(0f, 1f)
    val right: Float get() = (centerX + sizeRatio / 2).coerceIn(0f, 1f)
    val top: Float get() = (centerY - sizeRatio / 2).coerceIn(0f, 1f)
    val bottom: Float get() = (centerY + sizeRatio / 2).coerceIn(0f, 1f)
}

@Composable
fun InteractivePadSelector(
    bitmap: Bitmap,
    padBox: PadBox,
    onPadBoxChanged: (PadBox) -> Unit,
    sampledRgb: RgbColor,
    isPickingWhiteBalance: Boolean = false,
    whiteBalancePoint: Pair<Float, Float>? = null,
    onWhiteBalancePicked: (Float, Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    val aspect = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.5f, 2.0f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(Color.Black)
            .testTag("interactive_pad_selector")
    ) {
        val containerWidth = maxWidth
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .pointerInput(isPickingWhiteBalance, bitmap) {
                    if (isPickingWhiteBalance) {
                        detectTapGestures { offset ->
                            val normX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val normY = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            onWhiteBalancePicked(normX, normY)
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / size.width.toFloat()
                                val dy = dragAmount.y / size.height.toFloat()
                                val newX = (padBox.centerX + dx).coerceIn(0.05f, 0.95f)
                                val newY = (padBox.centerY + dy).coerceIn(0.05f, 0.95f)
                                onPadBoxChanged(padBox.copy(centerX = newX, centerY = newY))
                            }
                        )
                    }
                }
        ) {
            // Draw original bitmap scaled to fit canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawImage(
                    image = imageBitmap,
                    dstSize = IntSize(canvasWidth.toInt(), canvasHeight.toInt())
                )

                if (isPickingWhiteBalance) {
                    // White balance target crosshair
                    if (whiteBalancePoint != null) {
                        val wbX = whiteBalancePoint.first * canvasWidth
                        val wbY = whiteBalancePoint.second * canvasHeight

                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(wbX, wbY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(wbX - 16.dp.toPx(), wbY),
                            end = Offset(wbX + 16.dp.toPx(), wbY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(wbX, wbY - 16.dp.toPx()),
                            end = Offset(wbX, wbY + 16.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                } else {
                    // Reactive pad sampling box
                    val minDim = minOf(canvasWidth, canvasHeight)
                    val boxSizePx = padBox.sizeRatio * minDim
                    val leftPx = (padBox.centerX * canvasWidth) - (boxSizePx / 2f)
                    val topPx = (padBox.centerY * canvasHeight) - (boxSizePx / 2f)

                    // Dim out surrounding area slightly for focus
                    drawRect(
                        color = Color.Black.copy(alpha = 0.25f),
                        size = size
                    )

                    // Cut out / highlight the sampling box
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(leftPx, topPx),
                        size = Size(boxSizePx, boxSizePx),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)
                        )
                    )

                    // Center crosshair inside the sampling box
                    val centerX = padBox.centerX * canvasWidth
                    val centerY = padBox.centerY * canvasHeight
                    val crosshairLen = 8.dp.toPx()

                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(centerX - crosshairLen, centerY),
                        end = Offset(centerX + crosshairLen, centerY),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(centerX, centerY - crosshairLen),
                        end = Offset(centerX, centerY + crosshairLen),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Corner grip markers
                    val cornerGrip = 6.dp.toPx()
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(leftPx, topPx))
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(leftPx + boxSizePx, topPx))
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(leftPx, topPx + boxSizePx))
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(leftPx + boxSizePx, topPx + boxSizePx))
                }
            }

            // Magnifier Loupe: Top-Right or Top-Left floating circular zoom lens
            val loupeAlign = if (padBox.centerX > 0.6f && padBox.centerY < 0.4f) Alignment.TopStart else Alignment.TopEnd

            Box(
                modifier = Modifier
                    .align(loupeAlign)
                    .padding(10.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(2.5.dp, Color(0xFF00E5FF), CircleShape)
            ) {
                // Live Loupe: zoomed center
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val loupeRadius = size.width / 2f
                    val sampleX = (padBox.centerX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                    val sampleY = (padBox.centerY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)

                    // Draw zoomed patch of the bitmap
                    val cropSize = (bitmap.width * 0.10f).toInt().coerceAtLeast(10)
                    val cropLeft = (sampleX - cropSize / 2).coerceIn(0, bitmap.width - cropSize)
                    val cropTop = (sampleY - cropSize / 2).coerceIn(0, bitmap.height - cropSize)

                    try {
                        val patch = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropSize, cropSize)
                        drawImage(
                            image = patch.asImageBitmap(),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    } catch (_: Exception) {
                        drawCircle(sampledRgb.toComposeColor())
                    }

                    // Crosshair in center of loupe
                    drawLine(
                        color = Color.Red,
                        start = Offset(loupeRadius - 10f, loupeRadius),
                        end = Offset(loupeRadius + 10f, loupeRadius),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(loupeRadius, loupeRadius - 10f),
                        end = Offset(loupeRadius, loupeRadius + 10f),
                        strokeWidth = 2f
                    )
                }

                // Mini indicator swatch in the bottom corner of the loupe
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(sampledRgb.toComposeColor())
                        .border(1.dp, Color.White, CircleShape)
                )
            }

            // Top banner helper tip
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(sampledRgb.toComposeColor())
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPickingWhiteBalance) "Tap white paper reference" else "Drag box over strip pad",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick reset button
            IconButton(
                onClick = { onPadBoxChanged(PadBox(0.5f, 0.5f, 0.14f)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Center Probe",
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
