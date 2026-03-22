package com.alfredJenny.app.data.repository

import com.alfredJenny.app.data.local.MemoDao
import com.alfredJenny.app.data.local.MemoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepository @Inject constructor(
    private val dao: MemoDao
) {
    fun getAllMemos(): Flow<List<MemoEntity>> = dao.getAllMemos()

    suspend fun saveMemo(title: String, content: String, companion: String = "alfred"): MemoEntity {
        val memo = MemoEntity(title = title, content = content, companion = companion)
        val id = dao.insertMemo(memo)
        return memo.copy(id = id)
    }

    suspend fun updateMemo(memo: MemoEntity) = dao.updateMemo(memo)

    suspend fun deleteMemo(id: Long) = dao.deleteMemoById(id)

    suspend fun togglePin(memo: MemoEntity) = dao.updateMemo(memo.copy(isPinned = !memo.isPinned))
}
