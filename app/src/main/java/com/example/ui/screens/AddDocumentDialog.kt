package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttachedDocumentEntity
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    plotId: Long?,
    familyMemberId: Long?,
    onDismiss: () -> Unit,
    onSave: (AttachedDocumentEntity) -> Unit
) {
    val context = LocalContext.current
    var documentName by remember { mutableStateOf("") }
    var documentType by remember { mutableStateOf("RoR") }
    var documentNumber by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var issuingAuthority by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var localCachedPath by remember { mutableStateOf<String?>(null) }

    var docTypeExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "document_${System.currentTimeMillis()}"
            // Copy to local app storage for permanent offline PDF inclusion
            try {
                val dir = File(context.filesDir, "user_documents")
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, "doc_${System.currentTimeMillis()}_${selectedFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}")
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                localCachedPath = targetFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                localCachedPath = uri.toString()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val dir = File(context.filesDir, "user_documents")
                if (!dir.exists()) dir.mkdirs()
                val photoFile = File(dir, "camera_doc_${System.currentTimeMillis()}.jpg")
                FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                selectedFileName = photoFile.name
                localCachedPath = photoFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val documentTypeOptions = listOf(
        "RoR",
        "Khata/Khatiyan",
        "Patta",
        "Sale Deed",
        "Mutation",
        "Tax Receipt",
        "Survey Document",
        "Map",
        "Inheritance Document",
        "Partition Document",
        "Gift Deed",
        "Court Document",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Attach Land Document / Deed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Document Name
                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Document Name *") },
                    placeholder = { Text("e.g. Registered Sale Deed 2002") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_doc_name")
                )

                // Document Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = docTypeExpanded,
                    onExpandedChange = { docTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = documentType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_doc_type")
                    )
                    ExposedDropdownMenu(
                        expanded = docTypeExpanded,
                        onDismissRequest = { docTypeExpanded = false }
                    ) {
                        documentTypeOptions.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    documentType = type
                                    docTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Document Number & Date
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = documentNumber,
                        onValueChange = { documentNumber = it },
                        label = { Text("Document / Deed No.") },
                        placeholder = { Text("e.g. SD-2002-1842") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_doc_num")
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date of Issue") },
                        placeholder = { Text("e.g. 20-Oct-2002") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_doc_date")
                    )
                }

                // Issuing Authority
                OutlinedTextField(
                    value = issuingAuthority,
                    onValueChange = { issuingAuthority = it },
                    label = { Text("Issuing Authority / Office") },
                    placeholder = { Text("e.g. Sub-Registrar Baranga / Tahasildar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_doc_authority")
                )

                // Attach File / Photo Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Photo / Scan / PDF Attachment:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                        if (selectedFileName.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Attached: $selectedFileName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch() },
                                modifier = Modifier.weight(1f).testTag("btn_take_doc_camera")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Camera", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).testTag("btn_pick_doc_image")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Gallery", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1.1f).testTag("btn_pick_doc_pdf")
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("File/PDF", maxLines = 1)
                            }
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes & Verification") },
                    placeholder = { Text("Volume number, page numbers, mutation order details...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_doc_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (documentName.isNotBlank()) {
                        val doc = AttachedDocumentEntity(
                            plotId = plotId,
                            familyMemberId = familyMemberId,
                            documentName = documentName.trim(),
                            documentType = documentType.trim(),
                            documentNumber = documentNumber.trim(),
                            date = date.trim(),
                            issuingAuthority = issuingAuthority.trim(),
                            imagePath = localCachedPath,
                            originalFileName = selectedFileName.ifEmpty { "$documentName.pdf" },
                            fileSizeText = if (localCachedPath != null && File(localCachedPath!!).exists()) "${(File(localCachedPath!!).length() / 1024)} KB" else "Recorded Deed",
                            notes = notes.trim()
                        )
                        onSave(doc)
                    }
                },
                modifier = Modifier.testTag("btn_save_document")
            ) {
                Text("Attach Document")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_doc")) {
                Text("Cancel")
            }
        }
    )
}
