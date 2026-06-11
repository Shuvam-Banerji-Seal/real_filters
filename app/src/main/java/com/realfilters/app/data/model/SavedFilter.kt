package com.realfilters.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_filters",
    indices = [Index(value = ["createdAt"])]
)
data class SavedFilter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "color_matrix" or "convolution"
    val matrixData: String, // JSON serialized matrix data
    val createdAt: Long = System.currentTimeMillis()
)
