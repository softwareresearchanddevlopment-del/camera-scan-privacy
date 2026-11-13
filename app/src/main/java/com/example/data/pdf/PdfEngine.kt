package com.example.data.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object PdfEngine {

    /**
     * Creates a PDF from a list of bitmap images with flexible layout and styling.
     */
    suspend fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File,
        format: PageFormat = PageFormat.A4,
        orientation: DocOrientation = DocOrientation.PORTRAIT,
        imageFit: ImageFit = ImageFit.FIT_PAGE,
        margin: MarginOption = MarginOption.NONE,
        qualityPercent: Int = 90,
        pageNumberConfig: PageNumberConfig? = null,
        watermarkConfig: WatermarkConfig? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            val totalPages = bitmaps.size

            bitmaps.forEachIndexed { index, rawBmp ->
                val (pageWidth, pageHeight) = calculatePageDimensions(rawBmp, format, orientation)
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)

                val padding = margin.paddingPt.toFloat()
                val availableWidth = (pageWidth - 2 * padding).coerceAtLeast(10f)
                val availableHeight = (pageHeight - 2 * padding).coerceAtLeast(10f)

                // Scale & draw image
                val (destRect, srcRect) = calculateImageBounds(rawBmp, availableWidth, availableHeight, padding, imageFit)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(rawBmp, srcRect, destRect, paint)

                // Watermark
                watermarkConfig?.let { wm ->
                    drawWatermark(canvas, pageWidth.toFloat(), pageHeight.toFloat(), wm)
                }

                // Page Number
                pageNumberConfig?.let { pnc ->
                    if (pnc.enabled) {
                        drawPageNumber(canvas, pageWidth.toFloat(), pageHeight.toFloat(), index + pnc.startNumber, totalPages, pnc)
                    }
                }

                pdfDoc.finishPage(page)
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a PDF from text / notes.
     */
    suspend fun createTextPdf(
        title: String,
        content: String,
        outputFile: File,
        format: PageFormat = PageFormat.A4
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val (pageWidth, pageHeight) = calculatePageDimensions(null, format, DocOrientation.PORTRAIT)
            val pdfDoc = PdfDocument()

            val margin = 50f
            val contentWidth = pageWidth - 2 * margin
            val contentHeight = pageHeight - 2 * margin

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val lineSpacing = 19f
            val words = content.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (bodyPaint.measureText(testLine) <= contentWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine.toString())

            var pageNum = 1
            var lineIdx = 0
            val linesPerPage = ((contentHeight - 60f) / lineSpacing).toInt().coerceAtLeast(1)

            while (lineIdx < lines.size || lineIdx == 0) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                var yPos = margin + 30f

                if (pageNum == 1) {
                    canvas.drawText(title, margin, yPos, titlePaint)
                    yPos += 35f
                    // Decorative line
                    val linePaint = Paint().apply {
                        color = Color.parseColor("#007AFF")
                        strokeWidth = 2.5f
                    }
                    canvas.drawLine(margin, yPos, margin + 80f, yPos, linePaint)
                    yPos += 25f
                }

                val endIdx = min(lineIdx + linesPerPage, lines.size)
                for (i in lineIdx until endIdx) {
                    canvas.drawText(lines[i], margin, yPos, bodyPaint)
                    yPos += lineSpacing
                }
                lineIdx = endIdx

                // Footer page number
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GRAY
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("- Page $pageNum -", pageWidth / 2f, pageHeight - 30f, footerPaint)

                pdfDoc.finishPage(page)
                pageNum++
                if (lineIdx >= lines.size) break
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets page count of any PDF file safely.
     */
    fun getPageCount(file: File): Int {
        if (!file.exists() || file.length() == 0L) return 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            0
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Renders a specific PDF page to a Bitmap.
     */
    suspend fun renderPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        targetWidth: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val finalWidth = targetWidth.coerceAtLeast(300)
            val finalHeight = (finalWidth * aspectRatio).toInt().coerceAtLeast(300)

            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Merges multiple PDF files into one output file.
     */
    suspend fun mergePdfs(
        inputFiles: List<File>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            var globalPageNumber = 1

            for (inputFile in inputFiles) {
                if (!inputFile.exists() || inputFile.length() == 0L) continue
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)

                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, globalPageNumber).create()
                        val docPage = pdfDoc.startPage(pageInfo)

                        val canvas = docPage.canvas
                        canvas.drawColor(Color.WHITE)

                        // Render onto temp bitmap and draw
                        val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        val bmpCanvas = Canvas(bmp)
                        bmpCanvas.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        canvas.drawBitmap(bmp, 0f, 0f, null)
                        bmp.recycle()
                        page.close()

                        pdfDoc.finishPage(docPage)
                        globalPageNumber++
                    }
                } finally {
                    renderer?.close()
                    pfd?.close()
                }
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Splits a PDF file by extracting selected page numbers (0-indexed).
     */
    suspend fun extractPages(
        inputFile: File,
        pageIndices: List<Int>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            try {
                pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                var outPageNum = 1
                for (idx in pageIndices) {
                    if (idx < 0 || idx >= renderer.pageCount) continue
                    val page = renderer.openPage(idx)
                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, outPageNum).create()
                    val docPage = pdfDoc.startPage(pageInfo)

                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    val bmpCanvas = Canvas(bmp)
                    bmpCanvas.drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    docPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                    bmp.recycle()
                    page.close()

                    pdfDoc.finishPage(docPage)
                    outPageNum++
                }
            } finally {
                renderer?.close()
                pfd?.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compresses a PDF file by rendering pages and re-encoding at chosen JPEG quality/resolution.
     */
    suspend fun compressPdf(
        inputFile: File,
        outputFile: File,
        level: CompressionLevel
    ): Result<Pair<Long, Long>> = withContext(Dispatchers.IO) {
        try {
            val originalSize = inputFile.length()
            val pdfDoc = PdfDocument()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            try {
                pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val scale = level.scaleFactor
                    val targetW = (page.width * scale).toInt().coerceAtLeast(300)
                    val targetH = (page.height * scale).toInt().coerceAtLeast(300)

                    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                    val bmpCanvas = Canvas(bmp)
                    bmpCanvas.drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Compress to byte array
                    val bos = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, bos)
                    val compressedBytes = bos.toByteArray()
                    val compressedBmp = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                    bmp.recycle()

                    val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, i + 1).create()
                    val docPage = pdfDoc.startPage(pageInfo)
                    docPage.canvas.drawBitmap(compressedBmp, 0f, 0f, null)
                    compressedBmp.recycle()
                    pdfDoc.finishPage(docPage)
                }
            } finally {
                renderer?.close()
                pfd?.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()

            val compressedSize = outputFile.length()
            Result.success(Pair(originalSize, compressedSize))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts all pages of a PDF to image files (JPG or PNG).
     */
    suspend fun convertPdfToImages(
        inputFile: File,
        outputDir: File,
        isPng: Boolean = false
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val generatedFiles = mutableListOf<File>()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            if (!outputDir.exists()) outputDir.mkdirs()

            try {
                pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                val baseName = inputFile.nameWithoutExtension
                val ext = if (isPng) "png" else "jpg"

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val width = (page.width * 2).coerceIn(800, 2400)
                    val height = (page.height * 2).coerceIn(800, 3200)

                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val outFile = File(outputDir, "${baseName}_page_${i + 1}.$ext")
                    FileOutputStream(outFile).use { fos ->
                        if (isPng) {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        } else {
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                        }
                    }
                    bmp.recycle()
                    generatedFiles.add(outFile)
                }
            } finally {
                renderer?.close()
                pfd?.close()
            }

            Result.success(generatedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reorganize pages: reorder, rotate, or duplicate.
     */
    suspend fun reorganizePdf(
        inputFile: File,
        pageOrder: List<Int>, // 0-based page indices
        rotations: Map<Int, Int>, // page order index -> degrees to rotate
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            try {
                pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                pageOrder.forEachIndexed { newIndex, originalPageIndex ->
                    if (originalPageIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(originalPageIndex)
                        val rotation = rotations[newIndex] ?: 0

                        val isQuarterTurn = (rotation % 180 != 0)
                        val outWidth = if (isQuarterTurn) page.height else page.width
                        val outHeight = if (isQuarterTurn) page.width else page.height

                        val pageInfo = PdfDocument.PageInfo.Builder(outWidth, outHeight, newIndex + 1).create()
                        val docPage = pdfDoc.startPage(pageInfo)
                        val canvas = docPage.canvas
                        canvas.drawColor(Color.WHITE)

                        val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        val bmpCanvas = Canvas(bmp)
                        bmpCanvas.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        if (rotation != 0) {
                            val matrix = Matrix().apply {
                                postRotate(rotation.toFloat(), bmp.width / 2f, bmp.height / 2f)
                                if (isQuarterTurn) {
                                    postTranslate((outWidth - bmp.width) / 2f, (outHeight - bmp.height) / 2f)
                                }
                            }
                            canvas.drawBitmap(bmp, matrix, null)
                        } else {
                            canvas.drawBitmap(bmp, 0f, 0f, null)
                        }
                        bmp.recycle()

                        pdfDoc.finishPage(docPage)
                    }
                }
            } finally {
                renderer?.close()
                pfd?.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Overlay annotations or signature on a PDF page and write to a new copy.
     */
    suspend fun saveAnnotatedPdf(
        inputFile: File,
        targetPageIndex: Int,
        annotationBitmap: Bitmap,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            try {
                pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                    val docPage = pdfDoc.startPage(pageInfo)
                    val canvas = docPage.canvas

                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    val bmpCanvas = Canvas(bmp)
                    bmpCanvas.drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    canvas.drawBitmap(bmp, 0f, 0f, null)
                    bmp.recycle()

                    // If this is the annotated page, overlay
                    if (i == targetPageIndex) {
                        val src = Rect(0, 0, annotationBitmap.width, annotationBitmap.height)
                        val dst = Rect(0, 0, page.width, page.height)
                        canvas.drawBitmap(annotationBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                    }

                    pdfDoc.finishPage(docPage)
                }
            } finally {
                renderer?.close()
                pfd?.close()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Image Processing Filters ---

    fun applyFilterToBitmap(src: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.ORIGINAL) return src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)

        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorMatrix = ColorMatrix()

        when (filter) {
            ScanFilter.COLOR -> {
                // Boost saturation
                colorMatrix.setSaturation(1.35f)
            }
            ScanFilter.GRAYSCALE -> {
                colorMatrix.setSaturation(0f)
            }
            ScanFilter.BLACK_WHITE -> {
                // High contrast grayscale threshold simulation
                val gray = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 1.8f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.setConcat(contrastMatrix, gray)
            }
            ScanFilter.ENHANCED -> {
                // Boost brightness and contrast for clean document background
                val contrast = 1.35f
                val brightness = 20f
                colorMatrix.set(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ScanFilter.HIGH_CONTRAST -> {
                val contrast = 2.0f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                colorMatrix.set(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ScanFilter.ORIGINAL -> {}
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    // --- Internal Helpers ---

    private fun calculatePageDimensions(
        bmp: Bitmap?,
        format: PageFormat,
        orientation: DocOrientation
    ): Pair<Int, Int> {
        if (format == PageFormat.ORIGINAL && bmp != null) {
            return Pair(bmp.width, bmp.height)
        }
        val baseW = format.widthPt
        val baseH = format.heightPt

        return if (orientation == DocOrientation.PORTRAIT) {
            Pair(baseW, baseH)
        } else {
            Pair(baseH, baseW)
        }
    }

    private fun calculateImageBounds(
        bmp: Bitmap,
        availW: Float,
        availH: Float,
        padding: Float,
        fit: ImageFit
    ): Pair<RectF, Rect?> {
        when (fit) {
            ImageFit.FIT_PAGE -> {
                val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                val pageRatio = availW / availH

                val (drawW, drawH) = if (imgRatio > pageRatio) {
                    val w = availW
                    val h = availW / imgRatio
                    Pair(w, h)
                } else {
                    val h = availH
                    val w = availH * imgRatio
                    Pair(w, h)
                }

                val left = padding + (availW - drawW) / 2f
                val top = padding + (availH - drawH) / 2f
                return Pair(RectF(left, top, left + drawW, top + drawH), null)
            }
            ImageFit.FILL_PAGE -> {
                return Pair(RectF(padding, padding, padding + availW, padding + availH), null)
            }
            ImageFit.ORIGINAL_RATIO -> {
                val scale = min(availW / bmp.width, availH / bmp.height).coerceAtMost(1.0f)
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val left = padding + (availW - drawW) / 2f
                val top = padding + (availH - drawH) / 2f
                return Pair(RectF(left, top, left + drawW, top + drawH), null)
            }
        }
    }

    private fun drawWatermark(canvas: Canvas, w: Float, h: Float, config: WatermarkConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try { Color.parseColor(config.colorHex) } catch (e: Exception) { Color.GRAY }
            alpha = (config.opacity.coerceIn(0.05f, 1.0f) * 255).toInt()
            textSize = config.fontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.save()
        canvas.translate(w / 2f, h / 2f)
        canvas.rotate(config.rotationDegrees)
        canvas.drawText(config.text, 0f, config.fontSize / 3f, paint)
        canvas.restore()
    }

    private fun drawPageNumber(
        canvas: Canvas,
        w: Float,
        h: Float,
        currentNum: Int,
        totalNum: Int,
        config: PageNumberConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = config.fontSize
            textAlign = Paint.Align.CENTER
        }

        val text = when {
            config.format.contains("%d") && config.format.contains("of") -> String.format(config.format, currentNum, totalNum)
            config.format.contains("%d") -> String.format(config.format, currentNum)
            else -> "$currentNum"
        }

        val margin = 24f
        val (x, y) = when (config.position) {
            PageNumPosition.BOTTOM_CENTER -> Pair(w / 2f, h - margin)
            PageNumPosition.BOTTOM_RIGHT -> {
                paint.textAlign = Paint.Align.RIGHT
                Pair(w - margin, h - margin)
            }
            PageNumPosition.BOTTOM_LEFT -> {
                paint.textAlign = Paint.Align.LEFT
                Pair(margin, h - margin)
            }
            PageNumPosition.TOP_CENTER -> Pair(w / 2f, margin + config.fontSize)
        }

        canvas.drawText(text, x, y, paint)
    }

    /**
     * Crops a quadrilateral area from a bitmap using 4 corner points (normalized 0..1),
     * applying perspective correction.
     */
    fun cropQuadrilateral(
        srcBitmap: Bitmap,
        tl: PointF,
        tr: PointF,
        br: PointF,
        bl: PointF
    ): Bitmap {
        val srcWidth = srcBitmap.width.toFloat()
        val srcHeight = srcBitmap.height.toFloat()

        val pTL = PointF(tl.x.coerceIn(0f, 1f) * srcWidth, tl.y.coerceIn(0f, 1f) * srcHeight)
        val pTR = PointF(tr.x.coerceIn(0f, 1f) * srcWidth, tr.y.coerceIn(0f, 1f) * srcHeight)
        val pBR = PointF(br.x.coerceIn(0f, 1f) * srcWidth, br.y.coerceIn(0f, 1f) * srcHeight)
        val pBL = PointF(bl.x.coerceIn(0f, 1f) * srcWidth, bl.y.coerceIn(0f, 1f) * srcHeight)

        val topWidth = kotlin.math.hypot((pTR.x - pTL.x).toDouble(), (pTR.y - pTL.y).toDouble()).toFloat()
        val bottomWidth = kotlin.math.hypot((pBR.x - pBL.x).toDouble(), (pBR.y - pBL.y).toDouble()).toFloat()
        val leftHeight = kotlin.math.hypot((pBL.x - pTL.x).toDouble(), (pBL.y - pTL.y).toDouble()).toFloat()
        val rightHeight = kotlin.math.hypot((pBR.x - pTR.x).toDouble(), (pBR.y - pTR.y).toDouble()).toFloat()

        val outWidth = maxOf(100, ((topWidth + bottomWidth) / 2f).toInt())
        val outHeight = maxOf(100, ((leftHeight + rightHeight) / 2f).toInt())

        val srcPoints = floatArrayOf(
            pTL.x, pTL.y,
            pTR.x, pTR.y,
            pBR.x, pBR.y,
            pBL.x, pBL.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            outWidth.toFloat(), 0f,
            outWidth.toFloat(), outHeight.toFloat(),
            0f, outHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(srcBitmap, matrix, paint)
        return resultBitmap
    }

    /**
     * Rotates bitmap by 90 degrees.
     */
    fun rotateBitmap90(src: Bitmap, clockwise: Boolean = true): Bitmap {
        val matrix = Matrix().apply {
            postRotate(if (clockwise) 90f else -90f)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    /**
     * Applies document scan filters (color, grayscale, high contrast B&W, enhance).
     */
    fun applyScanFilter(src: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.ORIGINAL) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filter) {
            ScanFilter.ORIGINAL -> return src
            ScanFilter.COLOR -> {
                val cm = ColorMatrix()
                cm.setSaturation(1.3f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ScanFilter.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ScanFilter.BLACK_WHITE -> {
                val cm = ColorMatrix(floatArrayOf(
                    2.8f, 0f, 0f, 0f, -170f,
                    0f, 2.8f, 0f, 0f, -170f,
                    0f, 0f, 2.8f, 0f, -170f,
                    0f, 0f, 0f, 1f, 0f
                ))
                val gray = ColorMatrix()
                gray.setSaturation(0f)
                gray.postConcat(cm)
                paint.colorFilter = ColorMatrixColorFilter(gray)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ScanFilter.ENHANCED -> {
                val cm = ColorMatrix(floatArrayOf(
                    1.35f, 0f, 0f, 0f, -25f,
                    0f, 1.35f, 0f, 0f, -25f,
                    0f, 0f, 1.35f, 0f, -25f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ScanFilter.HIGH_CONTRAST -> {
                val cm = ColorMatrix(floatArrayOf(
                    1.7f, 0f, 0f, 0f, -50f,
                    0f, 1.7f, 0f, 0f, -50f,
                    0f, 0f, 1.7f, 0f, -50f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
        }
        return out
    }

    /**
     * Checks if a file is a valid, non-corrupted PDF.
     */
    fun isPdfValid(file: File): Boolean {
        if (!file.exists() || file.length() < 10) return false
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount > 0
        } catch (_: Exception) {
            false
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Checks if a content URI points to a valid, readable PDF.
     */
    fun isPdfValid(context: Context, uri: android.net.Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val valid = renderer.pageCount > 0
                renderer.close()
                valid
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
