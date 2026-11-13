package com.example.ui.util

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी")
}

object Strings {
    private var currentLanguage: AppLanguage = AppLanguage.ENGLISH

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun getLanguage(): AppLanguage = currentLanguage

    fun isHindi(): Boolean = currentLanguage == AppLanguage.HINDI

    // Common
    fun appName() = "Camera Scan: All Doc, PDF Tools"
    fun search() = if (isHindi()) "दस्तावेज़ खोजें..." else "Search documents..."
    fun cancel() = if (isHindi()) "रद्द करें" else "Cancel"
    fun save() = if (isHindi()) "सहेजें" else "Save"
    fun delete() = if (isHindi()) "हटाएं" else "Delete"
    fun share() = if (isHindi()) "शेयर करें" else "Share"
    fun print() = if (isHindi()) "प्रिंट करें" else "Print"
    fun rename() = if (isHindi()) "नाम बदलें" else "Rename"
    fun done() = if (isHindi()) "पूर्ण" else "Done"
    fun back() = if (isHindi()) "पीछे" else "Back"
    fun confirm() = if (isHindi()) "पुष्टि करें" else "Confirm"
    fun close() = if (isHindi()) "बंद करें" else "Close"
    fun apply() = if (isHindi()) "लागू करें" else "Apply"
    fun export() = if (isHindi()) "निर्यात करें" else "Export"
    fun preview() = if (isHindi()) "पूर्वावलोकन" else "Preview"
    fun copy() = if (isHindi()) "कॉपी करें" else "Copy"

    // Navigation
    fun tabHome() = if (isHindi()) "होम" else "Home"
    fun tabTools() = if (isHindi()) "टूल्स" else "Tools"
    fun tabFiles() = if (isHindi()) "फ़ाइलें" else "Files"
    fun tabSettings() = if (isHindi()) "सेटिंग्स" else "Settings"

    // Home
    fun quickActions() = if (isHindi()) "त्वरित क्रियाएं" else "Quick Actions"
    fun scanDocument() = if (isHindi()) "दस्तावेज़ स्कैन करें" else "Scan Document"
    fun scanDocumentSub() = if (isHindi()) "कैमरा से बहु-पृष्ठ स्कैन" else "Multi-page camera scan"
    fun imagesToPdf() = if (isHindi()) "फ़ोटो से PDF" else "Images to PDF"
    fun imagesToPdfSub() = if (isHindi()) "गैलरी से तुरंत बनाएं" else "Convert gallery photos"
    fun createPdf() = if (isHindi()) "नया PDF बनाएं" else "Create PDF"
    fun createPdfSub() = if (isHindi()) "रिक्त पृष्ठ या टेक्स्ट से" else "From text or canvas"
    fun mergePdf() = if (isHindi()) "PDF जोड़ें (मर्ज)" else "Merge PDF"
    fun mergePdfSub() = if (isHindi()) "अनेक PDF मिलाएं" else "Combine multiple files"

    fun allTools() = if (isHindi()) "सभी टूल्स" else "All Tools"
    fun recentDocuments() = if (isHindi()) "हाल के दस्तावेज़" else "Recent Documents"
    fun noRecentDocs() = if (isHindi()) "अभी तक कोई दस्तावेज़ नहीं" else "No recent documents yet"
    fun noRecentDocsSub() = if (isHindi()) "नया दस्तावेज़ स्कैन करें या PDF बनाएं" else "Scan a document or create your first PDF"

