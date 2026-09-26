package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.PlotEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlotDialog(
    plotToEdit: PlotEntity?,
    allMembers: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (PlotEntity) -> Unit,
    onQuickAddPerson: () -> Unit
) {
    // Form fields (Notice: NO fake default values for new record! Empty stays empty!)
    var plotNumber by remember { mutableStateOf(plotToEdit?.plotNumber ?: "") }
    var khataNumber by remember { mutableStateOf(plotToEdit?.khataNumber ?: "") }
    var landRecordNumber by remember { mutableStateOf(plotToEdit?.landRecordNumber ?: "") }
    var landType by remember { mutableStateOf(plotToEdit?.landType ?: "Agricultural") }

    var village by remember { mutableStateOf(plotToEdit?.village ?: "") }
    var gramPanchayat by remember { mutableStateOf(plotToEdit?.gramPanchayat ?: "") }
    var tahasil by remember { mutableStateOf(plotToEdit?.tahasil ?: "") }
    var district by remember { mutableStateOf(plotToEdit?.district ?: "") }
    var state by remember { mutableStateOf(plotToEdit?.state ?: "Odisha") }
    var pinCode by remember { mutableStateOf(plotToEdit?.pinCode ?: "") }

    var areaText by remember { mutableStateOf(if (plotToEdit != null && plotToEdit.originalArea > 0) plotToEdit.originalArea.toString() else "") }
    var originalUnit by remember { mutableStateOf(plotToEdit?.originalUnit ?: "Acres") }

    // Family Member Selection (Proper relational linking!)
    var recordedOwnerId by remember { mutableStateOf<Long?>(plotToEdit?.recordedOwnerId) }
    var recordedOwnerName by remember { mutableStateOf(plotToEdit?.recordedOwner ?: "") }

    var previousOwnerId by remember { mutableStateOf<Long?>(plotToEdit?.previousOwnerId) }
    var previousOwnerName by remember { mutableStateOf(plotToEdit?.previousOwner ?: "") }

    var relationshipToUser by remember { mutableStateOf(plotToEdit?.relationshipToUser ?: "") }
    var relationshipDescription by remember { mutableStateOf(plotToEdit?.relationshipDescription ?: "") }

    var ownershipStatus by remember { mutableStateOf(plotToEdit?.ownershipStatus ?: "Ancestral Inherited") }
    var dateAcquired by remember { mutableStateOf(plotToEdit?.dateAcquired ?: "") }
    var sourceOfRecord by remember { mutableStateOf(plotToEdit?.sourceOfRecord ?: "") }
    var documentReference by remember { mutableStateOf(plotToEdit?.documentReference ?: "") }

    var notes by remember { mutableStateOf(plotToEdit?.notes ?: "") }
    var latText by remember { mutableStateOf(plotToEdit?.latitude?.toString() ?: "") }
    var lonText by remember { mutableStateOf(plotToEdit?.longitude?.toString() ?: "") }

    // Dropdown expanded states
    var landTypeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var recordedOwnerExpanded by remember { mutableStateOf(false) }
    var prevOwnerExpanded by remember { mutableStateOf(false) }
    var relExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }

    val landTypeOptions = listOf(
        "Agricultural",
        "Homestead",
        "Commercial",
        "Pond / Fishery",
        "Industrial",
        "Orchard / Plantation",
        "Forest / Woodlot",
        "Barren",
        "Mixed Use",
        "Other"
    )

    val unitOptions = listOf("Acres", "Hectares", "Guntha", "Bigha", "Cent", "Decimal", "Sq. Ft")

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

    val ownershipStatusOptions = listOf(
        "Ancestral Inherited",
        "Purchased (Sale Deed)",
        "Partition Settlement",
        "Family Gift Deed",
        "Under Mutation",
        "Homestead Custody",
        "Disputed",
        "Clear Title"
    )

    val sourceOptions = listOf(
        "Record of Rights (RoR) / Patta",
        "Registered Sale Deed",
        "Tahasil Mutation Order",
        "Partition Deed",
        "Settlement Khatian",
        "Gift Deed",
        "Court Decree / Order",
        "Family Oral Transmission",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Landscape, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (plotToEdit == null) "Add Land Plot Record" else "Edit Plot ${plotToEdit.plotNumber}",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: Land Identification
                SectionHeader("1. Land Identification")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = plotNumber,
                        onValueChange = { plotNumber = it },
                        label = { Text("Plot No. *") },
                        placeholder = { Text("e.g. 123/45") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_plot_number")
                    )
                    OutlinedTextField(
                        value = khataNumber,
                        onValueChange = { khataNumber = it },
                        label = { Text("Khata No. *") },
                        placeholder = { Text("e.g. 456") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_khata_number")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = landRecordNumber,
                        onValueChange = { landRecordNumber = it },
                        label = { Text("Land Record / Ref No.") },
                        placeholder = { Text("e.g. LR-2023-9842") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_record_ref")
                    )

                    // Land Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = landTypeExpanded,
                        onExpandedChange = { landTypeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = landType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Land Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = landTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("dropdown_land_type")
                        )
                        ExposedDropdownMenu(
                            expanded = landTypeExpanded,
                            onDismissRequest = { landTypeExpanded = false }
                        ) {
                            landTypeOptions.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        landType = t
                                        landTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Section: Location
                SectionHeader("2. Location")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text("Village *") },
                        placeholder = { Text("e.g. Rampur") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_village")
                    )
                    OutlinedTextField(
                        value = gramPanchayat,
                        onValueChange = { gramPanchayat = it },
                        label = { Text("Gram Panchayat") },
                        placeholder = { Text("e.g. Rampur GP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_gp")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tahasil,
                        onValueChange = { tahasil = it },
                        label = { Text("Tahasil *") },
                        placeholder = { Text("e.g. Sadar") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_tahasil")
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District *") },
                        placeholder = { Text("e.g. Cuttack") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_district")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_state")
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("PIN Code") },
                        placeholder = { Text("e.g. 753001") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_pincode")
                    )
                }

                // Section: Area
                SectionHeader("3. Area")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = areaText,
                        onValueChange = { areaText = it },
                        label = { Text("Area Value *") },
                        placeholder = { Text("e.g. 1.75") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_area")
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = originalUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("dropdown_unit")
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            unitOptions.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        originalUnit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Section: Ownership & Family Member Selection
                SectionHeader("4. Ownership & Family Member Connection")

                // Recorded Owner Selector
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ExposedDropdownMenuBox(
                        expanded = recordedOwnerExpanded,
                        onExpandedChange = { recordedOwnerExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = recordedOwnerName.ifEmpty { "Select Family Member" },
                            onValueChange = { recordedOwnerName = it },
                            label = { Text("Recorded Owner *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recordedOwnerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable).testTag("selector_recorded_owner")
                        )
                        ExposedDropdownMenu(
                            expanded = recordedOwnerExpanded,
                            onDismissRequest = { recordedOwnerExpanded = false }
                        ) {
                            allMembers.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                    onClick = {
                                        recordedOwnerId = m.id
                                        recordedOwnerName = m.fullName
                                        if (m.relationshipToUser.isNotBlank() && relationshipToUser.isBlank()) {
                                            relationshipToUser = m.relationshipToUser
                                            relationshipDescription = m.relationshipDescription
                                        }
                                        recordedOwnerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onQuickAddPerson,
                        modifier = Modifier.padding(start = 4.dp).testTag("btn_quick_add_owner")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Family Member", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Previous Owner Selector
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ExposedDropdownMenuBox(
                        expanded = prevOwnerExpanded,
                        onExpandedChange = { prevOwnerExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = previousOwnerName.ifEmpty { "Select Previous Owner / Ancestor" },
                            onValueChange = { previousOwnerName = it },
                            label = { Text("Previous Owner") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prevOwnerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable).testTag("selector_previous_owner")
                        )
                        ExposedDropdownMenu(
                            expanded = prevOwnerExpanded,
                            onDismissRequest = { prevOwnerExpanded = false }
                        ) {
                            allMembers.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                    onClick = {
                                        previousOwnerId = m.id
                                        previousOwnerName = m.fullName
                                        prevOwnerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onQuickAddPerson,
                        modifier = Modifier.padding(start = 4.dp).testTag("btn_quick_add_prev_owner")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Family Member", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Relationship to Current User
                ExposedDropdownMenuBox(
                    expanded = relExpanded,
                    onExpandedChange = { relExpanded = it }
                ) {
                    OutlinedTextField(
                        value = relationshipToUser,
                        onValueChange = { relationshipToUser = it },
                        label = { Text("Relationship to Current User *") },
                        placeholder = { Text("e.g. Great-grandfather, Father, Self") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable).testTag("dropdown_relationship")
                    )
                    ExposedDropdownMenu(
                        expanded = relExpanded,
                        onDismissRequest = { relExpanded = false }
                    ) {
                        relationshipOptions.forEach { rel ->
                            DropdownMenuItem(
                                text = { Text(rel) },
                                onClick = {
                                    relationshipToUser = rel
                                    relExpanded = false
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

                // Ownership Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = it }
                ) {
                    OutlinedTextField(
                        value = ownershipStatus,
                        onValueChange = { ownershipStatus = it },
                        label = { Text("Ownership Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable).testTag("dropdown_ownership_status")
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        ownershipStatusOptions.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    ownershipStatus = s
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Section: Acquisition Details
                SectionHeader("5. Acquisition & Source")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateAcquired,
                        onValueChange = { dateAcquired = it },
                        label = { Text("Date Acquired / Inherited") },
                        placeholder = { Text("e.g. 12-Mar-1984") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_date_acquired")
                    )
                    OutlinedTextField(
                        value = documentReference,
                        onValueChange = { documentReference = it },
                        label = { Text("Document Reference") },
                        placeholder = { Text("e.g. Deed #412/1984") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_doc_ref")
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sourceOfRecord,
                        onValueChange = { sourceOfRecord = it },
                        label = { Text("Source of Record") },
                        placeholder = { Text("e.g. RoR / Patta, Registered Sale Deed") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable).testTag("dropdown_source_record")
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        sourceOptions.forEach { src ->
                            DropdownMenuItem(
                                text = { Text(src) },
                                onClick = {
                                    sourceOfRecord = src
                                    sourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Section: GPS Demarcation (Optional)
                SectionHeader("6. GPS Demarcation (Optional)")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_lat")
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitude") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_lon")
                    )
                }

                // Section: Notes
                SectionHeader("7. Notes & Observations")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Observations") },
                    placeholder = { Text("Boundaries, natural features, water rights, irrigation...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plotNumber.isNotBlank() && khataNumber.isNotBlank()) {
                        val parsedArea = areaText.toDoubleOrNull() ?: 0.0
                        val latVal = latText.toDoubleOrNull()
                        val lonVal = lonText.toDoubleOrNull()

                        val convertedText = if (parsedArea > 0) {
                            when (originalUnit) {
                                "Acres" -> "$parsedArea Acres (${(parsedArea * 43560).toInt()} Sq. Ft / ${(parsedArea * 40.4686).toInt()} Dec)"
                                "Hectares" -> "$parsedArea Ha (${(parsedArea * 2.47105).format(2)} Acres)"
                                else -> "$parsedArea $originalUnit"
                            }
                        } else ""

                        val updated = (plotToEdit ?: PlotEntity()).copy(
                            plotNumber = plotNumber.trim(),
                            khataNumber = khataNumber.trim(),
                            landRecordNumber = landRecordNumber.trim(),
                            landType = landType.trim(),
                            village = village.trim(),
                            gramPanchayat = gramPanchayat.trim(),
                            tahasil = tahasil.trim(),
                            district = district.trim(),
                            state = state.trim(),
                            pinCode = pinCode.trim(),
                            originalArea = parsedArea,
                            originalUnit = originalUnit,
                            convertedAreaText = convertedText,
                            recordedOwnerId = recordedOwnerId,
                            recordedOwner = recordedOwnerName.trim(),
                            previousOwnerId = previousOwnerId,
                            previousOwner = previousOwnerName.trim(),
                            relationshipToUser = relationshipToUser.trim(),
                            relationshipDescription = relationshipDescription.trim(),
                            ownershipStatus = ownershipStatus.trim(),
                            dateAcquired = dateAcquired.trim(),
                            sourceOfRecord = sourceOfRecord.trim(),
                            documentReference = documentReference.trim(),
                            notes = notes.trim(),
                            latitude = latVal,
                            longitude = lonVal
                        )
                        onSave(updated)
                    }
                },
                modifier = Modifier.testTag("btn_save_plot")
            ) {
                Text("Save Plot Record")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_plot")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

private fun Double.format(digits: Int) = String.format(java.util.Locale.US, "%.${digits}f", this)
