package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.office.DocxEngine
import com.example.data.office.OfficeParsedResult
import com.example.data.office.OfficeParser
import com.example.data.office.PptxEngine
import com.example.data.pdf.PdfEngine
import com.example.data.pdf.PdfPrintAdapter
import com.example.data.repository.DocumentRepository
import com.example.ui.util.AppLanguage
import com.example.ui.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    HOME,
    DOCUMENTS,
    CREATE,
    TOOLS,
    SETTINGS
}

enum class AppSubScreen {
    NONE,
    SCANNER,
    PDF_MANAGER,
    IMAGE_TO_PDF,
    IMAGE_TO_PPT,
    CANVAS_EDITOR,
    PDF_READER,
    PDF_EDITOR,
    PDF_ORGANIZER,
    PDF_MERGER,
    PDF_SPLITTER,
    PDF_COMPRESSOR,
    PDF_CONVERTER,
    OFFICE_VIEWER,
    SIGNATURE,
    WATERMARK,
    TEMPLATES,
    CREATE_BLANK_PDF
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class DocuFlowViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DocumentRepository(application)

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentSubScreen = MutableStateFlow(AppSubScreen.NONE)
    val currentSubScreen: StateFlow<AppSubScreen> = _currentSubScreen.asStateFlow()

    private val subScreenStack = mutableListOf<AppSubScreen>()

    // Documents
    val allDocuments = repository.allDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentDocuments = repository.recentDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteDocuments = repository.favoriteDocuments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val grantedFolders = repository.grantedFolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active document
    private val _activeDoc = MutableStateFlow<DocItem?>(null)
    val activeDoc: StateFlow<DocItem?> = _activeDoc.asStateFlow()

    // Search and filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow("All PDFs")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    // Language and Theme
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // UI feedback
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Camera action dialog
    private val _showCameraActionSheet = MutableStateFlow(false)
    val showCameraActionSheet: StateFlow<Boolean> = _showCameraActionSheet.asStateFlow()

    // Scanner state
    private val _scannedPages = MutableStateFlow<List<ScanPage>>(emptyList())
    val scannedPages: StateFlow<List<ScanPage>> = _scannedPages.asStateFlow()

    private val _scannerFilter = MutableStateFlow(ScanFilter.ENHANCED)
    val scannerFilter: StateFlow<ScanFilter> = _scannerFilter.asStateFlow()

    // Image to PDF state
    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    private val _pageFormat = MutableStateFlow(PageFormat.A4)
    val pageFormat: StateFlow<PageFormat> = _pageFormat.asStateFlow()

    private val _docOrientation = MutableStateFlow(DocOrientation.PORTRAIT)
    val docOrientation: StateFlow<DocOrientation> = _docOrientation.asStateFlow()

    private val _imageFit = MutableStateFlow(ImageFit.FIT_PAGE)
    val imageFit: StateFlow<ImageFit> = _imageFit.asStateFlow()

    private val _marginOption = MutableStateFlow(MarginOption.NONE)
    val marginOption: StateFlow<MarginOption> = _marginOption.asStateFlow()

    // Image to PPT state
    private val _pptSlides = MutableStateFlow<List<PptSlideItem>>(emptyList())
    val pptSlides: StateFlow<List<PptSlideItem>> = _pptSlides.asStateFlow()

    private val _pptAspect = MutableStateFlow(PptAspect.WIDESCREEN_16_9)
    val pptAspect: StateFlow<PptAspect> = _pptAspect.asStateFlow()

    // Canvas Document Editor state
    private val _editorDocTitle = MutableStateFlow("Untitled Document")
    val editorDocTitle: StateFlow<String> = _editorDocTitle.asStateFlow()

    private val _editorBlocks = MutableStateFlow<List<DocBlock>>(listOf(
        DocBlock(type = DocBlockType.TITLE, text = "Document Title"),
        DocBlock(type = DocBlockType.PARAGRAPH, text = "Start typing your content here...")
    ))
    val editorBlocks: StateFlow<List<DocBlock>> = _editorBlocks.asStateFlow()

