@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.pdf.PdfEngine
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ToolDocumentSelector
import com.example.ui.theme.*
import com.example.ui.util.Strings
import com.example.ui.viewmodel.AppSubScreen
import com.example.ui.viewmodel.DocuFlowViewModel
import java.io.File

@Composable
fun ImageToPdfScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val selectedUris by viewModel.selectedImageUris.collectAsState()
    val pageFormat by viewModel.pageFormat.collectAsState()
    val orientation by viewModel.docOrientation.collectAsState()
    val imageFit by viewModel.imageFit.collectAsState()
    val marginOption by viewModel.marginOption.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var docTitle by remember { mutableStateOf("Images_${System.currentTimeMillis() % 100000}") }
    var watermarkText by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImageUris(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.imagesToPdf(), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (selectedUris.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearImageUris() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = {
                            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add More")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (selectedUris.isNotEmpty()) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedUris.size} ${if (Strings.isHindi()) "इमेज चुनी गईं" else "Images Selected"}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${pageFormat.displayName} • ${orientation.name.lowercase().capitalize()}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generatePdfFromImages(docTitle, watermarkText) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_pdf_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (Strings.isHindi()) "बनाया जा रहा है..." else "Generating PDF...")
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Strings.createPdfAction(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (selectedUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.Collections,
                    title = Strings.selectImages(),
                    subtitle = if (Strings.isHindi()) "फ़ोन की गैलरी से एक या अनेक इमेज चुनें" else "Pick multiple photos from your gallery to create a PDF",
                    buttonText = Strings.selectImages(),
                    onButtonClick = {
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("PDF Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                itemsIndexed(selectedUris, key = { idx, uri -> "$idx-$uri" }) { index, uri ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("image_page_$index"),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prominent Page Number Box
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Image ${index + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .background(IosBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Strings.pageNumber(index + 1),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (index == 0) "Cover Page" else "Document Page",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Reordering controls (Move Up, Move Down, Delete)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (index > 0) viewModel.reorderImages(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.LightGray
                                    )
                                }
                                IconButton(
                                    onClick = { if (index < selectedUris.lastIndex) viewModel.reorderImages(index, index + 1) },
                                    enabled = index < selectedUris.lastIndex,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (index < selectedUris.lastIndex) MaterialTheme.colorScheme.primary else Color.LightGray
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeImageUri(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IosRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PDF Configuration Modal BottomSheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(Strings.pdfSettings(), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                // Page Format
                Text(Strings.pageSize(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PageFormat.values().size) { i ->
                        val fmt = PageFormat.values()[i]
                        FilterChip(
                            selected = pageFormat == fmt,
                            onClick = { viewModel.setPageFormat(fmt) },
                            label = { Text(fmt.displayName) }
                        )
                    }
                }

                // Orientation
                Text(Strings.orientation(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DocOrientation.PORTRAIT to Strings.portrait(), DocOrientation.LANDSCAPE to Strings.landscape()).forEach { (ori, label) ->
                        FilterChip(
                            selected = orientation == ori,
                            onClick = { viewModel.setDocOrientation(ori) },
                            label = { Text(label) }
                        )
                    }
                }

                // Image Fit
                Text(Strings.imageFit(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ImageFit.FIT_PAGE to Strings.fitPage(),
                        ImageFit.FILL_PAGE to Strings.fillPage(),
                        ImageFit.ORIGINAL_RATIO to Strings.originalSize()
                    ).forEach { (fit, label) ->
                        FilterChip(
                            selected = imageFit == fit,
                            onClick = { viewModel.setImageFit(fit) },
                            label = { Text(label) }
                        )
                    }
                }

                // Margins
                Text(Strings.margins(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        MarginOption.NONE to Strings.marginNone(),
                        MarginOption.SMALL to Strings.marginSmall(),
                        MarginOption.NORMAL to Strings.marginNormal()
                    ).forEach { (m, label) ->
                        FilterChip(
                            selected = marginOption == m,
                            onClick = { viewModel.setMarginOption(m) },
                            label = { Text(label) }
                        )
                    }
                }

                // Watermark
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text(Strings.watermarkText() + " (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = { showSettingsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Strings.apply())
                }
            }
        }
    }
}

@Composable
fun PdfReaderScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDoc by viewModel.activeDoc.collectAsState()
    val doc = activeDoc

    if (doc == null) {
        val allDocs by viewModel.allDocuments.collectAsState()
        ToolDocumentSelector(
            toolTitle = Strings.toolReader(),
            toolSubtitle = if (Strings.isHindi()) "PDF पढ़ें और देखें" else "Read and view PDF documents",
            availableDocs = allDocs,
            onDocumentSelected = { selected ->
                viewModel.setActiveDoc(selected)
            },
            onImportUri = { uri ->
                viewModel.importSafPdf(uri) { newDoc ->
                    viewModel.setActiveDoc(newDoc)
                }
            },
            onBack = { viewModel.navigateBack() }
        )
        return
    }

    val file = remember(doc.filePath) { File(doc.filePath) }
    val totalPages = remember(file) { PdfEngine.getPageCount(file).coerceAtLeast(1) }
    var currentPage by remember { mutableStateOf(1) }
    var zoomScale by remember { mutableStateOf(1.0f) }
    var showThumbnailsDrawer by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Track scroll position to update current page indicator
    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentPage = (listState.firstVisibleItemIndex + 1).coerceIn(1, totalPages)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(Strings.pageIndicator(currentPage, totalPages), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Change Document
                    IconButton(onClick = { viewModel.clearActiveDoc() }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Change PDF")
                    }
                    // Zoom In / Out
                    IconButton(onClick = { zoomScale = if (zoomScale >= 2.0f) 1.0f else zoomScale + 0.5f }) {
                        Icon(if (zoomScale > 1.0f) Icons.Default.ZoomOut else Icons.Default.ZoomIn, contentDescription = "Zoom")
                    }
                    // Edit
                    IconButton(onClick = { viewModel.navigateToSubScreen(AppSubScreen.PDF_EDITOR, doc) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    // Print
                    IconButton(onClick = { viewModel.printDocument(doc) }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                    // Share
                    IconButton(onClick = {
                        val uri = viewModel.getShareUri(doc)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share PDF"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Thumbnails jump bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Strings.pageIndicator(currentPage, totalPages)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Row {
                        TextButton(onClick = { viewModel.navigateToSubScreen(AppSubScreen.PDF_ORGANIZER, doc) }) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.toolOrganizer())
                        }
                        TextButton(onClick = { viewModel.navigateToSubScreen(AppSubScreen.SIGNATURE, doc) }) {
                            Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.toolSignature())
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE5E5EA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(totalPages) { pageIndex ->
                var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(pageIndex) {
                    pageBitmap = PdfEngine.renderPageToBitmap(file, pageIndex, 1200)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
                        .testTag("pdf_page_$pageIndex"),
                    shadowElevation = 4.dp,
                    color = Color.White
                ) {
                    if (pageBitmap != null) {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "Page ${pageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    pageBitmap!!.width.toFloat() / pageBitmap!!.height.toFloat()
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateBlankPdfScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val isProcessing by viewModel.isProcessing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.createPdf(), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Document Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Document Notes / Text Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createTextDocument(title, content)
                    } else {
                        viewModel.showSnackbar("Please enter a title")
                    }
                },
                enabled = !isProcessing && title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_text_pdf_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.createPdfAction(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
