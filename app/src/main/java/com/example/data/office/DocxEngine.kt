package com.example.data.office

import com.example.data.model.DocBlock
import com.example.data.model.DocBlockType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxEngine {

    /**
     * Generates a genuine, standard-compliant Microsoft Word (.docx) document.
     * Compatible with Microsoft Word, Google Docs, Apple Pages, and LibreOffice.
     */
    suspend fun createWordDocument(
        title: String,
        blocks: List<DocBlock>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                // 1. [Content_Types].xml
                val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>""".trimIndent()
                writeZipEntry(zos, "[Content_Types].xml", contentTypes)

                // 2. _rels/.rels
                val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>""".trimIndent()
                writeZipEntry(zos, "_rels/.rels", rootRels)

                // 3. word/document.xml
                val bodySb = StringBuilder()
                bodySb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                bodySb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
                bodySb.append("  <w:body>\n")

                // Document Header Title
                if (title.isNotBlank()) {
                    bodySb.append("    <w:p>\n")
                    bodySb.append("      <w:pPr><w:jc w:val=\"center\"/></w:pPr>\n")
                    bodySb.append("      <w:r><w:rPr><w:b/><w:sz w:val=\"48\"/><w:color w:val=\"007AFF\"/></w:rPr>\n")
                    bodySb.append("        <w:t>${escapeXml(title)}</w:t>\n")
                    bodySb.append("      </w:r>\n")
                    bodySb.append("    </w:p>\n")
                }

                // Render each block
                for (block in blocks) {
                    when (block.type) {
                        DocBlockType.TABLE -> {
                            val table = block.tableData
                            if (table != null && table.isNotEmpty()) {
                                bodySb.append("    <w:tbl>\n")
                                bodySb.append("      <w:tblPr><w:tblBorders><w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CCCCCC\"/><w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CCCCCC\"/><w:left w:val=\"none\"/><w:right w:val=\"none\"/><w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"E5E5EA\"/><w:insideV w:val=\"none\"/></w:tblBorders></w:tblPr>\n")
                                for (row in table) {
                                    bodySb.append("      <w:tr>\n")
                                    for (cell in row) {
                                        bodySb.append("        <w:tc><w:p><w:r><w:t>${escapeXml(cell)}</w:t></w:r></w:p></w:tc>\n")
                                    }
                                    bodySb.append("      </w:tr>\n")
                                }
                                bodySb.append("    </w:tbl>\n")
                            }
                        }
                        DocBlockType.PAGE_BREAK -> {
                            bodySb.append("    <w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
                        }
                        else -> {
                            val (sz, isBold, color, align) = when (block.type) {
                                DocBlockType.TITLE -> listOf("44", true, "1C1C1E", "center")
                                DocBlockType.SUBTITLE -> listOf("28", false, "666666", "center")
                                DocBlockType.HEADING_1 -> listOf("36", true, "007AFF", "left")
                                DocBlockType.HEADING_2 -> listOf("30", true, "1C1C1E", "left")
                                DocBlockType.HEADING_3 -> listOf("24", true, "333333", "left")
                                DocBlockType.QUOTE -> listOf("22", false, "555555", "left")
                                DocBlockType.CODE_BLOCK -> listOf("18", false, "006699", "left")
                                else -> listOf("22", block.formatting.isBold, "1C1C1E", block.formatting.alignment.lowercase())
                            }

                            bodySb.append("    <w:p>\n")
                            bodySb.append("      <w:pPr><w:jc w:val=\"$align\"/></w:pPr>\n")
                            bodySb.append("      <w:r>\n")
                            bodySb.append("        <w:rPr>")
                            if (isBold as Boolean) bodySb.append("<w:b/>")
                            if (block.formatting.isItalic || block.type == DocBlockType.QUOTE) bodySb.append("<w:i/>")
                            if (block.formatting.isUnderline) bodySb.append("<w:u w:val=\"single\"/>")
                            if (block.formatting.isStrikethrough) bodySb.append("<w:strike/>")
                            bodySb.append("<w:sz w:val=\"$sz\"/>")
                            bodySb.append("<w:color w:val=\"${(color as String).removePrefix("#")}\"/>")
                            bodySb.append("</w:rPr>\n")
                            bodySb.append("        <w:t>${escapeXml(block.text)}</w:t>\n")
                            bodySb.append("      </w:r>\n")
                            bodySb.append("    </w:p>\n")
                        }
                    }
                }

                bodySb.append("  </w:body>\n")
                bodySb.append("</w:document>")
                writeZipEntry(zos, "word/document.xml", bodySb.toString())
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
