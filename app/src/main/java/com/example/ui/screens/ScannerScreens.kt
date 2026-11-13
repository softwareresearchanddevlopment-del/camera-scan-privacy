@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.pdf.PdfEngine
import com.example.ui.theme.*
import com.example.ui.util.Strings
import com.example.ui.viewmodel.AppSubScreen
import com.example.ui.viewmodel.DocuFlowViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ScannerWorkflowState {
    CAMERA,           // Live Camera Preview with document edge guide overlay
    CROP_EDITOR,      // Full-screen 4-corner perspective crop editor
    NEXT_ACTION,      // Ask for next action: Add another page or finish scanning
    ORGANIZER         // Multi-page organizer, reordering, and final PDF generation
}

@Composable
fun ScannerScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var workflowState by remember { mutableStateOf(ScannerWorkflowState.CAMERA) }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Active image in Crop Editor
    var imageToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var editingPageId by remember { mutableStateOf<String?>(null) } // Non-null if re-cropping an existing page

    // Gallery Review Queue
    val galleryQueue = remember { mutableStateListOf<Bitmap>() }

    // Photo picker for multi-image gallery scan
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            galleryQueue.clear()
            uris.forEach { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { bmp ->
                            galleryQueue.add(bmp)
                        }
                    }
                } catch (_: Exception) {}
            }
            if (galleryQueue.isNotEmpty()) {
                imageToCrop = galleryQueue.removeAt(0)
                editingPageId = null
                workflowState = ScannerWorkflowState.CROP_EDITOR
            }
        }
    }

    val scannedPages by viewModel.scannedPages.collectAsState()
    val activeFilter by viewModel.scannerFilter.collectAsState()

    var flashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (workflowState) {
            // =========================================================
            // STEP 1 & 2: LIVE CAMERA PREVIEW WITH DOCUMENT EDGE GUIDES
            // =========================================================
            ScannerWorkflowState.CAMERA -> {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture
                                    )
                                    cameraControl = cam.cameraControl
                                    cam.cameraControl.enableTorch(flashEnabled)
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback for missing permission
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (Strings.isHindi()) "कैमरा अनुमति आवश्यक है" else "Camera Permission Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (Strings.isHindi())
                                "दस्तावेज़ स्कैन करने के लिए कैमरा अनुमति दें, या गैलरी से चित्र चुनें।"
                            else
                                "Grant camera access to scan documents directly, or import images from your gallery.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                        ) {
                            Text(if (Strings.isHindi()) "अनुमति दें" else "Grant Permission")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Text(if (Strings.isHindi()) "गैलरी से आयात करें" else "Import from Gallery", color = Color.White)
                        }
                    }
                }

                // STEP 2: BEFORE CAPTURE – DOCUMENT EDGE GUIDES OVERLAY
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 100.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cornerLen = 42f
                        val strokeW = 4f
                        val color = Color.White

                        // Outer framing border
                        drawRoundRect(
                            color = color.copy(alpha = 0.45f),
                            size = size,
                            style = Stroke(width = 1.5f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                        )

                        // 4 Prominent Corner Guides
                        // Top Left
                        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)
                        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)
                        // Top Right
                        drawLine(color, Offset(w - cornerLen, 0f), Offset(w, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)
                        drawLine(color, Offset(w, 0f), Offset(w, cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)
                        // Bottom Right
                        drawLine(color, Offset(w - cornerLen, h), Offset(w, h), strokeWidth = strokeW, cap = StrokeCap.Round)
                        drawLine(color, Offset(w, h - cornerLen), Offset(w, h), strokeWidth = strokeW, cap = StrokeCap.Round)
                        // Bottom Left
                        drawLine(color, Offset(0f, h), Offset(cornerLen, h), strokeWidth = strokeW, cap = StrokeCap.Round)
                        drawLine(color, Offset(0f, h - cornerLen), Offset(0f, h), strokeWidth = strokeW, cap = StrokeCap.Round)
                    }

                    // Alignment Tip
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (Strings.isHindi()) "दस्तावेज़ को फ्रेम में संरेखित करें" else "Align document inside frame",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Top Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Page counter badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (scannedPages.isNotEmpty()) IosBlue else Color.Black.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = "Pages", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${scannedPages.size} ${if (Strings.isHindi()) "पृष्ठ" else "pages"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear / Reset Scanned Pages
                        if (scannedPages.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearScannedPages() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Pages",
                                    tint = Color(0xFFFF5252)
                                )
                            }
                        }

                        // Flashlight
                        IconButton(
                            onClick = {
                                flashEnabled = !flashEnabled
                                cameraControl?.enableTorch(flashEnabled)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (flashEnabled) Color.Yellow else Color.White
                            )
                        }

                        // Gallery Button (Multiple image selection)
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .testTag("btn_scanner_gallery")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                        }
                    }
                }

                // Bottom Controls: Shutter & Session
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp)
                ) {
                    // Shutter & Actions Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Organizer / Thumbnail button if pages scanned
                        if (scannedPages.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { workflowState = ScannerWorkflowState.ORGANIZER },
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val lastPage = scannedPages.last()
                                    AsyncImage(
                                        model = File(lastPage.imagePath),
                                        contentDescription = "Last Scanned",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(16.dp)
                                            .background(IosBlue, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${scannedPages.size}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(52.dp))
                        }

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .clickable(enabled = !isCapturing) {
                                    isCapturing = true
                                    val capture = imageCapture
                                    if (capture != null) {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        capture.takePicture(
                                            executor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val buffer = image.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)
                                                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                    val rotated = PdfEngine.rotateBitmap(bmp, image.imageInfo.rotationDegrees)
                                                    image.close()
                                                    isCapturing = false

                                                    // Open in Crop Editor!
                                                    imageToCrop = rotated
                                                    editingPageId = null
                                                    workflowState = ScannerWorkflowState.CROP_EDITOR
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    isCapturing = false
                                                    viewModel.showSnackbar("Capture error: ${exception.message}")
                                                }
                                            }
                                        )
                                    } else {
                                        isCapturing = false
                                    }
                                }
                                .padding(6.dp)
                                .background(if (isCapturing) Color.LightGray else Color.White, CircleShape)
                                .testTag("shutter_button")
                        )

                        // Right: Finish Scanning Button
                        if (scannedPages.isNotEmpty()) {
                            Button(
                                onClick = { workflowState = ScannerWorkflowState.ORGANIZER },
                                colors = ButtonDefaults.buttonColors(containerColor = IosGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("finish_scanning_btn")
                            ) {
                                Text(if (Strings.isHindi()) "पूर्ण" else "Done", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(52.dp))
                        }
                    }
                }
            }

            // =========================================================
            // STEP 4: AFTER CAPTURE – FULL SCREEN 4-CORNER CROP EDITOR
            // =========================================================
            ScannerWorkflowState.CROP_EDITOR -> {
                val currentBmp = imageToCrop
                if (currentBmp != null) {
                    FullCropEditorScreen(
                        sourceBitmap = currentBmp,
                        isEditingExisting = editingPageId != null,
                        queueRemaining = galleryQueue.size,
                        onRetake = {
                            imageToCrop = null
                            editingPageId = null
                            workflowState = ScannerWorkflowState.CAMERA
                        },
                        onCropAccepted = { croppedBmp ->
                            if (editingPageId != null) {
                                // Replaced existing page
                                viewModel.updateScannedPage(editingPageId!!, croppedBmp)
                                viewModel.showSnackbar(if (Strings.isHindi()) "पृष्ठ पुनः क्रॉप किया गया" else "Page re-cropped successfully")
                                editingPageId = null
                                imageToCrop = null
                                workflowState = ScannerWorkflowState.ORGANIZER
                            } else {
                                // Add to scanned pages and assign page number
                                viewModel.addScannedPage(croppedBmp)
                                imageToCrop = null

                                // Check gallery review queue
                                if (galleryQueue.isNotEmpty()) {
                                    imageToCrop = galleryQueue.removeAt(0)
                                    workflowState = ScannerWorkflowState.CROP_EDITOR
                                } else {
                                    workflowState = ScannerWorkflowState.NEXT_ACTION
                                }
                            }
                        },
                        onUseOriginal = {
                            if (editingPageId != null) {
                                editingPageId = null
                                imageToCrop = null
                                workflowState = ScannerWorkflowState.ORGANIZER
                            } else {
                                viewModel.addScannedPage(currentBmp)
                                imageToCrop = null
                                if (galleryQueue.isNotEmpty()) {
                                    imageToCrop = galleryQueue.removeAt(0)
                                    workflowState = ScannerWorkflowState.CROP_EDITOR
                                } else {
                                    workflowState = ScannerWorkflowState.NEXT_ACTION
                                }
                            }
                        }
                    )
                } else {
                    workflowState = ScannerWorkflowState.CAMERA
                }
            }

            // =========================================================
            // STEP 6: ASK FOR NEXT ACTION SCREEN
            // =========================================================
            ScannerWorkflowState.NEXT_ACTION -> {
                NextActionScreen(
                    scannedPages = scannedPages,
                    onAddAnotherPage = {
                        workflowState = ScannerWorkflowState.CAMERA
                    },
                    onFinishScanning = {
                        workflowState = ScannerWorkflowState.ORGANIZER
                    },
                    onRetakeLastPage = {
                        if (scannedPages.isNotEmpty()) {
                            viewModel.removeScannedPage(scannedPages.last().id)
                        }
                        workflowState = ScannerWorkflowState.CAMERA
                    },
                    onDeleteLastPage = {
                        if (scannedPages.isNotEmpty()) {
                            viewModel.removeScannedPage(scannedPages.last().id)
                        }
                        if (scannedPages.isEmpty()) {
                            workflowState = ScannerWorkflowState.CAMERA
                        }
                    }
                )
            }

            // =========================================================
            // STEP 7 & 8: MULTI-PAGE SCAN ORGANIZER & FINAL CREATION
            // =========================================================
            ScannerWorkflowState.ORGANIZER -> {
                MultiPageScanOrganizer(
                    viewModel = viewModel,
                    scannedPages = scannedPages,
                    onAddMorePages = {
                        workflowState = ScannerWorkflowState.CAMERA
                    },
                    onRecropPage = { page ->
                        val bmp = BitmapFactory.decodeFile(page.imagePath)
                        if (bmp != null) {
                            imageToCrop = bmp
                            editingPageId = page.id
                            workflowState = ScannerWorkflowState.CROP_EDITOR
                        }
                    },
                    onBackToCamera = {
                        workflowState = ScannerWorkflowState.CAMERA
                    }
                )
            }
        }
    }
}

