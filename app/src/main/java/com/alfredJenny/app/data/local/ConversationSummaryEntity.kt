package com.alfredJenny.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_summaries")
data class ConversationSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val summaryText: String,
    val coveringUpToMessageId: Long,   // id of the last message included in this summary
    val createdAt: Long = System.currentTimeMillis()
)
