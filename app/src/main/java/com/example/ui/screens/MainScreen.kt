package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.PdfExportMode
import com.example.data.model.PlotEntity
import com.example.pdf.PdfPrintHelper
import com.example.ui.viewmodel.LandRecordViewModel
import java.io.File

enum class MainTab {
    DASHBOARD,
    PLOTS,
    FAMILY,
    DOCUMENTS,
    PRINT_HUB
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LandRecordViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var selectedPlotForDetail by remember { mutableStateOf<PlotEntity?>(null) }

    // Dialog states
    var plotToEdit by remember { mutableStateOf<PlotEntity?>(null) }
    var showAddEditPlotDialog by remember { mutableStateOf(false) }

    var memberToEdit by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var showAddEditMemberDialog by remember { mutableStateOf(false) }

    var targetPlotIdForDoc by remember { mutableStateOf<Long?>(null) }
    var showAddDocDialog by remember { mutableStateOf(false) }

    var targetPlotIdForHistory by remember { mutableStateOf<Long?>(null) }
    var showAddHistoryDialog by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Observable states from ViewModel
    val plots by viewModel.filteredPlots.collectAsStateWithLifecycle()
    val allPlotsRaw by viewModel.allPlots.collectAsStateWithLifecycle()
    val familyMembers by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val selectedPlotIds by viewModel.selectedPlotIds.collectAsStateWithLifecycle()
    val selectedDocIds by viewModel.selectedDocIds.collectAsStateWithLifecycle()
    val exportConfig by viewModel.exportConfig.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val isLoadingPreview by viewModel.isLoadingPreview.collectAsStateWithLifecycle()
    val lastGeneratedPdf by viewModel.lastGeneratedPdf.collectAsStateWithLifecycle()
    val previewBitmaps by viewModel.previewBitmaps.collectAsStateWithLifecycle()
    val generatedPdfs by viewModel.generatedPdfs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activePlotHistory by viewModel.activePlotHistory.collectAsStateWithLifecycle()

    // When viewing plot detail, load history
    LaunchedEffect(selectedPlotForDetail?.id) {
        selectedPlotForDetail?.let {
            viewModel.loadHistoryForPlot(it.id)
        }
    }

    // Keep detail plot synced with database updates
    LaunchedEffect(allPlotsRaw) {
        if (selectedPlotForDetail != null) {
            val updated = allPlotsRaw.firstOrNull { it.id == selectedPlotForDetail!!.id }
            if (updated != null) {
                selectedPlotForDetail = updated
            }
        }
    }

    // Back button handling
    BackHandler(enabled = selectedPlotForDetail != null || currentTab != MainTab.DASHBOARD || showHistoryDialog) {
        if (showHistoryDialog) {
            showHistoryDialog = false
        } else if (selectedPlotForDetail != null) {
            selectedPlotForDetail = null
        } else if (currentTab != MainTab.DASHBOARD) {
            currentTab = MainTab.DASHBOARD
        }
    }

