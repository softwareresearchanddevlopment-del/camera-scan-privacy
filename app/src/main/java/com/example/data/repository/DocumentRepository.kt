package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.DocDao
import com.example.data.db.DocDatabase
import com.example.data.model.*
import com.example.data.office.DocxEngine
import com.example.data.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val context: Context,
    private val docDao: DocDao = DocDatabase.getInstance(context).docDao()
) {
    val allDocuments: Flow<List<DocItem>> = docDao.getAllDocuments()
    val recentDocuments: Flow<List<DocItem>> = docDao.getRecentDocuments()
    val favoriteDocuments: Flow<List<DocItem>> = docDao.getFavoriteDocuments()
    val grantedFolders: Flow<List<GrantedFolder>> = docDao.getGrantedFolders()
    val editorDocuments: Flow<List<EditorDocument>> = docDao.getAllEditorDocuments()

    fun getByCategory(category: String): Flow<List<DocItem>> = docDao.getDocumentsByCategory(category)

    fun search(query: String): Flow<List<DocItem>> = docDao.searchDocuments(query)

    suspend fun getDocumentById(id: Long): DocItem? = docDao.getDocumentById(id)

    /**
     * Rescans real documents from device storage, user-granted folders, and MediaStore.
     */
    suspend fun refreshDeviceDocuments() = withContext(Dispatchers.IO) {
        val folders = docDao.getGrantedFolders().first()
        val scannedDocs = DeviceFileScanner.scanAllDeviceDocuments(context, folders)
        if (scannedDocs.isNotEmpty()) {
            docDao.insertDocuments(scannedDocs)
        }
    }

    /**
     * Saves a user-granted SAF folder tree permission and scans its documents.
     */
    suspend fun addGrantedFolder(treeUri: Uri, displayName: String) = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}

        val folder = GrantedFolder(
            treeUri = treeUri.toString(),
            displayName = displayName
        )
        docDao.insertGrantedFolder(folder)
        refreshDeviceDocuments()
    }

    /**
     * Imports an external document chosen by the user via SAF.
     */
    suspend fun importSafDocument(uri: Uri): DocItem? = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {}

        var fileName = "imported_document"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx != -1) cursor.getString(nameIdx)?.let { fileName = it }
                if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
            }
        }

        val ext = fileName.substringAfterLast(".", "").lowercase()
        val category = DeviceFileScanner.determineCategory(ext, "Device")
        val mime = DeviceFileScanner.determineMimeType(ext)

        // Validate PDF if applicable
        if (ext == "pdf" || mime == "application/pdf") {
            if (!PdfEngine.isPdfValid(context, uri)) {
                return@withContext null
            }
        }

        // Make durable working copy in documents dir
        val localDest = File(getDocumentsDir(), fileName)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localDest).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {}

        val actualFile = if (localDest.exists() && localDest.length() > 0) localDest else null
        if (ext == "pdf" && actualFile != null && !PdfEngine.isPdfValid(actualFile)) {
            actualFile.delete()
            return@withContext null
        }

        val path = actualFile?.absolutePath ?: uri.toString()
        val finalSize = actualFile?.length() ?: fileSize
        val pageCount = if (ext == "pdf" && actualFile != null) PdfEngine.getPageCount(actualFile).coerceAtLeast(1) else 1

        val doc = DocItem(
            name = fileName,
            filePath = path,
            uriString = uri.toString(),
            mimeType = mime,
            sizeBytes = finalSize,
            pageCount = pageCount,
            category = category,
            storageLocation = "Imported"
        )
        val id = docDao.insertDocument(doc)
        doc.copy(id = id)
    }

    suspend fun saveDocument(file: File, category: String = "pdf", location: String = "Created"): DocItem = withContext(Dispatchers.IO) {
        val pageCount = if (file.extension.equals("pdf", ignoreCase = true)) {
            PdfEngine.getPageCount(file).coerceAtLeast(1)
        } else 1

        val doc = DocItem(
            name = file.name,
            filePath = file.absolutePath,
            mimeType = DeviceFileScanner.determineMimeType(file.extension.lowercase()),
            sizeBytes = file.length(),
            pageCount = pageCount,
            category = category,
            storageLocation = location
        )
        val id = docDao.insertDocument(doc)
        doc.copy(id = id)
    }

    suspend fun renameDocument(id: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        val doc = docDao.getDocumentById(id) ?: return@withContext false
        val oldFile = File(doc.filePath)
        if (oldFile.exists()) {
            val safeName = if (newName.contains(".")) newName else "$newName.${oldFile.extension}"
            val newFile = File(oldFile.parentFile, safeName)
            if (oldFile.renameTo(newFile)) {
                val updated = doc.copy(name = safeName, filePath = newFile.absolutePath)
                docDao.updateDocument(updated)
                return@withContext true
            }
        }
        // Update database record name
        docDao.renameDocument(id, newName)
        true
    }

    suspend fun deleteDocument(id: Long): Boolean = withContext(Dispatchers.IO) {
        val doc = docDao.getDocumentById(id) ?: return@withContext false
        val file = File(doc.filePath)
        if (file.exists()) {
            file.delete()
        }
        docDao.deleteDocumentById(id)
        true
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        docDao.setFavorite(id, isFavorite)
    }

    fun getShareUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun getDocumentsDir(): File {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getScansDir(): File {
        val dir = File(context.filesDir, "scans")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCacheDir(): File {
        val dir = File(context.cacheDir, "pdf_temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun getStorageUsage(): Long = withContext(Dispatchers.IO) {
        val docDir = getDocumentsDir()
        val scanDir = getScansDir()
        val cacheDir = getCacheDir()
        (dirSize(docDir) + dirSize(scanDir) + dirSize(cacheDir))
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir()
        val freed = dirSize(cacheDir)
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        freed
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { if (it.isFile) total += it.length() }
        return total
    }

    // --- Rich Document Editor Operations ---

    suspend fun saveEditorDocument(doc: EditorDocument): Long = withContext(Dispatchers.IO) {
        docDao.insertEditorDocument(doc)
    }

    suspend fun getEditorDocument(id: Long): EditorDocument? = withContext(Dispatchers.IO) {
        docDao.getEditorDocumentById(id)
    }

    suspend fun exportEditorDocumentToPdf(title: String, blocks: List<DocBlock>): Result<File> = withContext(Dispatchers.IO) {
        val outFile = File(getDocumentsDir(), "${title.replace(" ", "_")}.pdf")
        val content = blocks.joinToString("\n\n") { block ->
            when (block.type) {
                DocBlockType.TABLE -> block.tableData?.joinToString("\n") { it.joinToString("  |  ") } ?: ""
                DocBlockType.PAGE_BREAK -> "\n--- Page Break ---\n"
                else -> block.text
            }
        }
        val res = PdfEngine.createTextPdf(title, content, outFile, PageFormat.A4)
        if (res.isSuccess) {
            saveDocument(outFile, "created", "Documents")
        }
        res
    }

    suspend fun exportEditorDocumentToWord(title: String, blocks: List<DocBlock>): Result<File> = withContext(Dispatchers.IO) {
        val outFile = File(getDocumentsDir(), "${title.replace(" ", "_")}.docx")
        val res = DocxEngine.createWordDocument(title, blocks, outFile)
        if (res.isSuccess) {
            saveDocument(outFile, "word", "Documents")
        }
        res
    }

    suspend fun seedStarterDocumentsIfNeeded() = withContext(Dispatchers.IO) {
        // Ensure documents directory exists. Never create fake sample PDFs.
        getDocumentsDir().mkdirs()
    }
}
