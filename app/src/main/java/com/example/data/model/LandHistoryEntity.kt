package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "land_history")
data class LandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plotId: Long,
    val eventDate: String = "",
    val eventName: String = "Inheritance", // Inheritance, Transfer, Purchase, Sale, Partition, Mutation, Settlement, Gift Deed, Other
    val fromPerson: String = "",
    val toPerson: String = "",
    val fromMemberId: Long? = null,
    val toMemberId: Long? = null,
    val areaTransferred: String = "",
    val documentReference: String = "",
    val notes: String = ""
)
