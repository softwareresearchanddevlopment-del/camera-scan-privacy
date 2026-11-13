@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PptAspect
import com.example.data.model.PptLayout
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosRed
import com.example.ui.viewmodel.DocuFlowViewModel

@Composable
fun ImageToPptScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val slides by viewModel.pptSlides.collectAsState()
    val aspect by viewModel.pptAspect.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var presentationTitle by remember { mutableStateOf("Presentation_${System.currentTimeMillis() % 100000}") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPptSlideImages(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image to PowerPoint (PPTX)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (slides.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearPptSlides() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Slides", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add More Slides")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (slides.isNotEmpty()) {
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
                            onClick = { viewModel.generatePresentation(presentationTitle) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_pptx_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Building PPTX Presentation...")
                            } else {
                                Icon(Icons.Default.Slideshow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create PowerPoint (${slides.size} Slides)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (slides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.Slideshow,
                    title = "Create Presentation",
                    subtitle = "Select images from your gallery to generate a real Microsoft PowerPoint (.pptx) file",
                    buttonText = "Select Slide Images",
                    onButtonClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        } else {
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
                        value = presentationTitle,
                        onValueChange = { presentationTitle = it },
                        label = { Text("Presentation Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Slide Aspect Ratio
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Slide Aspect Ratio:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                PptAspect.WIDESCREEN_16_9 to "16:9 Widescreen",
                                PptAspect.STANDARD_4_3 to "4:3 Standard"
                            ).forEach { (asp, label) ->
                                val isSelected = aspect == asp
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setPptAspect(asp) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                itemsIndexed(slides, key = { idx, s -> "$idx-${s.id}" }) { index, slide ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Slide Image Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = Uri.parse(slide.imageUri),
                                        contentDescription = "Slide ${index + 1}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(3.dp)
                                            .size(18.dp)
                                            .background(IosOrange, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Slide ${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = slide.layout.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Reorder buttons
                                Row {
                                    IconButton(
                                        onClick = { if (index > 0) viewModel.reorderPptSlides(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                    }
                                    IconButton(
                                        onClick = { if (index < slides.lastIndex) viewModel.reorderPptSlides(index, index + 1) },
                                        enabled = index < slides.lastIndex,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                    }
                                    IconButton(
                                        onClick = { viewModel.removePptSlide(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = IosRed)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Layout selection chips for this slide
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(PptLayout.values().size) { lIdx ->
                                    val layout = PptLayout.values()[lIdx]
                                    FilterChip(
                                        selected = slide.layout == layout,
                                        onClick = { viewModel.updateSlideLayout(index, layout) },
                                        label = { Text(layout.displayName, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
