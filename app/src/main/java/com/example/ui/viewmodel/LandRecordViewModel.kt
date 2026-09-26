package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SampleDataInitializer
import com.example.data.model.AttachedDocumentEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.GeneratedPdfItem
import com.example.data.model.LandHistoryEntity
import com.example.data.model.PdfExportConfig
import com.example.data.model.PdfExportMode
import com.example.data.model.PdfOrientation
import com.example.data.model.PlotEntity
import com.example.data.repository.BackupManager
import com.example.data.repository.LandRecordRepository
import com.example.pdf.PdfDocumentGenerator
import com.example.pdf.PdfRendererHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class LandRecordViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = LandRecordRepository(database)
    private val pdfGenerator = PdfDocumentGenerator(application)

    val allPlots: StateFlow<List<PlotEntity>> = repository.allPlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFamilyMembers: StateFlow<List<FamilyMemberEntity>> = repository.allFamilyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<AttachedDocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlotIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPlotIds: StateFlow<Set<Long>> = _selectedPlotIds.asStateFlow()

    private val _selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocIds: StateFlow<Set<Long>> = _selectedDocIds.asStateFlow()

    private val _exportConfig = MutableStateFlow(PdfExportConfig())
    val exportConfig: StateFlow<PdfExportConfig> = _exportConfig.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _isLoadingPreview = MutableStateFlow(false)
    val isLoadingPreview: StateFlow<Boolean> = _isLoadingPreview.asStateFlow()

    private val _lastGeneratedPdf = MutableStateFlow<GeneratedPdfItem?>(null)
    val lastGeneratedPdf: StateFlow<GeneratedPdfItem?> = _lastGeneratedPdf.asStateFlow()

    private val _previewBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val previewBitmaps: StateFlow<List<Bitmap>> = _previewBitmaps.asStateFlow()

    private val _generatedPdfs = MutableStateFlow<List<GeneratedPdfItem>>(emptyList())
    val generatedPdfs: StateFlow<List<GeneratedPdfItem>> = _generatedPdfs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-faceted search: Plot number, Khata, Village, Owner, Family Member, Document Number, Reference Number
    val filteredPlots = combine(allPlots, allFamilyMembers, allDocuments, _searchQuery) { plots, members, docs, query ->
        if (query.isBlank()) {
            plots
        } else {
            val q = query.trim()
            val matchingMemberIds = members.filter { it.fullName.contains(q, ignoreCase = true) }.map { it.id }.toSet()
            val matchingPlotIdsFromDocs = docs.filter {
                it.documentNumber.contains(q, ignoreCase = true) || it.documentName.contains(q, ignoreCase = true)
            }.mapNotNull { it.plotId }.toSet()

            plots.filter { plot ->
                plot.plotNumber.contains(q, ignoreCase = true) ||
                        plot.khataNumber.contains(q, ignoreCase = true) ||
                        plot.village.contains(q, ignoreCase = true) ||
                        plot.tahasil.contains(q, ignoreCase = true) ||
                        plot.district.contains(q, ignoreCase = true) ||
                        plot.recordedOwner.contains(q, ignoreCase = true) ||
                        plot.previousOwner.contains(q, ignoreCase = true) ||
                        plot.landRecordNumber.contains(q, ignoreCase = true) ||
                        plot.documentReference.contains(q, ignoreCase = true) ||
                        plot.landType.contains(q, ignoreCase = true) ||
                        plot.relationshipToUser.contains(q, ignoreCase = true) ||
                        (plot.recordedOwnerId != null && matchingMemberIds.contains(plot.recordedOwnerId)) ||
                        (plot.previousOwnerId != null && matchingMemberIds.contains(plot.previousOwnerId)) ||
                        matchingPlotIdsFromDocs.contains(plot.id)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active plot's history flow helper
    private val _activePlotHistory = MutableStateFlow<List<LandHistoryEntity>>(emptyList())
    val activePlotHistory: StateFlow<List<LandHistoryEntity>> = _activePlotHistory.asStateFlow()

    init {
        viewModelScope.launch {
            SampleDataInitializer.populateIfEmpty(application, database)
            loadSavedPdfs()
        }
    }

    private fun loadSavedPdfs() {
        val pdfDir = File(getApplication<Application>().cacheDir, "pdfs")
        if (!pdfDir.exists()) return
        val files = pdfDir.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) } ?: return

        val items = files.mapNotNull { file ->
            try {
                GeneratedPdfItem(
                    id = file.name,
                    title = file.nameWithoutExtension.replace("_", " "),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeFormatted = "${(file.length() / 1024).coerceAtLeast(1)} KB",
                    pageCount = 1,
                    generatedAtMillis = file.lastModified(),
                    exportType = "Print Ready"
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.generatedAtMillis }

        _generatedPdfs.value = items
    }

    fun loadHistoryForPlot(plotId: Long) {
        viewModelScope.launch {
            _activePlotHistory.value = repository.getHistoryListForPlot(plotId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePlotSelection(plotId: Long) {
        val current = _selectedPlotIds.value.toMutableSet()
        if (current.contains(plotId)) {
            current.remove(plotId)
        } else {
            current.add(plotId)
        }
        _selectedPlotIds.value = current
    }

    fun selectAllPlots(selectAll: Boolean) {
        if (selectAll) {
            _selectedPlotIds.value = allPlots.value.map { it.id }.toSet()
        } else {
            _selectedPlotIds.value = emptySet()
        }
    }

    fun toggleDocSelection(docId: Long) {
        val current = _selectedDocIds.value.toMutableSet()
        if (current.contains(docId)) {
            current.remove(docId)
        } else {
            current.add(docId)
        }
        _selectedDocIds.value = current
    }

    fun configureExport(
        mode: PdfExportMode,
        targetPlotId: Long? = null,
        targetMemberId: Long? = null
    ) {
        val plotsList = allPlots.value
        val membersList = allFamilyMembers.value

        val defaultFilename = when (mode) {
            PdfExportMode.CURRENT_PLOT -> {
                val p = plotsList.firstOrNull { it.id == targetPlotId } ?: plotsList.firstOrNull()
                if (p != null) "Land_Record_Plot_${p.plotNumber.replace(Regex("[^a-zA-Z0-9]"), "_")}.pdf"
                else "Land_Record_Plot.pdf"
            }
            PdfExportMode.SELECTED_PLOTS -> {
                val selected = _selectedPlotIds.value
                val firstPlot = plotsList.firstOrNull { selected.contains(it.id) }
                if (firstPlot != null) "Plot_${firstPlot.plotNumber.replace(Regex("[^a-zA-Z0-9]"), "_")}_Khata_${firstPlot.khataNumber}.pdf"
                else "Selected_Plots_Record.pdf"
            }
            PdfExportMode.ALL_PLOTS -> "Family_Land_Records_2026.pdf"
            PdfExportMode.FAMILY_MEMBER -> {
                val m = membersList.firstOrNull { it.id == targetMemberId } ?: membersList.firstOrNull()
                if (m != null) "Family_Record_${m.fullName.replace(' ', '_')}.pdf"
                else "Family_Member_Record.pdf"
            }
            PdfExportMode.FAMILY_WITH_LAND -> "Family_Lineage_And_Land_Records.pdf"
            PdfExportMode.COMBINED_DOCUMENTS -> "Combined_Land_Documents_Deeds.pdf"
        }

        _exportConfig.value = _exportConfig.value.copy(
            exportMode = mode,
            targetPlotId = targetPlotId,
            targetFamilyMemberId = targetMemberId,
            selectedPlotIds = if (mode == PdfExportMode.SELECTED_PLOTS) _selectedPlotIds.value else emptySet(),
            selectedDocumentIds = if (mode == PdfExportMode.COMBINED_DOCUMENTS) _selectedDocIds.value else emptySet(),
            filename = defaultFilename
        )
    }

    fun updateExportOrientation(orientation: PdfOrientation) {
        _exportConfig.value = _exportConfig.value.copy(orientation = orientation)
    }

    fun updateExportOption(
        includeLandDetails: Boolean? = null,
        includeFamilyRelationship: Boolean? = null,
        includeLandHistory: Boolean? = null,
        includeDocuments: Boolean? = null,
        includeNotes: Boolean? = null,
        includeImages: Boolean? = null,
        filename: String? = null
    ) {
        _exportConfig.value = _exportConfig.value.copy(
            includeLandDetails = includeLandDetails ?: _exportConfig.value.includeLandDetails,
            includeFamilyRelationship = includeFamilyRelationship ?: _exportConfig.value.includeFamilyRelationship,
            includeLandHistory = includeLandHistory ?: _exportConfig.value.includeLandHistory,
            includeDocuments = includeDocuments ?: _exportConfig.value.includeDocuments,
            includeNotes = includeNotes ?: _exportConfig.value.includeNotes,
            includeImages = includeImages ?: _exportConfig.value.includeImages,
            filename = filename ?: _exportConfig.value.filename
        )
    }

    fun generatePreview() {
        viewModelScope.launch {
            _isLoadingPreview.value = true
            try {
                val file = buildPdfFileInternal()
                val bitmaps = PdfRendererHelper.renderPdfPages(file, targetWidth = 900)
                _previewBitmaps.value = bitmaps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingPreview.value = false
            }
        }
    }

    fun generatePdf(onSuccess: (GeneratedPdfItem) -> Unit) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val file = buildPdfFileInternal()
                val pageCount = PdfRendererHelper.getPageCount(file)
                val item = GeneratedPdfItem(
                    id = file.name,
                    title = file.nameWithoutExtension.replace("_", " "),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeFormatted = "${(file.length() / 1024).coerceAtLeast(1)} KB",
                    pageCount = pageCount,
                    generatedAtMillis = System.currentTimeMillis(),
                    exportType = _exportConfig.value.exportMode.name.replace("_", " ")
                )
                _lastGeneratedPdf.value = item
                _generatedPdfs.value = listOf(item) + _generatedPdfs.value.filter { it.fileName != item.fileName }
                onSuccess(item)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    private suspend fun buildPdfFileInternal(): File {
        val config = _exportConfig.value
        val plotsList = allPlots.value
        val membersList = allFamilyMembers.value
        val docsList = allDocuments.value

        val plotIds = when (config.exportMode) {
            PdfExportMode.CURRENT_PLOT -> listOfNotNull(config.targetPlotId ?: plotsList.firstOrNull()?.id)
            PdfExportMode.SELECTED_PLOTS -> config.selectedPlotIds.toList().ifEmpty { plotsList.map { it.id } }
            PdfExportMode.ALL_PLOTS, PdfExportMode.FAMILY_WITH_LAND -> plotsList.map { it.id }
            else -> plotsList.map { it.id }
        }

        val historyMap = mutableMapOf<Long, List<LandHistoryEntity>>()
        val documentsMap = mutableMapOf<Long, List<AttachedDocumentEntity>>()

        for (pId in plotIds) {
            historyMap[pId] = repository.getHistoryListForPlot(pId)
            documentsMap[pId] = repository.getDocumentsListForPlot(pId)
        }

        return pdfGenerator.generatePdf(
            config = config,
            plots = plotsList,
            familyMembers = membersList,
            historyMap = historyMap,
            documentsMap = documentsMap,
            allDocuments = docsList
        )
    }

    fun savePlot(plot: PlotEntity, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = if (plot.id == 0L) {
                repository.insertPlot(plot.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            } else {
                repository.updatePlot(plot.copy(updatedAt = System.currentTimeMillis()))
                plot.id
            }
            onComplete?.invoke(id)
        }
    }

    fun deletePlot(plot: PlotEntity) {
        viewModelScope.launch {
            repository.deletePlot(plot)
            _selectedPlotIds.value = _selectedPlotIds.value - plot.id
        }
    }

    fun saveFamilyMember(member: FamilyMemberEntity, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = if (member.id == 0L) {
                repository.insertFamilyMember(member)
            } else {
                repository.updateFamilyMember(member)
                member.id
            }
            onComplete?.invoke(id)
        }
    }

    fun deleteFamilyMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            repository.deleteFamilyMember(member)
        }
    }

    fun addHistoryEvent(history: LandHistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(history)
            loadHistoryForPlot(history.plotId)
        }
    }

    fun deleteHistoryEvent(history: LandHistoryEntity) {
        viewModelScope.launch {
            repository.deleteHistory(history)
            loadHistoryForPlot(history.plotId)
        }
    }

    fun addDocument(doc: AttachedDocumentEntity) {
        viewModelScope.launch {
            repository.insertDocument(doc)
        }
    }

    fun deleteDocument(doc: AttachedDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            _selectedDocIds.value = _selectedDocIds.value - doc.id
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = BackupManager.createBackupJson(database)
            onResult(json)
        }
    }

    fun restoreBackup(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupManager.restoreFromJson(database, json)
            onResult(success)
        }
    }
}
