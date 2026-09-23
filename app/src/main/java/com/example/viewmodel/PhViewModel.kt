package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.color.ColorScience
import com.example.color.ConfidenceLevel
import com.example.color.PhCategory
import com.example.color.PhEstimateResult
import com.example.color.RgbColor
import com.example.data.AppDatabase
import com.example.data.PhRepository
import com.example.model.BuiltInCharts
import com.example.model.PhChart
import com.example.model.PhReading
import com.example.model.ReferencePoint
import com.example.ui.components.PadBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

data class UiState(
    val currentBitmap: Bitmap? = null,
    val padBox: PadBox = PadBox(0.5f, 0.5f, 0.14f),
    val activeChart: PhChart = BuiltInCharts.PROTOTYPE_WIDE,
    val isWhiteBalanceActive: Boolean = false,
    val whiteRefPoint: Pair<Float, Float>? = null,
    val whiteRefRgb: RgbColor? = null,
    val sampledRawRgb: RgbColor = RgbColor(215, 216, 60),
    val sampledCorrectedRgb: RgbColor = RgbColor(215, 216, 60),
    val estimateResult: PhEstimateResult? = null,
    val isCiede2000: Boolean = false,
    val isPickingWhiteBalance: Boolean = false,
    val calibrationStep: Int = -1, // -1 if not in calibration mode
    val calibrationPoints: MutableList<ReferencePoint> = mutableListOf(),
    val isSavingSuccess: Boolean = false
)

class PhViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhRepository(AppDatabase.getDatabase(application))

    val allReadings = repository.allReadings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val customCharts = repository.customCharts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val readingCount = repository.readingCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val averagePh = repository.averagePh.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Load default sample strip on start so user immediately has an active session!
        loadSampleStrip(R.drawable.sample_ph_strip_neutral)
        seedRealisticHistoryIfEmpty()
    }

    private fun seedRealisticHistoryIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            val count = db.phReadingDao().getAllReadings()
            // We can prefill a couple realistic readings if completely fresh
        }
    }

    fun loadSampleStrip(drawableResId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(
                    getApplication<Application>().resources,
                    drawableResId
                )
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            currentBitmap = bitmap,
                            padBox = PadBox(0.5f, 0.5f, 0.14f),
                            whiteRefPoint = null,
                            whiteRefRgb = null,
                            isPickingWhiteBalance = false
                        )
                        recomputeAnalysis()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            currentBitmap = bitmap,
                            padBox = PadBox(0.5f, 0.5f, 0.14f),
                            whiteRefPoint = null,
                            whiteRefRgb = null,
                            isPickingWhiteBalance = false
                        )
                        recomputeAnalysis()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePadBox(newBox: PadBox) {
        _uiState.value = _uiState.value.copy(padBox = newBox)
        recomputeAnalysis()
    }

    fun selectChart(chart: PhChart) {
        _uiState.value = _uiState.value.copy(activeChart = chart)
        recomputeAnalysis()
    }

    fun toggleCiede2000(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isCiede2000 = enabled)
        recomputeAnalysis()
    }

    fun setPickingWhiteBalance(picking: Boolean) {
        _uiState.value = _uiState.value.copy(isPickingWhiteBalance = picking)
    }

    fun onWhiteBalancePointPicked(normX: Float, normY: Float) {
        val bmp = _uiState.value.currentBitmap ?: return
        val rawWhite = ColorScience.sampleRegionMedian(
            bmp,
            (normX - 0.03f).coerceIn(0f, 1f),
            (normY - 0.03f).coerceIn(0f, 1f),
            (normX + 0.03f).coerceIn(0f, 1f),
            (normY + 0.03f).coerceIn(0f, 1f)
        )

        _uiState.value = _uiState.value.copy(
            whiteRefPoint = Pair(normX, normY),
            whiteRefRgb = rawWhite,
            isWhiteBalanceActive = true,
            isPickingWhiteBalance = false
        )
        recomputeAnalysis()
    }

    fun toggleWhiteBalance(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isWhiteBalanceActive = enabled)
        recomputeAnalysis()
    }

    fun clearWhiteBalance() {
        _uiState.value = _uiState.value.copy(
            whiteRefPoint = null,
            whiteRefRgb = null,
            isWhiteBalanceActive = false,
            isPickingWhiteBalance = false
        )
        recomputeAnalysis()
    }

    fun recomputeAnalysis() {
        val state = _uiState.value
        val bitmap = state.currentBitmap ?: return

        // 1. Sample median color from reactive pad region
        val box = state.padBox
        val rawRgb = ColorScience.sampleRegionMedian(
            bitmap,
            box.left,
            box.top,
            box.right,
            box.bottom
        )

        // 2. Optional white balance adaptation
        val finalRgb = if (state.isWhiteBalanceActive && state.whiteRefRgb != null) {
            ColorScience.applyWhiteBalance(rawRgb, state.whiteRefRgb)
        } else {
            rawRgb
        }

        // 3. Match against active reference chart
        val estimate = ColorScience.estimatePh(
            sampledRgb = finalRgb,
            referenceChart = state.activeChart.toRgbMap(),
            useCiede2000 = state.isCiede2000
        )

        _uiState.value = state.copy(
            sampledRawRgb = rawRgb,
            sampledCorrectedRgb = finalRgb,
            estimateResult = estimate
        )
    }

    fun saveCurrentReading(
        solutionName: String,
        category: String,
        notes: String
    ) {
        val state = _uiState.value
        val result = state.estimateResult ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val reading = PhReading(
                interpolatedPh = result.interpolatedPh,
                bestMatchPh = result.bestMatchPh,
                bestMatchDeltaE = result.bestMatchDeltaE,
                sampledR = result.sampledRgb.r,
                sampledG = result.sampledRgb.g,
                sampledB = result.sampledRgb.b,
                confidenceLabel = result.confidence.label,
                categoryLabel = category.ifBlank { result.category.label },
                chartName = state.activeChart.name,
                solutionName = solutionName.ifBlank { "Sample Solution" },
                notes = notes
            )
            repository.insertReading(reading)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isSavingSuccess = true)
            }
        }
    }

    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(isSavingSuccess = false)
    }

    fun deleteReading(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReadingById(id)
        }
    }

    fun clearAllReadings() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllReadings()
        }
    }

    fun saveCustomChart(
        name: String,
        description: String,
        minPh: Float,
        maxPh: Float,
        points: List<ReferencePoint>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newChart = PhChart(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                minPh = minPh,
                maxPh = maxPh,
                points = points.sortedBy { it.ph },
                isCustom = true
            )
            repository.saveCustomChart(newChart)
            withContext(Dispatchers.Main) {
                selectChart(newChart)
            }
        }
    }

    fun deleteCustomChart(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomChart(id)
            withContext(Dispatchers.Main) {
                if (_uiState.value.activeChart.id == id) {
                    selectChart(BuiltInCharts.PROTOTYPE_WIDE)
                }
            }
        }
    }
}
