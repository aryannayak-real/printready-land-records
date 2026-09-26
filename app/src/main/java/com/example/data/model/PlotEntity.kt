package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plot_records")
data class PlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plotNumber: String = "",
    val khataNumber: String = "",
    val landRecordNumber: String = "",
    val landType: String = "Agricultural", // Agricultural, Homestead, Commercial, Pond, Industrial, Forest, Barren, Mixed, Other
    val village: String = "",
    val gramPanchayat: String = "",
    val tahasil: String = "",
    val district: String = "",
    val state: String = "Odisha",
    val pinCode: String = "",
    val originalArea: Double = 0.0,
    val originalUnit: String = "Acres",
    val convertedAreaText: String = "",
    val recordedOwnerId: Long? = null,
    val recordedOwner: String = "",
    val previousOwnerId: Long? = null,
    val previousOwner: String = "",
    val relationshipToUser: String = "", // e.g. "Great-grandfather", "Grandfather", "Father", "Self", etc.
    val relationshipDescription: String = "", // e.g. "Grandfather's father"
    val ownershipStatus: String = "Ancestral Inherited",
    val dateAcquired: String = "",
    val sourceOfRecord: String = "", // RoR / Patta, Registered Deed, Tahasil Order, Family Records, etc.
    val documentReference: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
