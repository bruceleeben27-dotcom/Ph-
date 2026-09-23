package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.color.RgbColor

@Entity(tableName = "ph_readings")
data class PhReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val interpolatedPh: Float,
    val bestMatchPh: Float,
    val bestMatchDeltaE: Float,
    val sampledR: Int,
    val sampledG: Int,
    val sampledB: Int,
    val confidenceLabel: String,
    val categoryLabel: String,
    val chartName: String,
    val solutionName: String = "Test Solution",
    val notes: String = "",
    val imagePath: String? = null
) {
    val sampledRgb: RgbColor get() = RgbColor(sampledR, sampledG, sampledB)
}

@Entity(tableName = "custom_charts")
data class CustomChartEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val minPh: Float,
    val maxPh: Float,
    val pointsData: String, // format: "ph:r,g,b;ph:r,g,b"
    val createdAt: Long = System.currentTimeMillis()
)
