@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocItem
import com.example.data.pdf.PdfEngine
import com.example.ui.components.DocumentListItem
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.util.Strings
import com.example.ui.viewmodel.AppSubScreen
import com.example.ui.viewmodel.DocuFlowViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class PdfManagerTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: (DocItem) -> Unit
)

@Composable
fun PdfManagerScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val context = LocalContext.current

    // Filter only genuine PDFs in recents
    val recentPdfs = remember(recentDocs) {
        recentDocs.filter { it.category in listOf("pdf", "scan") || it.name.endsWith(".pdf", ignoreCase = true) }
    }

    // Native SAF PDF Picker
    val safPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSafPdf(uri)
        }
    }

    // SAF Save Copy Picker
    var docToExport by remember { mutableStateOf<DocItem?>(null) }
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri != null && docToExport != null) {
            val srcFile = File(docToExport!!.filePath)
            if (srcFile.exists()) {
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        srcFile.inputStream().use { it.copyTo(out) }
                    }
                    viewModel.showSnackbar(if (Strings.isHindi()) "प्रति सुरक्षित की गई" else "Copy saved to device")
                } catch (e: Exception) {
                    viewModel.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    // 16 Tools Definition
    val toolsList = remember(activeDoc) {
        listOf(
            PdfManagerTool(
                id = "view",
                title = if (Strings.isHindi()) "PDF देखें" else "View PDF",
                subtitle = if (Strings.isHindi()) "रीडर मोड में खोलें" else "Open full reader",
                icon = Icons.Default.Visibility,
                color = IosBlue,
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_READER, doc) }
            ),
            PdfManagerTool(
                id = "edit",
                title = if (Strings.isHindi()) "PDF संपादित करें" else "Edit PDF",
                subtitle = if (Strings.isHindi()) "एनोटेट और ड्रा करें" else "Annotate & draw",
                icon = Icons.Default.Edit,
                color = IosIndigo,
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_EDITOR, doc) }
            ),
            PdfManagerTool(
                id = "merge",
                title = if (Strings.isHindi()) "PDF मर्ज करें" else "Merge PDF",
                subtitle = if (Strings.isHindi()) "अन्य PDF फ़ाइलें जोड़ें" else "Combine with other PDFs",
                icon = Icons.Default.CallMerge,
                color = IosOrange,
                onClick = { doc ->
                    // Set active doc as first in merge list and navigate
                    if (!viewModel.selectedMergeDocs.value.any { it.id == doc.id }) {
                        viewModel.toggleMergeDoc(doc)
                    }
                    viewModel.navigateToSubScreen(AppSubScreen.PDF_MERGER)
                }
            ),
            PdfManagerTool(
                id = "split",
                title = if (Strings.isHindi()) "PDF विभाजित करें" else "Split PDF",
                subtitle = if (Strings.isHindi()) "पृष्ठ सीमा अलग करें" else "Split into parts",
                icon = Icons.Default.CallSplit,
                color = IosRed,
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_SPLITTER, doc) }
            ),
            PdfManagerTool(
                id = "compress",
                title = if (Strings.isHindi()) "PDF कंप्रेस करें" else "Compress PDF",
                subtitle = if (Strings.isHindi()) "फ़ाइल साइज़ घटाएं" else "Reduce file size",
                icon = Icons.Default.Compress,
                color = IosTeal,
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_COMPRESSOR, doc) }
            ),
            PdfManagerTool(
                id = "rotate",
                title = if (Strings.isHindi()) "पृष्ठ घुमाएं" else "Rotate Pages",
                subtitle = if (Strings.isHindi()) "90° या 180° घुमाएं" else "Rotate 90° or 180°",
                icon = Icons.Default.RotateRight,
                color = Color(0xFF673AB7),
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_ORGANIZER, doc) }
            ),
            PdfManagerTool(
                id = "reorder",
                title = if (Strings.isHindi()) "पृष्ठ पुनर्व्यवस्थित" else "Reorder Pages",
                subtitle = if (Strings.isHindi()) "क्रम बदलें" else "Rearrange page order",
                icon = Icons.Default.LowPriority,
                color = Color(0xFF009688),
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_ORGANIZER, doc) }
            ),
            PdfManagerTool(
                id = "delete_pages",
                title = if (Strings.isHindi()) "पृष्ठ हटाएं" else "Delete Pages",
                subtitle = if (Strings.isHindi()) "अवांछित पृष्ठ हटाएं" else "Remove specific pages",
                icon = Icons.Default.DeleteSweep,
                color = IosRed,
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_ORGANIZER, doc) }
            ),
            PdfManagerTool(
                id = "extract",
                title = if (Strings.isHindi()) "पृष्ठ निकालें" else "Extract Pages",
                subtitle = if (Strings.isHindi()) "चुने हुए पृष्ठ सहेजें" else "Extract selected range",
                icon = Icons.Default.ContentCut,
                color = Color(0xFFE91E63),
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_SPLITTER, doc) }
            ),
            PdfManagerTool(
                id = "watermark",
                title = if (Strings.isHindi()) "वॉटरमार्क जोड़ें" else "Add Watermark",
                subtitle = if (Strings.isHindi()) "सुरक्षा टेक्स्ट लगाएं" else "Confidential mark",
                icon = Icons.Default.BrandingWatermark,
                color = Color(0xFF3F51B5),
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.WATERMARK, doc) }
            ),
            PdfManagerTool(
                id = "page_numbers",
                title = if (Strings.isHindi()) "पृष्ठ संख्याएं" else "Add Page Numbers",
                subtitle = if (Strings.isHindi()) "पेज नंबर जोड़ें" else "Page 1 of N",
                icon = Icons.Default.FormatListNumbered,
                color = Color(0xFF00ACC1),
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.WATERMARK, doc) }
            ),
            PdfManagerTool(
                id = "signature",
                title = if (Strings.isHindi()) "हस्ताक्षर जोड़ें" else "Add Signature",
                subtitle = if (Strings.isHindi()) "डिजिटल साइन करें" else "Sign PDF pages",
                icon = Icons.Default.Draw,
                color = IosGreen,
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.SIGNATURE, doc) }
            ),
            PdfManagerTool(
                id = "to_images",
                title = if (Strings.isHindi()) "PDF से फ़ोटो" else "PDF to Images",
                subtitle = if (Strings.isHindi()) "JPEG/PNG में बदलें" else "Export pages as photos",
                icon = Icons.Default.Image,
                color = Color(0xFFFF8F00),
                onClick = { doc -> viewModel.navigateToSubScreen(AppSubScreen.PDF_CONVERTER, doc) }
            ),
            PdfManagerTool(
                id = "print",
                title = if (Strings.isHindi()) "प्रिंट करें" else "Print PDF",
                subtitle = if (Strings.isHindi()) "सिस्टम प्रिंटर" else "System print manager",
                icon = Icons.Default.Print,
                color = Color(0xFF455A64),
                onClick = { doc -> viewModel.printDocument(doc) }
            ),
            PdfManagerTool(
                id = "share",
                title = if (Strings.isHindi()) "शेयर करें" else "Share PDF",
                subtitle = if (Strings.isHindi()) "ईमेल या ऐप्स में" else "Send via WhatsApp/Mail",
                icon = Icons.Default.Share,
                color = IosBlue,
                onClick = { doc ->
                    val uri = viewModel.getShareUri(doc)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                }
            ),
            PdfManagerTool(
                id = "save_copy",
                title = if (Strings.isHindi()) "प्रति सहेजें" else "Save a Copy",
                subtitle = if (Strings.isHindi()) "डिवाइस में निर्यात" else "Save copy to device",
                icon = Icons.Default.SaveAlt,
                color = Color(0xFF2E7D32),
                onClick = { doc ->
                    docToExport = doc
                    exportPdfLauncher.launch(doc.name)
                }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (Strings.isHindi()) "PDF प्रबंधक" else "PDF Manager",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeDoc != null) {
                        TextButton(onClick = { safPdfPicker.launch(arrayOf("application/pdf")) }) {
                            Text(if (Strings.isHindi()) "बदलें" else "Change", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // CASE 1: NO PDF SELECTED (IMPORT FIRST)
            // ==========================================
            if (activeDoc == null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(IosBlue.copy(alpha = 0.15f), IosIndigo.copy(alpha = 0.2f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF Manager",
                                    tint = IosBlue,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = if (Strings.isHindi()) "PDF चुनें या आयात करें" else "Import PDF to Begin",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (Strings.isHindi())
                                    "PDF संपादन, कंप्रेस, मर्ज, विभाजन या हस्ताक्षर के लिए अपने डिवाइस से एक PDF चुनें।"
                                else
                                    "Select or import a PDF from your device storage to access 16 professional document tools.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Primary Action: Import PDF
                            Button(
                                onClick = { safPdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_import_pdf"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (Strings.isHindi()) "PDF आयात करें" else "Import PDF",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Secondary Action: Select from Device
                            OutlinedButton(
                                onClick = { safPdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_select_device_pdf"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (Strings.isHindi()) "डिवाइस से चुनें (Downloads/Storage)" else "Select PDF from Device",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Recent PDFs Section (ONLY if genuine previously opened PDFs exist)
                if (recentPdfs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = if (Strings.isHindi()) "हाल के PDF दस्तावेज़" else "Previously Opened PDFs"
                        )
                    }

                    items(recentPdfs.take(5), key = { it.id }) { doc ->
                        DocumentListItem(
                            doc = doc,
                            onClick = {
                                viewModel.setActiveDoc(doc)
                                viewModel.showSnackbar(if (Strings.isHindi()) "${doc.name} चयनित किया गया" else "Selected ${doc.name}")
                            },
                            onMenuClick = {
                                viewModel.setActiveDoc(doc)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) }
                        )
                    }
                }
            }

            // ==========================================
            // CASE 2: PDF SELECTED SCREEN & TOOLS
            // ==========================================
            else {
                val doc = activeDoc!!

                // STEP 3: SELECTED PDF HEADER CARD
                item {
                    val file = remember(doc.filePath) { File(doc.filePath) }
                    var thumbnailBmp by remember(doc.filePath) { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(doc.filePath) {
                        thumbnailBmp = PdfEngine.renderPageToBitmap(file, 0, 360)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        shadowElevation = 3.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // PDF Live Page 1 Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF2F2F7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbnailBmp != null) {
                                        Image(
                                            bitmap = thumbnailBmp!!.asImageBitmap(),
                                            contentDescription = "PDF Page 1",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = IosRed,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = IosBlue.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${doc.pageCount} ${if (doc.pageCount == 1) "Page" else "Pages"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IosBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${doc.sizeBytes / 1024} KB • ${doc.storageLocation}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(doc.modifiedAt)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons: Change PDF & Clear Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { safPdfPicker.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (Strings.isHindi()) "दूसरा PDF चुनें" else "Change PDF", fontSize = 13.sp)
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.clearActiveDoc()
                                        viewModel.showSnackbar(if (Strings.isHindi()) "चयन साफ़ किया गया" else "Selection cleared")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = IosRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (Strings.isHindi()) "चयन हटाएं" else "Clear Selection", color = IosRed, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // STEP 4: PDF MANAGER TOOL DASHBOARD (16 Tools)
                item {
                    Text(
                        text = if (Strings.isHindi()) "उपलब्ध PDF टूल्स" else "PDF Tools Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (Strings.isHindi()) "चुने हुए दस्तावेज़ पर सीधे लागू करें" else "All actions work directly on the active document",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2-column grid of all 16 tools
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in toolsList.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val tool1 = toolsList[i]
                                ToolDashboardCard(
                                    tool = tool1,
                                    onClick = { tool1.onClick(doc) },
                                    modifier = Modifier.weight(1f)
                                )

                                if (i + 1 < toolsList.size) {
                                    val tool2 = toolsList[i + 1]
                                    ToolDashboardCard(
                                        tool = tool2,
                                        onClick = { tool2.onClick(doc) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolDashboardCard(
    tool: PdfManagerTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("tool_${tool.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
