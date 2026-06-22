package com.example.data.api

import com.example.data.model.SurahEditionsResponse
import com.example.data.model.SurahListResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApi {
    @GET("surah")
    suspend fun getSurahs(): SurahListResponse

    @GET("surah/{number}/editions/quran-uthmani,bn.bengali,en.transliteration,{qari}")
    suspend fun getSurahEditions(
        @Path("number") number: Int,
        @Path("qari") qari: String
    ): SurahEditionsResponse
}
