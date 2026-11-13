@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.util.Strings
import com.example.ui.viewmodel.DocuFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CanvasEditorScreen(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docTitle by viewModel.editorDocTitle.collectAsState()
    val blocks by viewModel.editorBlocks.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var activeBlockIndex by remember { mutableStateOf(0) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addEditorBlock(DocBlockType.IMAGE)
            val lastIdx = viewModel.editorBlocks.value.lastIndex
            if (lastIdx >= 0) {
                val list = viewModel.editorBlocks.value.toMutableList()
                list[lastIdx] = list[lastIdx].copy(imageUri = uri.toString())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = docTitle,
                        onValueChange = { viewModel.setEditorDocTitle(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetCanvasEditor() }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New Document", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showInsertMenu = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Insert", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export & Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Text Formatting & Styling Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Style Presets (Title, Subtitle, Heading 1, 2, 3, Normal Text, Quote)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                        val currentType = activeBlock?.type ?: DocBlockType.PARAGRAPH

                        listOf(
                            DocBlockType.TITLE to "Title",
                            DocBlockType.SUBTITLE to "Subtitle",
                            DocBlockType.HEADING_1 to "Heading 1",
                            DocBlockType.HEADING_2 to "Heading 2",
                            DocBlockType.HEADING_3 to "Heading 3",
                            DocBlockType.PARAGRAPH to "Normal Text",
                            DocBlockType.QUOTE to "Quote",
                            DocBlockType.BULLET_LIST_ITEM to "Bullet",
                            DocBlockType.NUMBERED_LIST_ITEM to "Number",
                            DocBlockType.CODE_BLOCK to "Code"
                        ).forEach { (type, label) ->
                            val isSelected = currentType == type
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.updateEditorBlockType(activeBlockIndex, type) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Formatting Buttons: Bold, Italic, Alignment, Insert Row/Col
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                        val fmt = activeBlock?.formatting ?: TextFormatting()

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Bold
                            IconButton(
                                onClick = { viewModel.toggleEditorBlockBold(activeBlockIndex) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (fmt.isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = if (fmt.isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            // Italic
                            IconButton(
                                onClick = { viewModel.toggleEditorBlockItalic(activeBlockIndex) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (fmt.isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = if (fmt.isItalic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            // Align Left
                            IconButton(
                                onClick = { viewModel.setEditorBlockAlign(activeBlockIndex, "LEFT") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.FormatAlignLeft, contentDescription = "Left", tint = if (fmt.alignment == "LEFT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            // Align Center
                            IconButton(
                                onClick = { viewModel.setEditorBlockAlign(activeBlockIndex, "CENTER") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", tint = if (fmt.alignment == "CENTER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Add new paragraph or delete current block
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                viewModel.addEditorBlock(DocBlockType.PARAGRAPH, "")
                                activeBlockIndex = viewModel.editorBlocks.value.lastIndex
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Line", fontSize = 12.sp)
                            }

                            if (blocks.size > 1) {
                                IconButton(
                                    onClick = {
                                        viewModel.removeEditorBlock(activeBlockIndex)
                                        activeBlockIndex = (activeBlockIndex - 1).coerceAtLeast(0)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = IosRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Document Canvas (A4 sheet representation)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFEBEBF0)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                        .testTag("document_canvas"),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        blocks.forEachIndexed { index, block ->
                            val isFocused = activeBlockIndex == index

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(if (isFocused) 1.5.dp else 0.dp, if (isFocused) IosBlue else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { activeBlockIndex = index }
                                    .padding(4.dp)
                            ) {
                                when (block.type) {
                                    DocBlockType.IMAGE -> {
                                        if (block.imageUri != null) {
                                            AsyncImage(
                                                model = block.imageUri,
                                                contentDescription = "Inserted Image",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }
                                    DocBlockType.TABLE -> {
                                        val tableData = block.tableData ?: listOf(listOf("Header 1", "Header 2"), listOf("Data 1", "Data 2"))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .border(1.dp, Color(0xFFD1D1D6), RoundedCornerShape(6.dp))
                                        ) {
                                            tableData.forEachIndexed { rIdx, row ->
                                                Row(
                                                    modifier = Modifier
                                                        .background(if (rIdx == 0) Color(0xFFF2F2F7) else Color.White)
                                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                                ) {
                                                    row.forEachIndexed { cIdx, cell ->
                                                        BasicTextField(
                                                            value = cell,
                                                            onValueChange = { newCell ->
                                                                viewModel.updateTableCell(index, rIdx, cIdx, newCell)
                                                            },
                                                            textStyle = TextStyle(
                                                                fontSize = 13.sp,
                                                                fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                                color = Color.Black
                                                            ),
                                                            modifier = Modifier
                                                                .width(110.dp)
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                if (rIdx < tableData.lastIndex) {
                                                    Divider(color = Color(0xFFE5E5EA))
                                                }
                                            }

                                            // Table action buttons (+ Row, + Column)
                                            if (isFocused) {
                                                Row(
                                                    modifier = Modifier.padding(6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    TextButton(onClick = { viewModel.addRowToTable(index) }) {
                                                        Text("+ Row", fontSize = 11.sp)
                                                    }
                                                    TextButton(onClick = { viewModel.addColToTable(index) }) {
                                                        Text("+ Column", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    DocBlockType.PAGE_BREAK -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Divider(modifier = Modifier.weight(1f), color = IosBlue.copy(alpha = 0.5f))
                                            Text(
                                                text = " Page Break ",
                                                fontSize = 10.sp,
                                                color = IosBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Divider(modifier = Modifier.weight(1f), color = IosBlue.copy(alpha = 0.5f))
                                        }
                                    }
                                    else -> {
                                        // Typography according to block type
                                        val style = when (block.type) {
                                            DocBlockType.TITLE -> TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                                            DocBlockType.SUBTITLE -> TextStyle(fontSize = 15.sp, color = Color(0xFF8E8E93), fontStyle = FontStyle.Italic)
                                            DocBlockType.HEADING_1 -> TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                                            DocBlockType.HEADING_2 -> TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                                            DocBlockType.HEADING_3 -> TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3A3A3C))
                                            DocBlockType.QUOTE -> TextStyle(fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Color(0xFF636366))
                                            DocBlockType.CAPTION -> TextStyle(fontSize = 12.sp, color = Color(0xFF8E8E93))
                                            DocBlockType.CODE_BLOCK -> TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF006699))
                                            else -> TextStyle(
                                                fontSize = 15.sp,
                                                fontWeight = if (block.formatting.isBold) FontWeight.Bold else FontWeight.Normal,
                                                fontStyle = if (block.formatting.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                textDecoration = when {
                                                    block.formatting.isUnderline && block.formatting.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                                    block.formatting.isUnderline -> TextDecoration.Underline
                                                    block.formatting.isStrikethrough -> TextDecoration.LineThrough
                                                    else -> TextDecoration.None
                                                },
                                                textAlign = when (block.formatting.alignment) {
                                                    "CENTER" -> TextAlign.Center
                                                    "RIGHT" -> TextAlign.Right
                                                    else -> TextAlign.Left
                                                },
                                                color = Color(0xFF1C1C1E)
                                            )
                                        }

                                        BasicTextField(
                                            value = block.text,
                                            onValueChange = { viewModel.updateEditorBlockText(index, it) },
                                            textStyle = style,
                                            modifier = Modifier.fillMaxWidth()
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

    // Insert Modal BottomSheet
    if (showInsertMenu) {
        ModalBottomSheet(
            onDismissRequest = { showInsertMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Insert into Document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))

                InsertMenuItem(Icons.Default.Image, "Insert Image from Gallery") {
                    showInsertMenu = false
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                InsertMenuItem(Icons.Default.TableChart, "Insert Table") {
                    showInsertMenu = false
                    viewModel.addEditorBlock(
                        DocBlockType.TABLE,
                        table = listOf(
                            listOf("Item", "Description", "Value"),
                            listOf("1", "Service Sample", "$100"),
                            listOf("2", "Product Sample", "$250")
                        )
                    )
                }
                InsertMenuItem(Icons.Default.FormatQuote, "Insert Quote Block") {
                    showInsertMenu = false
                    viewModel.addEditorBlock(DocBlockType.QUOTE, "Your quote here...")
                }
                InsertMenuItem(Icons.Default.Code, "Insert Code Block") {
                    showInsertMenu = false
                    viewModel.addEditorBlock(DocBlockType.CODE_BLOCK, "// code snippet here\nfun execute() {\n}")
                }
                InsertMenuItem(Icons.Default.ContentCut, "Insert Page Break") {
                    showInsertMenu = false
                    viewModel.addEditorBlock(DocBlockType.PAGE_BREAK)
                }
                InsertMenuItem(Icons.Default.CalendarToday, "Insert Today's Date") {
                    showInsertMenu = false
                    val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
                    viewModel.addEditorBlock(DocBlockType.PARAGRAPH, "Date: $date")
                }
            }
        }
    }

    // Export Options Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export & Share Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose format to save this document:")
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportEditorToPdf()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to PDF Document")
                    }

                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportEditorToWord()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B579A))
                    ) {
                        Icon(Icons.Default.Article, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to Word (DOCX)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun InsertMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
