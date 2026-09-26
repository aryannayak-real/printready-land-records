package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.data.model.AttachedDocumentEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LandHistoryEntity
import com.example.data.model.PlotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleDataInitializer {

    suspend fun populateIfEmpty(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val plotCount = database.plotDao().getPlotCount()
        if (plotCount > 0) return@withContext

        // Create sample document scans on disk
        val doc1Path = createSampleDeedImage(
            context = context,
            fileName = "ror_patta_1984.png",
            title = "GOVERNMENT OF ODISHA - REVENUE DEPARTMENT",
            subtitle = "RECORD OF RIGHTS (RoR) / PATTA CERTIFICATE",
            docNumber = "ROR-OD-456/123",
            plotInfo = "Plot No: 123/45 | Khata No: 456 | Village: Rampur",
            details = listOf(
                "Original Holder: Late Baikuntha Nath Nayak",
                "Subsequent Inheritor: Late Bhabani Charan Nayak",
                "Recorded Area: 1.75 Acres | Nature: Agricultural / Homestead",
                "Settlement Authority: Tahasildar Sadar Cuttack",
                "Registered Reference: Volume 14, Page 88, Register of 1984"
            )
        )

        val doc2Path = createSampleDeedImage(
            context = context,
            fileName = "mutation_cert_2021.png",
            title = "OFFICE OF THE TAHSILDAR : SADAR CUTTACK",
            subtitle = "MUTATION & REVENUE RECORD ENTRY ORDER",
            docNumber = "MUT-2021-8891",
            plotInfo = "Plot No: 123/45 | Khata No: 456 | Touzi: 881",
            details = listOf(
                "Applicant / Present Owner: Ramesh Chandra Nayak",
                "Father's Name: Subash Chandra Nayak",
                "Mode of Title: Succession & Family Partition",
                "Total Area Confirmed: 1.75 Acres (76,230 Sq. Ft)",
                "Verification: All rent dues verified and recorded up to date"
            )
        )

        val doc3Path = createSampleDeedImage(
            context = context,
            fileName = "sale_deed_2002.png",
            title = "SUB-REGISTRAR OFFICE : BARANGA",
            subtitle = "REGISTERED SALE DEED (TITLE DOCUMENT)",
            docNumber = "SD-2002-1842",
            plotInfo = "Plot No: 78-A | Khata No: 102 | Mouza: Madhupur",
            details = listOf(
                "Vendor: Kishore Kumar Dash",
                "Purchaser: Subash Chandra Nayak",
                "Consideration Paid: In Full with Absolute Rights",
                "Area: 0.85 Acres with 40ft Highway Frontage",
                "Registration Book: Book 1, Volume 42, Pages 101 to 109"
            )
        )

        // 1. Insert Family Members with Relational Parent/Child IDs
        val m1 = FamilyMemberEntity(
            fullName = "Late Baikuntha Nath Nayak",
            gender = "Male",
            relationshipToUser = "Great-grandfather",
            relationshipDescription = "Father's paternal grandfather",
            generationLevel = 1,
            dateOfBirth = "1898",
            dateOfDeath = "1972",
            isDeceased = true,
            notes = "Original allottee and founder of the ancestral family estate."
        )
        val m1Id = database.familyDao().insertMember(m1)

        val m2 = FamilyMemberEntity(
            fullName = "Late Bhabani Charan Nayak",
            gender = "Male",
            relationshipToUser = "Grandfather",
            relationshipDescription = "Father's father",
            generationLevel = 2,
            dateOfBirth = "1924",
            dateOfDeath = "2004",
            isDeceased = true,
            fatherId = m1Id,
            notes = "Formalized family partition and established settlement records in 1984."
        )
        val m2Id = database.familyDao().insertMember(m2)

        val m3 = FamilyMemberEntity(
            fullName = "Subash Chandra Nayak",
            gender = "Male",
            relationshipToUser = "Father",
            relationshipDescription = "Paternal father",
            generationLevel = 3,
            dateOfBirth = "1954",
            dateOfDeath = "",
            isDeceased = false,
            fatherId = m2Id,
            notes = "Elder family trustee who acquired Plot 78-A and settled homesteads."
        )
        val m3Id = database.familyDao().insertMember(m3)

        val m4 = FamilyMemberEntity(
            fullName = "Ramesh Chandra Nayak",
            gender = "Male",
            relationshipToUser = "Self",
            relationshipDescription = "Current account holder & recorded heir",
            generationLevel = 4,
            dateOfBirth = "1982",
            dateOfDeath = "",
            isDeceased = false,
            fatherId = m3Id,
            isUser = true,
            notes = "Current user maintaining estate archives and digital land records."
        )
        val m4Id = database.familyDao().insertMember(m4)

        val m5 = FamilyMemberEntity(
            fullName = "Suresh Chandra Nayak",
            gender = "Male",
            relationshipToUser = "Brother",
            relationshipDescription = "Co-owner brother",
            generationLevel = 4,
            dateOfBirth = "1985",
            dateOfDeath = "",
            isDeceased = false,
            fatherId = m3Id,
            notes = "Joint owner of commercial parcel Plot 78-A."
        )
        val m5Id = database.familyDao().insertMember(m5)

        val m6 = FamilyMemberEntity(
            fullName = "Aarav Nayak",
            gender = "Male",
            relationshipToUser = "Son",
            relationshipDescription = "Successor heir",
            generationLevel = 5,
            dateOfBirth = "2012",
            dateOfDeath = "",
            isDeceased = false,
            fatherId = m4Id,
            notes = "Next-generation lineage heir."
        )
        database.familyDao().insertMember(m6)

        // 2. Insert Plots with Real Foreign Key Links
        val plot1 = PlotEntity(
            plotNumber = "123/45",
            khataNumber = "456",
            landRecordNumber = "LR-1984-OD-7729",
            landType = "Agricultural & Homestead",
            village = "Rampur",
            gramPanchayat = "Rampur GP",
            tahasil = "Sadar Tahasil",
            district = "Cuttack",
            state = "Odisha",
            pinCode = "753001",
            originalArea = 1.75,
            originalUnit = "Acres",
            convertedAreaText = "1.75 Acres (76,230 Sq. Ft / 70.82 Dec)",
            recordedOwnerId = m4Id,
            recordedOwner = "Ramesh Chandra Nayak",
            previousOwnerId = m1Id,
            previousOwner = "Late Baikuntha Nath Nayak",
            relationshipToUser = "Self",
            relationshipDescription = "Lineal descendant of original allottee",
            ownershipStatus = "Ancestral Inherited",
            dateAcquired = "04-Jul-2021",
            sourceOfRecord = "RoR Patta & Mutation Order No. 912/2005",
            documentReference = "MUT-2021-8891",
            notes = "Prime agricultural & homestead land bordering north irrigation canal. Clear mutation order issued in 2021. Annual land revenue receipts up to date.",
            latitude = 20.4625,
            longitude = 85.8828
        )

        val plot2 = PlotEntity(
            plotNumber = "78-A",
            khataNumber = "102",
            landRecordNumber = "LR-2002-OD-3105",
            landType = "Commercial Road-Front",
            village = "Madhupur",
            gramPanchayat = "Madhupur Shasan",
            tahasil = "Baranga",
            district = "Cuttack",
            state = "Odisha",
            pinCode = "754005",
            originalArea = 0.85,
            originalUnit = "Acres",
            convertedAreaText = "0.85 Acres (37,026 Sq. Ft / 34.40 Dec)",
            recordedOwnerId = m4Id,
            recordedOwner = "Ramesh Chandra Nayak & Suresh Chandra Nayak",
            previousOwnerId = m3Id,
            previousOwner = "Subash Chandra Nayak (Purchased from Kishore Kumar Dash)",
            relationshipToUser = "Self & Brother",
            relationshipDescription = "Joint family gift deed from Father",
            ownershipStatus = "Purchased (Registered Sale Deed)",
            dateAcquired = "10-May-2019",
            sourceOfRecord = "Sale Deed #1842/2002 & Family Gift Deed #740/2019",
            documentReference = "SD-2002-1842",
            notes = "Commercial road-facing plot with 40ft road connectivity. 30-year non-encumbrance certificate verified at District Sub-Registrar.",
            latitude = 20.4190,
            longitude = 85.8320
        )

        val plot3 = PlotEntity(
            plotNumber = "214/2",
            khataNumber = "88",
            landRecordNumber = "LR-2015-OD-9014",
            landType = "Homestead & Orchard",
            village = "Rampur",
            gramPanchayat = "Rampur GP",
            tahasil = "Sadar Tahasil",
            district = "Cuttack",
            state = "Odisha",
            pinCode = "753001",
            originalArea = 2.40,
            originalUnit = "Acres",
            convertedAreaText = "2.40 Acres (104,544 Sq. Ft / 97.12 Dec)",
            recordedOwnerId = m3Id,
            recordedOwner = "Subash Chandra Nayak",
            previousOwnerId = m2Id,
            previousOwner = "Late Bhabani Charan Nayak",
            relationshipToUser = "Father",
            relationshipDescription = "Father's ancestral homestead and pond",
            ownershipStatus = "Ancestral Homestead",
            dateAcquired = "02-Feb-1990",
            sourceOfRecord = "Settlement RoR #SET-90/88",
            documentReference = "SET-1952-88",
            notes = "Ancestral homestead, pond (pokhari), and coconut orchard. Preserved under parental family custody.",
            latitude = 20.4640,
            longitude = 85.8850
        )

        val plot1Id = database.plotDao().insertPlot(plot1)
        val plot2Id = database.plotDao().insertPlot(plot2)
        val plot3Id = database.plotDao().insertPlot(plot3)

        // 3. Insert History for Plots
        val history1 = listOf(
            LandHistoryEntity(
                plotId = plot1Id,
                eventDate = "15-Jan-1952",
                eventName = "Settlement Allotment",
                fromPerson = "Tahasildar Cuttack",
                toPerson = "Late Baikuntha Nath Nayak",
                fromMemberId = null,
                toMemberId = m1Id,
                areaTransferred = "3.50 Acres",
                documentReference = "Settlement RoR Vol 14 Page 88",
                notes = "Initial post-independence provincial land settlement"
            ),
            LandHistoryEntity(
                plotId = plot1Id,
                eventDate = "12-Mar-1984",
                eventName = "Family Partition Deed",
                fromPerson = "Late Baikuntha Nath Nayak",
                toPerson = "Late Bhabani Charan Nayak",
                fromMemberId = m1Id,
                toMemberId = m2Id,
                areaTransferred = "1.75 Acres",
                documentReference = "Registered Deed No. 412/1984",
                notes = "Equitable family division between brother branches"
            ),
            LandHistoryEntity(
                plotId = plot1Id,
                eventDate = "18-Nov-2005",
                eventName = "Mutation on Succession",
                fromPerson = "Late Bhabani Charan Nayak",
                toPerson = "Subash Chandra Nayak",
                fromMemberId = m2Id,
                toMemberId = m3Id,
                areaTransferred = "1.75 Acres",
                documentReference = "Mutation Order No. 912/2005",
                notes = "Succession mutation following demise of grandfather"
            ),
            LandHistoryEntity(
                plotId = plot1Id,
                eventDate = "04-Jul-2021",
                eventName = "Succession Transfer",
                fromPerson = "Subash Chandra Nayak",
                toPerson = "Ramesh Chandra Nayak",
                fromMemberId = m3Id,
                toMemberId = m4Id,
                areaTransferred = "1.75 Acres",
                documentReference = "Land Record Entry #LT-2021-301",
                notes = "Transferred to current holder Ramesh Chandra Nayak"
            )
        )

        val history2 = listOf(
            LandHistoryEntity(
                plotId = plot2Id,
                eventDate = "20-Oct-2002",
                eventName = "Registered Sale Deed",
                fromPerson = "Kishore Kumar Dash",
                toPerson = "Subash Chandra Nayak",
                fromMemberId = null,
                toMemberId = m3Id,
                areaTransferred = "0.85 Acres",
                documentReference = "Sale Deed #1842/2002, Sub-Registrar Baranga",
                notes = "Purchased with clear vacant title and peaceful possession"
            ),
            LandHistoryEntity(
                plotId = plot2Id,
                eventDate = "10-May-2019",
                eventName = "Joint Family Gift Settlement",
                fromPerson = "Subash Chandra Nayak",
                toPerson = "Ramesh Chandra Nayak & Suresh Nayak",
                fromMemberId = m3Id,
                toMemberId = m4Id,
                areaTransferred = "0.85 Acres",
                documentReference = "Gift Deed #740/2019, Sub-Registrar",
                notes = "Settled jointly upon sons with equal undivided share"
            )
        )

        val history3 = listOf(
            LandHistoryEntity(
                plotId = plot3Id,
                eventDate = "15-Jan-1952",
                eventName = "Original Settlement Entry",
                fromPerson = "Government of Odisha",
                toPerson = "Late Baikuntha Nath Nayak",
                fromMemberId = null,
                toMemberId = m1Id,
                areaTransferred = "2.40 Acres",
                documentReference = "RoR Khata 88 Plot 214",
                notes = "Ancestral homestead parcel with orchard"
            ),
            LandHistoryEntity(
                plotId = plot3Id,
                eventDate = "02-Feb-1990",
                eventName = "Ancestral Succession",
                fromPerson = "Late Baikuntha Nath Nayak",
                toPerson = "Subash Chandra Nayak",
                fromMemberId = m1Id,
                toMemberId = m3Id,
                areaTransferred = "2.40 Acres",
                documentReference = "Settlement RoR #SET-90/88",
                notes = "Homestead succession confirmed"
            )
        )
        database.landHistoryDao().insertHistoryList(history1 + history2 + history3)

        // 4. Insert Attached Documents
        val documents = listOf(
            AttachedDocumentEntity(
                plotId = plot1Id,
                familyMemberId = m4Id,
                documentName = "Record of Rights (RoR) Patta 1984",
                documentType = "RoR",
                documentNumber = "ROR-OD-456/123",
                date = "12-Mar-1984",
                issuingAuthority = "Tahasildar Sadar Cuttack",
                imagePath = doc1Path,
                originalFileName = "ror_patta_1984.png",
                fileSizeText = "195 KB",
                notes = "Official certified photocopy of the 1984 RoR record"
            ),
            AttachedDocumentEntity(
                plotId = plot1Id,
                familyMemberId = m4Id,
                documentName = "Mutation Certificate 2021",
                documentType = "Mutation",
                documentNumber = "MUT-2021-8891",
                date = "04-Jul-2021",
                issuingAuthority = "Revenue Officer, Sadar Tahasil",
                imagePath = doc2Path,
                originalFileName = "mutation_cert_2021.png",
                fileSizeText = "182 KB",
                notes = "Latest mutation certificate issued in the name of Ramesh Chandra Nayak"
            ),
            AttachedDocumentEntity(
                plotId = plot2Id,
                familyMemberId = m3Id,
                documentName = "Registered Sale Deed 2002",
                documentType = "Sale Deed",
                documentNumber = "SD-2002-1842",
                date = "20-Oct-2002",
                issuingAuthority = "Sub-Registrar Office, Baranga",
                imagePath = doc3Path,
                originalFileName = "sale_deed_2002.png",
                fileSizeText = "210 KB",
                notes = "Stamped and registered sale deed with plan map"
            ),
            AttachedDocumentEntity(
                plotId = plot2Id,
                familyMemberId = m4Id,
                documentName = "Non-Encumbrance Certificate (30 Yrs)",
                documentType = "Tax Receipt",
                documentNumber = "EC-2022-4410",
                date = "14-Feb-2022",
                issuingAuthority = "District Sub-Registrar Cuttack",
                imagePath = doc2Path,
                originalFileName = "encumbrance_cert_2022.png",
                fileSizeText = "175 KB",
                notes = "Certified nil encumbrance from 1992 to 2022"
            )
        )
        database.documentDao().insertDocuments(documents)
    }

    private fun createSampleDeedImage(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        docNumber: String,
        plotInfo: String,
        details: List<String>
    ): String {
        val dir = File(context.filesDir, "sample_documents")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        if (file.exists()) return file.absolutePath

        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRect(40f, 40f, width - 40f, height - 40f, paint)

        paint.strokeWidth = 2f
        canvas.drawRect(52f, 52f, width - 52f, height - 52f, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 34f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, width / 2f, 130f, paint)

        paint.textSize = 28f
        canvas.drawText(subtitle, width / 2f, 185f, paint)

        paint.strokeWidth = 3f
        canvas.drawLine(150f, 220f, width - 150f, 220f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText("DOCUMENT REF: $docNumber", 100f, 280f, paint)
        canvas.drawText("PARCEL: $plotInfo", 100f, 320f, paint)

        paint.strokeWidth = 1.5f
        canvas.drawLine(100f, 345f, width - 100f, 345f, paint)

        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        paint.textSize = 24f
        var y = 410f
        for (item in details) {
            canvas.drawText("• $item", 110f, y, paint)
            y += 50f
        }

        y += 30f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(100f, y, width - 100f, y + 360f, paint)

        val boxBottom = y + 360f
        paint.strokeWidth = 1f
        var lx = 100f
        while (lx < width - 100f) {
            canvas.drawLine(lx, y, lx + 60f, boxBottom, paint)
            lx += 80f
        }

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 22f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("[ OFFICIAL SURVEY DIAGRAM / BOUNDARY DEMARCATION ]", width / 2f, y + 190f, paint)

        val sealX = 300f
        val sealY = boxBottom + 160f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(sealX, sealY, 90f, paint)
        paint.strokeWidth = 2f
        canvas.drawCircle(sealX, sealY, 80f, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("GOVERNMENT", sealX, sealY - 30f, paint)
        canvas.drawText("★ SEAL ★", sealX, sealY, paint)
        canvas.drawText("OFFICIAL", sealX, sealY + 30f, paint)

        val sigX = width - 320f
        paint.textAlign = Paint.Align.CENTER
        paint.strokeWidth = 2f
        canvas.drawLine(sigX - 140f, sealY + 30f, sigX + 140f, sealY + 30f, paint)
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText("Authorized Officer", sigX, sealY + 60f, paint)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        canvas.drawText("Registration & Revenue Wing", sigX, sealY + 90f, paint)

        paint.textSize = 17f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Certified Xerox Copy Archive • Family Record Repository", width / 2f, height - 70f, paint)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return file.absolutePath
    }
}
