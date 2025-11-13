package com.ai.vis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edited_photos")
data class EditedPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val thumbnailPath: String? = null,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)
