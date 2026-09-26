package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.PlotEntity

@Composable
fun PdfHubScreen(
    plots: List<PlotEntity>,
    members: List<FamilyMemberEntity>,
    onExportCurrentPlot: (PlotEntity) -> Unit,
    onExportSelectedPlots: () -> Unit,
    onExportAllPlots: () -> Unit,
    onExportFamilyMember: (FamilyMemberEntity) -> Unit,
    onExportFamilyWithLand: () -> Unit,
    onExportCombinedDocs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().testTag("card_pdf_hub_hero")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "PDF Print & Xerox Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Professional print-ready documents engineered for A4 paper, cyber cafes, and high-clarity black & white photocopying (Xerox).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SpecBadge("Paper", "ISO A4")
                        SpecBadge("Margins", "36 pt / 0.5\"")
                        SpecBadge("Toner", "Ink-Safe 5% Gray")
                        SpecBadge("Break", "Auto-Paginated")
                    }
                }
            }
        }

        Text(
            text = "Export Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Export Current Plot
        if (plots.isNotEmpty()) {
            val firstPlot = plots.first()
            ExportOptionTile(
                title = "Export Current Plot as PDF",
                subtitle = "Single plot dossier: Plot ${firstPlot.plotNumber}, Khata ${firstPlot.khataNumber} with ownership chain & history",
                icon = Icons.Default.Landscape,
                testTag = "tile_export_current_plot",
                onClick = { onExportCurrentPlot(firstPlot) }
            )
        }

        // 2. Export Selected Plots
        ExportOptionTile(
            title = "Export Selected Plots as PDF",
            subtitle = "Custom selection with summary table followed by individual plot dossiers",
            icon = Icons.Default.Check,
            testTag = "tile_export_selected_plots",
            onClick = onExportSelectedPlots
        )

        // 3. Export All Land Records
        ExportOptionTile(
            title = "Export All Land Records as PDF",
            subtitle = "Consolidated summary table and complete multi-page portfolio of all ${plots.size} plots",
            icon = Icons.Default.PictureAsPdf,
            testTag = "tile_export_all_plots",
            onClick = onExportAllPlots
        )

        // 4. Export Family Member Details
        if (members.isNotEmpty()) {
            val primaryMember = members.firstOrNull { it.generationLevel == 4 } ?: members.first()
            ExportOptionTile(
                title = "Export Family Member Details as PDF",
                subtitle = "Member dossier for ${primaryMember.fullName} (${primaryMember.relationshipToUser}) with lineage tree & linked plots",
                icon = Icons.Default.People,
                testTag = "tile_export_family_member",
                onClick = { onExportFamilyMember(primaryMember) }
            )
        }

        // 5. Export Family + Linked Land Records
        ExportOptionTile(
            title = "Export Family + Linked Land Records as PDF",
            subtitle = "Complete multi-generational lineage roster integrated with all family land parcels",
            icon = Icons.Default.AccountTree,
            testTag = "tile_export_family_with_land",
            onClick = onExportFamilyWithLand
        )

        // 6. Export Selected Documents / Records as Combined PDF
        ExportOptionTile(
            title = "Export Combined Documents / Deeds as PDF",
            subtitle = "Master document index table followed by high-resolution A4 reproductions of deeds & Pattas",
            icon = Icons.Default.Description,
            testTag = "tile_export_combined_docs",
            onClick = onExportCombinedDocs
        )

        Spacer(Modifier.height(8.dp))

        // Xerox Quality Specs Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Print / Xerox Guarantee:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("• 100% black text and vector rules prevent toner bleeding on photocopiers.", style = MaterialTheme.typography.bodySmall)
                Text("• Running headers, footers, and 'Page X of Y' numbering ensure pages never get mixed up at cyber cafes.", style = MaterialTheme.typography.bodySmall)
                Text("• Required statutory disclaimer included on final page footer.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}

@Composable
fun SpecBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExportOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
