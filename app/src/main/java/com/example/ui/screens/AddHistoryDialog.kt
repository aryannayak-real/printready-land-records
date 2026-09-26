package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.LandHistoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHistoryDialog(
    plotId: Long,
    allMembers: List<FamilyMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (LandHistoryEntity) -> Unit
) {
    var eventDate by remember { mutableStateOf("") }
    var eventName by remember { mutableStateOf("Inheritance") }
    var fromPerson by remember { mutableStateOf("") }
    var toPerson by remember { mutableStateOf("") }
    var areaTransferred by remember { mutableStateOf("") }
    var documentReference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var eventNameExpanded by remember { mutableStateOf(false) }
    var fromPersonExpanded by remember { mutableStateOf(false) }
    var toPersonExpanded by remember { mutableStateOf(false) }

    val eventNameOptions = listOf(
        "Inheritance Succession",
        "Family Partition Settlement",
        "Registered Sale Deed",
        "Tahasil Mutation Order",
        "Family Gift Settlement",
        "Original Settlement Allotment",
        "Court Settlement",
        "Exchange / Mutual Transfer",
        "Other Event"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add Land History Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Event Date
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    label = { Text("Event Date / Year *") },
                    placeholder = { Text("e.g. 12-Mar-1984 or 1984") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_history_date")
                )

                // Event Name Dropdown
                ExposedDropdownMenuBox(
                    expanded = eventNameExpanded,
                    onExpandedChange = { eventNameExpanded = it }
                ) {
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Event Nature *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventNameExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("dropdown_history_event_name")
                    )
                    ExposedDropdownMenu(
                        expanded = eventNameExpanded,
                        onDismissRequest = { eventNameExpanded = false }
                    ) {
                        eventNameOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    eventName = opt
                                    eventNameExpanded = false
                                }
                            )
                        }
                    }
                }

                // From Person (Selector or typed)
                ExposedDropdownMenuBox(
                    expanded = fromPersonExpanded,
                    onExpandedChange = { fromPersonExpanded = it }
                ) {
                    OutlinedTextField(
                        value = fromPerson,
                        onValueChange = { fromPerson = it },
                        label = { Text("From Person / Authority *") },
                        placeholder = { Text("e.g. Late Baikuntha Nath Nayak or Tahasildar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromPersonExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("input_history_from_person")
                    )
                    ExposedDropdownMenu(
                        expanded = fromPersonExpanded,
                        onDismissRequest = { fromPersonExpanded = false }
                    ) {
                        allMembers.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                onClick = {
                                    fromPerson = m.fullName
                                    fromPersonExpanded = false
                                }
                            )
                        }
                    }
                }

                // To Person (Selector or typed)
                ExposedDropdownMenuBox(
                    expanded = toPersonExpanded,
                    onExpandedChange = { toPersonExpanded = it }
                ) {
                    OutlinedTextField(
                        value = toPerson,
                        onValueChange = { toPerson = it },
                        label = { Text("To Person / Recipient *") },
                        placeholder = { Text("e.g. Subash Chandra Nayak") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toPersonExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("input_history_to_person")
                    )
                    ExposedDropdownMenu(
                        expanded = toPersonExpanded,
                        onDismissRequest = { toPersonExpanded = false }
                    ) {
                        allMembers.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.relationshipToUser})") },
                                onClick = {
                                    toPerson = m.fullName
                                    toPersonExpanded = false
                                }
                            )
                        }
                    }
                }

                // Area Transferred & Document Reference
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = areaTransferred,
                        onValueChange = { areaTransferred = it },
                        label = { Text("Area Transferred") },
                        placeholder = { Text("e.g. 1.75 Acres") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_history_area")
                    )
                    OutlinedTextField(
                        value = documentReference,
                        onValueChange = { documentReference = it },
                        label = { Text("Document Reference") },
                        placeholder = { Text("e.g. Deed #412/1984") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_history_doc_ref")
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Registration Details") },
                    placeholder = { Text("Sub-Registrar office, case numbers, notes...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_history_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (eventDate.isNotBlank() && fromPerson.isNotBlank() && toPerson.isNotBlank()) {
                        val history = LandHistoryEntity(
                            plotId = plotId,
                            eventDate = eventDate.trim(),
                            eventName = eventName.trim(),
                            fromPerson = fromPerson.trim(),
                            toPerson = toPerson.trim(),
                            areaTransferred = areaTransferred.trim(),
                            documentReference = documentReference.trim(),
                            notes = notes.trim()
                        )
                        onSave(history)
                    }
                },
                modifier = Modifier.testTag("btn_save_history")
            ) {
                Text("Add History Record")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_history")) {
                Text("Cancel")
            }
        }
    )
}
