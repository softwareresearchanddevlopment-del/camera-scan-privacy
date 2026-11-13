@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.util.AppLanguage
import com.example.ui.util.Strings
import com.example.ui.viewmodel.*
import java.io.File
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val showCameraSheet by viewModel.showCameraActionSheet.collectAsState()

    var selectedDocForMenu by remember { mutableStateOf<DocItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<DocItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<DocItem?>(null) }
    var showDocInfoDialog by remember { mutableStateOf<DocItem?>(null) }

    // Universal File Importer launcher (SAF)
    val safFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSafDocument(uri)
        }
    }

    // SAF Folder Tree launcher
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val name = treeUri.lastPathSegment ?: "Device Folder"
            viewModel.addGrantedFolder(treeUri, name)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(IosBlue, IosIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Camera Scan",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All Doc, PDF Tools",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosBlue
                            )
                        }
                    }

                    // Language Toggle & Import Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val next = if (language == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
                                    viewModel.setLanguage(next)
                                }
                                .testTag("quick_lang_toggle"),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (language == AppLanguage.ENGLISH) "हिंदी" else "English",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                safFilePicker.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Import",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Search Bar & Import Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { safFilePicker.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (Strings.isHindi()) "आयात" else "Import",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Hero Card: Multi-Page Scanner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { viewModel.navigateToSubScreen(AppSubScreen.SCANNER) }
                        .testTag("hero_scan_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(IosBlue.copy(alpha = 0.12f), IosIndigo.copy(alpha = 0.08f))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(IosBlue, Color(0xFF0055D4)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Strings.scanDocument(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Strings.scanDocumentSub(),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { viewModel.navigateToSubScreen(AppSubScreen.SCANNER) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (Strings.isHindi()) "शुरू करें" else "Scan")
                            }
                        }
                    }
                }
            }

            // Quick Studio Creation (2x2 Grid)
            item {
                Text(
                    text = Strings.quickActions(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = Strings.imagesToPdf(),
                            subtitle = Strings.imagesToPdfSub(),
                            icon = Icons.Default.Collections,
                            accentColor = IosGreen,
                            onClick = { viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PDF) },
                            modifier = Modifier.weight(1f),
                            testTag = "action_images_to_pdf"
                        )
                        QuickActionCard(
                            title = "Image to PPT",
                            subtitle = "Editable PowerPoint",
                            icon = Icons.Default.Slideshow,
                            accentColor = Color(0xFFD24726),
                            onClick = { viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PPT) },
                            modifier = Modifier.weight(1f),
                            testTag = "action_image_to_ppt"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = "Canvas Editor",
                            subtitle = "Word & Pages style",
                            icon = Icons.Default.EditNote,
                            accentColor = IosIndigo,
                            onClick = {
                                viewModel.setEditorDocTitle("New Document")
                                viewModel.navigateToSubScreen(AppSubScreen.CANVAS_EDITOR)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "action_canvas_editor"
                        )
                        QuickActionCard(
                            title = Strings.mergePdf(),
                            subtitle = Strings.mergePdfSub(),
                            icon = Icons.Default.CallMerge,
                            accentColor = IosOrange,
                            onClick = { viewModel.navigateToSubScreen(AppSubScreen.PDF_MERGER) },
                            modifier = Modifier.weight(1f),
                            testTag = "action_merge_pdf"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = if (Strings.isHindi()) "PDF प्रबंधक" else "PDF Manager",
                            subtitle = if (Strings.isHindi()) "इम्पोर्ट फर्स्ट व 16 टूल्स" else "Import First & 16 Tools",
                            icon = Icons.Default.FolderShared,
                            accentColor = IosBlue,
                            onClick = { viewModel.navigateToSubScreen(AppSubScreen.PDF_MANAGER) },
                            modifier = Modifier.weight(1f),
                            testTag = "action_pdf_manager"
                        )
                        QuickActionCard(
                            title = Strings.scanDocument(),
                            subtitle = if (Strings.isHindi()) "क्रॉप व प्रीव्यू" else "Crop Before & After",
                            icon = Icons.Default.CameraAlt,
                            accentColor = Color(0xFF00C7BE),
                            onClick = { viewModel.navigateToSubScreen(AppSubScreen.SCANNER) },
                            modifier = Modifier.weight(1f),
                            testTag = "action_camera_scanner"
                        )
                    }
                }
            }

            // Device Folders Access Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (Strings.isHindi()) "डिवाइस फ़ोल्डर एक्सेस" else "Device Folders Access",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (Strings.isHindi()) "+ फ़ोल्डर जोड़ें" else "+ Add Folder", fontSize = 12.sp)
                    }
                }
            }

            // Recent Real Documents Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.recentDocuments(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { viewModel.selectTab(AppTab.DOCUMENTS) }) {
                        Text(
                            text = if (Strings.isHindi()) "सभी देखें" else "View All",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Recent Documents List (Actual Device Files)
            val filteredRecent = if (searchQuery.isNotBlank()) {
                recentDocs.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                recentDocs
            }

            if (filteredRecent.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.FolderOpen,
                        title = Strings.noRecentDocs(),
                        subtitle = "Import files from your phone or scan a new document",
                        buttonText = "Import Document",
                        onButtonClick = { safFilePicker.launch(arrayOf("*/*")) }
                    )
                }
            } else {
                items(filteredRecent, key = { it.id }) { doc ->
                    DocumentListItem(
                        doc = doc,
                        onClick = {
                            if (doc.category in listOf("word", "ppt", "excel", "office")) {
                                viewModel.navigateToSubScreen(AppSubScreen.OFFICE_VIEWER, doc)
                            } else {
                                viewModel.navigateToSubScreen(AppSubScreen.PDF_READER, doc)
                            }
                        },
                        onMenuClick = { selectedDocForMenu = doc },
                        onToggleFavorite = { viewModel.toggleFavorite(doc) }
                    )
                }
            }
        }

        // Floating Universal Camera Button
        FloatingActionButton(
            onClick = { viewModel.setCameraActionSheetVisible(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
                .size(60.dp)
                .testTag("floating_camera_btn"),
            containerColor = IosBlue,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Camera Studio", modifier = Modifier.size(28.dp))
        }
    }

    // Camera Action BottomSheet
    if (showCameraSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setCameraActionSheetVisible(false) },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Camera Studio Creation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select what to create from the camera:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                DocMenuOptionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "1. Scan Document (Multi-Page)",
                    onClick = {
                        viewModel.setCameraActionSheetVisible(false)
                        viewModel.navigateToSubScreen(AppSubScreen.SCANNER)
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "2. Create PDF from Camera",
                    onClick = {
                        viewModel.setCameraActionSheetVisible(false)
                        viewModel.navigateToSubScreen(AppSubScreen.SCANNER)
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Slideshow,
                    title = "3. Create PowerPoint from Camera Photos",
                    onClick = {
                        viewModel.setCameraActionSheetVisible(false)
                        viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PPT)
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.EditNote,
                    title = "4. Insert Photo into Document Editor",
                    onClick = {
                        viewModel.setCameraActionSheetVisible(false)
                        viewModel.navigateToSubScreen(AppSubScreen.CANVAS_EDITOR)
                    }
                )
            }
        }
    }

    // Options Bottom Sheet
    selectedDocForMenu?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { selectedDocForMenu = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(doc.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                DocMenuOptionItem(
                    icon = Icons.Default.Share,
                    title = Strings.share(),
                    onClick = {
                        selectedDocForMenu = null
                        val uri = viewModel.getShareUri(doc)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = doc.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Print,
                    title = Strings.print(),
                    onClick = {
                        selectedDocForMenu = null
                        viewModel.printDocument(doc)
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Edit,
                    title = Strings.rename(),
                    onClick = {
                        val target = selectedDocForMenu
                        selectedDocForMenu = null
                        showRenameDialog = target
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Info,
                    title = Strings.docInfo(),
                    onClick = {
                        val target = selectedDocForMenu
                        selectedDocForMenu = null
                        showDocInfoDialog = target
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Delete,
                    title = Strings.delete(),
                    color = IosRed,
                    onClick = {
                        val target = selectedDocForMenu
                        selectedDocForMenu = null
                        showDeleteConfirmDialog = target
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Dialog
    showRenameDialog?.let { doc ->
        var newName by remember { mutableStateOf(doc.name.substringBeforeLast(".")) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(Strings.rename()) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) viewModel.renameDocument(doc, newName.trim())
                    showRenameDialog = null
                }) { Text(Strings.save()) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text(Strings.cancel()) }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(Strings.delete()) },
            text = { Text(Strings.deleteConfirm(doc.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosRed)
                ) { Text(Strings.delete()) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text(Strings.cancel()) }
            }
        )
    }

    // Document Info Dialog
    showDocInfoDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showDocInfoDialog = null },
            title = { Text(Strings.docInfo()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${doc.name}", fontWeight = FontWeight.SemiBold)
                    Text("Location: ${doc.storageLocation}")
                    Text("Pages: ${doc.pageCount}")
                    Text("Size: ${doc.sizeBytes / 1024} KB")
                    Text("Path / URI: ${doc.filePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocInfoDialog = null }) { Text(Strings.close()) }
            }
        )
    }
}

@Composable
fun FilesScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val allDocs by viewModel.allDocuments.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var sortBy by remember { mutableStateOf("date") }
    var selectedDocForMenu by remember { mutableStateOf<DocItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<DocItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<DocItem?>(null) }
    var showDocInfoDialog by remember { mutableStateOf<DocItem?>(null) }
    val context = LocalContext.current

    // SAF Folder Tree launcher
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val name = treeUri.lastPathSegment ?: "Device Folder"
            viewModel.addGrantedFolder(treeUri, name)
        }
    }

    val safFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSafDocument(uri)
        }
    }

    val categories = listOf(
        "All PDFs", "Downloads", "Documents", "Recent PDFs", "Favorites",
        "Created PDFs", "Scanned PDFs", "Word", "PowerPoint", "Excel", "Images", "Text"
    )

    val filteredList = remember(allDocs, activeFilter, searchQuery, sortBy) {
        var list = allDocs
        when (activeFilter) {
            "All PDFs" -> list = list.filter { it.name.endsWith(".pdf", ignoreCase = true) || it.mimeType == "application/pdf" }
            "Downloads" -> list = list.filter { it.storageLocation == "Downloads" }
            "Documents" -> list = list.filter { it.storageLocation == "Documents" }
            "Favorites" -> list = list.filter { it.isFavorite }
            "Created PDFs" -> list = list.filter { it.category == "created" }
            "Scanned PDFs" -> list = list.filter { it.category == "scan" }
            "Word" -> list = list.filter { it.name.endsWith(".docx", ignoreCase = true) || it.name.endsWith(".doc", ignoreCase = true) || it.category == "word" }
            "PowerPoint" -> list = list.filter { it.name.endsWith(".pptx", ignoreCase = true) || it.name.endsWith(".ppt", ignoreCase = true) || it.category == "ppt" }
            "Excel" -> list = list.filter { it.name.endsWith(".xlsx", ignoreCase = true) || it.name.endsWith(".xls", ignoreCase = true) || it.category == "excel" }
            "Images" -> list = list.filter { it.category == "image" || it.name.endsWith(".jpg", ignoreCase = true) || it.name.endsWith(".png", ignoreCase = true) }
            "Text" -> list = list.filter { it.category == "text" || it.name.endsWith(".txt", ignoreCase = true) || it.name.endsWith(".csv", ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        when (sortBy) {
            "name" -> list.sortedBy { it.name.lowercase() }
            "size" -> list.sortedByDescending { it.sizeBytes }
            else -> list.sortedByDescending { it.modifiedAt }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (Strings.isHindi()) "डिवाइस दस्तावेज़ लाइब्रेरी" else "Device Documents",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = { viewModel.refreshDeviceDocuments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            IosSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )
        }

        // Real Categories Filter Horizontal Strip
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = activeFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setActiveFilter(cat) },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Sort By & File Count Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredList.size} ${if (Strings.isHindi()) "वास्तविक फ़ाइलें" else "real files found"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${Strings.sortBy()}: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("date" to Strings.sortDate(), "name" to Strings.sortName(), "size" to Strings.sortSize()).forEach { (key, label) ->
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (sortBy == key) FontWeight.Bold else FontWeight.Normal,
                            color = if (sortBy == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { sortBy = key }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Document List
        if (filteredList.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.FolderOpen,
                    title = if (Strings.isHindi()) "कोई फ़ाइल नहीं मिली" else "No files in $activeFilter",
                    subtitle = "Grant folder access to Downloads / Documents or import files",
                    buttonText = "+ Grant Folder Access",
                    onButtonClick = { folderPicker.launch(null) }
                )
            }
        } else {
            items(filteredList, key = { it.id }) { doc ->
                DocumentListItem(
                    doc = doc,
                    onClick = {
                        if (doc.category in listOf("word", "ppt", "excel", "office")) {
                            viewModel.navigateToSubScreen(AppSubScreen.OFFICE_VIEWER, doc)
                        } else {
                            viewModel.navigateToSubScreen(AppSubScreen.PDF_READER, doc)
                        }
                    },
                    onMenuClick = { selectedDocForMenu = doc },
                    onToggleFavorite = { viewModel.toggleFavorite(doc) }
                )
            }
        }
    }

    // Modal Bottom Sheet & Dialogs for Files
    selectedDocForMenu?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { selectedDocForMenu = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(doc.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                DocMenuOptionItem(
                    icon = Icons.Default.Share,
                    title = Strings.share(),
                    onClick = {
                        selectedDocForMenu = null
                        val uri = viewModel.getShareUri(doc)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = doc.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Print,
                    title = Strings.print(),
                    onClick = {
                        selectedDocForMenu = null
                        viewModel.printDocument(doc)
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Edit,
                    title = Strings.rename(),
                    onClick = {
                        val target = selectedDocForMenu
                        selectedDocForMenu = null
                        showRenameDialog = target
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Info,
                    title = Strings.docInfo(),
                    onClick = {
                        val target = selectedDocForMenu
                        selectedDocForMenu = null
                        showDocInfoDialog = target
                    }
                )
                DocMenuOptionItem(
                    icon = Icons.Default.Delete,
                    title = Strings.delete(),
                    color = IosRed,
                    onClick = {
                        val target = selectedDocForMenu
                        selectedDocForMenu = null
                        showDeleteConfirmDialog = target
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    showRenameDialog?.let { doc ->
        var newName by remember { mutableStateOf(doc.name.substringBeforeLast(".")) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(Strings.rename()) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) viewModel.renameDocument(doc, newName.trim())
                    showRenameDialog = null
                }) { Text(Strings.save()) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text(Strings.cancel()) }
            }
        )
    }

    showDeleteConfirmDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(Strings.delete()) },
            text = { Text(Strings.deleteConfirm(doc.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosRed)
                ) { Text(Strings.delete()) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text(Strings.cancel()) }
            }
        )
    }

    showDocInfoDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showDocInfoDialog = null },
            title = { Text(Strings.docInfo()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${doc.name}", fontWeight = FontWeight.SemiBold)
                    Text("Location: ${doc.storageLocation}")
                    Text("Pages: ${doc.pageCount}")
                    Text("Size: ${doc.sizeBytes / 1024} KB")
                    Text("Path: ${doc.filePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocInfoDialog = null }) { Text(Strings.close()) }
            }
        )
    }
}

