package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attached_documents")
data class AttachedDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plotId: Long? = null,
    val familyMemberId: Long? = null,
    val documentName: String,
    val documentType: String, // "RoR (Record of Rights)", "Sale Deed", "Patta", "Mutation Certificate", "Encumbrance Certificate", "Possession Certificate"
    val documentNumber: String,
    val date: String,
    val issuingAuthority: String,
    val imagePath: String? = null, // local file path to cached deed image / scan
    val originalFileName: String = "",
    val fileSizeText: String = "240 KB",
    val notes: String = ""
)
