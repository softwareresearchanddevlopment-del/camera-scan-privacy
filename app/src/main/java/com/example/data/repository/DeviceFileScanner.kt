package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.data.model.DocItem
import com.example.data.model.GrantedFolder
import com.example.data.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DeviceFileScanner {

    private val SUPPORTED_EXTENSIONS = setOf(
        "pdf", "docx", "doc", "pptx", "ppt", "xlsx", "xls",
        "jpg", "jpeg", "png", "webp", "txt", "csv", "rtf", "odt", "ods", "odp"
    )

    /**
     * Scans real accessible device documents from MediaStore, standard folders,
     * and user-granted Storage Access Framework folder trees.
     */
    suspend fun scanAllDeviceDocuments(
        context: Context,
        grantedFolders: List<GrantedFolder>
    ): List<DocItem> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, DocItem>()

        // 1. Scan user-granted SAF folder trees
        for (folder in grantedFolders) {
            try {
                val folderUri = Uri.parse(folder.treeUri)
                val treeDocs = scanTreeUri(context, folderUri, folder.displayName)
                treeDocs.forEach { results[it.filePath] = it }
            } catch (_: Exception) {}
        }

        // 2. Scan standard device public directories (Downloads & Documents)
        scanStandardDirectory(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Downloads",
            results
        )
        scanStandardDirectory(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Documents",
            results
        )

        // 3. Scan app's durable external documents directory
        context.getExternalFilesDir("documents")?.let {
            scanStandardDirectory(it, "Created", results)
        }
        context.getExternalFilesDir("scans")?.let {
            scanStandardDirectory(it, "Scanned", results)
        }

        // 4. Query MediaStore for accessible documents on device
        scanMediaStore(context, results)

        results.values.toList()
    }

    private fun scanStandardDirectory(dir: File?, locationName: String, outMap: MutableMap<String, DocItem>) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() > 0) {
                    val ext = file.extension.lowercase()
                    if (ext in SUPPORTED_EXTENSIONS) {
                        val category = determineCategory(ext, locationName)
                        val pageCount = if (ext == "pdf") PdfEngine.getPageCount(file).coerceAtLeast(1) else 1
                        val item = DocItem(
                            name = file.name,
                            filePath = file.absolutePath,
                            mimeType = determineMimeType(ext),
                            sizeBytes = file.length(),
                            pageCount = pageCount,
                            modifiedAt = file.lastModified(),
                            category = category,
                            storageLocation = locationName
                        )
                        outMap[file.absolutePath] = item
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun scanMediaStore(context: Context, outMap: MutableMap<String, DocItem>) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.docx' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.pptx' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.xlsx' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.txt'"
            val selectionArgs = arrayOf("application/%")

            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = if (dataIdx != -1) cursor.getString(dataIdx) else null
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: path?.let { File(it).name } else null
                    val size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) * 1000L else System.currentTimeMillis()
                    val mime = if (mimeIdx != -1) cursor.getString(mimeIdx) ?: "" else ""

                    if (path != null && name != null && size > 0) {
                        val file = File(path)
                        val ext = file.extension.lowercase()
                        if (ext in SUPPORTED_EXTENSIONS) {
                            val location = when {
                                path.contains("/Download", ignoreCase = true) -> "Downloads"
                                path.contains("/Documents", ignoreCase = true) -> "Documents"
                                else -> "Device"
                            }
                            val category = determineCategory(ext, location)
                            val pageCount = if (ext == "pdf") PdfEngine.getPageCount(file).coerceAtLeast(1) else 1
                            val docItem = DocItem(
                                name = name,
                                filePath = path,
                                mimeType = if (mime.isNotBlank()) mime else determineMimeType(ext),
                                sizeBytes = size,
                                pageCount = pageCount,
                                modifiedAt = date,
                                category = category,
                                storageLocation = location
                            )
                            outMap[path] = docItem
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun scanTreeUri(context: Context, treeUri: Uri, locationName: String): List<DocItem> {
        val items = mutableListOf<DocItem>()
        val contentResolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val dateIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx) ?: "document"
                val mime = cursor.getString(mimeIdx) ?: "application/octet-stream"
                val size = cursor.getLong(sizeIdx)
                val date = cursor.getLong(dateIdx)
                val ext = name.substringAfterLast(".", "").lowercase()

                if (ext in SUPPORTED_EXTENSIONS) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val category = determineCategory(ext, locationName)
                    items.add(
                        DocItem(
                            name = name,
                            filePath = docUri.toString(),
                            uriString = docUri.toString(),
                            mimeType = mime,
                            sizeBytes = size,
                            pageCount = 1,
                            modifiedAt = date,
                            category = category,
                            storageLocation = locationName
                        )
                    )
                }
            }
        }
        return items
    }

    fun determineCategory(ext: String, location: String): String {
        return when (ext) {
            "pdf" -> if (location == "Scanned") "scan" else if (location == "Created") "created" else "pdf"
            "docx", "doc" -> "word"
            "pptx", "ppt" -> "ppt"
            "xlsx", "xls" -> "excel"
            "jpg", "jpeg", "png", "webp" -> "image"
            "txt", "csv", "rtf" -> "text"
            else -> "other"
        }
    }

    fun determineMimeType(ext: String): String {
        return when (ext) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "rtf" -> "application/rtf"
            else -> "application/octet-stream"
        }
    }
}
