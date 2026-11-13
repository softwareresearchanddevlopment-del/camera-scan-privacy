@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.ui.draw.shadow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.office.OfficeParsedResult
import com.example.data.pdf.PdfEngine
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ToolDocumentSelector
import com.example.ui.components.ActiveDocumentHeader
import com.example.ui.theme.*
import com.example.ui.util.Strings
import com.example.ui.viewmodel.AppSubScreen
import com.example.ui.viewmodel.DocuFlowViewModel
import java.io.File
import java.util.Locale

// --- PDF EDITOR ---

data class DrawPoint(val x: Float, val y: Float)
data class DrawPath(val points: List<DrawPoint>, val color: Color, val strokeWidth: Float, val isHighlight: Boolean = false)

@Composable
fun PdfEditorScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.toolEditor(),
            toolSubtitle = if (Strings.isHindi()) "संपादित और एनोटेट करने के लिए एक PDF चुनें" else "Select a PDF document to annotate and draw on",
            icon = Icons.Default.Draw,
            iconColor = IosIndigo,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    val file = remember(doc.filePath) { File(doc.filePath) }
    val totalPages = remember(file) { PdfEngine.getPageCount(file).coerceAtLeast(1) }

    var selectedPageIndex by remember { mutableStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentColor by remember { mutableStateOf(Color.Red) }
    var currentStrokeWidth by remember { mutableStateOf(6f) }
    var isHighlightMode by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val paths = remember { mutableStateListOf<DrawPath>() }
    var currentPathPoints by remember { mutableStateOf<List<DrawPoint>>(emptyList()) }

    LaunchedEffect(selectedPageIndex) {
        paths.clear()
        pageBitmap = PdfEngine.renderPageToBitmap(file, selectedPageIndex, 1000)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.toolEditor(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Change File
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    // Undo
                    IconButton(
                        onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex) },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    // Save Copy
                    IconButton(onClick = {
                        val bmp = pageBitmap ?: return@IconButton
                        // Render drawn paths on top of bitmap
                        val annotated = bmp.copy(Bitmap.Config.ARGB_8888, true)
                        val canvas = Canvas(annotated)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }

                        paths.forEach { p ->
                            paint.color = android.graphics.Color.argb(
                                if (p.isHighlight) 100 else 255,
                                (p.color.red * 255).toInt(),
                                (p.color.green * 255).toInt(),
                                (p.color.blue * 255).toInt()
                            )
                            paint.strokeWidth = p.strokeWidth * (annotated.width / 400f)

                            val androidPath = Path()
                            p.points.forEachIndexed { idx, pt ->
                                val realX = pt.x * annotated.width
                                val realY = pt.y * annotated.height
                                if (idx == 0) androidPath.moveTo(realX, realY) else androidPath.lineTo(realX, realY)
                            }
                            canvas.drawPath(androidPath, paint)
                        }

                        val outFile = File(file.parentFile, "${file.nameWithoutExtension}_edited.pdf")
                        coroutineScope.launch {
                            PdfEngine.saveAnnotatedPdf(file, selectedPageIndex, annotated, outFile)
                            viewModel.saveDocument(outFile, "pdf")
                            viewModel.updateStorageInfo()
                            viewModel.showSnackbar(if (Strings.isHindi()) "संपादित प्रति सहेजी गई!" else "Edited copy saved!")
                            viewModel.navigateToSubScreen(AppSubScreen.PDF_READER, doc)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) {
                    // Page picker if multi-page
                    if (totalPages > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(totalPages) { idx ->
                                FilterChip(
                                    selected = selectedPageIndex == idx,
                                    onClick = { selectedPageIndex = idx },
                                    label = { Text("Page ${idx + 1}") }
                                )
                            }
                        }
                    }

                    // Tool row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Pen tool
                            IconButton(
                                onClick = { isHighlightMode = false },
                                modifier = Modifier.background(if (!isHighlightMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Pen", tint = if (!isHighlightMode) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            // Highlighter tool
                            IconButton(
                                onClick = { isHighlightMode = true },
                                modifier = Modifier.background(if (isHighlightMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = "Highlight", tint = if (isHighlightMode) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                        }

                        // Colors
                        val colors = listOf(Color.Red, Color.Blue, Color(0xFF2E7D32), Color(0xFFFFB300), Color.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(if (currentColor == c) 2.dp else 0.dp, Color.White, CircleShape)
                                        .clickable { currentColor = c }
                                )
                            }
                        }

                        // Clear page drawing
                        TextButton(onClick = { paths.clear() }) {
                            Text(Strings.clearSignature(), color = IosRed)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE5E5EA)),
            contentAlignment = Alignment.Center
        ) {
            if (pageBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(pageBitmap!!.width.toFloat() / pageBitmap!!.height.toFloat())
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val normX = offset.x / size.width
                                    val normY = offset.y / size.height
                                    currentPathPoints = listOf(DrawPoint(normX, normY))
                                },
                                onDrag = { change, _ ->
                                    val normX = change.position.x / size.width
                                    val normY = change.position.y / size.height
                                    currentPathPoints = currentPathPoints + DrawPoint(normX, normY)
                                },
                                onDragEnd = {
                                    if (currentPathPoints.isNotEmpty()) {
                                        paths.add(
                                            DrawPath(
                                                points = currentPathPoints,
                                                color = currentColor,
                                                strokeWidth = if (isHighlightMode) 22f else 6f,
                                                isHighlight = isHighlightMode
                                            )
                                        )
                                        currentPathPoints = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { p ->
                            for (i in 0 until p.points.size - 1) {
                                val p1 = p.points[i]
                                val p2 = p.points[i + 1]
                                drawLine(
                                    color = if (p.isHighlight) p.color.copy(alpha = 0.35f) else p.color,
                                    start = Offset(p1.x * size.width, p1.y * size.height),
                                    end = Offset(p2.x * size.width, p2.y * size.height),
                                    strokeWidth = p.strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                        for (i in 0 until currentPathPoints.size - 1) {
                            val p1 = currentPathPoints[i]
                            val p2 = currentPathPoints[i + 1]
                            drawLine(
                                color = if (isHighlightMode) currentColor.copy(alpha = 0.35f) else currentColor,
                                start = Offset(p1.x * size.width, p1.y * size.height),
                                end = Offset(p2.x * size.width, p2.y * size.height),
                                strokeWidth = if (isHighlightMode) 22f else 6f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

// --- PDF PAGE ORGANIZER ---

@Composable
fun PdfOrganizerScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.toolOrganizer(),
            toolSubtitle = if (Strings.isHindi()) "पेजों का क्रम बदलने और व्यवस्थित करने के लिए PDF चुनें" else "Select a PDF to reorder, rotate and manage pages",
            icon = Icons.Default.Layers,
            iconColor = IosBlue,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    val file = remember(doc.filePath) { File(doc.filePath) }
    val pageCount = remember(file) { PdfEngine.getPageCount(file).coerceAtLeast(1) }

    val pageOrder = remember { mutableStateListOf<Int>().apply { addAll(0 until pageCount) } }
    val rotations = remember { mutableStateMapOf<Int, Int>() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.toolOrganizer(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            val outFile = File(file.parentFile, "${file.nameWithoutExtension}_reorganized.pdf")
                            coroutineScope.launch {
                                PdfEngine.reorganizePdf(file, pageOrder.toList(), rotations.toMap(), outFile)
                                val saved = viewModel.saveDocument(outFile, "pdf")
                                viewModel.showSnackbar(if (Strings.isHindi()) "नया PDF तैयार किया गया!" else "Reorganized PDF saved!")
                                viewModel.navigateToSubScreen(AppSubScreen.PDF_READER, saved)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Strings.save())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(pageOrder) { index, originalPageIdx ->
                var bmp by remember(originalPageIdx) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(originalPageIdx) {
                    bmp = PdfEngine.renderPageToBitmap(file, originalPageIdx, 400)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF2F2F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp!!.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Page number badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .background(IosBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls: Move Left, Move Right, Rotate, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val item = pageOrder.removeAt(index)
                                        pageOrder.add(index - 1, item)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = {
                                    val currentRot = rotations[index] ?: 0
                                    rotations[index] = (currentRot + 90) % 360
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    if (pageOrder.size > 1) {
                                        pageOrder.removeAt(index)
                                    } else {
                                        viewModel.showSnackbar("Cannot delete only page")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IosRed, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    if (index < pageOrder.lastIndex) {
                                        val item = pageOrder.removeAt(index)
                                        pageOrder.add(index + 1, item)
                                    }
                                },
                                enabled = index < pageOrder.lastIndex,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PDF MERGER ---

@Composable
fun PdfMergerScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val allDocs by viewModel.allDocuments.collectAsState()
    val pdfDocs = remember(allDocs) { allDocs.filter { it.category == "pdf" || it.category == "scan" } }
    val selectedDocs by viewModel.selectedMergeDocs.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var outputName by remember { mutableStateOf("Merged_${System.currentTimeMillis() % 100000}") }

    val safPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.importAndSelectForMerge(uris)
        }
    }

    val totalPages = remember(selectedDocs) { selectedDocs.sumOf { it.pageCount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.mergePdf(), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedDocs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSelectedMergeDocs() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Selection", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { safPdfPicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.Add, contentDescription = "Pick PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (selectedDocs.size >= 2) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.mergeSelectedPdfs(outputName) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("merge_action_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.CallMerge, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${Strings.mergeFiles()} (${selectedDocs.size} files, $totalPages pages)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("Output Merged PDF Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // External Picker Button
            item {
                OutlinedButton(
                    onClick = { safPdfPicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (Strings.isHindi()) "+ डिवाइस से PDF फ़ाइलें चुनें" else "+ Pick External PDFs (Device Storage)")
                }
            }

            // Order of selected PDFs (Drag / Move Up / Move Down)
            if (selectedDocs.isNotEmpty()) {
                item {
                    Text(
                        text = if (Strings.isHindi()) "मर्ज क्रम (${selectedDocs.size} चुने गए, $totalPages कुल पृष्ठ)" else "Merge Sequence (${selectedDocs.size} selected, $totalPages pages total)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                itemsIndexed(selectedDocs, key = { _, doc -> "selected_${doc.id}" }) { idx, doc ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    doc.name,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${doc.pageCount} pages • ${doc.sizeBytes / 1024} KB",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { if (idx > 0) viewModel.reorderMergeDocs(idx, idx - 1) },
                                    enabled = idx > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { if (idx < selectedDocs.lastIndex) viewModel.reorderMergeDocs(idx, idx + 1) },
                                    enabled = idx < selectedDocs.lastIndex,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeMergeDoc(doc.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = IosRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (Strings.isHindi()) "लाइब्रेरी से PDF जोड़ें" else "Add PDFs from Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (pdfDocs.isEmpty()) {
                item {
                    Text(
                        text = if (Strings.isHindi()) "कोई PDF नहीं मिली। ऊपर बटन से डिवाइस से PDF चुनें।" else "No PDFs found in library. Tap above to pick from device storage.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(pdfDocs, key = { it.id }) { doc ->
                    val isSelected = selectedDocs.any { it.id == doc.id }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.toggleMergeDoc(doc) },
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleMergeDoc(doc) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${doc.pageCount} pages • ${doc.sizeBytes / 1024} KB • ${doc.storageLocation}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PDF SPLITTER ---

@Composable
fun PdfSplitterScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.splitPdf(),
            toolSubtitle = if (Strings.isHindi()) "पेज अलग करने या विभाजित करने के लिए PDF चुनें" else "Select a PDF to split or extract specific pages",
            icon = Icons.Default.CallSplit,
            iconColor = IosRed,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    val file = remember(doc.filePath) { File(doc.filePath) }
    val pageCount = remember(file) { PdfEngine.getPageCount(file).coerceAtLeast(1) }

    val selectedPages = remember { mutableStateListOf<Int>().apply { add(0) } }
    var outputName by remember { mutableStateOf("${file.nameWithoutExtension}_split") }
    val isProcessing by viewModel.isProcessing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.splitPdf(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.splitPdfPages(doc, selectedPages.toList(), outputName) },
                        enabled = !isProcessing && selectedPages.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.CallSplit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${Strings.extractSelected()} (${selectedPages.size} pages)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output PDF Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Select pages to extract:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pageCount) { idx ->
                    val isChecked = selectedPages.contains(idx)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isChecked) selectedPages.remove(idx) else selectedPages.add(idx)
                            },
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Page ${idx + 1}", fontWeight = FontWeight.Bold, color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) selectedPages.remove(idx) else selectedPages.add(idx)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- PDF COMPRESSOR ---

@Composable
fun PdfCompressorScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.compressPdf(),
            toolSubtitle = if (Strings.isHindi()) "फ़ाइल साइज़ कम करने के लिए PDF चुनें" else "Select a PDF to reduce file size with smart compression",
            icon = Icons.Default.Compress,
            iconColor = IosTeal,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    var selectedLevel by remember { mutableStateOf(CompressionLevel.BALANCED) }
    val isProcessing by viewModel.isProcessing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.compressPdf(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // File Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Compress, contentDescription = null, tint = IosTeal)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(doc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${Strings.originalFileSize()}: ${doc.sizeBytes / 1024} KB",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text("Select Compression Preset:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    CompressionLevel.HIGH_QUALITY to ("High Quality" to "Mild compression, best visual clarity (~15-30% reduction)"),
                    CompressionLevel.BALANCED to ("Balanced (Recommended)" to "Optimal balance of size & readability (~40-60% reduction)"),
                    CompressionLevel.SMALL_SIZE to ("Small Size" to "Maximum compression for email & WhatsApp sharing (~65-80% reduction)")
                ).forEach { (level, desc) ->
                    val isSelected = selectedLevel == level
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedLevel = level },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { selectedLevel = level })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(desc.first, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Text(desc.second, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.compressDocument(doc, selectedLevel) },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("compress_action_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Compressing...")
                } else {
                    Icon(Icons.Default.Compress, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.compressPdf(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// --- OFFICE DOCUMENT VIEWER ---

@Composable
fun OfficeViewerScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.toolOffice(),
            toolSubtitle = if (Strings.isHindi()) "Word, Excel, PowerPoint दस्तावेज़ देखने के लिए चुनें" else "Select Word (.docx), Excel (.xlsx) or PowerPoint (.pptx) file",
            icon = Icons.Default.FolderShared,
            iconColor = IosIndigo,
            isOfficeTool = true,
            availableDocs = allDocs,
            onDocumentSelected = {
                viewModel.setActiveDoc(it)
                viewModel.loadOfficeDocument(it)
            },
            onImportUri = { viewModel.importSafOffice(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val parsedResult by viewModel.parsedOfficeResult.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activeDoc?.name ?: Strings.toolOffice(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change File", tint = MaterialTheme.colorScheme.primary)
                    }
                    parsedResult?.let { res ->
                        IconButton(onClick = { viewModel.exportOfficeToPdf(res) }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    activeDoc?.let { doc ->
                        IconButton(onClick = {
                            val uri = viewModel.getShareUri(doc)
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, doc.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(viewIntent)
                            } catch (_: Exception) {
                                viewModel.showSnackbar("No external Office app installed")
                            }
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open External")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isProcessing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (parsedResult == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select an Office file to view")
            }
        } else {
            val res = parsedResult!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Type Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IosIndigo.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${res.type} Document",
                                color = IosIndigo,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = res.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // If Excel / CSV Table
                if (res.tableRows.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                res.tableRows.forEachIndexed { rowIdx, cells ->
                                    Row(
                                        modifier = Modifier
                                            .background(if (rowIdx == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        cells.forEach { cell ->
                                            Text(
                                                text = cell,
                                                fontWeight = if (rowIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                modifier = Modifier
                                                    .width(120.dp)
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }

                // If PowerPoint Slides
                if (res.slides.isNotEmpty()) {
                    itemsIndexed(res.slides) { idx, slide ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Slide ${idx + 1}", fontWeight = FontWeight.Bold, color = IosOrange, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(slide, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }

                // If Word / Text paragraphs
                if (res.paragraphs.isNotEmpty()) {
                    items(res.paragraphs) { paragraph ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = paragraph,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SIGNATURE SCREEN ---

@Composable
fun SignatureScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.toolSignature(),
            toolSubtitle = if (Strings.isHindi()) "डिजिटल हस्ताक्षर करने के लिए PDF चुनें" else "Select a PDF document to sign digitally",
            icon = Icons.Default.Gesture,
            iconColor = IosGreen,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.toolSignature(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { paths.clear() }) {
                        Text(Strings.clearSignature(), color = IosRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Strings.drawSignaturePrompt(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Signature Pad Canvas
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPath = listOf(offset) },
                            onDrag = { change, _ -> currentPath = currentPath + change.position },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths.add(currentPath)
                                    currentPath = emptyList()
                                }
                            }
                        )
                    },
                color = Color.White
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { p ->
                        for (i in 0 until p.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = p[i],
                                end = p[i + 1],
                                strokeWidth = 5f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                    for (i in 0 until currentPath.size - 1) {
                        drawLine(
                            color = Color.Black,
                            start = currentPath[i],
                            end = currentPath[i + 1],
                            strokeWidth = 5f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (paths.isEmpty()) {
                        viewModel.showSnackbar("Please draw signature first")
                        return@Button
                    }
                    if (doc != null) {
                        viewModel.showSnackbar(if (Strings.isHindi()) "हस्ताक्षर सुरक्षित किया गया" else "Signature attached to PDF")
                        viewModel.navigateBack()
                    } else {
                        viewModel.showSnackbar("Signature saved")
                        viewModel.navigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Gesture, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.saveSignature(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- WATERMARK & PAGE NUMBERS ---

@Composable
fun WatermarkScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.toolWatermark(),
            toolSubtitle = if (Strings.isHindi()) "वॉटरमार्क और पेज नंबर लगाने के लिए PDF चुनें" else "Select a PDF to add custom watermark and page numbers",
            icon = Icons.Default.BrandingWatermark,
            iconColor = IosOrange,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var addPageNumbers by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.toolWatermark(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = watermarkText,
                onValueChange = { watermarkText = it },
                label = { Text(Strings.watermarkText()) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Page Numbers (Bottom Center)", fontWeight = FontWeight.Medium)
                Switch(checked = addPageNumbers, onCheckedChange = { addPageNumbers = it })
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.applyWatermarkAndPageNumbers(doc, watermarkText, addPageNumbers)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(Strings.apply(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- PDF CONVERTER SCREEN ---

@Composable
fun PdfConverterScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDoc.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    if (activeDoc == null) {
        ToolDocumentSelector(
            toolTitle = Strings.toolConverter(),
            toolSubtitle = if (Strings.isHindi()) "इमेज (JPG/PNG) में बदलने के लिए PDF चुनें" else "Select a PDF to extract pages as JPG or PNG images",
            icon = Icons.Default.Transform,
            iconColor = IosRed,
            availableDocs = allDocs,
            onDocumentSelected = { viewModel.setActiveDoc(it) },
            onImportUri = { viewModel.importSafPdf(it) },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val doc = activeDoc!!
    var format by remember { mutableStateOf("jpg") } // "jpg" or "png"
    val isProcessing by viewModel.isProcessing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.toolConverter(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(doc.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null, tint = IosRed)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(doc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${doc.pageCount} pages to export as images", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("Select Output Image Format:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("jpg" to "JPEG (Smaller file)", "png" to "PNG (Lossless)").forEach { (f, label) ->
                    val isSelected = format == f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { format = f },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(f.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.convertPdfToImages(doc, isPng = format == "png") },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (Strings.isHindi()) "इमेज में बदलें" else "Convert to Images", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
