package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.BuiltInCharts
import com.example.model.PhChart
import com.example.ui.components.ColorSwatchComparison
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.InteractivePadSelector
import com.example.ui.components.PhSpectrumBar
import com.example.viewmodel.PhViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyzerScreen(
    viewModel: PhViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val result = state.estimateResult
    val customCharts by viewModel.customCharts.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showChartDropdown by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCameraScreen by remember { mutableStateOf(false) }

    // Temporary camera image URI
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.loadFromUri(tempCameraUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadFromUri(uri)
        }
    }

    fun launchCamera() {
        try {
            val photoFile = File.createTempFile("ph_strip_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("Failed to launch camera: ${e.message}") }
        }
    }

    LaunchedEffect(state.isSavingSuccess) {
        if (state.isSavingSuccess) {
            snackbarHostState.showSnackbar("Reading saved to History!")
            viewModel.resetSaveSuccess()
            showSaveSheet = false
        }
    }

    val allAvailableCharts = remember(customCharts) {
        BuiltInCharts.ALL + customCharts
    }

    if (showCameraScreen) {
        CameraScreen(
            onImageCaptured = { uri ->
                viewModel.loadFromUri(uri)
                showCameraScreen = false
            },
            onDismiss = {
                showCameraScreen = false
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showChartDropdown = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_launcher_icon),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "pH Color Matcher",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.activeChart.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Select Chart",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showChartDropdown,
                            onDismissRequest = { showChartDropdown = false }
                        ) {
                            Text(
                                text = "Built-in Charts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            BuiltInCharts.ALL.forEach { chart ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(chart.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "pH ${chart.minPh}–${chart.maxPh}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectChart(chart)
                                        showChartDropdown = false
                                    }
                                )
                            }

                            if (customCharts.isNotEmpty()) {
                                Text(
                                    text = "Custom Calibrated Charts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                customCharts.forEach { chart ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(chart.name, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "pH ${chart.minPh}–${chart.maxPh}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectChart(chart)
                                            showChartDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Algorithm Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Capture & Sample Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCameraScreen = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_camera"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_gallery"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontSize = 13.sp)
                    }
                }

                // Sample Strip Quick-Load Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Demo Strips:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.loadSampleStrip(R.drawable.sample_ph_strip_neutral) }
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("sample_neutral_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8DC24A))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Neutral (~pH 7)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.loadSampleStrip(R.drawable.sample_ph_strip_acid) }
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("sample_acid_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE65100))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Acidic (~pH 5)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Section 2: Interactive Pad Selector (Draggable Probe + Zoom Loupe)
            item {
                if (state.currentBitmap != null) {
                    InteractivePadSelector(
                        bitmap = state.currentBitmap!!,
                        padBox = state.padBox,
                        onPadBoxChanged = { viewModel.updatePadBox(it) },
                        sampledRgb = state.sampledCorrectedRgb,
                        isPickingWhiteBalance = state.isPickingWhiteBalance,
                        whiteBalancePoint = state.whiteRefPoint,
                        onWhiteBalancePicked = { x, y -> viewModel.onWhiteBalancePointPicked(x, y) }
                    )
                }
            }

            // Section 3: White Balance Toolbar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InvertColors,
                            contentDescription = null,
                            tint = if (state.isWhiteBalanceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isWhiteBalanceActive) "White Balance Active" else "White Balance Calibration",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (state.isWhiteBalanceActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isPickingWhiteBalance) {
                            TextButton(onClick = { viewModel.setPickingWhiteBalance(false) }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.setPickingWhiteBalance(true) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("wb_pick_btn")
                            ) {
                                Text("Pick Neutral White", fontSize = 12.sp)
                            }
                        }

                        if (state.whiteRefPoint != null) {
                            IconButton(onClick = { viewModel.clearWhiteBalance() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear White Balance",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Live pH Estimation Hero Card
            if (result != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ph_result_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "ESTIMATED pH",
                                        style = MaterialTheme.typography.labelSmall,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${result.interpolatedPh}",
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("estimated_ph_value")
                                        )
                                        Text(
                                            text = " pH",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }

                                    // Category Pill
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(android.graphics.Color.parseColor(result.category.colorHex)).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = result.category.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(android.graphics.Color.parseColor(result.category.colorHex)),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Quick Save Log Button
                                Button(
                                    onClick = { showSaveSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("save_reading_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Spectrum Bar Visualizer
                            PhSpectrumBar(
                                currentPh = result.interpolatedPh,
                                minPh = state.activeChart.minPh,
                                maxPh = state.activeChart.maxPh
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interpolation Breakdown Details
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Best Snap", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            "pH ${result.bestMatchPh}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Delta E (ΔE)", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            "${result.bestMatchDeltaE}",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Algorithm", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            if (state.isCiede2000) "CIEDE2000" else "CIE76 Lab",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 5: Side-by-side Swatch & Delta E Comparison Card
                item {
                    ColorSwatchComparison(
                        result = result,
                        chartName = state.activeChart.name
                    )
                }

                // Section 6: Closest Chart Matches Table
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Top Reference Chart Matches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            result.closestMatches.forEachIndexed { index, match ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(28.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(match.refRgb.toComposeColor())
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "pH ${match.ph}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (match.ph in state.activeChart.points.map { it.ph }) {
                                            val label = state.activeChart.points.find { it.ph == match.ph }?.label ?: ""
                                            if (label.isNotEmpty()) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "ΔE ${match.deltaE.toInt()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet: Save Reading Log
    val currentResult = result
    if (showSaveSheet && currentResult != null) {
        ModalBottomSheet(
            onDismissRequest = { showSaveSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            var solutionName by remember { mutableStateOf("") }
            var noteText by remember { mutableStateOf("") }
            var selectedCategory by remember { mutableStateOf("General") }

            val categories = listOf("General", "Drinking Water", "Aquarium", "Pool & Spa", "Soil / Plants", "Fermentation", "Health / Saliva", "Lab Chemical")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Save pH Reading to Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estimated pH ${currentResult.interpolatedPh} (${currentResult.category.label})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = solutionName,
                    onValueChange = { solutionName = it },
                    label = { Text("Sample / Solution Name") },
                    placeholder = { Text("e.g. Kitchen Tap, Main Fish Tank, Soil Bed #3") },
                    modifier = Modifier.fillMaxWidth().testTag("input_solution_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Category Tag",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Lighting condition, temperature, strip brand...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_notes"),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.saveCurrentReading(
                            solutionName = solutionName,
                            category = selectedCategory,
                            notes = noteText
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("confirm_save_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Reading")
                }
            }
        }
    }

    // Settings Dialog: Algorithm & Delta E Formula
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Color Science Settings") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use CIEDE2000 Distance", fontWeight = FontWeight.Bold)
                            Text(
                                "More computationally rigorous perceptual weighting than standard CIE76 Euclidean distance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isCiede2000,
                            onCheckedChange = { viewModel.toggleCiede2000(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Interpolation Method",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Inverse-distance weighted linear interpolation between the two closest CIE Lab reference chart points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