// =========================================================================
// FULL SCREEN 4-CORNER CROP EDITOR COMPOSABLE
// =========================================================================

@Composable
fun FullCropEditorScreen(
    sourceBitmap: Bitmap,
    isEditingExisting: Boolean,
    queueRemaining: Int,
    onRetake: () -> Unit,
    onCropAccepted: (Bitmap) -> Unit,
    onUseOriginal: () -> Unit
) {
    var workingBitmap by remember(sourceBitmap) { mutableStateOf(sourceBitmap) }

    // Normalized coordinates (0f..1f) for the 4 corners
    var tl by remember { mutableStateOf(Offset(0.06f, 0.06f)) }
    var tr by remember { mutableStateOf(Offset(0.94f, 0.06f)) }
    var br by remember { mutableStateOf(Offset(0.94f, 0.94f)) }
    var bl by remember { mutableStateOf(Offset(0.06f, 0.94f)) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (queueRemaining > 0) "Review Photo (${queueRemaining + 1} remaining)" else if (isEditingExisting) "Re-Crop Page" else "Adjust Document Crop",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Drag the 4 corner handles to align",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            // Reset crop handles to default
            TextButton(
                onClick = {
                    tl = Offset(0.06f, 0.06f)
                    tr = Offset(0.94f, 0.06f)
                    br = Offset(0.94f, 0.94f)
                    bl = Offset(0.06f, 0.94f)
                }
            ) {
                Text("Reset", color = IosBlue, fontWeight = FontWeight.Bold)
            }
        }

        // Center: Interactive Image & Draggable Quadrilateral Crop
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .onSizeChanged { containerSize = it },
            contentAlignment = Alignment.Center
        ) {
            val bmpAspect = workingBitmap.width.toFloat() / workingBitmap.height.toFloat()

            // Calculate display dimensions inside container
            val containerAspect = if (containerSize.height > 0) containerSize.width.toFloat() / containerSize.height.toFloat() else 1f
            val displayWidth: Float
            val displayHeight: Float

            if (bmpAspect > containerAspect) {
                displayWidth = containerSize.width.toFloat().coerceAtLeast(100f)
                displayHeight = displayWidth / bmpAspect
            } else {
                displayHeight = containerSize.height.toFloat().coerceAtLeast(100f)
                displayWidth = displayHeight * bmpAspect
            }

            Box(
                modifier = Modifier
                    .size(width = (displayWidth / LocalContext.current.resources.displayMetrics.density).dp, height = (displayHeight / LocalContext.current.resources.displayMetrics.density).dp)
                    .shadow(8.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                // Background Bitmap
                Image(
                    bitmap = workingBitmap.asImageBitmap(),
                    contentDescription = "Document to crop",
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Crop Quadrilateral Canvas
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val pTL = Offset(tl.x * size.width, tl.y * size.height)
                    val pTR = Offset(tr.x * size.width, tr.y * size.height)
                    val pBR = Offset(br.x * size.width, br.y * size.height)
                    val pBL = Offset(bl.x * size.width, bl.y * size.height)

                    // Draw semi-transparent dark overlay outside crop path
                    val quadPath = Path().apply {
                        moveTo(pTL.x, pTL.y)
                        lineTo(pTR.x, pTR.y)
                        lineTo(pBR.x, pBR.y)
                        lineTo(pBL.x, pBL.y)
                        close()
                    }

                    // Bright boundary lines
                    drawPath(
                        path = quadPath,
                        color = Color(0xFF007AFF),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Subtle grid inside crop
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset((pTL.x + pBL.x) / 2f, (pTL.y + pBL.y) / 2f),
                        end = Offset((pTR.x + pBR.x) / 2f, (pTR.y + pBR.y) / 2f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset((pTL.x + pTR.x) / 2f, (pTL.y + pTR.y) / 2f),
                        end = Offset((pBL.x + pBR.x) / 2f, (pBL.y + pBR.y) / 2f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw 4 corner handles
                    val handleRadius = 14.dp.toPx()
                    listOf(pTL, pTR, pBR, pBL).forEach { pt ->
                        drawCircle(Color.White, radius = handleRadius, center = pt)
                        drawCircle(Color(0xFF007AFF), radius = handleRadius - 3.dp.toPx(), center = pt)
                    }
                }

                // Invisible touch overlays for the 4 corners (Each 56dp x 56dp touch target)
                val handleSizeDp = 56.dp

                // Top-Left Handle
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((tl.x * displayWidth) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp,
                            y = ((tl.y * displayHeight) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp
                        )
                        .size(handleSizeDp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                tl = Offset(
                                    x = (tl.x + dragAmount.x / displayWidth).coerceIn(0f, tr.x - 0.05f),
                                    y = (tl.y + dragAmount.y / displayHeight).coerceIn(0f, bl.y - 0.05f)
                                )
                            }
                        }
                )

                // Top-Right Handle
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((tr.x * displayWidth) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp,
                            y = ((tr.y * displayHeight) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp
                        )
                        .size(handleSizeDp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                tr = Offset(
                                    x = (tr.x + dragAmount.x / displayWidth).coerceIn(tl.x + 0.05f, 1f),
                                    y = (tr.y + dragAmount.y / displayHeight).coerceIn(0f, br.y - 0.05f)
                                )
                            }
                        }
                )

                // Bottom-Right Handle
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((br.x * displayWidth) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp,
                            y = ((br.y * displayHeight) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp
                        )
                        .size(handleSizeDp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                br = Offset(
                                    x = (br.x + dragAmount.x / displayWidth).coerceIn(bl.x + 0.05f, 1f),
                                    y = (br.y + dragAmount.y / displayHeight).coerceIn(tr.y + 0.05f, 1f)
                                )
                            }
                        }
                )

                // Bottom-Left Handle
                Box(
                    modifier = Modifier
                        .offset(
                            x = ((bl.x * displayWidth) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp,
                            y = ((bl.y * displayHeight) / LocalContext.current.resources.displayMetrics.density).dp - 28.dp
                        )
                        .size(handleSizeDp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                bl = Offset(
                                    x = (bl.x + dragAmount.x / displayWidth).coerceIn(0f, br.x - 0.05f),
                                    y = (bl.y + dragAmount.y / displayHeight).coerceIn(tl.y + 0.05f, 1f)
                                )
                            }
                        }
                )
            }
        }

        // Middle Toolbar: Rotate Left & Rotate Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { workingBitmap = PdfEngine.rotateBitmap90(workingBitmap, clockwise = false) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rotate Left", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            FilledTonalButton(
                onClick = { workingBitmap = PdfEngine.rotateBitmap90(workingBitmap, clockwise = true) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rotate Right", fontSize = 12.sp)
            }
        }

        // Bottom Action Bar: Retake, Use Original, Crop & Continue
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Retake", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onUseOriginal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Use Original", color = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action: Apply Crop & Continue
                Button(
                    onClick = {
                        val cropped = PdfEngine.cropQuadrilateral(
                            workingBitmap,
                            PointF(tl.x, tl.y),
                            PointF(tr.x, tr.y),
                            PointF(br.x, br.y),
                            PointF(bl.x, bl.y)
                        )
                        onCropAccepted(cropped)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_crop_continue"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crop & Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// STEP 6: ASK FOR NEXT ACTION SCREEN
// =========================================================================

@Composable
fun NextActionScreen(
    scannedPages: List<ScanPage>,
    onAddAnotherPage: () -> Unit,
    onFinishScanning: () -> Unit,
    onRetakeLastPage: () -> Unit,
    onDeleteLastPage: () -> Unit
) {
    val lastPage = scannedPages.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IosGreen.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IosGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Page ${scannedPages.size} Accepted",
                        color = IosGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "What would you like to do next?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Center: Thumbnail Preview of Accepted Page
        if (lastPage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(0.72f)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box {
                    AsyncImage(
                        model = File(lastPage.imagePath),
                        contentDescription = "Accepted Page",
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(IosBlue, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Page ${scannedPages.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 1: Add Another Page
            Button(
                onClick = onAddAnotherPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_add_another_page"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Another Page", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Action 2: Finish Scanning
            Button(
                onClick = onFinishScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_finish_scanning"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosGreen)
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finish Scanning (${scannedPages.size} ${if (scannedPages.size == 1) "Page" else "Pages"})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Secondary: Retake or Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onRetakeLastPage) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake This Page", color = Color.LightGray)
                }

                TextButton(onClick = onDeleteLastPage) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = IosRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Page", color = IosRed)
                }
            }
        }
    }
}

// =========================================================================
// STEP 7 & 8: MULTI-PAGE SCAN ORGANIZER & FINAL CREATION
// =========================================================================

@Composable
fun MultiPageScanOrganizer(
    viewModel: DocuFlowViewModel,
    scannedPages: List<ScanPage>,
    onAddMorePages: () -> Unit,
    onRecropPage: (ScanPage) -> Unit,
    onBackToCamera: () -> Unit
) {
    var outputName by remember { mutableStateOf("Scan_${System.currentTimeMillis() % 100000}") }
    var selectedFormat by remember { mutableStateOf(PageFormat.A4) }
    var selectedOrientation by remember { mutableStateOf(DocOrientation.PORTRAIT) }
    var selectedMargin by remember { mutableStateOf(MarginOption.NONE) }
    var selectedFilter by remember { mutableStateOf(ScanFilter.ENHANCED) }
    val isProcessing by viewModel.isProcessing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Page Scan Organizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToCamera) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onAddMorePages) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Page", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.finishScanningAndCreatePdf(outputName) },
                        enabled = scannedPages.isNotEmpty() && !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_create_final_pdf"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create PDF (${scannedPages.size} Pages)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // PDF Document Settings
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output PDF Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Page format & orientation chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(PageFormat.A4, PageFormat.A3, PageFormat.LETTER).forEach { fmt ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(fmt.displayName, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Scanned Page Items
            itemsIndexed(scannedPages, key = { _, page -> "${page.id}_${page.timestamp}" }) { index, page ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF2F2F7))
                        ) {
                            AsyncImage(
                                model = File(page.imagePath),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Page Number Badge
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
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls: Move Left, Crop Again, Rotate, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Move Left
                            IconButton(
                                onClick = { if (index > 0) viewModel.reorderScannedPages(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                            }

                            // Re-Crop
                            IconButton(
                                onClick = { onRecropPage(page) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = "Crop Again", tint = IosBlue, modifier = Modifier.size(16.dp))
                            }

                            // Rotate
                            IconButton(
                                onClick = { viewModel.rotateScannedPage(page.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(16.dp))
                            }

                            // Delete
                            IconButton(
                                onClick = { viewModel.removeScannedPage(page.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IosRed, modifier = Modifier.size(16.dp))
                            }

                            // Move Right
                            IconButton(
                                onClick = { if (index < scannedPages.lastIndex) viewModel.reorderScannedPages(index, index + 1) },
                                enabled = index < scannedPages.lastIndex,
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
