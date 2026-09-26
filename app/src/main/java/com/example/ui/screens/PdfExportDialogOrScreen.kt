package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GeneratedPdfItem
import com.example.data.model.PdfExportConfig
import com.example.data.model.PdfOrientation
import com.example.pdf.PdfPrintHelper
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfExportDialog(
    config: PdfExportConfig,
    isGenerating: Boolean,
    isLoadingPreview: Boolean,
    previewBitmaps: List<Bitmap>,
    lastGeneratedPdf: GeneratedPdfItem?,
    onDismiss: () -> Unit,
    onOrientationChange: (PdfOrientation) -> Unit,
    onOptionChange: (
        includeLandDetails: Boolean?,
        includeFamilyRelationship: Boolean?,
        includeLandHistory: Boolean?,
        includeDocuments: Boolean?,
        includeNotes: Boolean?,
        includeImages: Boolean?,
        filename: String?
    ) -> Unit,
    onRequestPreview: () -> Unit,
    onGeneratePdf: (onSuccess: (GeneratedPdfItem) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var filenameText by remember(config.filename) { mutableStateOf(config.filename) }
    var currentGeneratedItem by remember(lastGeneratedPdf) { mutableStateOf(lastGeneratedPdf) }
    var showPreviewViewer by remember { mutableStateOf(false) }

    // SAF File Saver Launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && currentGeneratedItem != null) {
            val sourceFile = File(currentGeneratedItem!!.filePath)
            val success = PdfPrintHelper.savePdfToUri(context, sourceFile, uri)
            if (success) {
                Toast.makeText(context, "Saved to device storage successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Export",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "PDF Export",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Optimized for Xerox & A4 Printing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_export_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Success Card (if just generated)
                AnimatedVisibility(visible = currentGeneratedItem != null) {
                    val item = currentGeneratedItem
                    if (item != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth().testTag("card_pdf_created_success")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "PDF created successfully",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "File: ${item.fileName} • ${item.fileSizeFormatted} • ${item.pageCount} page(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                // Action Buttons: [Open PDF] [Share] [Print] [Save]
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            PdfPrintHelper.openPdf(context, File(item.filePath))
                                        },
                                        modifier = Modifier.testTag("btn_open_pdf")
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Open PDF")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            PdfPrintHelper.sharePdf(context, File(item.filePath))
                                        },
                                        modifier = Modifier.testTag("btn_share_pdf")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            PdfPrintHelper.printPdf(context, File(item.filePath), item.title)
                                        },
                                        modifier = Modifier.testTag("btn_print_pdf")
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Print")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            saveFileLauncher.launch(item.fileName)
                                        },
                                        modifier = Modifier.testTag("btn_save_pdf")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }

                // Paper Specification
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Paper:", style = MaterialTheme.typography.labelMedium)
                            Text("A4 (595 × 842 pt)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Standard Xerox / Cyber Cafe Size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Orientation
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Orientation:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.orientation == PdfOrientation.PORTRAIT,
                            onClick = { onOrientationChange(PdfOrientation.PORTRAIT) },
                            label = { Text("Portrait (Recommended)") },
                            leadingIcon = if (config.orientation == PdfOrientation.PORTRAIT) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("chip_orientation_portrait")
                        )
                        FilterChip(
                            selected = config.orientation == PdfOrientation.LANDSCAPE,
                            onClick = { onOrientationChange(PdfOrientation.LANDSCAPE) },
                            label = { Text("Landscape") },
                            leadingIcon = if (config.orientation == PdfOrientation.LANDSCAPE) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("chip_orientation_landscape")
                        )
                    }
                }

                // Include Checkboxes
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Include:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                    ExportCheckboxRow(
                        label = "Land details",
                        checked = config.includeLandDetails,
                        testTag = "checkbox_land_details",
                        onCheckedChange = { onOptionChange(it, null, null, null, null, null, null) }
                    )

                    ExportCheckboxRow(
                        label = "Family relationship",
                        checked = config.includeFamilyRelationship,
                        testTag = "checkbox_family_relationship",
                        onCheckedChange = { onOptionChange(null, it, null, null, null, null, null) }
                    )

                    ExportCheckboxRow(
                        label = "Land history",
                        checked = config.includeLandHistory,
                        testTag = "checkbox_land_history",
                        onCheckedChange = { onOptionChange(null, null, it, null, null, null, null) }
                    )

                    ExportCheckboxRow(
                        label = "Documents",
                        checked = config.includeDocuments,
                        testTag = "checkbox_documents",
                        onCheckedChange = { onOptionChange(null, null, null, it, null, null, null) }
                    )

                    ExportCheckboxRow(
                        label = "Notes",
                        checked = config.includeNotes,
                        testTag = "checkbox_notes",
                        onCheckedChange = { onOptionChange(null, null, null, null, it, null, null) }
                    )

                    ExportCheckboxRow(
                        label = "Include attached document images",
                        checked = config.includeImages,
                        testTag = "checkbox_images",
                        onCheckedChange = { onOptionChange(null, null, null, null, null, it, null) }
                    )
                }

                // Filename
                OutlinedTextField(
                    value = filenameText,
                    onValueChange = {
                        filenameText = it
                        onOptionChange(null, null, null, null, null, null, it)
                    },
                    label = { Text("PDF Filename") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_pdf_filename")
                )

                // Preview Gallery (if loaded)
                if (isLoadingPreview) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (previewBitmaps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Live Print Preview (${previewBitmaps.size} page(s)):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            previewBitmaps.forEachIndexed { index, bitmap ->
                                Card(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Page ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(if (config.orientation == PdfOrientation.LANDSCAPE) 842f / 595f else 595f / 842f)
                                        )
                                        Text(
                                            text = "Page ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(4.dp),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Options Buttons
                Text("Options:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showPreviewViewer = true
                            onRequestPreview()
                        },
                        enabled = !isGenerating && !isLoadingPreview,
                        modifier = Modifier.weight(1f).testTag("btn_preview_pdf")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Preview PDF")
                    }

                    Button(
                        onClick = {
                            onGeneratePdf { item ->
                                currentGeneratedItem = item
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier.weight(1f).testTag("btn_generate_pdf")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCheckboxRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