    // Merger state
    private val _selectedMergeDocs = MutableStateFlow<List<DocItem>>(emptyList())
    val selectedMergeDocs: StateFlow<List<DocItem>> = _selectedMergeDocs.asStateFlow()

    // Office parser state
    private val _parsedOfficeResult = MutableStateFlow<OfficeParsedResult?>(null)
    val parsedOfficeResult: StateFlow<OfficeParsedResult?> = _parsedOfficeResult.asStateFlow()

    // Storage info
    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes: StateFlow<Long> = _storageBytes.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedStarterDocumentsIfNeeded()
            updateStorageInfo()
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (_currentSubScreen.value != AppSubScreen.NONE) {
            _currentSubScreen.value = AppSubScreen.NONE
            subScreenStack.clear()
        }
    }

    fun navigateToSubScreen(screen: AppSubScreen, doc: DocItem? = null) {
        doc?.let { _activeDoc.value = it }
        subScreenStack.add(_currentSubScreen.value)
        _currentSubScreen.value = screen

        if (screen == AppSubScreen.OFFICE_VIEWER && doc != null) {
            loadOfficeDocument(doc)
        }
    }

    fun navigateBack(): Boolean {
        if (_currentSubScreen.value != AppSubScreen.NONE) {
            if (subScreenStack.isNotEmpty()) {
                val previous = subScreenStack.removeAt(subScreenStack.lastIndex)
                _currentSubScreen.value = previous
            } else {
                _currentSubScreen.value = AppSubScreen.NONE
            }
            return true
        }
        return false
    }

    fun setLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        Strings.setLanguage(lang)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun setCameraActionSheetVisible(visible: Boolean) {
        _showCameraActionSheet.value = visible
    }

    // --- Device File Library & SAF ---

    fun refreshDeviceDocuments() {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.refreshDeviceDocuments()
            updateStorageInfo()
            _isProcessing.value = false
            showSnackbar(if (Strings.isHindi()) "दस्तावेज़ सूची ताज़ा की गई" else "Document library refreshed")
        }
    }

    fun addGrantedFolder(treeUri: Uri, displayName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.addGrantedFolder(treeUri, displayName)
            _isProcessing.value = false
            showSnackbar(if (Strings.isHindi()) "'$displayName' फ़ोल्डर जोड़ा गया" else "Folder '$displayName' added")
        }
    }

    fun importSafDocument(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val doc = repository.importSafDocument(uri)
            _isProcessing.value = false
            if (doc != null) {
                showSnackbar(if (Strings.isHindi()) "${doc.name} आयात किया गया" else "Imported ${doc.name}")
                // Auto route based on format
                when (doc.category) {
                    "pdf" -> navigateToSubScreen(AppSubScreen.PDF_READER, doc)
                    "office", "word", "ppt", "excel" -> navigateToSubScreen(AppSubScreen.OFFICE_VIEWER, doc)
                    else -> navigateToSubScreen(AppSubScreen.PDF_READER, doc)
                }
            } else {
                showSnackbar("Failed to import file")
            }
        }
    }

    fun clearActiveDoc() {
        _activeDoc.value = null
    }

    fun setActiveDoc(doc: DocItem?) {
        _activeDoc.value = doc
    }

    fun importSafPdf(uri: Uri, onSuccess: ((DocItem) -> Unit)? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            val doc = repository.importSafDocument(uri)
            _isProcessing.value = false
            if (doc != null && doc.category in listOf("pdf", "scan")) {
                _activeDoc.value = doc
                showSnackbar(if (Strings.isHindi()) "${doc.name} चयनित किया गया" else "Selected ${doc.name}")
                onSuccess?.invoke(doc)
            } else {
                showSnackbar(if (Strings.isHindi()) "अमान्य या दूषित PDF दस्तावेज़" else "Invalid or unreadable PDF document")
            }
        }
    }

    fun importSafOffice(uri: Uri, onSuccess: ((DocItem) -> Unit)? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            val doc = repository.importSafDocument(uri)
            _isProcessing.value = false
            if (doc != null) {
                _activeDoc.value = doc
                loadOfficeDocument(doc)
                showSnackbar(if (Strings.isHindi()) "${doc.name} चयनित किया गया" else "Selected ${doc.name}")
                onSuccess?.invoke(doc)
            } else {
                showSnackbar(if (Strings.isHindi()) "अमान्य या अपठनीय ऑफिस फ़ाइल" else "Invalid or unreadable office document")
            }
        }
    }

    fun clearImageUris() {
        _selectedImageUris.value = emptyList()
    }

    fun clearPptSlides() {
        _pptSlides.value = emptyList()
    }

    fun resetCanvasEditor() {
        _editorDocTitle.value = "New Document"
        _editorBlocks.value = emptyList()
    }

    fun clearSelectedMergeDocs() {
        _selectedMergeDocs.value = emptyList()
    }

    fun toggleFavorite(doc: DocItem) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id, !doc.isFavorite)
        }
    }

    fun renameDocument(doc: DocItem, newName: String) {
        viewModelScope.launch {
            val success = repository.renameDocument(doc.id, newName)
            if (success) {
                showSnackbar(if (Strings.isHindi()) "नाम बदला गया" else "Document renamed")
                updateStorageInfo()
            }
        }
    }

    fun deleteDocument(doc: DocItem) {
        viewModelScope.launch {
            val success = repository.deleteDocument(doc.id)
            if (success) {
                showSnackbar(if (Strings.isHindi()) "दस्तावेज़ हटाया गया" else "Document deleted")
                if (_activeDoc.value?.id == doc.id) {
                    _activeDoc.value = null
                    navigateBack()
                }
                updateStorageInfo()
            }
        }
    }

    fun printDocument(doc: DocItem) {
        val file = File(doc.filePath)
        if (file.exists() && file.extension.equals("pdf", ignoreCase = true)) {
            PdfPrintAdapter.printPdf(getApplication(), file)
        } else {
            showSnackbar(if (Strings.isHindi()) "केवल PDF फ़ाइलें सीधे प्रिंट की जा सकती हैं" else "Only PDF files can be printed directly")
        }
    }

    fun getShareUri(doc: DocItem): Uri {
        return doc.uriString?.let { Uri.parse(it) } ?: repository.getShareUri(File(doc.filePath))
    }

    suspend fun saveDocument(file: File, category: String = "pdf"): DocItem {
        return repository.saveDocument(file, category)
    }

    // --- Scanner Operations ---

    fun setScannerFilter(filter: ScanFilter) {
        _scannerFilter.value = filter
    }

    fun addScannedPage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val scansDir = repository.getScansDir()
            val filename = "scan_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val file = File(scansDir, filename)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            val page = ScanPage(
                imagePath = file.absolutePath,
                filterType = _scannerFilter.value
            )
            _scannedPages.value = _scannedPages.value + page
            showSnackbar(Strings.pagesScanned(_scannedPages.value.size))
        }
    }

    fun removeScannedPage(pageId: String) {
        _scannedPages.value = _scannedPages.value.filter { it.id != pageId }
    }

    fun reorderScannedPages(fromIndex: Int, toIndex: Int) {
        val list = _scannedPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _scannedPages.value = list
        }
    }

    fun rotateScannedPage(pageId: String) {
        val list = _scannedPages.value.map { page ->
            if (page.id == pageId) {
                page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360, timestamp = System.currentTimeMillis())
            } else page
        }
        _scannedPages.value = list
    }

    fun updateScannedPage(pageId: String, newBitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val page = _scannedPages.value.firstOrNull { it.id == pageId } ?: return@launch
            val file = File(page.imagePath)
            FileOutputStream(file).use { fos ->
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            _scannedPages.value = _scannedPages.value.map {
                if (it.id == pageId) it.copy(timestamp = System.currentTimeMillis()) else it
            }
        }
    }

    fun clearScannedPages() {
        _scannedPages.value = emptyList()
    }

    fun finishScanningAndCreatePdf(pdfName: String? = null) {
        val pages = _scannedPages.value
        if (pages.isEmpty()) {
            showSnackbar(if (Strings.isHindi()) "कोई स्कैन किया पृष्ठ नहीं है" else "No pages scanned yet")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val name = pdfName?.takeIf { it.isNotBlank() }
                ?: "Scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val safeName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
            val outFile = File(repository.getDocumentsDir(), safeName)

            withContext(Dispatchers.IO) {
                val bitmaps = pages.mapNotNull { scanPage ->
                    val raw = BitmapFactory.decodeFile(scanPage.imagePath) ?: return@mapNotNull null
                    val rotated = if (scanPage.rotationDegrees != 0) PdfEngine.rotateBitmap(raw, scanPage.rotationDegrees) else raw
                    val filtered = PdfEngine.applyFilterToBitmap(rotated, scanPage.filterType)
                    if (rotated != raw) rotated.recycle()
                    if (filtered != rotated && rotated != raw) raw.recycle()
                    filtered
                }

                val res = PdfEngine.createPdfFromBitmaps(
                    bitmaps = bitmaps,
                    outputFile = outFile,
                    format = PageFormat.A4,
                    orientation = DocOrientation.PORTRAIT,
                    imageFit = ImageFit.FIT_PAGE,
                    margin = MarginOption.NONE,
                    pageNumberConfig = PageNumberConfig(enabled = true, position = PageNumPosition.BOTTOM_CENTER)
                )

                bitmaps.forEach { it.recycle() }

                if (res.isSuccess) {
                    val savedDoc = repository.saveDocument(outFile, "scan", "Scanned")
                    _activeDoc.value = savedDoc
                    clearScannedPages()
                    updateStorageInfo()
                    withContext(Dispatchers.Main) {
                        showSnackbar(if (Strings.isHindi()) "PDF सफलतापूर्वक बनाया गया!" else "PDF created successfully!")
                        navigateToSubScreen(AppSubScreen.PDF_READER, savedDoc)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Error: ${res.exceptionOrNull()?.localizedMessage}")
                    }
                }
            }
            _isProcessing.value = false
        }
    }

    // --- Image to PPT Operations ---

    fun setPptAspect(aspect: PptAspect) {
        _pptAspect.value = aspect
    }

    fun addPptSlideImages(uris: List<Uri>) {
        val currentSize = _pptSlides.value.size
        val newSlides = uris.mapIndexed { idx, uri ->
            PptSlideItem(
                imageUri = uri.toString(),
                title = "Slide ${currentSize + idx + 1}",
                subtitle = "Camera Scan Presentation",
                layout = PptLayout.IMAGE_WITH_TITLE
            )
        }
        _pptSlides.value = _pptSlides.value + newSlides
    }

    fun removePptSlide(index: Int) {
        val list = _pptSlides.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            // Renumber
            list.forEachIndexed { i, s -> s.title = "Slide ${i + 1}" }
            _pptSlides.value = list
        }
    }

    fun reorderPptSlides(from: Int, to: Int) {
        val list = _pptSlides.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            list.forEachIndexed { i, s -> s.title = "Slide ${i + 1}" }
            _pptSlides.value = list
        }
    }

    fun updateSlideLayout(index: Int, layout: PptLayout) {
        val list = _pptSlides.value.toMutableList()
        if (index in list.indices) {
            list[index].layout = layout
            _pptSlides.value = list
        }
    }

    fun generatePresentation(title: String) {
        val slides = _pptSlides.value
        if (slides.isEmpty()) {
            showSnackbar("Please add images for slides first")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val safeName = if (title.endsWith(".pptx", ignoreCase = true)) title else "$title.pptx"
            val outFile = File(repository.getDocumentsDir(), safeName)

            val res = PptxEngine.createPresentation(
                context = getApplication(),
                slides = slides,
                aspect = _pptAspect.value,
                outputFile = outFile
            )

            if (res.isSuccess) {
                val saved = repository.saveDocument(outFile, "ppt", "Created")
                _pptSlides.value = emptyList()
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "PowerPoint प्रस्तुति तैयार की गई!" else "PowerPoint presentation created!")
                navigateToSubScreen(AppSubScreen.OFFICE_VIEWER, saved)
            } else {
                showSnackbar("Failed to create PPTX: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    // --- Canvas Document Editor Operations ---

    fun setEditorDocTitle(title: String) {
        _editorDocTitle.value = title
    }

    fun loadTemplate(template: DocTemplate) {
        _editorDocTitle.value = template.title
        _editorBlocks.value = template.blocks.map { it.copy(id = UUID.randomUUID().toString()) }
        navigateToSubScreen(AppSubScreen.CANVAS_EDITOR)
    }

    fun addEditorBlock(type: DocBlockType, text: String = "", table: List<List<String>>? = null) {
        val newBlock = DocBlock(
            type = type,
            text = text,
            tableData = table
        )
        _editorBlocks.value = _editorBlocks.value + newBlock
    }

    fun updateEditorBlockText(index: Int, text: String) {
        val list = _editorBlocks.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(text = text)
            _editorBlocks.value = list
        }
    }

    fun updateEditorBlockType(index: Int, type: DocBlockType) {
        val list = _editorBlocks.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(type = type)
            _editorBlocks.value = list
        }
    }

    fun toggleEditorBlockBold(index: Int) {
        val list = _editorBlocks.value.toMutableList()
        if (index in list.indices) {
            val cur = list[index].formatting
            list[index] = list[index].copy(formatting = cur.copy(isBold = !cur.isBold))
            _editorBlocks.value = list
        }
    }

    fun toggleEditorBlockItalic(index: Int) {
        val list = _editorBlocks.value.toMutableList()
        if (index in list.indices) {
            val cur = list[index].formatting
            list[index] = list[index].copy(formatting = cur.copy(isItalic = !cur.isItalic))
            _editorBlocks.value = list
        }
    }

    fun setEditorBlockAlign(index: Int, align: String) {
        val list = _editorBlocks.value.toMutableList()
        if (index in list.indices) {
            val cur = list[index].formatting
            list[index] = list[index].copy(formatting = cur.copy(alignment = align))
            _editorBlocks.value = list
        }
    }

    fun removeEditorBlock(index: Int) {
        val list = _editorBlocks.value.toMutableList()
        if (index in list.indices && list.size > 1) {
            list.removeAt(index)
            _editorBlocks.value = list
        }
    }

    fun addRowToTable(blockIndex: Int) {
        val list = _editorBlocks.value.toMutableList()
        if (blockIndex in list.indices) {
            val block = list[blockIndex]
            val table = block.tableData?.toMutableList() ?: mutableListOf(listOf("Header 1", "Header 2"))
            val colCount = table.firstOrNull()?.size ?: 2
            val newRow = List(colCount) { "Cell" }
            table.add(newRow)
            list[blockIndex] = block.copy(tableData = table)
            _editorBlocks.value = list
        }
    }

    fun addColToTable(blockIndex: Int) {
        val list = _editorBlocks.value.toMutableList()
        if (blockIndex in list.indices) {
            val block = list[blockIndex]
            val table = block.tableData?.map { row -> row + "Cell" } ?: listOf(listOf("Col 1", "Col 2"))
            list[blockIndex] = block.copy(tableData = table)
            _editorBlocks.value = list
        }
    }

    fun updateTableCell(blockIndex: Int, row: Int, col: Int, text: String) {
        val list = _editorBlocks.value.toMutableList()
        if (blockIndex in list.indices) {
            val block = list[blockIndex]
            val table = block.tableData?.mapIndexed { rIdx, rList ->
                if (rIdx == row) {
                    rList.mapIndexed { cIdx, cell -> if (cIdx == col) text else cell }
                } else rList
            }
            list[blockIndex] = block.copy(tableData = table)
            _editorBlocks.value = list
        }
    }

    fun exportEditorToPdf() {
        viewModelScope.launch {
            _isProcessing.value = true
            val title = _editorDocTitle.value
            val res = repository.exportEditorDocumentToPdf(title, _editorBlocks.value)
            _isProcessing.value = false
            if (res.isSuccess) {
                val file = res.getOrThrow()
                val doc = repository.saveDocument(file, "pdf", "Created")
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "PDF में निर्यात किया गया!" else "Exported to PDF successfully!")
                navigateToSubScreen(AppSubScreen.PDF_READER, doc)
            } else {
                showSnackbar("Export failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun exportEditorToWord() {
        viewModelScope.launch {
            _isProcessing.value = true
            val title = _editorDocTitle.value
            val res = repository.exportEditorDocumentToWord(title, _editorBlocks.value)
            _isProcessing.value = false
            if (res.isSuccess) {
                val file = res.getOrThrow()
                val doc = repository.saveDocument(file, "word", "Created")
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "Word (DOCX) में निर्यात किया गया!" else "Exported to Word (DOCX) successfully!")
                navigateToSubScreen(AppSubScreen.OFFICE_VIEWER, doc)
            } else {
                showSnackbar("Export failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // --- Image to PDF Operations ---

    fun setImageUris(uris: List<Uri>) { _selectedImageUris.value = uris }
    fun addImageUris(newUris: List<Uri>) { _selectedImageUris.value = _selectedImageUris.value + newUris }
    fun removeImageUri(index: Int) {
        val list = _selectedImageUris.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _selectedImageUris.value = list
        }
    }
    fun reorderImages(fromIndex: Int, toIndex: Int) {
        val list = _selectedImageUris.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _selectedImageUris.value = list
        }
    }
    fun setPageFormat(format: PageFormat) { _pageFormat.value = format }
    fun setDocOrientation(ori: DocOrientation) { _docOrientation.value = ori }
    fun setImageFit(fit: ImageFit) { _imageFit.value = fit }
    fun setMarginOption(margin: MarginOption) { _marginOption.value = margin }

    fun generatePdfFromImages(name: String, watermark: String? = null) {
        val uris = _selectedImageUris.value
        if (uris.isEmpty()) {
            showSnackbar(if (Strings.isHindi()) "कृपया पहले इमेज चुनें" else "Please select images first")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val safeName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
            val outFile = File(repository.getDocumentsDir(), safeName)

            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val bitmaps = uris.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                val wmConfig = watermark?.takeIf { it.isNotBlank() }?.let { WatermarkConfig(text = it) }

                val res = PdfEngine.createPdfFromBitmaps(
                    bitmaps = bitmaps,
                    outputFile = outFile,
                    format = _pageFormat.value,
                    orientation = _docOrientation.value,
                    imageFit = _imageFit.value,
                    margin = _marginOption.value,
                    watermarkConfig = wmConfig,
                    pageNumberConfig = PageNumberConfig(enabled = true)
                )

                bitmaps.forEach { it.recycle() }

                if (res.isSuccess) {
                    val saved = repository.saveDocument(outFile, "pdf", "Created")
                    _selectedImageUris.value = emptyList()
                    updateStorageInfo()
                    withContext(Dispatchers.Main) {
                        showSnackbar(if (Strings.isHindi()) "PDF सफलतापूर्वक बनाया गया!" else "PDF generated successfully!")
                        navigateToSubScreen(AppSubScreen.PDF_READER, saved)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Failed: ${res.exceptionOrNull()?.localizedMessage}")
                    }
                }
            }
            _isProcessing.value = false
        }
    }

    // --- Merger Operations ---

    fun toggleMergeDoc(doc: DocItem) {
        val list = _selectedMergeDocs.value.toMutableList()
        val existing = list.indexOfFirst { it.id == doc.id }
        if (existing >= 0) {
            list.removeAt(existing)
        } else {
            list.add(doc)
        }
        _selectedMergeDocs.value = list
    }

    fun reorderMergeDocs(from: Int, to: Int) {
        val list = _selectedMergeDocs.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _selectedMergeDocs.value = list
        }
    }

    fun removeMergeDoc(docId: Long) {
        _selectedMergeDocs.value = _selectedMergeDocs.value.filter { it.id != docId }
    }

    fun importAndSelectForMerge(uris: List<Uri>) {
        viewModelScope.launch {
            _isProcessing.value = true
            for (uri in uris) {
                val doc = repository.importSafDocument(uri)
                if (doc != null && !_selectedMergeDocs.value.any { it.id == doc.id }) {
                    _selectedMergeDocs.value = _selectedMergeDocs.value + doc
                }
            }
            _isProcessing.value = false
        }
    }

    fun mergeSelectedPdfs(outputName: String) {
        val docs = _selectedMergeDocs.value
        if (docs.size < 2) {
            showSnackbar(if (Strings.isHindi()) "कम से कम 2 PDF चुनें" else "Select at least 2 PDFs to merge")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val safeName = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
            val outFile = File(repository.getDocumentsDir(), safeName)
            val files = docs.map { File(it.filePath) }

            val res = PdfEngine.mergePdfs(files, outFile)
            if (res.isSuccess) {
                val mergedDoc = repository.saveDocument(outFile, "pdf", "Created")
                _selectedMergeDocs.value = emptyList()
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "PDF सफलतापूर्वक मर्ज किया गया!" else "PDFs merged successfully!")
                navigateToSubScreen(AppSubScreen.PDF_READER, mergedDoc)
            } else {
                showSnackbar("Merge failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    // --- Splitter & Compressor ---

    fun splitPdfPages(doc: DocItem, selectedPages: List<Int>, outputName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val safeName = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
            val outFile = File(repository.getDocumentsDir(), safeName)

            val res = PdfEngine.extractPages(File(doc.filePath), selectedPages, outFile)
            if (res.isSuccess) {
                val splitDoc = repository.saveDocument(outFile, "pdf", "Created")
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "पेज अलग कर लिए गए!" else "Pages extracted successfully!")
                navigateToSubScreen(AppSubScreen.PDF_READER, splitDoc)
            } else {
                showSnackbar("Split failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    fun compressDocument(doc: DocItem, level: CompressionLevel) {
        viewModelScope.launch {
            _isProcessing.value = true
            val baseName = doc.name.substringBeforeLast(".")
            val outFile = File(repository.getDocumentsDir(), "${baseName}_compressed_${level.name.lowercase().take(5)}.pdf")

            val res = PdfEngine.compressPdf(File(doc.filePath), outFile, level)
            if (res.isSuccess) {
                val (orig, comp) = res.getOrThrow()
                val savedBytes = (orig - comp).coerceAtLeast(0)
                val pct = if (orig > 0) ((savedBytes.toFloat() / orig) * 100).toInt() else 0
                val compressedDoc = repository.saveDocument(outFile, "pdf", "Created")
                updateStorageInfo()
                showSnackbar(Strings.savedPercentage(pct))
                navigateToSubScreen(AppSubScreen.PDF_READER, compressedDoc)
            } else {
                showSnackbar("Compression failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    fun convertPdfToImages(doc: DocItem, isPng: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            val outDir = File(repository.getDocumentsDir(), "${doc.name.substringBeforeLast(".")}_images")
            val res = PdfEngine.convertPdfToImages(File(doc.filePath), outDir, isPng)
            if (res.isSuccess) {
                val files = res.getOrThrow()
                files.forEach { repository.saveDocument(it, "image", "Created") }
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "${files.size} इमेज सुरक्षित की गईं" else "${files.size} images extracted")
                selectTab(AppTab.DOCUMENTS)
            } else {
                showSnackbar("Conversion failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    // --- Office Viewer ---

    fun loadOfficeDocument(doc: DocItem) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = OfficeParser.parseFile(File(doc.filePath))
            _parsedOfficeResult.value = result
            _isProcessing.value = false
        }
    }

    fun exportOfficeToPdf(result: OfficeParsedResult) {
        viewModelScope.launch {
            _isProcessing.value = true
            val safeName = "${result.title.substringBeforeLast(".")}_export.pdf"
            val outFile = File(repository.getDocumentsDir(), safeName)

            val content = when (result.type) {
                "Excel", "CSV" -> result.tableRows.joinToString("\n") { it.joinToString("  |  ") }
                "PowerPoint" -> result.slides.mapIndexed { i, s -> "Slide ${i + 1}:\n$s" }.joinToString("\n\n")
                else -> result.paragraphs.joinToString("\n\n")
            }

            val res = PdfEngine.createTextPdf(result.title, content, outFile, PageFormat.A4)
            if (res.isSuccess) {
                val saved = repository.saveDocument(outFile, "pdf", "Created")
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "Office दस्तावेज़ PDF में बदला गया" else "Office document converted to PDF")
                navigateToSubScreen(AppSubScreen.PDF_READER, saved)
            } else {
                showSnackbar("Export failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    fun createTextDocument(title: String, content: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val safeName = "${title.trim().replace(" ", "_")}_${System.currentTimeMillis() % 10000}.pdf"
            val outFile = File(repository.getDocumentsDir(), safeName)

            val res = PdfEngine.createTextPdf(title, content, outFile, _pageFormat.value)
            if (res.isSuccess) {
                val doc = repository.saveDocument(outFile, "pdf", "Created")
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "PDF बनाया गया" else "PDF Created")
                navigateToSubScreen(AppSubScreen.PDF_READER, doc)
            } else {
                showSnackbar("Error: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    fun applyWatermarkAndPageNumbers(doc: DocItem, watermark: String, pageNumbers: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            val file = File(doc.filePath)
            val pageCount = PdfEngine.getPageCount(file)
            val bitmaps = mutableListOf<Bitmap>()

            for (i in 0 until pageCount) {
                PdfEngine.renderPageToBitmap(file, i, 1200)?.let { bitmaps.add(it) }
            }

            val baseName = doc.name.substringBeforeLast(".")
            val outFile = File(repository.getDocumentsDir(), "${baseName}_stamped.pdf")

            val wmConfig = watermark.takeIf { it.isNotBlank() }?.let { WatermarkConfig(text = it) }
            val pnConfig = if (pageNumbers) PageNumberConfig(enabled = true) else null

            val res = PdfEngine.createPdfFromBitmaps(
                bitmaps = bitmaps,
                outputFile = outFile,
                format = PageFormat.A4,
                watermarkConfig = wmConfig,
                pageNumberConfig = pnConfig
            )

            bitmaps.forEach { it.recycle() }

            if (res.isSuccess) {
                val stampedDoc = repository.saveDocument(outFile, "pdf", "Created")
                updateStorageInfo()
                showSnackbar(if (Strings.isHindi()) "वॉटरमार्क और पेज नंबर लागू किए गए" else "Watermark & page numbers applied")
                navigateToSubScreen(AppSubScreen.PDF_READER, stampedDoc)
            } else {
                showSnackbar("Failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
            _isProcessing.value = false
        }
    }

    fun updateStorageInfo() {
        viewModelScope.launch {
            _storageBytes.value = repository.getStorageUsage()
        }
    }

    fun clearTempCache() {
        viewModelScope.launch {
            val freed = repository.clearCache()
            updateStorageInfo()
            val mb = String.format(Locale.US, "%.1f MB", freed / (1024f * 1024f))
            showSnackbar(if (Strings.isHindi()) "$mb कैश साफ़ किया गया" else "Cleared $mb cache")
        }
    }
}