    // Tools list
    fun toolEditor() = if (isHindi()) "PDF संपादक" else "PDF Editor"
    fun toolEditorSub() = if (isHindi()) "ड्राइंग, टेक्स्ट और एनोटेशन" else "Draw, text & annotations"
    fun toolReader() = if (isHindi()) "PDF रीडर" else "PDF Reader"
    fun toolReaderSub() = if (isHindi()) "ज़ूम, सर्च और बुकमार्क" else "Zoom, search & bookmark"
    fun toolCompressor() = if (isHindi()) "PDF कंप्रेसर" else "PDF Compressor"
    fun toolCompressorSub() = if (isHindi()) "फ़ाइल साइज़ कम करें" else "Reduce file size cleanly"
    fun toolSplitter() = if (isHindi()) "PDF स्प्लिटर" else "PDF Splitter"
    fun toolSplitterSub() = if (isHindi()) "पेज अलग या एक्सट्रेक्ट करें" else "Extract or split pages"
    fun toolConverter() = if (isHindi()) "PDF कनवर्टर" else "PDF Converter"
    fun toolConverterSub() = if (isHindi()) "PDF से इमेज, वर्ड, आदि" else "PDF to JPG/PNG & formats"
    fun toolSignature() = if (isHindi()) "डिजिटल हस्ताक्षर" else "PDF Signature"
    fun toolSignatureSub() = if (isHindi()) "हस्ताक्षर बनाएं और जोड़ें" else "Sign documents digitally"
    fun toolOffice() = if (isHindi()) "ऑफिस व्यूअर" else "Office Viewer"
    fun toolOfficeSub() = if (isHindi()) "Word, Excel, PPT फ़ाइलें" else "Word, Excel & PPT viewer"
    fun toolWatermark() = if (isHindi()) "वॉटरमार्क और पेज नंबर" else "Watermark & Page No."
    fun toolWatermarkSub() = if (isHindi()) "सुरक्षा और नंबरिंग" else "Security & page numbers"
    fun toolOrganizer() = if (isHindi()) "पेज मैनेजर" else "Page Organizer"
    fun toolOrganizerSub() = if (isHindi()) "क्रम बदलें, घुमाएं, हटाएं" else "Reorder, rotate & delete"

    // Scanner
    fun scannerTitle() = if (isHindi()) "दस्तावेज़ स्कैनर" else "Document Scanner"
    fun capture() = if (isHindi()) "कैप्चर करें" else "Capture"
    fun pagesScanned(count: Int) = if (isHindi()) "$count पृष्ठ स्कैन किए" else "$count pages scanned"
    fun finishAndGenerate() = if (isHindi()) "पूर्ण और PDF बनाएं" else "Finish & Create PDF"
    fun addMorePages() = if (isHindi()) "और पृष्ठ जोड़ें" else "Add More Pages"
    fun retake() = if (isHindi()) "पुनः लें" else "Retake"
    fun filterOriginal() = if (isHindi()) "मूल" else "Original"
    fun filterColor() = if (isHindi()) "रंग" else "Color"
    fun filterGrayscale() = if (isHindi()) "ग्रेस्केल" else "Grayscale"
    fun filterBw() = if (isHindi()) "ब्लैक & व्हाइट" else "B&W"
    fun filterEnhanced() = if (isHindi()) "उन्नत (Enhanced)" else "Enhanced"
    fun filterHighContrast() = if (isHindi()) "हाई कंट्रास्ट" else "High Contrast"

    // Image to PDF
    fun selectImages() = if (isHindi()) "इमेज चुनें" else "Select Images"
    fun selectedCount(count: Int) = if (isHindi()) "$count चयनित" else "$count Selected"
    fun pageNumber(num: Int) = if (isHindi()) "पृष्ठ $num" else "Page $num"
    fun pdfSettings() = if (isHindi()) "PDF सेटिंग्स" else "PDF Settings"
    fun pageSize() = if (isHindi()) "पृष्ठ आकार" else "Page Size"
    fun orientation() = if (isHindi()) "दिशा (Orientation)" else "Orientation"
    fun portrait() = if (isHindi()) "पोर्ट्रेट" else "Portrait"
    fun landscape() = if (isHindi()) "लैंडस्केप" else "Landscape"
    fun imageFit() = if (isHindi()) "इमेज फ़िट" else "Image Fit"
    fun fitPage() = if (isHindi()) "पेज पर फ़िट करें" else "Fit to Page"
    fun fillPage() = if (isHindi()) "पेज भरें (Fill)" else "Fill Page"
    fun originalSize() = if (isHindi()) "मूल आकार" else "Original"
    fun margins() = if (isHindi()) "मार्जिन" else "Margins"
    fun marginNone() = if (isHindi()) "कोई नहीं" else "None"
    fun marginSmall() = if (isHindi()) "छोटा" else "Small"
    fun marginNormal() = if (isHindi()) "सामान्य" else "Normal"
    fun quality() = if (isHindi()) "गुणवत्ता" else "Quality"
    fun createPdfAction() = if (isHindi()) "PDF उत्पन्न करें" else "Generate PDF"

