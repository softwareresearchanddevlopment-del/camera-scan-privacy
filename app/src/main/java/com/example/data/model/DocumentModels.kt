package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "documents")
data class DocItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val pageCount: Int = 1,
    val modifiedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val category: String = "pdf", // pdf, scan, image, word, ppt, excel, text, created, download
    val storageLocation: String = "Device", // Downloads, Documents, Created, Scans, SAF Folder, Device
    val uriString: String? = null,
    val thumbnailPath: String? = null
)

@Entity(tableName = "granted_folders")
data class GrantedFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "editor_documents")
data class EditorDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val blocksJson: String,
    val format: String = "A4",
    val orientation: String = "PORTRAIT",
    val lastModified: Long = System.currentTimeMillis()
)

enum class PageFormat(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    A3("A3", 842, 1191),
    A5("A5", 420, 595),
    LETTER("US Letter", 612, 792),
    LEGAL("US Legal", 612, 1008),
    ORIGINAL("Original Size", 0, 0)
}

enum class DocOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class ImageFit {
    FIT_PAGE,
    FILL_PAGE,
    ORIGINAL_RATIO
}

enum class MarginOption(val displayName: String, val paddingPt: Int) {
    NONE("None", 0),
    SMALL("Small (16pt)", 16),
    NORMAL("Normal (36pt)", 36)
}

enum class CompressionLevel(val displayName: String, val jpegQuality: Int, val scaleFactor: Float) {
    HIGH_QUALITY("High Quality (Mild)", 88, 1.0f),
    BALANCED("Balanced (Recommended)", 65, 0.85f),
    SMALL_SIZE("Small Size (Maximum)", 40, 0.65f)
}

enum class ScanFilter(val displayName: String) {
    ORIGINAL("Original"),
    COLOR("Color Boost"),
    GRAYSCALE("Grayscale"),
    BLACK_WHITE("B&W Clean"),
    ENHANCED("Enhanced Document"),
    HIGH_CONTRAST("High Contrast")
}

data class ScanPage(
    val id: String = UUID.randomUUID().toString(),
    val imagePath: String,
    var rotationDegrees: Int = 0,
    var filterType: ScanFilter = ScanFilter.ENHANCED,
    var cropNormalizedLeft: Float = 0f,
    var cropNormalizedTop: Float = 0f,
    var cropNormalizedRight: Float = 1f,
    var cropNormalizedBottom: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PageNumPosition {
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_CENTER
}

data class WatermarkConfig(
    val text: String = "CONFIDENTIAL",
    val opacity: Float = 0.25f,
    val fontSize: Float = 42f,
    val rotationDegrees: Float = -45f,
    val colorHex: String = "#808080"
)

data class PageNumberConfig(
    val enabled: Boolean = true,
    val position: PageNumPosition = PageNumPosition.BOTTOM_CENTER,
    val startNumber: Int = 1,
    val format: String = "Page %d",
    val fontSize: Float = 12f
)

// --- Canvas Document Editor Models ---

enum class DocBlockType(val displayName: String) {
    TITLE("Title"),
    SUBTITLE("Subtitle"),
    HEADING_1("Heading 1"),
    HEADING_2("Heading 2"),
    HEADING_3("Heading 3"),
    PARAGRAPH("Normal Text"),
    QUOTE("Quote"),
    CAPTION("Caption"),
    CODE_BLOCK("Code Block"),
    BULLET_LIST_ITEM("Bulleted List"),
    NUMBERED_LIST_ITEM("Numbered List"),
    IMAGE("Image"),
    TABLE("Table"),
    PAGE_BREAK("Page Break")
}

data class TextFormatting(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val fontSizeSp: Float = 16f,
    val textColorHex: String = "#1C1C1E",
    val highlightColorHex: String? = null,
    val alignment: String = "LEFT" // LEFT, CENTER, RIGHT
)

data class DocBlock(
    val id: String = UUID.randomUUID().toString(),
    var type: DocBlockType = DocBlockType.PARAGRAPH,
    var text: String = "",
    var formatting: TextFormatting = TextFormatting(),
    var imageUri: String? = null,
    var tableData: List<List<String>>? = null
)

// --- PowerPoint Presentation Models ---

enum class PptLayout(val displayName: String) {
    FULL_IMAGE("Full Image"),
    IMAGE_WITH_TITLE("Image with Title"),
    IMAGE_WITH_SUBTITLE("Image with Subtitle"),
    IMAGE_WITH_TEXT("Image with Text"),
    BLANK("Blank Slide")
}

enum class PptAspect(val displayName: String, val widthPt: Int, val heightPt: Int) {
    WIDESCREEN_16_9("16:9 Widescreen", 960, 540),
    STANDARD_4_3("4:3 Standard", 720, 540)
}

data class PptSlideItem(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    var title: String = "",
    var subtitle: String = "",
    var layout: PptLayout = PptLayout.IMAGE_WITH_TITLE
)

data class DocTemplate(
    val id: String,
    val title: String,
    val category: String, // Business, Education, Personal, Reports, Letters, Resumes
    val description: String,
    val blocks: List<DocBlock>
)
