@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocBlock
import com.example.data.model.DocBlockType
import com.example.data.model.DocumentTemplates
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.util.Strings
import com.example.ui.viewmodel.AppSubScreen
import com.example.ui.viewmodel.DocuFlowViewModel

@Composable
fun CreateScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (Strings.isHindi()) "दस्तावेज़ निर्माण केंद्र" else "Document Creation Center",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (Strings.isHindi()) "नया PDF, Word, PowerPoint या स्कैन बनाएं" else "Create new PDFs, Word documents, PowerPoint presentations or scans",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hero Quick Action: Canvas Document Editor
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        viewModel.setEditorDocTitle("New Document")
                        viewModel.navigateToSubScreen(AppSubScreen.CANVAS_EDITOR)
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF007AFF).copy(alpha = 0.08f), Color(0xFF5856D6).copy(alpha = 0.08f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(IosBlue, IosIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (Strings.isHindi()) "नया वर्ड / पेजेस दस्तावेज़ बनाएं" else "New Canvas Document",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (Strings.isHindi()) "टेक्स्ट फ़ॉर्मेटिंग, टेबल और PDF/Word निर्यात" else "Pages & Word style editor with tables & export",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = IosBlue)
                    }
                }
            }
        }

        // Section: Primary Document Types
        item {
            SectionHeader(if (Strings.isHindi()) "प्रमुख फ़ाइल प्रकार" else "Primary Document Types")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CreateOptionRow(
                    icon = Icons.Default.PictureAsPdf,
                    iconBg = IosRed,
                    title = "Create PDF from Photos",
                    subtitle = "Select multiple images, arrange page sequence & customize layout",
                    onClick = { viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PDF) }
                )
                CreateOptionRow(
                    icon = Icons.Default.Slideshow,
                    iconBg = Color(0xFFD24726),
                    title = "Create PowerPoint (.pptx)",
                    subtitle = "Convert images into an editable 16:9 or 4:3 slide presentation",
                    onClick = { viewModel.navigateToSubScreen(AppSubScreen.IMAGE_TO_PPT) }
                )
                CreateOptionRow(
                    icon = Icons.Default.CameraAlt,
                    iconBg = IosBlue,
                    title = "Multi-Page Camera Scanner",
                    subtitle = "Live capture with perspective correction, filters & PDF generation",
                    onClick = { viewModel.navigateToSubScreen(AppSubScreen.SCANNER) }
                )
                CreateOptionRow(
                    icon = Icons.Default.CallMerge,
                    iconBg = IosOrange,
                    title = "Merge Existing PDFs",
                    subtitle = "Combine multiple PDF files from your device into a single file",
                    onClick = { viewModel.navigateToSubScreen(AppSubScreen.PDF_MERGER) }
                )
                CreateOptionRow(
                    icon = Icons.Default.FolderSpecial,
                    iconBg = Color(0xFF673AB7),
                    title = if (Strings.isHindi()) "PDF प्रबंधक और टूल स्टूडियो" else "PDF Manager & Tool Studio",
                    subtitle = if (Strings.isHindi()) "किसी भी PDF को आयात करें और 16 टूल्स का उपयोग करें" else "Import any PDF to edit, compress, split, watermark, or sign",
                    onClick = { viewModel.navigateToSubScreen(AppSubScreen.PDF_MANAGER) }
                )
            }
        }

        // Section: Professional Document Templates
        item {
            SectionHeader(if (Strings.isHindi()) "व्यावसायिक टेम्पलेट" else "Professional Templates")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DocumentTemplates.templates.forEach { tmpl ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.loadTemplate(tmpl) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IosIndigo.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = IosIndigo)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tmpl.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(tmpl.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Text(tmpl.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateOptionRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconBg, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