    // PDF Reader
    fun pageIndicator(current: Int, total: Int) = if (isHindi()) "पृष्ठ $current / $total" else "Page $current / $total"
    fun fitWidth() = if (isHindi()) "चौड़ाई अनुसार" else "Fit Width"
    fun fullScreen() = if (isHindi()) "फुल स्क्रीन" else "Full Screen"
    fun rotatePage() = if (isHindi()) "पेज घुमाएं" else "Rotate Page"
    fun docInfo() = if (isHindi()) "दस्तावेज़ जानकारी" else "Document Info"

    // PDF Editor
    fun drawPen() = if (isHindi()) "पेन" else "Pen"
    fun highlight() = if (isHindi()) "हाइलाइट" else "Highlight"
    fun textAnnotation() = if (isHindi()) "टेक्स्ट" else "Text"
    fun addSignature() = if (isHindi()) "हस्ताक्षर" else "Signature"
    fun addShape() = if (isHindi()) "आकार" else "Shape"
    fun undo() = if (isHindi()) "वापस (Undo)" else "Undo"
    fun redo() = if (isHindi()) "दोबारा (Redo)" else "Redo"
    fun strokeWidth() = if (isHindi()) "मोटाई" else "Width"
    fun strokeColor() = if (isHindi()) "रंग" else "Color"
    fun saveCopy() = if (isHindi()) "प्रति सहेजें" else "Save Copy"

    // Merger & Splitter
    fun addPdfs() = if (isHindi()) "PDF जोड़ें" else "Add PDFs"
    fun mergeFiles() = if (isHindi()) "मर्ज करें" else "Merge Now"
    fun splitPdf() = if (isHindi()) "PDF विभाजित करें" else "Split PDF"
    fun splitRange() = if (isHindi()) "रेंज अनुसार विभाजित करें" else "Split by Range"
    fun splitAllPages() = if (isHindi()) "सभी पृष्ठ अलग करें" else "Split Every Page"
    fun extractSelected() = if (isHindi()) "चयनित पृष्ठ निकालें" else "Extract Selected Pages"

    // Compressor
    fun compressPdf() = if (isHindi()) "PDF कंप्रेस करें" else "Compress PDF"
    fun highQuality() = if (isHindi()) "उच्च गुणवत्ता (हल्का)" else "High Quality (Mild)"
    fun balanced() = if (isHindi()) "संतुलित (सिफारिश)" else "Balanced (Recommended)"
    fun smallSize() = if (isHindi()) "छोटा आकार (अधिक)" else "Small Size (Maximum)"
    fun originalFileSize() = if (isHindi()) "मूल फ़ाइल आकार" else "Original File Size"
    fun compressedFileSize() = if (isHindi()) "कंप्रेस किया आकार" else "Compressed File Size"
    fun savedPercentage(pct: Int) = if (isHindi()) "$pct% जगह बचाई" else "$pct% space saved"

    // Common utility
    fun copiedToast() = if (isHindi()) "क्लिपबोर्ड पर कॉपी हो गया" else "Copied to clipboard"

    // Signature
    fun drawSignaturePrompt() = if (isHindi()) "यहाँ अपनी उंगली से हस्ताक्षर करें" else "Draw your signature here with finger"
    fun clearSignature() = if (isHindi()) "हटाएं" else "Clear"
    fun saveSignature() = if (isHindi()) "हस्ताक्षर सहेजें" else "Save Signature"
    fun placeSignature() = if (isHindi()) "PDF पर लगाएं" else "Place on PDF"

