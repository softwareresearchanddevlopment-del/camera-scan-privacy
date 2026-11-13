package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DocItem
import com.example.data.model.EditorDocument
import com.example.data.model.GrantedFolder

@Database(entities = [DocItem::class, GrantedFolder::class, EditorDocument::class], version = 2, exportSchema = false)
abstract class DocDatabase : RoomDatabase() {
    abstract fun docDao(): DocDao

    companion object {
        @Volatile
        private var INSTANCE: DocDatabase? = null

        fun getInstance(context: Context): DocDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocDatabase::class.java,
                    "docuflow_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