@Composable
fun ToolsScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Strings.tabTools(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (Strings.isHindi()) "शक्तिशाली PDF और दस्तावेज़ उपकरण" else "Professional PDF & Document Tools",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Scan & Create
        item {
            SectionHeader(if (Strings.isHindi()) "स्कैन और निर्माण" else "Scan & Create")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolItemRow(
                    title = Strings.scanDocument(),
                    subtitle = Strings.scanDocumentSub(),
                    icon = Icons.Default.CameraAlt,
                    iconColor = IosBlue,
                    onClick = {
                        viewModel.clearScannedPages()
                        viewModel.navigateToSubScreen(AppSubScreen.SCANNER)
                    }
                )
                ToolItemRow(
                    title = Strings.imagesToPdf(),
                    subtitle = Strings.imagesToPdfSub(),
                    icon = Icons.Default.PhotoLibrary,
                    iconColor = IosGreen,
                    onClick = {
                        viewModel.clearImageUris()
                        viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PDF)
                    }
                )
                ToolItemRow(
                    title = "Image to PowerPoint (PPTX)",
                    subtitle = "Generate real 16:9 or 4:3 slide presentations",
                    icon = Icons.Default.Slideshow,
                    iconColor = Color(0xFFD24726),
                    onClick = {
                        viewModel.clearPptSlides()
                        viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PPT)
                    }
                )
                ToolItemRow(
                    title = "Canvas Document Editor",
                    subtitle = "Word & Pages style rich document editor",
                    icon = Icons.Default.EditNote,
                    iconColor = IosIndigo,
                    onClick = {
                        viewModel.resetCanvasEditor()
                        viewModel.navigateToSubScreen(AppSubScreen.CANVAS_EDITOR)
                    }
                )
            }
        }

        // Section: PDF Management
        item {
            SectionHeader(if (Strings.isHindi()) "PDF प्रबंधन" else "PDF Management")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolItemRow(
                    title = if (Strings.isHindi()) "PDF प्रबंधक (इम्पोर्ट फर्स्ट)" else "PDF Manager (Import First)",
                    subtitle = if (Strings.isHindi()) "किसी भी PDF को चुनें और 16 टूल्स का उपयोग करें" else "Select any PDF to access all 16 tools",
                    icon = Icons.Default.FolderShared,
                    iconColor = IosBlue,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_MANAGER)
                    },
                    testTag = "tool_pdf_manager"
                )
                ToolItemRow(
                    title = Strings.mergePdf(),
                    subtitle = Strings.mergePdfSub(),
                    icon = Icons.Default.CallMerge,
                    iconColor = IosOrange,
                    onClick = {
                        viewModel.clearSelectedMergeDocs()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_MERGER)
                    }
                )
                ToolItemRow(
                    title = Strings.toolSplitter(),
                    subtitle = Strings.toolSplitterSub(),
                    icon = Icons.Default.CallSplit,
                    iconColor = IosRed,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_SPLITTER)
                    }
                )
                ToolItemRow(
                    title = Strings.toolCompressor(),
                    subtitle = Strings.toolCompressorSub(),
                    icon = Icons.Default.Compress,
                    iconColor = IosTeal,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_COMPRESSOR)
                    }
                )
                ToolItemRow(
                    title = Strings.toolOrganizer(),
                    subtitle = Strings.toolOrganizerSub(),
                    icon = Icons.Default.Layers,
                    iconColor = IosBlue,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_ORGANIZER)
                    }
                )
            }
        }

        // Section: View & Edit
        item {
            SectionHeader(if (Strings.isHindi()) "व्यू और एडिट" else "View & Edit")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolItemRow(
                    title = Strings.toolReader(),
                    subtitle = Strings.toolReaderSub(),
                    icon = Icons.Default.MenuBook,
                    iconColor = IosBlue,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_READER)
                    }
                )
                ToolItemRow(
                    title = Strings.toolEditor(),
                    subtitle = Strings.toolEditorSub(),
                    icon = Icons.Default.Draw,
                    iconColor = IosIndigo,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_EDITOR)
                    }
                )
                ToolItemRow(
                    title = Strings.toolSignature(),
                    subtitle = Strings.toolSignatureSub(),
                    icon = Icons.Default.Gesture,
                    iconColor = IosGreen,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.SIGNATURE)
                    }
                )
                ToolItemRow(
                    title = Strings.toolWatermark(),
                    subtitle = Strings.toolWatermarkSub(),
                    icon = Icons.Default.BrandingWatermark,
                    iconColor = IosOrange,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.WATERMARK)
                    }
                )
            }
        }

        // Section: Office & Conversion
        item {
            SectionHeader(if (Strings.isHindi()) "ऑफिस और रूपांतरण" else "Office & Conversion")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolItemRow(
                    title = Strings.toolOffice(),
                    subtitle = Strings.toolOfficeSub(),
                    icon = Icons.Default.FolderShared,
                    iconColor = IosIndigo,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.OFFICE_VIEWER)
                    }
                )
                ToolItemRow(
                    title = Strings.toolConverter(),
                    subtitle = Strings.toolConverterSub(),
                    icon = Icons.Default.Transform,
                    iconColor = IosRed,
                    onClick = {
                        viewModel.clearActiveDoc()
                        viewModel.navigateToSubScreen(AppSubScreen.PDF_CONVERTER)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val storageBytes by viewModel.storageBytes.collectAsState()
    val grantedFolders by viewModel.grantedFolders.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val storageFormatted = remember(storageBytes) {
        String.format(Locale.US, "%.1f MB", storageBytes / (1024f * 1024f))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = Strings.settingsTitle(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Appearance
        item {
            SectionHeader(Strings.appearance())
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(ThemeMode.LIGHT, Strings.themeLight(), Icons.Default.LightMode),
                            Triple(ThemeMode.DARK, Strings.themeDark(), Icons.Default.DarkMode),
                            Triple(ThemeMode.SYSTEM, Strings.themeSystem(), Icons.Default.SettingsBrightness)
                        ).forEach { (mode, label, icon) ->
                            val isSelected = themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setThemeMode(mode) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language
        item {
            SectionHeader(Strings.language())
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Pair(AppLanguage.ENGLISH, "English"),
                            Pair(AppLanguage.HINDI, "हिंदी (Hindi)")
                        ).forEach { (lang, label) ->
                            val isSelected = language == lang
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setLanguage(lang) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Granted Device Folders
        if (grantedFolders.isNotEmpty()) {
            item {
                SectionHeader("Granted Device Folders (${grantedFolders.size})")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        grantedFolders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = IosBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder.displayName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("Active", fontSize = 11.sp, color = IosGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Storage & Privacy
        item {
            SectionHeader(Strings.storagePrivacy())
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Storage",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(Strings.storageUsed(), fontWeight = FontWeight.SemiBold)
                        }
                        Text(storageFormatted, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.clearTempCache() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.clearCache(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy",
                            tint = IosGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.privacyNote(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // AdMob Configuration info
        item {
            SectionHeader(if (Strings.isHindi()) "AdMob विज्ञापन स्थिति" else "Google AdMob Status")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdsClick, contentDescription = null, tint = IosBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (Strings.isHindi()) "नॉन-इंट्रूसिव टॉप बैनर सक्रिय" else "Non-Intrusive Top Banner Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (Strings.isHindi()) "स्क्रीन के टॉप पर, स्कैनिंग/एडिटिंग में कोई बाधा नहीं" else "Placed at the top of main tabs; never interrupts scanning or editing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Ad Unit ID: ${com.example.ui.components.AdMobConfig.activeBannerAdUnitId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (Strings.isHindi()) "लाइव AdMob IDs सक्रिय हैं।" else "Live AdMob IDs are active.",
                        fontSize = 11.sp,
                        color = IosGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Privacy Policy
        item {
            SectionHeader(Strings.privacyPolicy())
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showPrivacyDialog = true }
                    .testTag("privacy_policy_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IosGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = Strings.privacyPolicy(),
                                tint = IosGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.privacyPolicy(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(Strings.privacyPolicySub(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = Strings.privacyPolicy(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // About
        item {
            SectionHeader(Strings.aboutApp())
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showAboutDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Camera Scan: All Doc, PDF Tools", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(Strings.appVersion(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showPrivacyDialog) {
        val policyText = remember(language) {
            if (Strings.isHindi()) {
                """
                प्रभावी तिथि: 13 नवंबर 2026
                ऐप का नाम: Camera Scan: All Doc, PDF Tools
                पैकेज ID: com.aistudio.docuflow.pdfstudio.kqmr
                संपर्क ईमेल: alphaarastu@gmail.com

                1. 100% ऑन-डिवाइस प्रोसेसिंग:
                दस्तावेज़ स्कैनिंग, PDF निर्माण, संपादन, मर्ज, स्प्लिट, कंप्रेस, वॉटरमार्क, हस्ताक्षर और ऑफिस व्यूअर सभी कार्य पूरी तरह से आपके फ़ोन पर ही होते हैं। हम आपके दस्तावेज़ किसी सर्वर पर अपलोड नहीं करते।

                2. अनुमतियाँ (Permissions):
                • कैमरा (CAMERA): केवल दस्तावेज़ स्कैन करने के लिए उपयोग किया जाता है।
                • फ़ोटो पिकर / स्टोरेज (SAF): केवल वही फ़ाइलें या इमेज पढ़ी जाती हैं जिन्हें आप स्वयं चुनते हैं।
                • इंटरनेट (INTERNET): केवल Google AdMob बैनर विज्ञापन लोड करने के लिए उपयोग होता है।

                3. विज्ञापन (Google AdMob):
                ऐप में Google AdMob द्वारा नॉन-इंट्रूसिव बैनर विज्ञापन दिखाए जाते हैं। AdMob विज्ञापन दिखाने के लिए डिवाइस की Advertising ID (AD_ID) और सामान्य डायग्नोस्टिक जानकारी का उपयोग कर सकता है (https://policies.google.com/privacy)।

                4. बच्चों की गोपनीयता:
                यह ऐप 13 वर्ष से कम आयु के बच्चों के लिए नहीं बनाया गया है और हम जानबूझकर कोई व्यक्तिगत जानकारी एकत्र नहीं करते।

                5. संपर्क करें:
                किसी भी प्रश्न के लिए संपर्क करें: alphaarastu@gmail.com
                """.trimIndent()
            } else {
                """
                Effective Date: November 13, 2026
                App Name: Camera Scan: All Doc, PDF Tools
                Package ID: com.aistudio.docuflow.pdfstudio.kqmr
                Contact Email: alphaarastu@gmail.com

                1. 100% On-Device Processing:
                All document scanning, PDF creation, editing, merging, splitting, compression, watermarking, digital signatures, and Office viewing happen locally on your device. We never upload your documents to external servers.

                2. Permissions Used:
                • Camera (CAMERA): Used strictly for scanning physical documents inside the app.
                • Photo Picker & Storage Access (SAF): Only accesses specific photos, PDFs, or folders you explicitly choose.
                • Internet & Network State: Used solely to display Google AdMob banner ads.

                3. Advertising (Google AdMob):
                This app uses Google AdMob to display non-intrusive top banner ads. AdMob may collect limited device data such as Advertising ID (AD_ID) and ad diagnostics per Google's Privacy Policy (https://policies.google.com/privacy).

                4. Children's Privacy:
                This utility app is not directed at children under 13 and does not knowingly collect personal information.

                5. Contact Us:
                For privacy questions, contact: alphaarastu@gmail.com
                """.trimIndent()
            }
        }

        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(Strings.privacyPolicy(), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = policyText, fontSize = 13.sp, lineHeight = 19.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Privacy Policy", policyText))
                        viewModel.showSnackbar(Strings.copiedToast())
                    }
                ) {
                    Text(Strings.copy())
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(Strings.close())
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Camera Scan: All Doc, PDF Tools") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: 2.0 (Pro Studio)")
                    Text("Real device file library, multi-page scanner, Word & Pages style document editor, image-to-powerpoint (.pptx), PDF reader & compressor.")
                    Text("100% on-device processing keeping your documents private.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(Strings.close())
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun DocMenuOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
