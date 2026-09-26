package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlotDao {
    @Query("SELECT * FROM plot_records ORDER BY id ASC")
    fun getAllPlots(): Flow<List<PlotEntity>>

    @Query("SELECT * FROM plot_records ORDER BY id ASC")
    suspend fun getAllPlotsList(): List<PlotEntity>

    @Query("SELECT * FROM plot_records WHERE id = :id")
    suspend fun getPlotById(id: Long): PlotEntity?

    @Query("SELECT * FROM plot_records WHERE id IN (:ids)")
    suspend fun getPlotsByIds(ids: List<Long>): List<PlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlot(plot: PlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlots(plots: List<PlotEntity>)

    @Update
    suspend fun updatePlot(plot: PlotEntity)

    @Delete
    suspend fun deletePlot(plot: PlotEntity)

    @Query("SELECT COUNT(*) FROM plot_records")
    suspend fun getPlotCount(): Int
}
