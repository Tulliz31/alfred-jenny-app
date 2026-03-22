package com.alfredJenny.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: ConversationSummaryEntity)

    @Query("SELECT * FROM conversation_summaries WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSummary(sessionId: String): ConversationSummaryEntity?

    @Query("DELETE FROM conversation_summaries WHERE sessionId = :sessionId")
    suspend fun clearSummariesForSession(sessionId: String)

    @Query("DELETE FROM conversation_summaries")
    suspend fun clearAll()
}
