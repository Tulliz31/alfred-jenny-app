package com.alfredJenny.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val companion: String = "alfred",   // "alfred" | "jenny"
    val isPinned: Boolean = false,
)