    if (selectedPlotForDetail != null) {
        val plot = selectedPlotForDetail!!
        PlotDetailScreen(
            plot = plot,
            allMembers = familyMembers,
            historyList = activePlotHistory,
            documentsList = documents.filter { it.plotId == plot.id },
            onBack = { selectedPlotForDetail = null },
            onEdit = {
                plotToEdit = plot
                showAddEditPlotDialog = true
            },
            onAddDocument = {
                targetPlotIdForDoc = plot.id
                showAddDocDialog = true
            },
            onAddHistory = {
                targetPlotIdForHistory = plot.id
                showAddHistoryDialog = true
            },
            onAddFamilyMember = {
                memberToEdit = null
                showAddEditMemberDialog = true
            },
            onExportPdf = {
                viewModel.configureExport(PdfExportMode.CURRENT_PLOT, targetPlotId = plot.id)
                showExportDialog = true
            },
            onShare = {
                viewModel.configureExport(PdfExportMode.CURRENT_PLOT, targetPlotId = plot.id)
                viewModel.generatePdf { item ->
                    PdfPrintHelper.sharePdf(context, File(item.filePath), "Share Plot ${plot.plotNumber} Record")
                }
            },
            onDelete = {
                viewModel.deletePlot(plot)
                selectedPlotForDetail = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                MainTab.DASHBOARD -> "Land Record Dashboard"
                                MainTab.PLOTS -> "Land Plots Registry"
                                MainTab.FAMILY -> "Family Tree & Heritage"
                                MainTab.DOCUMENTS -> "Deeds & Documents"
                                MainTab.PRINT_HUB -> "PDF Print & Xerox Hub"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier.testTag("btn_top_history")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "PDF Archive History")
                        }

                        IconButton(
                            onClick = {
                                viewModel.configureExport(PdfExportMode.ALL_PLOTS)
                                showExportDialog = true
                            },
                            modifier = Modifier.testTag("btn_top_export_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Quick Export PDF")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.DASHBOARD,
                        onClick = { currentTab = MainTab.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.PLOTS,
                        onClick = { currentTab = MainTab.PLOTS },
                        icon = { Icon(Icons.Default.Landscape, contentDescription = "Plots") },
                        label = { Text("Plots") },
                        modifier = Modifier.testTag("tab_plots")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.FAMILY,
                        onClick = { currentTab = MainTab.FAMILY },
                        icon = { Icon(Icons.Default.People, contentDescription = "Family") },
                        label = { Text("Family") },
                        modifier = Modifier.testTag("tab_family")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.DOCUMENTS,
                        onClick = { currentTab = MainTab.DOCUMENTS },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Docs") },
                        label = { Text("Docs") },
                        modifier = Modifier.testTag("tab_docs")
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.PRINT_HUB,
                        onClick = { currentTab = MainTab.PRINT_HUB },
                        icon = { Icon(Icons.Default.Print, contentDescription = "Print Hub") },
                        label = { Text("Print Hub") },
                        modifier = Modifier.testTag("tab_print_hub")
                    )
                }
            },
            modifier = Modifier.fillMaxSize().testTag("screen_main")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showHistoryDialog) {
                    PdfHistoryScreen(generatedPdfs = generatedPdfs)
                } else {
                    when (currentTab) {
                        MainTab.DASHBOARD -> {
                            DashboardScreen(
                                plots = allPlotsRaw,
                                familyMembers = familyMembers,
                                documents = documents,
                                onAddPlot = {
                                    plotToEdit = null
                                    showAddEditPlotDialog = true
                                },
                                onAddFamilyMember = {
                                    memberToEdit = null
                                    showAddEditMemberDialog = true
                                },
                                onAddDocument = {
                                    targetPlotIdForDoc = allPlotsRaw.firstOrNull()?.id
                                    showAddDocDialog = true
                                },
                                onExportPdf = {
                                    viewModel.configureExport(PdfExportMode.ALL_PLOTS)
                                    showExportDialog = true
                                },
                                onPlotClick = { selectedPlotForDetail = it },
                                onBackupExport = { onResult ->
                                    viewModel.exportBackup(onResult)
                                },
                                onRestoreImport = { json, onResult ->
                                    viewModel.restoreBackup(json, onResult)
                                }
                            )
                        }

                        MainTab.PLOTS -> {
                            PlotsListScreen(
                                plots = plots,
                                selectedPlotIds = selectedPlotIds,
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onToggleSelectPlot = { viewModel.togglePlotSelection(it) },
                                onSelectAllPlots = { viewModel.selectAllPlots(it) },
                                onPlotClick = { selectedPlotForDetail = it },
                                onExportCurrentPlot = { plot ->
                                    viewModel.configureExport(PdfExportMode.CURRENT_PLOT, targetPlotId = plot.id)
                                    showExportDialog = true
                                },
                                onExportSelectedPlots = {
                                    viewModel.configureExport(PdfExportMode.SELECTED_PLOTS)
                                    showExportDialog = true
                                },
                                onExportAllPlots = {
                                    viewModel.configureExport(PdfExportMode.ALL_PLOTS)
                                    showExportDialog = true
                                },
                                onAddPlotClick = {
                                    plotToEdit = null
                                    showAddEditPlotDialog = true
                                }
                            )
                        }

                        MainTab.FAMILY -> {
                            FamilyScreen(
                                members = familyMembers,
                                plots = allPlotsRaw,
                                onAddMember = {
                                    memberToEdit = null
                                    showAddEditMemberDialog = true
                                },
                                onEditMember = { member ->
                                    memberToEdit = member
                                    showAddEditMemberDialog = true
                                },
                                onDeleteMember = { member ->
                                    viewModel.deleteFamilyMember(member)
                                },
                                onExportMemberPdf = { member ->
                                    viewModel.configureExport(PdfExportMode.FAMILY_MEMBER, targetMemberId = member.id)
                                    showExportDialog = true
                                },
                                onExportFamilyWithLandPdf = {
                                    viewModel.configureExport(PdfExportMode.FAMILY_WITH_LAND)
                                    showExportDialog = true
                                }
                            )
                        }

                        MainTab.DOCUMENTS -> {
                            DocumentsScreen(
                                documents = documents,
                                selectedDocIds = selectedDocIds,
                                onToggleSelectDoc = { viewModel.toggleDocSelection(it) },
                                onExportCombinedDocs = {
                                    viewModel.configureExport(PdfExportMode.COMBINED_DOCUMENTS)
                                    showExportDialog = true
                                }
                            )
                        }

                        MainTab.PRINT_HUB -> {
                            PdfHubScreen(
                                plots = allPlotsRaw,
                                members = familyMembers,
                                onExportCurrentPlot = { plot ->
                                    viewModel.configureExport(PdfExportMode.CURRENT_PLOT, targetPlotId = plot.id)
                                    showExportDialog = true
                                },
                                onExportSelectedPlots = {
                                    viewModel.configureExport(PdfExportMode.SELECTED_PLOTS)
                                    showExportDialog = true
                                },
                                onExportAllPlots = {
                                    viewModel.configureExport(PdfExportMode.ALL_PLOTS)
                                    showExportDialog = true
                                },
                                onExportFamilyMember = { member ->
                                    viewModel.configureExport(PdfExportMode.FAMILY_MEMBER, targetMemberId = member.id)
                                    showExportDialog = true
                                },
                                onExportFamilyWithLand = {
                                    viewModel.configureExport(PdfExportMode.FAMILY_WITH_LAND)
                                    showExportDialog = true
                                },
                                onExportCombinedDocs = {
                                    viewModel.configureExport(PdfExportMode.COMBINED_DOCUMENTS)
                                    showExportDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Add / Edit Plot Dialog
    if (showAddEditPlotDialog) {
        AddEditPlotDialog(
            plotToEdit = plotToEdit,
            allMembers = familyMembers,
            onDismiss = { showAddEditPlotDialog = false },
            onSave = { updatedPlot ->
                viewModel.savePlot(updatedPlot) { savedId ->
                    showAddEditPlotDialog = false
                    if (selectedPlotForDetail?.id == updatedPlot.id || selectedPlotForDetail?.id == savedId) {
                        selectedPlotForDetail = updatedPlot.copy(id = savedId)
                    }
                }
            },
            onQuickAddPerson = {
                memberToEdit = null
                showAddEditMemberDialog = true
            }
        )
    }

    // 2. Add / Edit Family Member Dialog
    if (showAddEditMemberDialog) {
        AddEditFamilyMemberDialog(
            memberToEdit = memberToEdit,
            allMembers = familyMembers,
            onDismiss = { showAddEditMemberDialog = false },
            onSave = { updatedMember ->
                viewModel.saveFamilyMember(updatedMember)
                showAddEditMemberDialog = false
            }
        )
    }

    // 3. Add Document Dialog
    if (showAddDocDialog) {
        AddDocumentDialog(
            plotId = targetPlotIdForDoc,
            familyMemberId = null,
            onDismiss = { showAddDocDialog = false },
            onSave = { doc ->
                viewModel.addDocument(doc)
                showAddDocDialog = false
            }
        )
    }

    // 4. Add History Dialog
    if (showAddHistoryDialog && targetPlotIdForHistory != null) {
        AddHistoryDialog(
            plotId = targetPlotIdForHistory!!,
            allMembers = familyMembers,
            onDismiss = { showAddHistoryDialog = false },
            onSave = { history ->
                viewModel.addHistoryEvent(history)
                showAddHistoryDialog = false
            }
        )
    }

    // 5. PDF Export Dialog
    if (showExportDialog) {
        PdfExportDialog(
            config = exportConfig,
            isGenerating = isGenerating,
            isLoadingPreview = isLoadingPreview,
            previewBitmaps = previewBitmaps,
            lastGeneratedPdf = lastGeneratedPdf,
            onDismiss = { showExportDialog = false },
            onOrientationChange = { viewModel.updateExportOrientation(it) },
            onOptionChange = { land, fam, hist, docs, notes, images, fname ->
                viewModel.updateExportOption(
                    includeLandDetails = land,
                    includeFamilyRelationship = fam,
                    includeLandHistory = hist,
                    includeDocuments = docs,
                    includeNotes = notes,
                    includeImages = images,
                    filename = fname
                )
            },
            onRequestPreview = { viewModel.generatePreview() },
            onGeneratePdf = { onSuccess ->
                viewModel.generatePdf(onSuccess)
            }
        )
    }
}
