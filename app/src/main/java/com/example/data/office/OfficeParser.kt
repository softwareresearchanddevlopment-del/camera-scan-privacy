package com.example.data.office

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class OfficeParsedResult(
    val title: String,
    val type: String, // "Word", "Excel", "PowerPoint", "CSV", "Text"
    val paragraphs: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val slides: List<String> = emptyList(),
    val rawText: String = "",
    val error: String? = null
)

object OfficeParser {

    suspend fun parseFile(file: File): OfficeParsedResult = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        try {
            when (extension) {
                "docx" -> parseDocx(file)
                "xlsx" -> parseXlsx(file)
                "pptx" -> parsePptx(file)
                "csv" -> parseCsv(file)
                "txt", "rtf", "log" -> parsePlainText(file)
                else -> OfficeParsedResult(
                    title = file.name,
                    type = extension.uppercase(),
                    rawText = "Document format: .${extension.uppercase()}\nUse the Open in Office App option to view this file with your installed office suite.",
                    error = "Binary or specialized format"
                )
            }
        } catch (e: Exception) {
            OfficeParsedResult(
                title = file.name,
                type = extension.uppercase(),
                error = "Could not parse document: ${e.localizedMessage}"
            )
        }
    }

    private fun parseDocx(file: File): OfficeParsedResult {
        val paragraphs = mutableListOf<String>()
        var docXml = ""

        ZipInputStream(file.inputStream()).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name == "word/document.xml") {
                    docXml = zis.bufferedReader().readText()
                    break
                }
            }
        }

        if (docXml.isNotEmpty()) {
            // Extract paragraphs <w:p>...</w:p>
            val pRegex = Regex("<w:p[ >](.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
            val tRegex = Regex("<w:t[ >](.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)

            pRegex.findAll(docXml).forEach { match ->
                val pContent = match.value
                val textBuilder = StringBuilder()
                tRegex.findAll(pContent).forEach { tMatch ->
                    val text = tMatch.groupValues[1]
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                    textBuilder.append(text)
                }
                val line = textBuilder.toString().trim()
                if (line.isNotEmpty()) {
                    paragraphs.add(line)
                }
            }
        }

        if (paragraphs.isEmpty()) {
            paragraphs.add("Empty or protected Word document.")
        }

        return OfficeParsedResult(
            title = file.name,
            type = "Word",
            paragraphs = paragraphs,
            rawText = paragraphs.joinToString("\n\n")
        )
    }

    private fun parseXlsx(file: File): OfficeParsedResult {
        val sharedStrings = mutableListOf<String>()
        var sheetXml = ""

        ZipInputStream(file.inputStream()).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name == "xl/sharedStrings.xml") {
                    val xml = zis.bufferedReader().readText()
                    val tRegex = Regex("<t[ >](.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    tRegex.findAll(xml).forEach { m ->
                        sharedStrings.add(m.groupValues[1].replace("&amp;", "&"))
                    }
                } else if (entry!!.name == "xl/worksheets/sheet1.xml") {
                    sheetXml = zis.bufferedReader().readText()
                }
            }
        }

        val rows = mutableListOf<List<String>>()
        if (sheetXml.isNotEmpty()) {
            val rowRegex = Regex("<row[ >](.*?)</row>", RegexOption.DOT_MATCHES_ALL)
            val cRegex = Regex("<c[^>]*?(?:t=\"([^\"]*)\")?[^>]*?>.*?<v>(.*?)</v>.*?</c>", RegexOption.DOT_MATCHES_ALL)

            rowRegex.findAll(sheetXml).forEach { rMatch ->
                val rowCells = mutableListOf<String>()
                cRegex.findAll(rMatch.value).forEach { cMatch ->
                    val type = cMatch.groupValues[1]
                    val value = cMatch.groupValues[2]
                    val cellText = if (type == "s") {
                        val idx = value.toIntOrNull() ?: -1
                        if (idx in sharedStrings.indices) sharedStrings[idx] else value
                    } else {
                        value
                    }
                    rowCells.add(cellText)
                }
                if (rowCells.isNotEmpty()) {
                    rows.add(rowCells)
                }
            }
        }

        val summary = if (rows.isNotEmpty()) {
            rows.joinToString("\n") { it.joinToString("  |  ") }
        } else {
            "Empty or binary spreadsheet."
        }

        return OfficeParsedResult(
            title = file.name,
            type = "Excel",
            tableRows = rows,
            rawText = summary
        )
    }

    private fun parsePptx(file: File): OfficeParsedResult {
        val slides = mutableListOf<String>()
        val tRegex = Regex("<a:t[ >](.*?)</a:t>", RegexOption.DOT_MATCHES_ALL)

        ZipInputStream(file.inputStream()).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                if (entry!!.name.startsWith("ppt/slides/slide") && entry!!.name.endsWith(".xml")) {
                    val xml = zis.bufferedReader().readText()
                    val slideText = tRegex.findAll(xml).map { m ->
                        m.groupValues[1].replace("&amp;", "&")
                    }.filter { it.isNotBlank() }.joinToString("\n")

                    if (slideText.isNotEmpty()) {
                        slides.add(slideText)
                    }
                }
            }
        }

        if (slides.isEmpty()) {
            slides.add("Empty or protected presentation.")
        }

        return OfficeParsedResult(
            title = file.name,
            type = "PowerPoint",
            slides = slides,
            rawText = slides.mapIndexed { idx, s -> "--- Slide ${idx + 1} ---\n$s" }.joinToString("\n\n")
        )
    }

    private fun parseCsv(file: File): OfficeParsedResult {
        val rows = mutableListOf<List<String>>()
        BufferedReader(InputStreamReader(file.inputStream())).useLines { lines ->
            lines.forEach { line ->
                val cells = line.split(Regex("[,;\t]")).map { it.trim().trim('\"') }
                if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                    rows.add(cells)
                }
            }
        }

        return OfficeParsedResult(
            title = file.name,
            type = "CSV",
            tableRows = rows,
            rawText = rows.joinToString("\n") { it.joinToString("  |  ") }
        )
    }

    private fun parsePlainText(file: File): OfficeParsedResult {
        val text = file.readText()
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
        return OfficeParsedResult(
            title = file.name,
            type = "Text",
            paragraphs = paragraphs,
            rawText = text
        )
    }
}
