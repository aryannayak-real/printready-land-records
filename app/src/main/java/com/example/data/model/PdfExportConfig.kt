package com.example.data.model

enum class PdfOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class PdfExportMode {
    CURRENT_PLOT,
    SELECTED_PLOTS,
    ALL_PLOTS,
    FAMILY_MEMBER,
    FAMILY_WITH_LAND,
    COMBINED_DOCUMENTS
}

data class PdfExportConfig(
    val exportMode: PdfExportMode = PdfExportMode.CURRENT_PLOT,
    val orientation: PdfOrientation = PdfOrientation.PORTRAIT,
    val includeLandDetails: Boolean = true,
    val includeFamilyRelationship: Boolean = true,
    val includeLandHistory: Boolean = true,
    val includeDocuments: Boolean = true,
    val includeNotes: Boolean = true,
    val includeImages: Boolean = true,
    val filename: String = "Land_Record.pdf",
    val selectedPlotIds: Set<Long> = emptySet(),
    val targetPlotId: Long? = null,
    val targetFamilyMemberId: Long? = null,
    val selectedDocumentIds: Set<Long> = emptySet()
)

data class GeneratedPdfItem(
    val id: String,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val pageCount: Int,
    val generatedAtMillis: Long,
    val exportType: String
)
