package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String = "",
    val gender: String = "Male", // Male, Female, Other
    val relationshipToUser: String = "", // Great-grandfather, Grandfather, Father, Mother, Self, Spouse, Brother, Sister, Son, Daughter, Uncle, Aunt, Cousin, Other
    val relationshipDescription: String = "", // e.g. "Grandfather's father"
    val generationLevel: Int = 1, // 1: Great-grandparents, 2: Grandparents, 3: Parents, 4: Self, 5: Children
    val dateOfBirth: String = "",
    val dateOfDeath: String = "",
    val isDeceased: Boolean = false,
    val fatherId: Long? = null,
    val motherId: Long? = null,
    val spouseId: Long? = null,
    val isUser: Boolean = false,
    val notes: String = "",
    val photoUri: String? = null
) {
    // For backwards compatibility and convenient display
    val name: String get() = fullName
}
