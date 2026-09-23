package com.example.data

import com.example.color.RgbColor
import com.example.model.BuiltInCharts
import com.example.model.CustomChartEntity
import com.example.model.PhChart
import com.example.model.PhReading
import com.example.model.ReferencePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PhRepository(private val database: AppDatabase) {

    val allReadings: Flow<List<PhReading>> = database.phReadingDao().getAllReadings()
    val readingCount: Flow<Int> = database.phReadingDao().getReadingCount()
    val averagePh: Flow<Float?> = database.phReadingDao().getAveragePh()

    val customCharts: Flow<List<PhChart>> = database.customChartDao().getAllCustomCharts().map { entities ->
        entities.map { entity ->
            val points = parsePointsData(entity.pointsData)
            PhChart(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                minPh = entity.minPh,
                maxPh = entity.maxPh,
                points = points,
                isCustom = true
            )
        }
    }

    suspend fun insertReading(reading: PhReading): Long {
        return database.phReadingDao().insertReading(reading)
    }

    suspend fun deleteReading(reading: PhReading) {
        database.phReadingDao().deleteReading(reading)
    }

    suspend fun deleteReadingById(id: Long) {
        database.phReadingDao().deleteById(id)
    }

    suspend fun clearAllReadings() {
        database.phReadingDao().clearAll()
    }

    suspend fun saveCustomChart(chart: PhChart) {
        val entity = CustomChartEntity(
            id = chart.id,
            name = chart.name,
            description = chart.description,
            minPh = chart.minPh,
            maxPh = chart.maxPh,
            pointsData = serializePoints(chart.points),
            createdAt = System.currentTimeMillis()
        )
        database.customChartDao().insertChart(entity)
    }

    suspend fun deleteCustomChart(id: String) {
        database.customChartDao().deleteById(id)
    }

    private fun serializePoints(points: List<ReferencePoint>): String {
        return points.joinToString(";") { "${it.ph}:${it.rgb.r},${it.rgb.g},${it.rgb.b}:${it.label}" }
    }

    private fun parsePointsData(data: String): List<ReferencePoint> {
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size >= 2) {
                val ph = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val rgbParts = parts[1].split(",")
                if (rgbParts.size == 3) {
                    val r = rgbParts[0].toIntOrNull() ?: 0
                    val g = rgbParts[1].toIntOrNull() ?: 0
                    val b = rgbParts[2].toIntOrNull() ?: 0
                    val label = if (parts.size >= 3) parts[2] else ""
                    ReferencePoint(ph, RgbColor(r, g, b), label)
                } else null
            } else null
        }.sortedBy { it.ph }
    }

    suspend fun seedSampleDataIfEmpty() {
        // Will check and insert realistic starting measurements if empty
    }
}
