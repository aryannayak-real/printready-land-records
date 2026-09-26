package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.FamilyTreeHelper
import com.example.data.model.PlotEntity

@Composable
fun FamilyScreen(
    members: List<FamilyMemberEntity>,
    plots: List<PlotEntity>,
    onAddMember: () -> Unit,
    onEditMember: (FamilyMemberEntity) -> Unit,
    onDeleteMember: (FamilyMemberEntity) -> Unit,
    onExportMemberPdf: (FamilyMemberEntity) -> Unit,
    onExportFamilyWithLandPdf: () -> Unit
) {
    var selectedGenerationFilter by remember { mutableIntStateOf(0) } // 0 = All
    var viewingMember by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    var memberToDelete by remember { mutableStateOf<FamilyMemberEntity?>(null) }

    val filteredMembers = remember(members, selectedGenerationFilter) {
        if (selectedGenerationFilter == 0) members
        else members.filter { it.generationLevel == selectedGenerationFilter }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("screen_family")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // Action Card Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().testTag("card_family_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Family Tree & Heritage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${members.size} members across generations linked to land",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onAddMember,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_add_family_member")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Person")
                        }

                        OutlinedButton(
                            onClick = onExportFamilyWithLandPdf,
                            modifier = Modifier.testTag("btn_export_family_with_land")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Family PDF")
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Generation Tier Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedGenerationFilter == 0,
                    onClick = { selectedGenerationFilter = 0 },
                    label = { Text("All (${members.size})") }
                )
                FilterChip(
                    selected = selectedGenerationFilter == 1,
                    onClick = { selectedGenerationFilter = 1 },
                    label = { Text("Gen 1 (Great-grands)") }
                )
                FilterChip(
                    selected = selectedGenerationFilter == 2,
                    onClick = { selectedGenerationFilter = 2 },
                    label = { Text("Gen 2 (Grands)") }
                )
                FilterChip(
                    selected = selectedGenerationFilter == 3,
                    onClick = { selectedGenerationFilter = 3 },
                    label = { Text("Gen 3 (Parents)") }
                )
                FilterChip(
                    selected = selectedGenerationFilter == 4,
                    onClick = { selectedGenerationFilter = 4 },
                    label = { Text("Gen 4 (Self/Sibs)") }
                )
                FilterChip(
                    selected = selectedGenerationFilter == 5,
                    onClick = { selectedGenerationFilter = 5 },
                    label = { Text("Gen 5 (Children)") }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Member List
            if (filteredMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("No family members found", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + Add Person to start building the family lineage.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        val memberPlots = plots.filter {
                            it.recordedOwnerId == member.id ||
                                    it.previousOwnerId == member.id ||
                                    it.recordedOwner.contains(member.fullName, ignoreCase = true)
                        }

                        FamilyMemberRowCard(
                            member = member,
                            linkedPlotsCount = memberPlots.size,
                            onClick = { viewingMember = member },
                            onEdit = { onEditMember(member) },
                            onDelete = { memberToDelete = member },
                            onExportPdf = { onExportMemberPdf(member) }
                        )
                    }
                }
            }
        }
    }

    // View Member Detail Dialog (Includes "Land Records Linked to This Person" & Visual Family Connection)
    if (viewingMember != null) {
        val m = viewingMember!!
        val memberPlots = plots.filter {
            it.recordedOwnerId == m.id ||
                    it.previousOwnerId == m.id ||
                    it.recordedOwner.contains(m.fullName, ignoreCase = true)
        }
        val father = members.firstOrNull { it.id == m.fatherId }
        val mother = members.firstOrNull { it.id == m.motherId }
        val spouse = members.firstOrNull { it.id == m.spouseId }
        val children = members.filter { it.fatherId == m.id || it.motherId == m.id }
        val lineage = FamilyTreeHelper.traceLineageToUser(m.id, members)

        AlertDialog(
            onDismissRequest = { viewingMember = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (m.gender == "Female") Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (m.gender == "Female") Icons.Default.Female else Icons.Default.Male,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(m.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${m.relationshipToUser.ifEmpty { "Family Member" }} • Gen ${m.generationLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile details
                    DetailLine("Gender", m.gender)
                    DetailLine("Birth / Death", "${m.dateOfBirth.ifEmpty { "?" }} - ${if (m.isDeceased) m.dateOfDeath.ifEmpty { "Late" } else "Living"}")
                    if (m.relationshipDescription.isNotBlank()) {
                        DetailLine("Relationship Detail", m.relationshipDescription)
                    }
                    if (father != null) {
                        DetailLine("Father", father.fullName)
                    }
                    if (mother != null) {
                        DetailLine("Mother", mother.fullName)
                    }
                    if (spouse != null) {
                        DetailLine("Spouse", spouse.fullName)
                    }
                    if (children.isNotEmpty()) {
                        DetailLine("Children", children.joinToString { it.fullName })
                    }
                    if (m.notes.isNotBlank()) {
                        DetailLine("Notes", m.notes)
                    }

                    HorizontalDivider()

                    // Visual Family Lineage Connection
                    Text("Family Relationship Flow:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        lineage.forEachIndexed { idx, step ->
                            Text(step, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            if (idx < lineage.size - 1) {
                                Text("↓", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Land Records Linked to This Person (Prompt 4)
                    Text("Land Records Linked to This Person:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    if (memberPlots.isEmpty()) {
                        Text("No land plots currently recorded under this person's name.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        memberPlots.forEach { p ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Plot ${p.plotNumber} • Khata #${p.khataNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Text("${p.originalArea} ${p.originalUnit}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("Village: ${p.village} • Status: ${p.ownershipStatus}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewingMember = null
                        onExportMemberPdf(m)
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Member PDF")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewingMember = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (memberToDelete != null) {
        val m = memberToDelete!!
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Delete Family Member?") },
            text = { Text("Are you sure you want to remove ${m.fullName}? This person will be unlinked from records.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(m)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Person")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FamilyMemberRowCard(
    member: FamilyMemberEntity,
    linkedPlotsCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("card_member_${member.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (member.gender == "Female") Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (member.gender == "Female") Icons.Default.Female else Icons.Default.Male,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(member.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (member.isUser) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                            Text("Self", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text(
                    "${member.relationshipToUser.ifEmpty { "Family" }} • Gen ${member.generationLevel} • ${if (member.isDeceased) "Late" else "Living"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (linkedPlotsCount > 0) {
                    Text(
                        "$linkedPlotsCount linked land parcel(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onExportPdf, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
