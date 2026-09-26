package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LandHistoryDao {
    @Query("SELECT * FROM land_history WHERE plotId = :plotId ORDER BY id ASC")
    fun getHistoryForPlot(plotId: Long): Flow<List<LandHistoryEntity>>

    @Query("SELECT * FROM land_history WHERE plotId = :plotId ORDER BY id ASC")
    suspend fun getHistoryListForPlot(plotId: Long): List<LandHistoryEntity>

    @Query("SELECT * FROM land_history ORDER BY id ASC")
    fun getAllHistory(): Flow<List<LandHistoryEntity>>

    @Query("SELECT * FROM land_history ORDER BY id ASC")
    suspend fun getAllHistoryList(): List<LandHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: LandHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryList(historyList: List<LandHistoryEntity>)

    @Update
    suspend fun updateHistory(history: LandHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: LandHistoryEntity)
}