    // Watermark
    fun watermarkText() = if (isHindi()) "वॉटरमार्क टेक्स्ट" else "Watermark Text"
    fun watermarkOpacity() = if (isHindi()) "पारदर्शिता (Opacity)" else "Opacity"
    fun pageNumberPosition() = if (isHindi()) "पेज नंबर की स्थिति" else "Page Number Position"
    fun bottomCenter() = if (isHindi()) "नीचे बीच में" else "Bottom Center"
    fun bottomRight() = if (isHindi()) "नीचे दाईं ओर" else "Bottom Right"
    fun bottomLeft() = if (isHindi()) "नीचे बाईं ओर" else "Bottom Left"
    fun topCenter() = if (isHindi()) "ऊपर बीच में" else "Top Center"

    // Office
    fun officeTitle() = if (isHindi()) "ऑफिस दस्तावेज़ व्यूअर" else "Office Document Viewer"
    fun wordDocs() = if (isHindi()) "वर्ड (DOC/DOCX)" else "Word (DOC/DOCX)"
    fun excelSheets() = if (isHindi()) "एक्सेल (XLS/XLSX)" else "Excel (XLS/XLSX)"
    fun pptSlides() = if (isHindi()) "पावरपॉइंट (PPT/PPTX)" else "PowerPoint (PPT/PPTX)"
    fun csvFiles() = if (isHindi()) "CSV / टेक्स्ट" else "CSV / Text"
    fun openOfficeDoc() = if (isHindi()) "फ़ाइल खोलें" else "Open Office File"

    // File Manager
    fun allDocs() = if (isHindi()) "सभी दस्तावेज़" else "All Documents"
    fun pdfFilter() = if (isHindi()) "PDFs" else "PDFs"
    fun scansFilter() = if (isHindi()) "स्कैन" else "Scans"
    fun imagesFilter() = if (isHindi()) "इमेज" else "Images"
    fun officeFilter() = if (isHindi()) "ऑफिस" else "Office"
    fun favoritesFilter() = if (isHindi()) "पसंदीदा" else "Favorites"
    fun sortBy() = if (isHindi()) "क्रमबद्ध करें" else "Sort By"
    fun sortName() = if (isHindi()) "नाम" else "Name"
    fun sortDate() = if (isHindi()) "तारीख" else "Date"
    fun sortSize() = if (isHindi()) "आकार" else "Size"
    fun deleteConfirm(name: String) = if (isHindi()) "क्या आप '$name' को हटाना चाहते हैं?" else "Are you sure you want to delete '$name'?"

    // Settings
    fun settingsTitle() = if (isHindi()) "सेटिंग्स" else "Settings"
    fun appearance() = if (isHindi()) "दिखावट (Appearance)" else "Appearance"
    fun themeLight() = if (isHindi()) "लाइट थीम" else "Light Theme"
    fun themeDark() = if (isHindi()) "डार्क थीम" else "Dark Theme"
    fun themeSystem() = if (isHindi()) "सिस्टम डिफ़ॉल्ट" else "System Default"
    fun language() = if (isHindi()) "भाषा (Language)" else "Language"
    fun defaultSettings() = if (isHindi()) "डिफ़ॉल्ट सेटिंग्स" else "Defaults"
    fun storagePrivacy() = if (isHindi()) "स्टोरेज और गोपनीयता" else "Storage & Privacy"
    fun storageUsed() = if (isHindi()) "उपयोग की गई स्टोरेज" else "Storage Used"
    fun clearCache() = if (isHindi()) "कैश और टेम्पररी फ़ाइलें साफ़ करें" else "Clear Cache & Temp Files"
    fun privacyNote() = if (isHindi()) "100% सुरक्षित और ऑफलाइन: आपके दस्तावेज़ आपके फ़ोन पर ही रहते हैं।" else "100% Private & Offline: Your documents never leave your device."
    fun privacyPolicy() = if (isHindi()) "गोपनीयता नीति (Privacy Policy)" else "Privacy Policy"
    fun privacyPolicySub() = if (isHindi()) "कैमरा, स्टोरेज और AdMob डेटा जानकारी" else "Camera, storage & AdMob disclosure"
    fun aboutApp() = if (isHindi()) "Camera Scan के बारे में" else "About Camera Scan"
    fun appVersion() = "v2.0 (Pro Studio)"
}
