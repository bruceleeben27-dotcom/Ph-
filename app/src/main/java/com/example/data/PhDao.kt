package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CustomChartEntity
import com.example.model.PhReading
import kotlinx.coroutines.flow.Flow

@Dao
interface PhReadingDao {
    @Query("SELECT * FROM ph_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<PhReading>>

    @Query("SELECT * FROM ph_readings WHERE id = :id LIMIT 1")
    suspend fun getReadingById(id: Long): PhReading?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: PhReading): Long

    @Delete
    suspend fun deleteReading(reading: PhReading)

    @Query("DELETE FROM ph_readings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ph_readings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ph_readings")
    fun getReadingCount(): Flow<Int>

    @Query("SELECT AVG(interpolatedPh) FROM ph_readings")
    fun getAveragePh(): Flow<Float?>
}

@Dao
interface CustomChartDao {
    @Query("SELECT * FROM custom_charts ORDER BY createdAt DESC")
    fun getAllCustomCharts(): Flow<List<CustomChartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChart(chart: CustomChartEntity)

    @Delete
    suspend fun deleteChart(chart: CustomChartEntity)

    @Query("DELETE FROM custom_charts WHERE id = :id")
    suspend fun deleteById(id: String)
}
