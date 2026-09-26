package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FamilyMemberEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFamilyMemberDialog(
    memberToEdit: FamilyMemberEntity?,
    allMembers: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (FamilyMemberEntity) -> Unit
) {
    var fullName by remember { mutableStateOf(memberToEdit?.fullName ?: "") }
    var gender by remember { mutableStateOf(memberToEdit?.gender ?: "Male") }
    var relationshipToUser by remember { mutableStateOf(memberToEdit?.relationshipToUser ?: "") }
    var relationshipDescription by remember { mutableStateOf(memberToEdit?.relationshipDescription ?: "") }
    var generationLevel by remember { mutableIntStateOf(memberToEdit?.generationLevel ?: 3) }
    var dateOfBirth by remember { mutableStateOf(memberToEdit?.dateOfBirth ?: "") }
    var dateOfDeath by remember { mutableStateOf(memberToEdit?.dateOfDeath ?: "") }
    var isDeceased by remember { mutableStateOf(memberToEdit?.isDeceased ?: false) }
    var fatherId by remember { mutableStateOf<Long?>(memberToEdit?.fatherId) }
    var motherId by remember { mutableStateOf<Long?>(memberToEdit?.motherId) }
    var spouseId by remember { mutableStateOf<Long?>(memberToEdit?.spouseId) }
    var isUser by remember { mutableStateOf(memberToEdit?.isUser ?: false) }
    var notes by remember { mutableStateOf(memberToEdit?.notes ?: "") }

    var relDropdownExpanded by remember { mutableStateOf(false) }
    var fatherDropdownExpanded by remember { mutableStateOf(false) }
    var motherDropdownExpanded by remember { mutableStateOf(false) }
    var spouseDropdownExpanded by remember { mutableStateOf(false) }

    val relationshipOptions = listOf(
        "Self",
        "Father",
        "Mother",
        "Grandfather",
        "Grandmother",
        "Great-grandfather",
        "Great-grandmother",
        "Spouse",
        "Brother",
        "Sister",
        "Son",
        "Daughter",
        "Uncle",
        "Aunt",
        "Cousin",
        "Grandson",
        "Granddaughter",
        "Other"
    )

    // Filter potential parents/spouses (exclude self)
    val candidateMembers = remember(allMembers, memberToEdit) {
        allMembers.filter { it.id != (memberToEdit?.id ?: 0L) }
    }

    val selectedFatherName = candidateMembers.firstOrNull { it.id == fatherId }?.fullName ?: "None"
    val selectedMotherName = candidateMembers.firstOrNull { it.id == motherId }?.fullName ?: "None"
    val selectedSpouseName = candidateMembers.firstOrNull { it.id == spouseId }?.fullName ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (memberToEdit == null) "Add Family Member" else "Edit ${memberToEdit.fullName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Late Baikuntha Nath Nayak") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name")
                )

                // Gender Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Gender:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) }
                            )
                        }
                    }
                }

                // Relationship to User Dropdown
                ExposedDropdownMenuBox(
                    expanded = relDropdownExpanded,
                    onExpandedChange = { relDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = relationshipToUser,
                        onValueChange = { relationshipToUser = it },
                        label = { Text("Relationship to User *") },
                        placeholder = { Text("Select or type relationship") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_relationship_to_user")
                    )
                    ExposedDropdownMenu(
                        expanded = relDropdownExpanded,
                        onDismissRequest = { relDropdownExpanded = false }
                    ) {
                        relationshipOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    relationshipToUser = option
                                    if (option == "Self") isUser = true
                                    // Auto-suggest generation level
                                    when (option) {
                                        "Great-grandfather", "Great-grandmother" -> generationLevel = 1
                                        "Grandfather", "Grandmother" -> generationLevel = 2
                                        "Father", "Mother", "Uncle", "Aunt" -> generationLevel = 3
                                        "Self", "Spouse", "Brother", "Sister", "Cousin" -> generationLevel = 4
                                        "Son", "Daughter" -> generationLevel = 5
                                    }
                                    relDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Relationship Description
                OutlinedTextField(
                    value = relationshipDescription,
                    onValueChange = { relationshipDescription = it },
                    label = { Text("Relationship Description") },
                    placeholder = { Text("e.g. Grandfather's father / Paternal uncle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_relationship_desc")
                )

                // Is Current User Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isUser,
                        onCheckedChange = {
                            isUser = it
                            if (it) relationshipToUser = "Self"
                        }
                    )
                    Text("This person is the current App User (Self)", style = MaterialTheme.typography.bodyMedium)
                }

                // Generation Level (1 to 5)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Generation Tier: Gen $generationLevel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { lvl ->
                            FilterChip(
                                selected = generationLevel == lvl,
                                onClick = { generationLevel = lvl },
                                label = { Text("Gen $lvl") }
                            )
                        }
                    }
                }

                // Link Father Dropdown
                ExposedDropdownMenuBox(
                    expanded = fatherDropdownExpanded,
                    onExpandedChange = { fatherDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFatherName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link Father") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fatherDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_link_father")
                    )
                    ExposedDropdownMenu(
                        expanded = fatherDropdownExpanded,
                        onDismissRequest = { fatherDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Unknown / Root)") },
                            onClick = {
                                fatherId = null
                                fatherDropdownExpanded = false
                            }
                        )
                        candidateMembers.filter { it.gender != "Female" }.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                onClick = {
                                    fatherId = m.id
                                    fatherDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Link Mother Dropdown
                ExposedDropdownMenuBox(
                    expanded = motherDropdownExpanded,
                    onExpandedChange = { motherDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMotherName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link Mother") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = motherDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_link_mother")
                    )
                    ExposedDropdownMenu(
                        expanded = motherDropdownExpanded,
                        onDismissRequest = { motherDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                motherId = null
                                motherDropdownExpanded = false
                            }
                        )
                        candidateMembers.filter { it.gender != "Male" }.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                onClick = {
                                    motherId = m.id
                                    motherDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Link Spouse Dropdown
                ExposedDropdownMenuBox(
                    expanded = spouseDropdownExpanded,
                    onExpandedChange = { spouseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSpouseName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link Spouse") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = spouseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_link_spouse")
                    )
                    ExposedDropdownMenu(
                        expanded = spouseDropdownExpanded,
                        onDismissRequest = { spouseDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None / Unmarried") },
                            onClick = {
                                spouseId = null
                                spouseDropdownExpanded = false
                            }
                        )
                        candidateMembers.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                onClick = {
                                    spouseId = m.id
                                    spouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dates of Birth & Death
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text("Date / Year of Birth") },
                        placeholder = { Text("e.g. 1954") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = if (isDeceased) dateOfDeath else "Living",
                        onValueChange = { dateOfDeath = it },
                        label = { Text("Date of Death") },
                        placeholder = { Text("e.g. 2004") },
                        enabled = isDeceased,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDeceased,
                        onCheckedChange = { isDeceased = it }
                    )
                    Text("Deceased (Late)", style = MaterialTheme.typography.bodyMedium)
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Biographical Notes & Family Branch") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        val updated = (memberToEdit ?: FamilyMemberEntity()).copy(
                            fullName = fullName.trim(),
                            gender = gender,
                            relationshipToUser = relationshipToUser.trim(),
                            relationshipDescription = relationshipDescription.trim(),
                            generationLevel = generationLevel,
                            dateOfBirth = dateOfBirth.trim(),
                            dateOfDeath = if (isDeceased) dateOfDeath.trim() else "",
                            isDeceased = isDeceased,
                            fatherId = fatherId,
                            motherId = motherId,
                            spouseId = spouseId,
                            isUser = isUser,
                            notes = notes.trim()
                        )
                        onSave(updated)
                    }
                },
                modifier = Modifier.testTag("btn_save_member")
            ) {
                Text("Save Person")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_member")) {
                Text("Cancel")
            }
        }
    )
}
