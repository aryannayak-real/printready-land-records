package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttachedDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM attached_documents ORDER BY id ASC")
    fun getAllDocuments(): Flow<List<AttachedDocumentEntity>>

    @Query("SELECT * FROM attached_documents ORDER BY id ASC")
    suspend fun getAllDocumentsList(): List<AttachedDocumentEntity>

    @Query("SELECT * FROM attached_documents WHERE plotId = :plotId ORDER BY id ASC")
    fun getDocumentsForPlot(plotId: Long): Flow<List<AttachedDocumentEntity>>

    @Query("SELECT * FROM attached_documents WHERE plotId = :plotId ORDER BY id ASC")
    suspend fun getDocumentsListForPlot(plotId: Long): List<AttachedDocumentEntity>

    @Query("SELECT * FROM attached_documents WHERE id IN (:ids)")
    suspend fun getDocumentsByIds(ids: List<Long>): List<AttachedDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: AttachedDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(docs: List<AttachedDocumentEntity>)

    @Update
    suspend fun updateDocument(doc: AttachedDocumentEntity)

    @Delete
    suspend fun deleteDocument(doc: AttachedDocumentEntity)
}
