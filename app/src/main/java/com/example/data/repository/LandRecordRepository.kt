package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AttachedDocumentEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LandHistoryEntity
import com.example.data.model.PlotEntity
import kotlinx.coroutines.flow.Flow

class LandRecordRepository(private val database: AppDatabase) {

    val allPlots: Flow<List<PlotEntity>> = database.plotDao().getAllPlots()
    val allFamilyMembers: Flow<List<FamilyMemberEntity>> = database.familyDao().getAllMembers()
    val allDocuments: Flow<List<AttachedDocumentEntity>> = database.documentDao().getAllDocuments()

    suspend fun getPlotById(id: Long): PlotEntity? = database.plotDao().getPlotById(id)

    suspend fun getPlotsByIds(ids: List<Long>): List<PlotEntity> = database.plotDao().getPlotsByIds(ids)

    suspend fun insertPlot(plot: PlotEntity): Long = database.plotDao().insertPlot(plot)

    suspend fun updatePlot(plot: PlotEntity) = database.plotDao().updatePlot(plot)

    suspend fun deletePlot(plot: PlotEntity) = database.plotDao().deletePlot(plot)

    fun getHistoryForPlot(plotId: Long): Flow<List<LandHistoryEntity>> = database.landHistoryDao().getHistoryForPlot(plotId)

    suspend fun getHistoryListForPlot(plotId: Long): List<LandHistoryEntity> = database.landHistoryDao().getHistoryListForPlot(plotId)

    suspend fun getAllHistoryList(): List<LandHistoryEntity> = database.landHistoryDao().getAllHistoryList()

    suspend fun insertHistory(history: LandHistoryEntity): Long = database.landHistoryDao().insertHistory(history)

    suspend fun deleteHistory(history: LandHistoryEntity) = database.landHistoryDao().deleteHistory(history)

    fun getDocumentsForPlot(plotId: Long): Flow<List<AttachedDocumentEntity>> = database.documentDao().getDocumentsForPlot(plotId)

    suspend fun getDocumentsListForPlot(plotId: Long): List<AttachedDocumentEntity> = database.documentDao().getDocumentsListForPlot(plotId)

    suspend fun getAllDocumentsList(): List<AttachedDocumentEntity> = database.documentDao().getAllDocumentsList()

    suspend fun getDocumentsByIds(ids: List<Long>): List<AttachedDocumentEntity> = database.documentDao().getDocumentsByIds(ids)

    suspend fun insertDocument(doc: AttachedDocumentEntity): Long = database.documentDao().insertDocument(doc)

    suspend fun deleteDocument(doc: AttachedDocumentEntity) = database.documentDao().deleteDocument(doc)

    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long = database.familyDao().insertMember(member)

    suspend fun updateFamilyMember(member: FamilyMemberEntity) = database.familyDao().updateMember(member)

    suspend fun deleteFamilyMember(member: FamilyMemberEntity) = database.familyDao().deleteMember(member)
}
