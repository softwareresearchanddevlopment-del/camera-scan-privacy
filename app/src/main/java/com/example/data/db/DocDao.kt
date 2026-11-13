package com.example.data.db

import androidx.room.*
import com.example.data.model.DocItem
import com.example.data.model.EditorDocument
import com.example.data.model.GrantedFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface DocDao {
    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC")
    fun getAllDocuments(): Flow<List<DocItem>>

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY modifiedAt DESC")
    fun getDocumentsByCategory(category: String): Flow<List<DocItem>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY modifiedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocItem>>

    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC LIMIT 10")
    fun getRecentDocuments(): Flow<List<DocItem>>

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' ORDER BY modifiedAt DESC")
    fun searchDocuments(query: String): Flow<List<DocItem>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocItem?

    @Query("SELECT * FROM documents WHERE filePath = :path OR uriString = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): DocItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocItem>)

    @Update
    suspend fun updateDocument(document: DocItem)

    @Delete
    suspend fun deleteDocument(document: DocItem)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE documents SET name = :newName WHERE id = :id")
    suspend fun renameDocument(id: Long, newName: String)

    // Granted Folders
    @Query("SELECT * FROM granted_folders ORDER BY addedAt DESC")
    fun getGrantedFolders(): Flow<List<GrantedFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrantedFolder(folder: GrantedFolder): Long

    @Delete
    suspend fun deleteGrantedFolder(folder: GrantedFolder)

    // Editor Documents
    @Query("SELECT * FROM editor_documents ORDER BY lastModified DESC")
    fun getAllEditorDocuments(): Flow<List<EditorDocument>>

    @Query("SELECT * FROM editor_documents WHERE id = :id LIMIT 1")
    suspend fun getEditorDocumentById(id: Long): EditorDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditorDocument(doc: EditorDocument): Long

    @Delete
    suspend fun deleteEditorDocument(doc: EditorDocument)
}
