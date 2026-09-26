package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AttachedDocumentEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.FamilyTreeHelper
import com.example.data.model.LandHistoryEntity
import com.example.data.model.PdfExportConfig
import com.example.data.model.PdfExportMode
import com.example.data.model.PdfOrientation
import com.example.data.model.PlotEntity
import com.example.data.repository.BackupManager
import com.example.pdf.PdfDocumentGenerator
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun read_string_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LandRecord PDF", appName)
    }

    @Test
    fun test_13_production_steps(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // ----------------------------------------------------
        // TEST 1 — CREATE GREAT-GRANDFATHER
        // ----------------------------------------------------
        val ggf = FamilyMemberEntity(
            fullName = "Test Great-Grandfather",
            relationshipToUser = "Great-grandfather",
            generationLevel = 1,
            isDeceased = true,
            notes = "Original patriarch"
        )
        val ggfId = database.familyDao().insertMember(ggf)
        assertTrue("Person saves successfully", ggfId > 0)

        val membersAfter1 = database.familyDao().getAllMembersList()
        val savedGgf = membersAfter1.find { it.id == ggfId }
        assertNotNull("Person appears in Family list", savedGgf)
        assertEquals("Test Great-Grandfather", savedGgf?.fullName)

        // Verify person can be opened and edited
        val editedGgf = savedGgf!!.copy(notes = "Updated notes for Great-Grandfather")
        database.familyDao().updateMember(editedGgf)
        val refreshedGgf = database.familyDao().getMemberById(ggfId)
        assertEquals("Updated notes for Great-Grandfather", refreshedGgf?.notes)

        // ----------------------------------------------------
        // TEST 2 — CREATE GRANDFATHER
        // ----------------------------------------------------
        val gf = FamilyMemberEntity(
            fullName = "Test Grandfather",
            relationshipToUser = "Grandfather",
            generationLevel = 2,
            fatherId = ggfId,
            isDeceased = true
        )
        val gfId = database.familyDao().insertMember(gf)
        assertTrue("Grandfather saves successfully", gfId > 0)
        val savedGf = database.familyDao().getMemberById(gfId)
        assertEquals("Relationship stored correctly", ggfId, savedGf?.fatherId)

        // ----------------------------------------------------
        // TEST 3 — CREATE FATHER
        // ----------------------------------------------------
        val f = FamilyMemberEntity(
            fullName = "Test Father",
            relationshipToUser = "Father",
            generationLevel = 3,
            fatherId = gfId
        )
        val fId = database.familyDao().insertMember(f)
        assertTrue("Father saves successfully", fId > 0)
        val savedFather = database.familyDao().getMemberById(fId)
        assertEquals("Relationship stored correctly", gfId, savedFather?.fatherId)

        // ----------------------------------------------------
        // TEST 4 — CREATE USER & VERIFY FAMILY TREE
        // ----------------------------------------------------
        val user = FamilyMemberEntity(
            fullName = "Test User",
            relationshipToUser = "Self",
            generationLevel = 4,
            fatherId = fId,
            isUser = true
        )
        val userId = database.familyDao().insertMember(user)
        assertTrue("User saves successfully", userId > 0)

        val allMembers = database.familyDao().getAllMembersList()
        val lineage = FamilyTreeHelper.traceLineageToUser(ggfId, allMembers)
        assertEquals("Lineage chain must contain 4 members", 4, lineage.size)
        assertTrue("Level 1 is Great-grandfather", lineage[0].contains("Test Great-Grandfather"))
        assertTrue("Level 2 is Grandfather", lineage[1].contains("Test Grandfather"))
        assertTrue("Level 3 is Father", lineage[2].contains("Test Father"))
        assertTrue("Level 4 is User", lineage[3].contains("Test User"))

        // ----------------------------------------------------
        // TEST 5 — CREATE LAND RECORD
        // ----------------------------------------------------
        val plot1 = PlotEntity(
            plotNumber = "TEST-123",
            khataNumber = "TEST-456",
            landRecordNumber = "LR-TEST-001",
            landType = "Agricultural",
            village = "Test Village",
            tahasil = "Test Tahasil",
            district = "Test District",
            originalArea = 1.0,
            originalUnit = "Acres",
            recordedOwnerId = ggfId,
            recordedOwner = "Test Great-Grandfather",
            relationshipToUser = "Great-grandfather",
            ownershipStatus = "Ancestral Inherited"
        )
        val plot1Id = database.plotDao().insertPlot(plot1)
        assertTrue("Plot saves successfully", plot1Id > 0)

        // ----------------------------------------------------
        // TEST 6 — OPEN AND EDIT LAND
        // ----------------------------------------------------
        val retrievedPlot1 = database.plotDao().getPlotById(plot1Id)
        assertNotNull("Record found", retrievedPlot1)
        assertEquals("TEST-123", retrievedPlot1?.plotNumber)
        assertEquals("TEST-456", retrievedPlot1?.khataNumber)
        assertEquals("Test Village", retrievedPlot1?.village)
        assertEquals(1.0, retrievedPlot1?.originalArea ?: 0.0, 0.001)
        assertEquals("Test Great-Grandfather", retrievedPlot1?.recordedOwner)
        assertEquals("Ancestral Inherited", retrievedPlot1?.ownershipStatus)

        // Edit at least one field and save
        val editedPlot1 = retrievedPlot1!!.copy(notes = "Verified test land boundaries")
        database.plotDao().updatePlot(editedPlot1)
        val checkEditedPlot1 = database.plotDao().getPlotById(plot1Id)
        assertEquals("Verified test land boundaries", checkEditedPlot1?.notes)

        // ----------------------------------------------------
        // TEST 7 — ADD DOCUMENT
        // ----------------------------------------------------
        val doc1 = AttachedDocumentEntity(
            plotId = plot1Id,
            familyMemberId = ggfId,
            documentName = "Test Settlement RoR",
            documentType = "RoR",
            documentNumber = "ROR-DOC-123",
            date = "1970-01-01",
            issuingAuthority = "Test Tahasildar"
        )
        val doc1Id = database.documentDao().insertDocument(doc1)
        assertTrue("Document saves successfully", doc1Id > 0)
        val plotDocs = database.documentDao().getDocumentsListForPlot(plot1Id)
        assertEquals(1, plotDocs.size)
        assertEquals("Test Settlement RoR", plotDocs[0].documentName)

        // ----------------------------------------------------
        // TEST 8 — ADD LAND HISTORY (Verify owner is not mutated)
        // ----------------------------------------------------
        val history1 = LandHistoryEntity(
            plotId = plot1Id,
            eventDate = "1972-04-10",
            eventName = "Inherited",
            fromPerson = "Test Great-Grandfather",
            toPerson = "Test Grandfather",
            fromMemberId = ggfId,
            toMemberId = gfId,
            areaTransferred = "1.0 Acre"
        )
        val hist1Id = database.landHistoryDao().insertHistory(history1)
        assertTrue("History saves successfully", hist1Id > 0)

        val plotHistories = database.landHistoryDao().getHistoryListForPlot(plot1Id)
        assertEquals(1, plotHistories.size)
        assertEquals("Inherited", plotHistories[0].eventName)

        // IMPORTANT: Verify recorded owner did not mutate
        val plotAfterHistory = database.plotDao().getPlotById(plot1Id)
        assertEquals("Test Great-Grandfather", plotAfterHistory?.recordedOwner)
        assertEquals(ggfId, plotAfterHistory?.recordedOwnerId)

        // ----------------------------------------------------
        // TEST 9 — SEARCH (Exact and Partial)
        // ----------------------------------------------------
        val allPlotsList = database.plotDao().getAllPlotsList()
        val allDocsList = database.documentDao().getAllDocumentsList()

        fun performSearch(query: String): List<PlotEntity> {
            val q = query.trim()
            val matchingMemberIds = allMembers.filter { it.fullName.contains(q, ignoreCase = true) }.map { it.id }.toSet()
            val matchingPlotIdsFromDocs = allDocsList.filter {
                it.documentNumber.contains(q, ignoreCase = true) || it.documentName.contains(q, ignoreCase = true)
            }.mapNotNull { it.plotId }.toSet()

            return allPlotsList.filter { plot ->
                plot.plotNumber.contains(q, ignoreCase = true) ||
                        plot.khataNumber.contains(q, ignoreCase = true) ||
                        plot.village.contains(q, ignoreCase = true) ||
                        plot.recordedOwner.contains(q, ignoreCase = true) ||
                        (plot.recordedOwnerId != null && matchingMemberIds.contains(plot.recordedOwnerId)) ||
                        matchingPlotIdsFromDocs.contains(plot.id)
            }
        }

        val searchPlotNo = performSearch("TEST-123")
        assertEquals(1, searchPlotNo.size)
        assertEquals("TEST-123", searchPlotNo[0].plotNumber)

        val searchPartialPlot = performSearch("123")
        assertEquals(1, searchPartialPlot.size)

        val searchOwner = performSearch("Test Great-Grandfather")
        assertEquals(1, searchOwner.size)

        val searchPartialOwner = performSearch("Great-Grandfather")
        assertEquals(1, searchPartialOwner.size)

        // ----------------------------------------------------
        // TEST 10 — EXPORT SINGLE PLOT PDF CONFIG & GENERATION
        // ----------------------------------------------------
        val singleConfig = PdfExportConfig(
            exportMode = PdfExportMode.CURRENT_PLOT,
            targetPlotId = plot1Id,
            orientation = PdfOrientation.PORTRAIT,
            filename = "Single_Plot_TEST_123.pdf",
            includeLandHistory = true,
            includeDocuments = true,
            includeFamilyRelationship = true
        )
        val generator = PdfDocumentGenerator(context)
        val historyMap = mapOf(plot1Id to plotHistories)
        val docMap = mapOf(plot1Id to plotDocs)

        val singlePdfFile = generator.generatePdf(
            config = singleConfig,
            plots = listOf(retrievedPlot1),
            familyMembers = allMembers,
            historyMap = historyMap,
            documentsMap = docMap,
            allDocuments = plotDocs
        )
        assertTrue("Single plot PDF file exists", singlePdfFile.exists())
        assertTrue("Single plot PDF size > 0", singlePdfFile.length() > 0)

        // ----------------------------------------------------
        // TEST 11 — EXPORT MULTIPLE PLOTS PDF
        // ----------------------------------------------------
        val plot2 = PlotEntity(
            plotNumber = "TEST-789",
            khataNumber = "TEST-999",
            landType = "Homestead",
            village = "Test Village 2",
            originalArea = 0.5,
            originalUnit = "Acres",
            recordedOwner = "Test Father",
            recordedOwnerId = fId
        )
        val plot2Id = database.plotDao().insertPlot(plot2)
        val retrievedPlot2 = database.plotDao().getPlotById(plot2Id)!!

        val multiConfig = PdfExportConfig(
            exportMode = PdfExportMode.ALL_PLOTS,
            orientation = PdfOrientation.PORTRAIT,
            filename = "All_Plots_Report.pdf"
        )
        val multiPdfFile = generator.generatePdf(
            config = multiConfig,
            plots = listOf(retrievedPlot1, retrievedPlot2),
            familyMembers = allMembers,
            historyMap = historyMap,
            documentsMap = docMap,
            allDocuments = plotDocs
        )
        assertTrue("Multi-plot PDF file exists", multiPdfFile.exists())
        assertTrue("Multi-plot PDF size > 0", multiPdfFile.length() > 0)

        // ----------------------------------------------------
        // TEST 12 — BACKUP AND RESTORE
        // ----------------------------------------------------
        val backupJson = BackupManager.createBackupJson(database)
        assertTrue("Backup contains plot", backupJson.contains("TEST-123") || backupJson.contains("TEST\\/123"))
        assertTrue("Backup contains person", backupJson.contains("Test Great-Grandfather"))

        // Create a new fresh in-memory database to test restore
        val restoreDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val restoreSuccess = BackupManager.restoreFromJson(restoreDb, backupJson)
        assertTrue("Restore succeeded", restoreSuccess)

        val restoredPlots = restoreDb.plotDao().getAllPlotsList()
        assertEquals(2, restoredPlots.size)
        val restoredMembers = restoreDb.familyDao().getAllMembersList()
        assertEquals(4, restoredMembers.size)
        val restoredHist = restoreDb.landHistoryDao().getAllHistoryList()
        assertEquals(1, restoredHist.size)
        val restoredDocs = restoreDb.documentDao().getAllDocumentsList()
        assertEquals(1, restoredDocs.size)
        restoreDb.close()

        // ----------------------------------------------------
        // TEST 13 — RESTART / PERSISTENCE TEST
        // ----------------------------------------------------
        // Test database persistence using a named SQLite database file
        val diskDbFile = File(context.filesDir, "test_persistence.db")
        if (diskDbFile.exists()) diskDbFile.delete()

        val diskDb1 = Room.databaseBuilder(context, AppDatabase::class.java, diskDbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

        diskDb1.familyDao().insertMember(ggf)
        diskDb1.plotDao().insertPlot(plot1)
        diskDb1.close()

        // Re-open from disk (simulating app process restart)
        val diskDb2 = Room.databaseBuilder(context, AppDatabase::class.java, diskDbFile.absolutePath)
            .allowMainThreadQueries()
            .build()

        val reloadedMembers = diskDb2.familyDao().getAllMembersList()
        assertEquals("Family members persist across app restart", 1, reloadedMembers.size)
        assertEquals("Test Great-Grandfather", reloadedMembers[0].fullName)

        val reloadedPlots = diskDb2.plotDao().getAllPlotsList()
        assertEquals("Land records persist across app restart", 1, reloadedPlots.size)
        assertEquals("TEST-123", reloadedPlots[0].plotNumber)

        diskDb2.close()
        diskDbFile.delete()
    }
}
