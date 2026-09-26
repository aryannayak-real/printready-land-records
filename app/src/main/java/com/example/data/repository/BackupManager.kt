package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.model.AttachedDocumentEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.LandHistoryEntity
import com.example.data.model.PlotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val plots = database.plotDao().getAllPlotsList()
        val members = database.familyDao().getAllMembersList()
        val history = database.landHistoryDao().getAllHistoryList()
        val documents = database.documentDao().getAllDocumentsList()

        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("dateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Plots
        val plotsArray = JSONArray()
        for (p in plots) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("plotNumber", p.plotNumber)
            obj.put("khataNumber", p.khataNumber)
            obj.put("landRecordNumber", p.landRecordNumber)
            obj.put("landType", p.landType)
            obj.put("village", p.village)
            obj.put("gramPanchayat", p.gramPanchayat)
            obj.put("tahasil", p.tahasil)
            obj.put("district", p.district)
            obj.put("state", p.state)
            obj.put("pinCode", p.pinCode)
            obj.put("originalArea", p.originalArea)
            obj.put("originalUnit", p.originalUnit)
            obj.put("convertedAreaText", p.convertedAreaText)
            obj.put("recordedOwnerId", p.recordedOwnerId ?: JSONObject.NULL)
            obj.put("recordedOwner", p.recordedOwner)
            obj.put("previousOwnerId", p.previousOwnerId ?: JSONObject.NULL)
            obj.put("previousOwner", p.previousOwner)
            obj.put("relationshipToUser", p.relationshipToUser)
            obj.put("relationshipDescription", p.relationshipDescription)
            obj.put("ownershipStatus", p.ownershipStatus)
            obj.put("dateAcquired", p.dateAcquired)
            obj.put("sourceOfRecord", p.sourceOfRecord)
            obj.put("documentReference", p.documentReference)
            obj.put("notes", p.notes)
            obj.put("latitude", p.latitude ?: JSONObject.NULL)
            obj.put("longitude", p.longitude ?: JSONObject.NULL)
            plotsArray.put(obj)
        }
        root.put("plots", plotsArray)

        // Family Members
        val membersArray = JSONArray()
        for (m in members) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("fullName", m.fullName)
            obj.put("gender", m.gender)
            obj.put("relationshipToUser", m.relationshipToUser)
            obj.put("relationshipDescription", m.relationshipDescription)
            obj.put("generationLevel", m.generationLevel)
            obj.put("dateOfBirth", m.dateOfBirth)
            obj.put("dateOfDeath", m.dateOfDeath)
            obj.put("isDeceased", m.isDeceased)
            obj.put("fatherId", m.fatherId ?: JSONObject.NULL)
            obj.put("motherId", m.motherId ?: JSONObject.NULL)
            obj.put("spouseId", m.spouseId ?: JSONObject.NULL)
            obj.put("isUser", m.isUser)
            obj.put("notes", m.notes)
            obj.put("photoUri", m.photoUri ?: JSONObject.NULL)
            membersArray.put(obj)
        }
        root.put("familyMembers", membersArray)

        // Land History
        val historyArray = JSONArray()
        for (h in history) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("plotId", h.plotId)
            obj.put("eventDate", h.eventDate)
            obj.put("eventName", h.eventName)
            obj.put("fromPerson", h.fromPerson)
            obj.put("toPerson", h.toPerson)
            obj.put("fromMemberId", h.fromMemberId ?: JSONObject.NULL)
            obj.put("toMemberId", h.toMemberId ?: JSONObject.NULL)
            obj.put("areaTransferred", h.areaTransferred)
            obj.put("documentReference", h.documentReference)
            obj.put("notes", h.notes)
            historyArray.put(obj)
        }
        root.put("landHistory", historyArray)

        // Documents
        val docsArray = JSONArray()
        for (d in documents) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("plotId", d.plotId ?: JSONObject.NULL)
            obj.put("familyMemberId", d.familyMemberId ?: JSONObject.NULL)
            obj.put("documentName", d.documentName)
            obj.put("documentType", d.documentType)
            obj.put("documentNumber", d.documentNumber)
            obj.put("date", d.date)
            obj.put("issuingAuthority", d.issuingAuthority)
            obj.put("imagePath", d.imagePath ?: JSONObject.NULL)
            obj.put("originalFileName", d.originalFileName)
            obj.put("fileSizeText", d.fileSizeText)
            obj.put("notes", d.notes)
            docsArray.put(obj)
        }
        root.put("documents", docsArray)

        root.toString(2)
    }

    suspend fun restoreFromJson(database: AppDatabase, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Parse plots
            val plotsList = mutableListOf<PlotEntity>()
            val plotsArray = root.optJSONArray("plots") ?: JSONArray()
            for (i in 0 until plotsArray.length()) {
                val obj = plotsArray.getJSONObject(i)
                plotsList.add(
                    PlotEntity(
                        id = obj.optLong("id", 0L),
                        plotNumber = obj.optString("plotNumber", ""),
                        khataNumber = obj.optString("khataNumber", ""),
                        landRecordNumber = obj.optString("landRecordNumber", ""),
                        landType = obj.optString("landType", "Agricultural"),
                        village = obj.optString("village", ""),
                        gramPanchayat = obj.optString("gramPanchayat", ""),
                        tahasil = obj.optString("tahasil", ""),
                        district = obj.optString("district", ""),
                        state = obj.optString("state", "Odisha"),
                        pinCode = obj.optString("pinCode", ""),
                        originalArea = obj.optDouble("originalArea", 0.0),
                        originalUnit = obj.optString("originalUnit", "Acres"),
                        convertedAreaText = obj.optString("convertedAreaText", ""),
                        recordedOwnerId = if (obj.isNull("recordedOwnerId")) null else obj.optLong("recordedOwnerId"),
                        recordedOwner = obj.optString("recordedOwner", ""),
                        previousOwnerId = if (obj.isNull("previousOwnerId")) null else obj.optLong("previousOwnerId"),
                        previousOwner = obj.optString("previousOwner", ""),
                        relationshipToUser = obj.optString("relationshipToUser", ""),
                        relationshipDescription = obj.optString("relationshipDescription", ""),
                        ownershipStatus = obj.optString("ownershipStatus", "Ancestral Inherited"),
                        dateAcquired = obj.optString("dateAcquired", ""),
                        sourceOfRecord = obj.optString("sourceOfRecord", ""),
                        documentReference = obj.optString("documentReference", ""),
                        notes = obj.optString("notes", ""),
                        latitude = if (obj.isNull("latitude")) null else obj.optDouble("latitude"),
                        longitude = if (obj.isNull("longitude")) null else obj.optDouble("longitude")
                    )
                )
            }

            // Parse family members
            val membersList = mutableListOf<FamilyMemberEntity>()
            val membersArray = root.optJSONArray("familyMembers") ?: JSONArray()
            for (i in 0 until membersArray.length()) {
                val obj = membersArray.getJSONObject(i)
                membersList.add(
                    FamilyMemberEntity(
                        id = obj.optLong("id", 0L),
                        fullName = obj.optString("fullName", obj.optString("name", "")),
                        gender = obj.optString("gender", "Male"),
                        relationshipToUser = obj.optString("relationshipToUser", obj.optString("relationship", "")),
                        relationshipDescription = obj.optString("relationshipDescription", ""),
                        generationLevel = obj.optInt("generationLevel", 1),
                        dateOfBirth = obj.optString("dateOfBirth", ""),
                        dateOfDeath = obj.optString("dateOfDeath", ""),
                        isDeceased = obj.optBoolean("isDeceased", false),
                        fatherId = if (obj.isNull("fatherId")) null else obj.optLong("fatherId"),
                        motherId = if (obj.isNull("motherId")) null else obj.optLong("motherId"),
                        spouseId = if (obj.isNull("spouseId")) null else obj.optLong("spouseId"),
                        isUser = obj.optBoolean("isUser", false),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            // Parse land history
            val historyList = mutableListOf<LandHistoryEntity>()
            val historyArray = root.optJSONArray("landHistory") ?: JSONArray()
            for (i in 0 until historyArray.length()) {
                val obj = historyArray.getJSONObject(i)
                historyList.add(
                    LandHistoryEntity(
                        id = obj.optLong("id", 0L),
                        plotId = obj.optLong("plotId", 0L),
                        eventDate = obj.optString("eventDate", ""),
                        eventName = obj.optString("eventName", ""),
                        fromPerson = obj.optString("fromPerson", ""),
                        toPerson = obj.optString("toPerson", ""),
                        fromMemberId = if (obj.isNull("fromMemberId")) null else obj.optLong("fromMemberId"),
                        toMemberId = if (obj.isNull("toMemberId")) null else obj.optLong("toMemberId"),
                        areaTransferred = obj.optString("areaTransferred", ""),
                        documentReference = obj.optString("documentReference", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            // Parse documents
            val docList = mutableListOf<AttachedDocumentEntity>()
            val docArray = root.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until docArray.length()) {
                val obj = docArray.getJSONObject(i)
                docList.add(
                    AttachedDocumentEntity(
                        id = obj.optLong("id", 0L),
                        plotId = if (obj.isNull("plotId")) null else obj.optLong("plotId"),
                        familyMemberId = if (obj.isNull("familyMemberId")) null else obj.optLong("familyMemberId"),
                        documentName = obj.optString("documentName", ""),
                        documentType = obj.optString("documentType", "RoR"),
                        documentNumber = obj.optString("documentNumber", ""),
                        date = obj.optString("date", ""),
                        issuingAuthority = obj.optString("issuingAuthority", ""),
                        imagePath = if (obj.isNull("imagePath")) null else obj.optString("imagePath"),
                        originalFileName = obj.optString("originalFileName", ""),
                        fileSizeText = obj.optString("fileSizeText", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            // Replace into database
            database.plotDao().insertPlots(plotsList)
            database.familyDao().insertMembers(membersList)
            database.landHistoryDao().insertHistoryList(historyList)
            database.documentDao().insertDocuments(docList)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
