package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        if (hasCameraPermission) {
            CameraContent(
                onImageCaptured = onImageCaptured,
                onDismiss = onDismiss
            )
        } else {
            CameraPermissionFallback(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun CameraContent(
    onImageCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                // Sync torch state
                camera?.cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                Log.e("CameraScreen", "Use case binding failed", e)
                errorMessage = "Failed to start camera: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview Stream
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Centered pH Strip Alignment Overlay
        StripAlignmentOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls: Back Button, Title, Torch & Camera Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "pH Strip Scanner",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle
                IconButton(
                    onClick = {
                        val newState = !isTorchOn
                        isTorchOn = newState
                        camera?.cameraControl?.enableTorch(newState)
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("camera_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash Toggle",
                        tint = if (isTorchOn) Color(0xFFFFD54F) else Color.White
                    )
                }

                // Switch camera lens
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Capture Controls & Guidance
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instructions chip
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Center test pad inside green reticle • Hold still for 1s",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Capture Shutter Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                ) {}

                IconButton(
                    onClick = {
                        if (!isCapturing) {
                            isCapturing = true
                            takePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                executor = cameraExecutor,
                                onImageSaved = { uri ->
                                    isCapturing = false
                                    ContextCompat.getMainExecutor(context).execute {
                                        onImageCaptured(uri)
                                    }
                                },
                                onError = { exc ->
                                    isCapturing = false
                                    Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                                    errorMessage = "Photo capture failed: ${exc.message}"
                                }
                            )
                        }
                    },
                    enabled = !isCapturing,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White, CircleShape)
                        .testTag("camera_capture_button")
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Capture pH Strip",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Error message banner if any
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { errorMessage = null }) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

/**
 * Centered frame overlay for accurate strip alignment.
 * Darkens outer screen, creates a clear viewport cut-out,
 * and renders high-precision framing brackets with pad alignment marks.
 */
@Composable
private fun StripAlignmentOverlay(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Strip alignment frame dimensions:
        // A vertical rectangle tailored for standard pH test strips (e.g. 110dp wide x 260dp tall)
        val frameWidth = (screenWidth * 0.42f).coerceIn(120f, 220f)
        val frameHeight = (screenHeight * 0.42f).coerceIn(240f, 420f)

        val left = (screenWidth - frameWidth) / 2f
        val top = (screenHeight - frameHeight) / 2f
        val right = left + frameWidth
        val bottom = top + frameHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw darkened vignette with clear cutout using Path + FillType.EvenOdd
            val path = Path().apply {
                // Outer bounds
                addRect(Rect(0f, 0f, size.width, size.height))
                // Inner cutout
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.55f)
            )

            // Draw primary bounding frame
            drawRoundRect(
                color = Color(0xFF00E676), // High-visibility bright green
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw corner bracket accents
            val bracketLen = 28.dp.toPx()
            val bracketStroke = 4.dp.toPx()
            val bracketColor = Color.White

            // Top-Left bracket
            drawLine(bracketColor, Offset(left - 2f, top + bracketLen), Offset(left - 2f, top), bracketStroke)
            drawLine(bracketColor, Offset(left, top - 2f), Offset(left + bracketLen, top - 2f), bracketStroke)

            // Top-Right bracket
            drawLine(bracketColor, Offset(right + 2f, top + bracketLen), Offset(right + 2f, top), bracketStroke)
            drawLine(bracketColor, Offset(right, top - 2f), Offset(right - bracketLen, top - 2f), bracketStroke)

            // Bottom-Left bracket
            drawLine(bracketColor, Offset(left - 2f, bottom - bracketLen), Offset(left - 2f, bottom), bracketStroke)
            drawLine(bracketColor, Offset(left, bottom + 2f), Offset(left + bracketLen, bottom + 2f), bracketStroke)

            // Bottom-Right bracket
            drawLine(bracketColor, Offset(right + 2f, bottom - bracketLen), Offset(right + 2f, bottom), bracketStroke)
            drawLine(bracketColor, Offset(right, bottom + 2f), Offset(right - bracketLen, bottom + 2f), bracketStroke)

            // Target Pad Guide Line across center
            val centerY = (top + bottom) / 2f
            drawLine(
                color = Color(0xFF00E676).copy(alpha = 0.8f),
                start = Offset(left + 16.dp.toPx(), centerY),
                end = Offset(right - 16.dp.toPx(), centerY),
                strokeWidth = 2.dp.toPx()
            )

            // Sub-pad markings (standard 4-pad strip guide notches)
            val padSpacing = frameHeight / 5f
            for (i in 1..4) {
                val y = top + (i * padSpacing)
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(left + 6.dp.toPx(), y),
                    end = Offset(left + 18.dp.toPx(), y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(right - 18.dp.toPx(), y),
                    end = Offset(right - 6.dp.toPx(), y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Overlay text badge above and below the reticle
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(frameWidth.dp)
                .height(frameHeight.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFF00E676),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "ALIGN STRIP HERE",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "REACTIVE PAD",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To scan pH strips directly, pH Matcher needs access to your device camera to capture live images.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_button")
        ) {
            Text("Grant Camera Permission")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onImageSaved: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "ph_strip_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                onImageSaved(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
