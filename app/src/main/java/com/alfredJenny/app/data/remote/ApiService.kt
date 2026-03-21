package com.alfredJenny.app.data.remote

import com.alfredJenny.app.data.model.ChatRequest
import com.alfredJenny.app.data.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {

    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
