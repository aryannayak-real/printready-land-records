package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.AttachedDocumentEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LandHistoryEntity
import com.example.data.model.PdfExportConfig
import com.example.data.model.PdfExportMode
import com.example.data.model.PdfOrientation
import com.example.data.model.PlotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class PdfDocumentGenerator(private val context: Context) {

    private val dateFormatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

    suspend fun generatePdf(
        config: PdfExportConfig,
        plots: List<PlotEntity>,
        familyMembers: List<FamilyMemberEntity>,
        historyMap: Map<Long, List<LandHistoryEntity>>,
        documentsMap: Map<Long, List<AttachedDocumentEntity>>,
        allDocuments: List<AttachedDocumentEntity>
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "pdfs")
        if (!outputDir.exists()) outputDir.mkdirs()

        val cleanFilename = sanitizeFileName(config.filename)
        val finalFile = File(outputDir, cleanFilename)

        // Two-pass generation: Pass 1 calculates total page count, Pass 2 prints "Page X of TotalPages"
        val totalPages = executeGeneration(
            config = config,
            plots = plots,
            familyMembers = familyMembers,
            historyMap = historyMap,
            documentsMap = documentsMap,
            allDocuments = allDocuments,
            knownTotalPages = null,
            targetFile = null
        )

        executeGeneration(
            config = config,
            plots = plots,
            familyMembers = familyMembers,
            historyMap = historyMap,
            documentsMap = documentsMap,
            allDocuments = allDocuments,
            knownTotalPages = totalPages,
            targetFile = finalFile
        )

        finalFile
    }

    private fun executeGeneration(
        config: PdfExportConfig,
        plots: List<PlotEntity>,
        familyMembers: List<FamilyMemberEntity>,
        historyMap: Map<Long, List<LandHistoryEntity>>,
        documentsMap: Map<Long, List<AttachedDocumentEntity>>,
        allDocuments: List<AttachedDocumentEntity>,
        knownTotalPages: Int?,
        targetFile: File?
    ): Int {
        val isLandscape = config.orientation == PdfOrientation.LANDSCAPE
        val pageWidth = if (isLandscape) PdfLayoutConstants.A4_HEIGHT_POINTS else PdfLayoutConstants.A4_WIDTH_POINTS
        val pageHeight = if (isLandscape) PdfLayoutConstants.A4_WIDTH_POINTS else PdfLayoutConstants.A4_HEIGHT_POINTS

        val pdfDocument = PdfDocument()
        val pageBuilder = PdfPageSession(
            pdfDocument = pdfDocument,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            knownTotalPages = knownTotalPages,
            docTitle = when (config.exportMode) {
                PdfExportMode.CURRENT_PLOT -> "FAMILY LAND RECORD"
                PdfExportMode.SELECTED_PLOTS -> "FAMILY LAND RECORD SUMMARY & DOSSIER"
                PdfExportMode.ALL_PLOTS -> "FAMILY LAND PORTFOLIO RECORD"
                PdfExportMode.FAMILY_MEMBER -> "FAMILY & LAND RECORD"
                PdfExportMode.FAMILY_WITH_LAND -> "FAMILY LINEAGE & LAND HOLDINGS"
                PdfExportMode.COMBINED_DOCUMENTS -> "LAND DOCUMENTS & DEED ARCHIVE"
            },
            formattedDate = dateFormatter.format(Date())
        )

        // Route to the appropriate rendering logic
        when (config.exportMode) {
            PdfExportMode.CURRENT_PLOT -> {
                val targetPlot = plots.firstOrNull { it.id == config.targetPlotId } ?: plots.firstOrNull()
                if (targetPlot != null) {
                    renderSinglePlotRecord(
                        session = pageBuilder,
                        config = config,
                        plot = targetPlot,
                        familyMembers = familyMembers,
                        history = historyMap[targetPlot.id] ?: emptyList(),
                        documents = documentsMap[targetPlot.id] ?: emptyList(),
                        isMultiple = false
                    )
                }
            }

            PdfExportMode.SELECTED_PLOTS, PdfExportMode.ALL_PLOTS -> {
                val selectedPlots = if (config.exportMode == PdfExportMode.ALL_PLOTS) {
                    plots
                } else {
                    plots.filter { config.selectedPlotIds.contains(it.id) }.ifEmpty { plots }
                }

                // Render Summary Table first
                renderPlotsSummaryTable(session = pageBuilder, plots = selectedPlots)

                // Render each plot's detailed dossier
                for (plot in selectedPlots) {
                    pageBuilder.forceNewPage()
                    renderSinglePlotRecord(
                        session = pageBuilder,
                        config = config,
                        plot = plot,
                        familyMembers = familyMembers,
                        history = historyMap[plot.id] ?: emptyList(),
                        documents = documentsMap[plot.id] ?: emptyList(),
                        isMultiple = true
                    )
                }
            }

            PdfExportMode.FAMILY_MEMBER -> {
                val targetMember = familyMembers.firstOrNull { it.id == config.targetFamilyMemberId } ?: familyMembers.firstOrNull()
                if (targetMember != null) {
                    renderFamilyMemberDetails(
                        session = pageBuilder,
                        config = config,
                        member = targetMember,
                        allMembers = familyMembers,
                        allPlots = plots,
                        allDocs = allDocuments
                    )
                }
            }

            PdfExportMode.FAMILY_WITH_LAND -> {
                renderFamilyWithLandRecords(
                    session = pageBuilder,
                    config = config,
                    members = familyMembers,
                    plots = plots,
                    historyMap = historyMap,
                    documentsMap = documentsMap
                )
            }

            PdfExportMode.COMBINED_DOCUMENTS -> {
                val docsToExport = if (config.selectedDocumentIds.isNotEmpty()) {
                    allDocuments.filter { config.selectedDocumentIds.contains(it.id) }
                } else {
                    allDocuments
                }
                renderCombinedDocumentsRecord(
                    session = pageBuilder,
                    config = config,
                    documents = docsToExport,
                    plots = plots
                )
            }
        }

        // Close last page
        pageBuilder.finishCurrentPage(isLastPage = true)

        val totalPagesGenerated = pageBuilder.currentPageNumber

        if (targetFile != null) {
            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }
        }

        pdfDocument.close()
        return totalPagesGenerated
    }

    private fun renderPlotsSummaryTable(session: PdfPageSession, plots: List<PlotEntity>) {
        session.drawMainHeader("FAMILY LAND RECORD SUMMARY", "Consolidated Portfolio of Land Parcels")

        session.ensureSpace(60f)
        val headers = listOf("Sl. No.", "Plot No.", "Khata No.", "Village", "Area", "Recorded Owner")
        val colWeights = floatArrayOf(0.08f, 0.16f, 0.16f, 0.20f, 0.18f, 0.22f)

        val rows = plots.mapIndexed { index, plot ->
            listOf(
                (index + 1).toString(),
                plot.plotNumber,
                plot.khataNumber,
                plot.village,
                "${plot.originalArea} ${plot.originalUnit}",
                plot.recordedOwner
            )
        }

        session.drawTable(headers, colWeights, rows)

        session.ensureSpace(50f)
        session.drawNoticeBox(
            "Note: Detailed dossier for each parcel follows on the subsequent pages. Check individual section entries for history, lineage, and attached documents."
        )
    }

    private fun renderSinglePlotRecord(
        session: PdfPageSession,
        config: PdfExportConfig,
        plot: PlotEntity,
        familyMembers: List<FamilyMemberEntity>,
        history: List<LandHistoryEntity>,
        documents: List<AttachedDocumentEntity>,
        isMultiple: Boolean
    ) {
        val title = if (isMultiple) "PLOT DOSSIER: PLOT NO. ${plot.plotNumber}" else "FAMILY LAND RECORD"
        val subtitle = "Khata No. ${plot.khataNumber} | Village: ${plot.village} | Tahasil: ${plot.tahasil}"
        session.drawMainHeader(title, subtitle)

        if (config.includeLandDetails) {
            // 1. Land Identification
            session.drawSectionHeading("1. Land Identification")
            val idData = listOf(
                listOf("Plot Number:", plot.plotNumber, "Khata Number:", plot.khataNumber),
                listOf("Land Record Ref:", plot.landRecordNumber.ifEmpty { "Not Recorded" }, "Land Type:", plot.landType.ifEmpty { "Agricultural" }),
                listOf("Ownership Status:", plot.ownershipStatus, "Source of Record:", plot.sourceOfRecord.ifEmpty { "Official Register" })
            )
            session.drawKeyValueGrid(idData)

            // 2. Location
            session.drawSectionHeading("2. Location")
            val locData = listOf(
                listOf("Village:", plot.village, "Gram Panchayat:", plot.gramPanchayat.ifEmpty { "-" }),
                listOf("Tahasil:", plot.tahasil, "District:", plot.district),
                listOf("State:", plot.state, "PIN Code:", plot.pinCode.ifEmpty { "-" })
            )
            session.drawKeyValueGrid(locData)

            // 3. Land Area
            session.drawSectionHeading("3. Land Area")
            val areaData = listOf(
                listOf("Original Area:", "${plot.originalArea} ${plot.originalUnit}", "Converted Area:", plot.convertedAreaText.ifEmpty { "Standard metric conversion not logged" })
            )
            session.drawKeyValueGrid(areaData)

            // 4. Recorded Ownership Information
            session.drawSectionHeading("4. Recorded Ownership Information")
            val ownerData = listOf(
                listOf("Recorded Owner:", plot.recordedOwner.ifEmpty { "Unspecified" }, "Previous Owner:", plot.previousOwner.ifEmpty { "Not specified" }),
                listOf("Relationship to User:", plot.relationshipToUser.ifEmpty { "Ancestral" }, "Relationship Detail:", plot.relationshipDescription.ifEmpty { "-" }),
                listOf("Date Acquired / Inherited:", plot.dateAcquired.ifEmpty { "Recorded in RoR" }, "Document Ref:", plot.documentReference.ifEmpty { "-" })
            )
            session.drawKeyValueGrid(ownerData)
        }

        // 5. Family Relationship / Connection (dynamically traced from real Family Members database)
        if (config.includeFamilyRelationship) {
            session.drawSectionHeading("5. Family Connection")
            val lineageSteps = com.example.data.model.FamilyTreeHelper.traceLineageToUser(
                plot.recordedOwnerId ?: plot.previousOwnerId,
                familyMembers
            )
            if (lineageSteps.isNotEmpty()) {
                val formattedLineage = com.example.data.model.FamilyTreeHelper.formatLineageWithArrows(lineageSteps)
                session.drawFamilyLineageBox(formattedLineage)
            } else if (plot.relationshipToUser.isNotBlank()) {
                val fallbackLineage = "${plot.recordedOwner} (${plot.relationshipToUser})\n↓\nUser"
                session.drawFamilyLineageBox(fallbackLineage)
            } else {
                session.drawSimpleText("No direct pedigree lineage registered for this parcel.")
            }
        }

        // 6. Land History
        if (config.includeLandHistory) {
            session.drawSectionHeading("6. Land History")
            if (history.isEmpty()) {
                session.drawSimpleText("No historical transactions or mutations logged for this plot.")
            } else {
                val histHeaders = listOf("Date", "Event", "From Person", "To Person", "Area", "Document Ref")
                val histWeights = floatArrayOf(0.14f, 0.20f, 0.18f, 0.18f, 0.12f, 0.18f)
                val histRows = history.map {
                    listOf(
                        it.eventDate,
                        it.eventName,
                        it.fromPerson,
                        it.toPerson,
                        it.areaTransferred,
                        it.documentReference
                    )
                }
                session.drawTable(histHeaders, histWeights, histRows)
            }
        }

        // 7. Documents
        if (config.includeDocuments) {
            session.drawSectionHeading("7. Attached Documents")
            if (documents.isEmpty()) {
                session.drawSimpleText("No documents linked to this plot record.")
            } else {
                val docHeaders = listOf("Document Name", "Type", "Doc Number", "Date", "Issuing Authority")
                val docWeights = floatArrayOf(0.26f, 0.20f, 0.18f, 0.14f, 0.22f)
                val docRows = documents.map {
                    listOf(
                        it.documentName,
                        it.documentType,
                        it.documentNumber,
                        it.date,
                        it.issuingAuthority
                    )
                }
                session.drawTable(docHeaders, docWeights, docRows)
            }
        }

        // 8. Notes
        if (config.includeNotes && plot.notes.isNotBlank()) {
            session.drawSectionHeading("8. Notes & Remarks")
            session.drawBorderedTextBox(plot.notes)
        }

        // 9. Location (GPS)
        if (plot.latitude != null && plot.longitude != null) {
            session.drawSectionHeading("9. Geographic Location (GPS)")
            val coordText = String.format(Locale.US, "Latitude: %.5f° N  |  Longitude: %.5f° E", plot.latitude, plot.longitude)
            session.drawKeyValueGrid(listOf(listOf("GPS Coordinates:", coordText, "Demarcation:", "Reference Pin")))
            session.drawNoticeBox("Note: GPS coordinates are saved for personal reference only and do not constitute proof of legal survey boundaries.")
        }

        // Attached Document Scans (images)
        if (config.includeImages) {
            val docsWithImages = documents.filter { !it.imagePath.isNullOrBlank() && File(it.imagePath).exists() }
            for (doc in docsWithImages) {
                session.forceNewPage()
                session.drawMainHeader("ATTACHED DOCUMENT: ${doc.documentName.uppercase()}", "Type: ${doc.documentType} | Ref: ${doc.documentNumber}")
                session.drawDocumentImagePage(doc)
            }
        }
    }

    private fun renderFamilyMemberDetails(
        session: PdfPageSession,
        config: PdfExportConfig,
        member: FamilyMemberEntity,
        allMembers: List<FamilyMemberEntity>,
        allPlots: List<PlotEntity>,
        allDocs: List<AttachedDocumentEntity>
    ) {
        session.drawMainHeader("FAMILY & LAND RECORD", "Family Member Dossier: ${member.fullName}")

        val father = allMembers.firstOrNull { it.id == member.fatherId }
        val mother = allMembers.firstOrNull { it.id == member.motherId }
        val spouse = allMembers.firstOrNull { it.id == member.spouseId }
        val children = allMembers.filter { it.fatherId == member.id || it.motherId == member.id }

        val parentsText = listOfNotNull(father?.fullName, mother?.fullName).joinToString(" & ").ifEmpty { "Not recorded" }
        val spouseText = spouse?.fullName ?: "Not recorded"
        val childrenText = if (children.isEmpty()) "None recorded" else children.joinToString { it.fullName }

        session.drawSectionHeading("1. Member Profile & Lineage")
        val profileData = listOf(
            listOf("Full Name:", member.fullName, "Relationship to User:", member.relationshipToUser.ifEmpty { "Family Member" }),
            listOf("Generation Level:", "Gen ${member.generationLevel}", "Spouse:", spouseText),
            listOf("Parents:", parentsText, "Children:", childrenText)
        )
        session.drawKeyValueGrid(profileData)

        // Lineage Tree Diagram
        session.drawSectionHeading("2. Family Lineage Representation")
        val lineageSteps = com.example.data.model.FamilyTreeHelper.traceLineageToUser(member.id, allMembers)
        val treeText = if (lineageSteps.isNotEmpty()) {
            com.example.data.model.FamilyTreeHelper.formatLineageWithArrows(lineageSteps)
        } else {
            "Parents: $parentsText\n    ↓\nMember: ${member.fullName} (${member.relationshipToUser})\n    ↓ (Spouse: $spouseText)\nChildren: $childrenText"
        }
        session.drawFamilyLineageBox(treeText)

        // Linked Plots
        session.drawSectionHeading("3. Linked Land Parcels")
        val linkedPlotsList = allPlots.filter { plot ->
            plot.recordedOwnerId == member.id ||
                    plot.previousOwnerId == member.id ||
                    plot.recordedOwner.contains(member.fullName, ignoreCase = true)
        }

        if (linkedPlotsList.isEmpty()) {
            session.drawSimpleText("No land records currently linked to ${member.fullName}.")
        } else {
            val headers = listOf("Plot No.", "Khata No.", "Village", "Area", "Ownership Status")
            val weights = floatArrayOf(0.18f, 0.18f, 0.24f, 0.18f, 0.22f)
            val rows = linkedPlotsList.map {
                listOf(it.plotNumber, it.khataNumber, it.village, "${it.originalArea} ${it.originalUnit}", it.ownershipStatus)
            }
            session.drawTable(headers, weights, rows)
        }

        // Linked Documents
        session.drawSectionHeading("4. Linked Legal Documents")
        val linkedDocsList = allDocs.filter { doc ->
            doc.familyMemberId == member.id ||
                    linkedPlotsList.any { it.id == doc.plotId }
        }
        if (linkedDocsList.isEmpty()) {
            session.drawSimpleText("No specific deeds or documents registered under this member.")
        } else {
            val docHeaders = listOf("Document Name", "Type", "Doc Number", "Date", "Authority")
            val docWeights = floatArrayOf(0.28f, 0.20f, 0.18f, 0.14f, 0.20f)
            val docRows = linkedDocsList.map {
                listOf(it.documentName, it.documentType, it.documentNumber, it.date, it.issuingAuthority)
            }
            session.drawTable(docHeaders, docWeights, docRows)
        }

        if (member.notes.isNotBlank()) {
            session.drawSectionHeading("5. Remarks & Biographical Notes")
            session.drawBorderedTextBox(member.notes)
        }
    }

    private fun renderFamilyWithLandRecords(
        session: PdfPageSession,
        config: PdfExportConfig,
        members: List<FamilyMemberEntity>,
        plots: List<PlotEntity>,
        historyMap: Map<Long, List<LandHistoryEntity>>,
        documentsMap: Map<Long, List<AttachedDocumentEntity>>
    ) {
        session.drawMainHeader("FAMILY LINEAGE & LAND HOLDINGS", "Complete Ancestral Lineage & Land Portfolio")

        // Family Tree Overview
        session.drawSectionHeading("1. Family Lineage Overview")
        val lineageOverview = buildString {
            val grouped = members.groupBy { it.generationLevel }.toSortedMap()
            grouped.forEach { (gen, list) ->
                append("Generation $gen:\n")
                list.forEach { m ->
                    val memberPlots = plots.filter { p ->
                        p.recordedOwnerId == m.id || p.previousOwnerId == m.id || p.recordedOwner.contains(m.fullName, ignoreCase = true)
                    }
                    val plotsText = if (memberPlots.isEmpty()) "None" else memberPlots.joinToString { "Plot ${it.plotNumber}" }
                    append("  • ${m.fullName} (${m.relationshipToUser}) - Plots: $plotsText\n")
                }
            }
        }
        session.drawBorderedTextBox(lineageOverview.trimEnd())

        // Family Members Roster Table
        session.drawSectionHeading("2. Family Member Roster")
        val headers = listOf("Name", "Relationship", "Father", "Mother", "Status")
        val weights = floatArrayOf(0.26f, 0.22f, 0.20f, 0.18f, 0.14f)
        val memberMap = members.associateBy { it.id }
        val rows = members.map { m ->
            val fatherName = m.fatherId?.let { memberMap[it]?.fullName } ?: "-"
            val motherName = m.motherId?.let { memberMap[it]?.fullName } ?: "-"
            listOf(m.fullName, m.relationshipToUser, fatherName, motherName, if (m.isDeceased) "Late" else "Living")
        }
        session.drawTable(headers, weights, rows)

        // All Linked Plots
        session.ensureSpace(80f)
        session.drawSectionHeading("3. Consolidated Land Parcels")
        renderPlotsSummaryTable(session, plots)
    }

    private fun renderCombinedDocumentsRecord(
        session: PdfPageSession,
        config: PdfExportConfig,
        documents: List<AttachedDocumentEntity>,
        plots: List<PlotEntity>
    ) {
        session.drawMainHeader("LAND DOCUMENTS & DEED ARCHIVE", "Combined Legal Papers & Certified Photocopies")

        // Document Index Table
        session.drawSectionHeading("1. Document Master Index")
        val headers = listOf("Sl.", "Document Title", "Type", "Ref Number", "Issuing Authority", "Date")
        val weights = floatArrayOf(0.06f, 0.26f, 0.20f, 0.18f, 0.18f, 0.12f)
        val rows = documents.mapIndexed { index, doc ->
            listOf(
                (index + 1).toString(),
                doc.documentName,
                doc.documentType,
                doc.documentNumber,
                doc.issuingAuthority,
                doc.date
            )
        }
        session.drawTable(headers, weights, rows)

        session.ensureSpace(60f)
        session.drawNoticeBox(
            "The following pages reproduce the certified attached documents and scans in full A4 format for official reference and photocopy (Xerox) purposes."
        )

        // Render each attached document image
        for ((idx, doc) in documents.withIndex()) {
            session.forceNewPage()
            session.drawMainHeader(
                "DOCUMENT #${idx + 1}: ${doc.documentName.uppercase()}",
                "Type: ${doc.documentType} | Number: ${doc.documentNumber} | Authority: ${doc.issuingAuthority}"
            )
            session.drawDocumentImagePage(doc)
        }
    }

    private fun sanitizeFileName(name: String): String {
        var clean = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        if (!clean.endsWith(".pdf", ignoreCase = true)) {
            clean += ".pdf"
        }
        return clean
    }
}

/**
 * Manages pagination, margins, running headers, running footers, tables, and Xerox high-contrast drawing.
 */
class PdfPageSession(
    private val pdfDocument: PdfDocument,
    val pageWidth: Int,
    val pageHeight: Int,
    private val knownTotalPages: Int?,
    private val docTitle: String,
    private val formattedDate: String
) {
    var currentPageNumber = 0
        private set

    private var currentPdfPage: PdfDocument.Page? = null
    var canvas: Canvas? = null
        private set

    var currentY = 0f
        private set

    val contentLeft = PdfLayoutConstants.MARGIN_LEFT
    val contentRight = pageWidth - PdfLayoutConstants.MARGIN_RIGHT
    val contentWidth = contentRight - contentLeft
    val maxContentY = pageHeight - PdfLayoutConstants.MARGIN_BOTTOM - PdfLayoutConstants.FOOTER_HEIGHT

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = PdfLayoutConstants.COLOR_INK_BLACK
    }

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = PdfLayoutConstants.COLOR_BORDER_GRAY
        strokeWidth = 1f
    }

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    init {
        startNewPage()
    }

    fun startNewPage() {
        currentPageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        currentPdfPage = page
        val c = page.canvas
        canvas = c

        // Clear page with pure white background
        c.drawColor(PdfLayoutConstants.COLOR_WHITE)

        // Draw Running Header
        drawRunningHeader(c)

        currentY = PdfLayoutConstants.MARGIN_TOP + PdfLayoutConstants.HEADER_HEIGHT + 10f
    }

    fun forceNewPage() {
        finishCurrentPage(isLastPage = false)
        startNewPage()
    }

    fun finishCurrentPage(isLastPage: Boolean) {
        val c = canvas ?: return
        val page = currentPdfPage ?: return

        // If this is the last page, draw the required legal disclaimer at the bottom
        if (isLastPage) {
            drawLegalDisclaimer(c)
        }

        // Draw Running Footer
        drawRunningFooter(c)

        pdfDocument.finishPage(page)
        currentPdfPage = null
        canvas = null
    }

    fun ensureSpace(neededHeight: Float) {
        if (currentY + neededHeight > maxContentY) {
            forceNewPage()
        }
    }

    private fun drawRunningHeader(c: Canvas) {
        val top = PdfLayoutConstants.MARGIN_TOP
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 10f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawText(docTitle, contentLeft, top + 14f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        textPaint.textAlign = Paint.Align.RIGHT
        c.drawText("Date: $formattedDate", contentRight, top + 14f, textPaint)

        // Rule line under header
        strokePaint.strokeWidth = 1f
        strokePaint.color = PdfLayoutConstants.COLOR_BORDER_GRAY
        c.drawLine(contentLeft, top + 22f, contentRight, top + 22f, strokePaint)
    }

    private fun drawRunningFooter(c: Canvas) {
        val footerY = pageHeight - PdfLayoutConstants.MARGIN_BOTTOM
        strokePaint.strokeWidth = 0.75f
        strokePaint.color = PdfLayoutConstants.COLOR_LIGHT_BORDER
        c.drawLine(contentLeft, footerY - 14f, contentRight, footerY - 14f, strokePaint)

        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 8f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xFF555555.toInt()
        c.drawText("Family Land Record Archive • Print / Xerox Edition", contentLeft, footerY, textPaint)

        val totalStr = knownTotalPages?.toString() ?: ""
        val pageNumText = if (totalStr.isNotEmpty()) "Page $currentPageNumber of $totalStr" else "Page $currentPageNumber"
        textPaint.textAlign = Paint.Align.RIGHT
        c.drawText(pageNumText, contentRight, footerY, textPaint)
    }

    private fun drawLegalDisclaimer(c: Canvas) {
        val disclaimerTop = pageHeight - PdfLayoutConstants.MARGIN_BOTTOM - PdfLayoutConstants.FOOTER_HEIGHT - PdfLayoutConstants.DISCLAIMER_BOX_HEIGHT + 6f
        val boxRect = RectF(contentLeft, disclaimerTop, contentRight, disclaimerTop + PdfLayoutConstants.DISCLAIMER_BOX_HEIGHT)

        // Light gray background
        fillPaint.color = 0xFFF9F9F9.toInt()
        c.drawRoundRect(boxRect, 4f, 4f, fillPaint)

        strokePaint.color = 0xFF666666.toInt()
        strokePaint.strokeWidth = 0.75f
        c.drawRoundRect(boxRect, 4f, 4f, strokePaint)

        // Disclaimer Title
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 7.5f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xFF111111.toInt()
        c.drawText("LEGAL NOTICE & DISCLAIMER:", contentLeft + 8f, disclaimerTop + 13f, textPaint)

        // Disclaimer Text
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        textPaint.textSize = 7.5f
        val layout = StaticLayout.Builder.obtain(
            PdfLayoutConstants.LEGAL_DISCLAIMER_TEXT,
            0,
            PdfLayoutConstants.LEGAL_DISCLAIMER_TEXT.length,
            textPaint,
            (contentWidth - 16f).toInt()
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.1f).build()

        c.save()
        c.translate(contentLeft + 8f, disclaimerTop + 17f)
        layout.draw(c)
        c.restore()
    }

    fun drawMainHeader(title: String, subtitle: String) {
        val c = canvas ?: return
        ensureSpace(56f)

        // Primary Title
        textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textPaint.textSize = 15f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawText(title, contentLeft + contentWidth / 2f, currentY + 16f, textPaint)

        // Subtitle
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 9.5f
        c.drawText(subtitle, contentLeft + contentWidth / 2f, currentY + 32f, textPaint)

        // Double underline
        strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        strokePaint.strokeWidth = 1.5f
        c.drawLine(contentLeft + 40f, currentY + 40f, contentRight - 40f, currentY + 40f, strokePaint)
        strokePaint.strokeWidth = 0.5f
        c.drawLine(contentLeft + 40f, currentY + 43f, contentRight - 40f, currentY + 43f, strokePaint)

        currentY += 56f
    }

    fun drawSectionHeading(heading: String) {
        val c = canvas ?: return
        ensureSpace(34f)

        // Shaded bar background
        val barRect = RectF(contentLeft, currentY + 4f, contentRight, currentY + 22f)
        fillPaint.color = PdfLayoutConstants.COLOR_HEADER_BG
        c.drawRect(barRect, fillPaint)

        strokePaint.color = PdfLayoutConstants.COLOR_BORDER_GRAY
        strokePaint.strokeWidth = 0.75f
        c.drawRect(barRect, strokePaint)

        // Black left accent pill
        fillPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawRect(contentLeft, currentY + 4f, contentLeft + 4f, currentY + 22f, fillPaint)

        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 9.5f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawText(heading, contentLeft + 10f, currentY + 17f, textPaint)

        currentY += 28f
    }

    fun drawKeyValueGrid(rows: List<List<String>>) {
        val c = canvas ?: return
        val rowHeight = 22f
        val needed = rows.size * rowHeight + 10f
        ensureSpace(needed)

        val col1Width = contentWidth * 0.22f
        val col2Width = contentWidth * 0.28f
        val col3Width = contentWidth * 0.22f
        val col4Width = contentWidth * 0.28f

        for (row in rows) {
            val y = currentY
            // Border box for row
            strokePaint.strokeWidth = 0.5f
            strokePaint.color = PdfLayoutConstants.COLOR_BORDER_GRAY
            c.drawRect(contentLeft, y, contentRight, y + rowHeight, strokePaint)

            // Cell vertical divider lines
            val x1 = contentLeft + col1Width
            val x2 = x1 + col2Width
            val x3 = x2 + col3Width
            c.drawLine(x1, y, x1, y + rowHeight, strokePaint)
            c.drawLine(x2, y, x2, y + rowHeight, strokePaint)
            c.drawLine(x3, y, x3, y + rowHeight, strokePaint)

            // Draw Column 1 Key
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textPaint.textSize = 8.5f
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = 0xFF333333.toInt()
            c.drawText(row.getOrElse(0) { "" }, contentLeft + 6f, y + 15f, textPaint)

            // Draw Column 2 Value
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
            c.drawText(row.getOrElse(1) { "" }, x1 + 6f, y + 15f, textPaint)

            // Draw Column 3 Key
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textPaint.color = 0xFF333333.toInt()
            c.drawText(row.getOrElse(2) { "" }, x2 + 6f, y + 15f, textPaint)

            // Draw Column 4 Value
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
            c.drawText(row.getOrElse(3) { "" }, x3 + 6f, y + 15f, textPaint)

            currentY += rowHeight
        }

        currentY += 8f
    }

    fun drawTable(headers: List<String>, weights: FloatArray, rows: List<List<String>>) {
        val c = canvas ?: return
        ensureSpace(50f)

        val headerHeight = 22f
        val xPositions = FloatArray(headers.size + 1)
        xPositions[0] = contentLeft
        for (i in headers.indices) {
            xPositions[i + 1] = xPositions[i] + (contentWidth * weights[i])
        }
        xPositions[headers.size] = contentRight

        // Draw Table Header
        fillPaint.color = PdfLayoutConstants.COLOR_HEADER_BG
        c.drawRect(contentLeft, currentY, contentRight, currentY + headerHeight, fillPaint)

        strokePaint.strokeWidth = 1f
        strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawRect(contentLeft, currentY, contentRight, currentY + headerHeight, strokePaint)

        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 8f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK

        for (i in headers.indices) {
            c.drawText(headers[i], xPositions[i] + 4f, currentY + 14f, textPaint)
            if (i > 0) {
                c.drawLine(xPositions[i], currentY, xPositions[i], currentY + headerHeight, strokePaint)
            }
        }
        currentY += headerHeight

        // Draw Rows
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 8f

        for (row in rows) {
            // Measure row height based on cell text wrapping
            val layouts = row.mapIndexed { i, text ->
                val cellWidth = (xPositions[i + 1] - xPositions[i] - 8f).coerceAtLeast(10f)
                StaticLayout.Builder.obtain(text, 0, text.length, textPaint, cellWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.1f)
                    .build()
            }

            val maxTextHeight = layouts.maxOfOrNull { it.height.toFloat() } ?: 14f
            val cellHeight = (maxTextHeight + 10f).coerceAtLeast(18f)

            if (currentY + cellHeight > maxContentY) {
                forceNewPage()
                // Redraw table header on new page
                fillPaint.color = PdfLayoutConstants.COLOR_HEADER_BG
                c.drawRect(contentLeft, currentY, contentRight, currentY + headerHeight, fillPaint)
                strokePaint.strokeWidth = 1f
                strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
                c.drawRect(contentLeft, currentY, contentRight, currentY + headerHeight, strokePaint)
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                for (i in headers.indices) {
                    c.drawText(headers[i], xPositions[i] + 4f, currentY + 14f, textPaint)
                    if (i > 0) {
                        c.drawLine(xPositions[i], currentY, xPositions[i], currentY + headerHeight, strokePaint)
                    }
                }
                currentY += headerHeight
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            // Draw row rectangle
            strokePaint.strokeWidth = 0.5f
            strokePaint.color = PdfLayoutConstants.COLOR_BORDER_GRAY
            c.drawRect(contentLeft, currentY, contentRight, currentY + cellHeight, strokePaint)

            // Draw vertical column dividers
            for (i in 1 until headers.size) {
                c.drawLine(xPositions[i], currentY, xPositions[i], currentY + cellHeight, strokePaint)
            }

            // Draw wrapped cell text
            for (i in row.indices) {
                if (i < layouts.size) {
                    val sl = layouts[i]
                    c.save()
                    c.translate(xPositions[i] + 4f, currentY + 5f)
                    sl.draw(c)
                    c.restore()
                }
            }

            currentY += cellHeight
        }

        currentY += 8f
    }

    fun drawFamilyLineageBox(lineageText: String) {
        val c = canvas ?: return
        val steps = lineageText.split("\n").filter { it.isNotBlank() }
        val boxWidth = 260f
        val boxHeight = 22f
        val arrowGap = 16f
        val totalNeeded = steps.size * (boxHeight + arrowGap) + 20f

        ensureSpace(totalNeeded)

        val centerX = contentLeft + contentWidth / 2f
        var y = currentY + 5f

        for ((index, step) in steps.withIndex()) {
            val isArrow = step.trim() == "↓" || step.trim() == "->"
            if (isArrow) {
                // Draw crisp down arrow
                strokePaint.strokeWidth = 1.5f
                strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
                c.drawLine(centerX, y, centerX, y + arrowGap - 2f, strokePaint)
                // Arrowhead
                c.drawLine(centerX - 4f, y + arrowGap - 6f, centerX, y + arrowGap - 2f, strokePaint)
                c.drawLine(centerX + 4f, y + arrowGap - 6f, centerX, y + arrowGap - 2f, strokePaint)
                y += arrowGap
            } else {
                val boxRect = RectF(centerX - boxWidth / 2f, y, centerX + boxWidth / 2f, y + boxHeight)
                fillPaint.color = if (step.contains("User", ignoreCase = true) || step.contains("Self", ignoreCase = true)) {
                    0xFFEDEDED.toInt()
                } else {
                    PdfLayoutConstants.COLOR_WHITE
                }
                c.drawRoundRect(boxRect, 4f, 4f, fillPaint)

                strokePaint.strokeWidth = 1f
                strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
                c.drawRoundRect(boxRect, 4f, 4f, strokePaint)

                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textPaint.textSize = 8.5f
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
                c.drawText(step.trim(), centerX, y + 14.5f, textPaint)

                y += boxHeight

                // Draw automatic connecting arrow if next item is not already an arrow
                if (index < steps.size - 1 && steps[index + 1].trim() != "↓") {
                    strokePaint.strokeWidth = 1.5f
                    strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
                    c.drawLine(centerX, y, centerX, y + arrowGap - 2f, strokePaint)
                    c.drawLine(centerX - 4f, y + arrowGap - 6f, centerX, y + arrowGap - 2f, strokePaint)
                    c.drawLine(centerX + 4f, y + arrowGap - 6f, centerX, y + arrowGap - 2f, strokePaint)
                    y += arrowGap
                }
            }
        }

        currentY = y + 8f
    }

    fun drawBorderedTextBox(text: String) {
        val c = canvas ?: return
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 8.5f
        textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK

        val padding = 8f
        val layoutWidth = (contentWidth - padding * 2).toInt()
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

        val boxHeight = staticLayout.height + padding * 2
        ensureSpace(boxHeight + 10f)

        val boxRect = RectF(contentLeft, currentY, contentRight, currentY + boxHeight)
        fillPaint.color = 0xFFFAFAFA.toInt()
        c.drawRoundRect(boxRect, 3f, 3f, fillPaint)

        strokePaint.strokeWidth = 0.75f
        strokePaint.color = PdfLayoutConstants.COLOR_BORDER_GRAY
        c.drawRoundRect(boxRect, 3f, 3f, strokePaint)

        c.save()
        c.translate(contentLeft + padding, currentY + padding)
        staticLayout.draw(c)
        c.restore()

        currentY += boxHeight + 10f
    }

    fun drawSimpleText(text: String) {
        val c = canvas ?: return
        ensureSpace(20f)
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        textPaint.textSize = 8.5f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xFF555555.toInt()
        c.drawText(text, contentLeft + 6f, currentY + 12f, textPaint)
        currentY += 20f
    }

    fun drawNoticeBox(noticeText: String) {
        val c = canvas ?: return
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 8f
        textPaint.color = 0xFF222222.toInt()

        val padding = 6f
        val layoutWidth = (contentWidth - padding * 2 - 12f).toInt()
        val staticLayout = StaticLayout.Builder.obtain(noticeText, 0, noticeText.length, textPaint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()

        val boxHeight = staticLayout.height + padding * 2
        ensureSpace(boxHeight + 8f)

        val boxRect = RectF(contentLeft, currentY, contentRight, currentY + boxHeight)
        fillPaint.color = 0xFFF5F5F5.toInt()
        c.drawRoundRect(boxRect, 3f, 3f, fillPaint)

        strokePaint.strokeWidth = 0.5f
        strokePaint.color = 0xFF777777.toInt()
        c.drawRoundRect(boxRect, 3f, 3f, strokePaint)

        // Notice icon bar
        fillPaint.color = 0xFF444444.toInt()
        c.drawRect(contentLeft, currentY, contentLeft + 4f, currentY + boxHeight, fillPaint)

        c.save()
        c.translate(contentLeft + padding + 6f, currentY + padding)
        staticLayout.draw(c)
        c.restore()

        currentY += boxHeight + 8f
    }

    fun drawDocumentImagePage(doc: AttachedDocumentEntity) {
        val c = canvas ?: return
        val path = doc.imagePath ?: return
        val imageFile = File(path)
        if (!imageFile.exists()) return

        // Compute available space on this fresh A4 page
        val availableWidth = contentWidth
        val availableHeight = maxContentY - currentY - 30f

        // Decode bitmap safely with bounds calculation to prevent memory spikes
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) return

        // Compute sample size for memory-safe decoding
        var sampleSize = 1
        while ((origWidth / sampleSize) > 1600 || (origHeight / sampleSize) > 2200) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = try {
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: OutOfMemoryError) {
            decodeOptions.inSampleSize *= 2
            BitmapFactory.decodeFile(path, decodeOptions)
        } ?: return

        // Calculate aspect-ratio fit inside available box without stretching
        val scaleX = availableWidth / bitmap.width.toFloat()
        val scaleY = availableHeight / bitmap.height.toFloat()
        val scale = min(scaleX, scaleY)

        val destWidth = bitmap.width * scale
        val destHeight = bitmap.height * scale

        // Center horizontally
        val destLeft = contentLeft + (availableWidth - destWidth) / 2f
        val destTop = currentY + 10f

        // Draw decorative outer border for Xerox clarity
        strokePaint.strokeWidth = 1f
        strokePaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawRect(destLeft - 4f, destTop - 4f, destLeft + destWidth + 4f, destTop + destHeight + 4f, strokePaint)

        // Draw the document scan bitmap
        val destRect = RectF(destLeft, destTop, destLeft + destWidth, destTop + destHeight)
        val bmpPaint = Paint().apply { isFilterBitmap = true }
        c.drawBitmap(bitmap, null, destRect, bmpPaint)

        // Document caption at bottom
        val captionY = destTop + destHeight + 18f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.textSize = 8.5f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = PdfLayoutConstants.COLOR_INK_BLACK
        c.drawText("Original Document: ${doc.originalFileName.ifEmpty { doc.documentName }} (${doc.fileSizeText})", contentLeft + contentWidth / 2f, captionY, textPaint)

        bitmap.recycle()
        currentY = captionY + 12f
    }
}
