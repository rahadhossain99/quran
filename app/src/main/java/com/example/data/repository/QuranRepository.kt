package com.example.data.repository

import com.example.data.api.QuranApi
import com.example.data.db.QuranDao
import com.example.data.model.FavoriteSurah
import com.example.data.model.FormattedAyah
import com.example.data.model.FormattedSurah
import com.example.data.model.LastPlayed
import com.example.data.model.SurahModel
import com.example.data.model.UserSettings
import com.example.data.model.QARIS_LIST
import com.example.data.model.QuranStats
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class QuranRepository(private val quranDao: QuranDao, val context: android.content.Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val surahAdapter = moshi.adapter(FormattedSurah::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val quranApi: QuranApi = Retrofit.Builder()
        .baseUrl("https://api.alquran.cloud/v1/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()
        .create(QuranApi::class.java)

    // Helper to get local cached surah file
    fun getCachedSurahFile(number: Int, qari: String): java.io.File {
        val dir = java.io.File(context.filesDir, "cached_surahs")
        if (!dir.exists()) dir.mkdirs()
        return java.io.File(dir, "surah_${number}_${qari}.json")
    }

    // Check if a Surah has audio cached locally
    fun hasSurahAudioCached(number: Int, qari: String): Boolean {
        val textFile = getCachedSurahFile(number, qari)
        if (!textFile.exists() || textFile.length() == 0L) return false
        return try {
            val json = textFile.readText()
            val cachedSurah = surahAdapter.fromJson(json)
            val firstAudioUrl = cachedSurah?.ayahs?.firstOrNull()?.audioUrl
            if (firstAudioUrl != null) {
                val audioFile = getCachedAudioFile(context, firstAudioUrl)
                audioFile != null && audioFile.exists() && audioFile.length() > 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Delete a cached Surah cleanly from disk (both JSON and its audio wavefiles)
    fun deleteCachedSurah(number: Int, qari: String) {
        try {
            val textFile = getCachedSurahFile(number, qari)
            if (textFile.exists()) {
                try {
                    val json = textFile.readText()
                    val cachedSurah = surahAdapter.fromJson(json)
                    cachedSurah?.ayahs?.forEach { ayah ->
                        val audioUrl = ayah.audioUrl
                        if (audioUrl.isNotBlank()) {
                            val audioFile = getCachedAudioFile(context, audioUrl)
                            if (audioFile != null && audioFile.exists()) {
                                audioFile.delete()
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                textFile.delete()
            }
            // Delete other editions/qari JSON caches of this Surah too
            val dir = java.io.File(context.filesDir, "cached_surahs")
            if (dir.exists()) {
                val files = dir.listFiles { _, name -> name.startsWith("surah_${number}_") && name.endsWith(".json") }
                files?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper to get local cached audio file path based on MD5 of URL
    fun getCachedAudioFile(context: android.content.Context, audioUrl: String): java.io.File? {
        if (audioUrl.isBlank()) return null
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val bytes = md.digest(audioUrl.toByteArray())
            val hex = bytes.joinToString("") { "%02x".format(it) }
            val dir = java.io.File(context.filesDir, "cached_audio")
            if (!dir.exists()) dir.mkdirs()
            java.io.File(dir, "$hex.mp3")
        } catch (e: Exception) {
            null
        }
    }

    // Real audio file downloader using OkHttpClient
    suspend fun downloadAudioFile(url: String): Boolean {
        val destinationFile = getCachedAudioFile(context, url) ?: return false
        if (destinationFile.exists() && destinationFile.length() > 0) {
            return true // Already downloaded completed
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val body = response.body ?: return@withContext false
                    destinationFile.parentFile?.mkdirs()
                    destinationFile.outputStream().use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
                true
            } catch (e: Exception) {
                try { destinationFile.delete() } catch (ex: Exception) {}
                false
            }
        }
    }

    // Helper to get local cached surah list file
    fun getCachedSurahListFile(): java.io.File {
        return java.io.File(context.filesDir, "cached_surah_list.json")
    }

    // Check if any offline Qari data exists for a Surah, and return that Qari ID if found
    fun getOfflineQariForSurah(number: Int): String? {
        val dir = java.io.File(context.filesDir, "cached_surahs")
        if (!dir.exists()) return null
        val prefix = "surah_${number}_"
        val suffix = ".json"
        val files = dir.listFiles { _, name -> name.startsWith(prefix) && name.endsWith(suffix) }
        if (files.isNullOrEmpty()) return null
        // Find one that is completed (contains valid json and can be parsed)
        for (f in files) {
            val name = f.name
            val qariId = name.substring(prefix.length, name.length - suffix.length)
            if (f.length() > 0) {
                return qariId
            }
        }
        return null
    }

    // API calls with built-in automatic JSON fallback cache
    suspend fun getSurahs(): List<SurahModel> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cachedFile = getCachedSurahListFile()
        // Try network first
        try {
            val response = quranApi.getSurahs()
            val list = response.data
            if (list.isNotEmpty()) {
                // Save list to local cache as JSON string in background
                try {
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SurahModel::class.java)
                    val adapter = moshi.adapter<List<SurahModel>>(listType)
                    val json = adapter.toJson(list)
                    cachedFile.writeText(json)
                } catch (e: Exception) {
                    // Suppress write errors
                }
                return@withContext list
            }
        } catch (e: Exception) {
            // Offline or network error: Fallback to local cache if present
            if (cachedFile.exists() && cachedFile.length() > 0) {
                try {
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SurahModel::class.java)
                    val adapter = moshi.adapter<List<SurahModel>>(listType)
                    val json = cachedFile.readText()
                    val list = adapter.fromJson(json)
                    if (list != null && list.isNotEmpty()) {
                        return@withContext list
                    }
                } catch (ex: Exception) {
                    // Suppress write errors
                }
            }
            throw e // Rethrow the exception if no cache is available either
        }
        throw IllegalStateException("নেটওয়ার্ক কানেকশন প্রয়োজন। কোনো অফলাইন ক্যাশ পাওয়া যায়নি।")
    }

    suspend fun getSurahEditions(number: Int, qari: String): FormattedSurah = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var targetQari = qari
        var isOfflineFallback = false
        val requestedFile = getCachedSurahFile(number, qari)
        
        val online = isOnline()
        
        // Only trigger the offline qari fallback if the user is actually offline and cache doesn't exist
        if (!online) {
            if (!requestedFile.exists() || requestedFile.length() == 0L) {
                // Find if there is any other cached qari for this Surah
                val fallbackQari = getOfflineQariForSurah(number)
                if (fallbackQari != null && fallbackQari != qari) {
                    targetQari = fallbackQari
                    isOfflineFallback = true
                }
            }
        }

        val cachedFile = getCachedSurahFile(number, targetQari)
        if (cachedFile.exists() && cachedFile.length() > 0) {
            try {
                val json = cachedFile.readText()
                val cachedSurah = surahAdapter.fromJson(json)
                if (cachedSurah != null) {
                    return@withContext cachedSurah
                }
            } catch (e: Exception) {
                // Ignore and call network fallback
            }
        }

        // Fetch from network API
        val response = quranApi.getSurahEditions(number, targetQari)
        val data = response.data
        if (data.size < 4) {
            throw IllegalStateException("API returned incomplete editions for Surah $number")
        }
        val arabic = data[0]
        val bengali = data[1]
        val translit = data[2]
        val audio = data[3]

        val formattedAyahs = arabic.ayahs.mapIndexed { index, arabicAyah ->
            FormattedAyah(
                numberInSurah = arabicAyah.numberInSurah,
                arabicText = arabicAyah.text,
                bengaliText = if (index < bengali.ayahs.size) bengali.ayahs[index].text else "",
                transliterationText = if (index < translit.ayahs.size) translit.ayahs[index].text else "",
                audioUrl = if (index < audio.ayahs.size) audio.ayahs[index].audio ?: "" else ""
            )
        }

        val formattedSurah = FormattedSurah(
            number = arabic.number,
            name = arabic.name,
            englishName = arabic.englishName,
            englishNameTranslation = arabic.englishNameTranslation,
            revelationType = arabic.revelationType,
            numberOfAyahs = arabic.numberOfAyahs,
            ayahs = formattedAyahs
        )

        // Cache the newly fetched structure locally for future instant offline access
        try {
            val json = surahAdapter.toJson(formattedSurah)
            cachedFile.writeText(json)
        } catch (e: Exception) {
            // Ignored
        }

        return@withContext formattedSurah
    }

    // Room operations
    fun getFavoriteSurahs(): Flow<List<FavoriteSurah>> = quranDao.getFavoriteSurahs()

    suspend fun insertFavorite(favorite: FavoriteSurah) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        quranDao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(surahNumber: Int) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        quranDao.deleteFavorite(surahNumber)
    }

    fun isFavorite(surahNumber: Int): Flow<Boolean> = quranDao.isFavorite(surahNumber)

    fun getLastPlayed(): Flow<LastPlayed?> = quranDao.getLastPlayed()

    suspend fun saveLastPlayed(lastPlayed: LastPlayed) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        quranDao.saveLastPlayed(lastPlayed)
    }

    fun getUserSettings(): Flow<UserSettings?> = quranDao.getUserSettings()

    suspend fun saveUserSettings(settings: UserSettings) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        quranDao.saveUserSettings(settings)
    }

    suspend fun incrementStats(dateStr: String, durationSecondsDelta: Long, ayahsDelta: Int) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val current = quranDao.getStatsForDate(dateStr)
        if (current == null) {
            quranDao.insertStats(QuranStats(
                dateStr = dateStr,
                durationSeconds = durationSecondsDelta,
                ayahsReadCount = ayahsDelta
            ))
        } else {
            quranDao.insertStats(current.copy(
                durationSeconds = current.durationSeconds + durationSecondsDelta,
                ayahsReadCount = current.ayahsReadCount + ayahsDelta
            ))
        }
    }

    fun getAllStatsFlow(): Flow<List<QuranStats>> = quranDao.getAllStatsFlow()

    // Surah stats operations
    fun getAllSurahStatsFlow(): Flow<List<com.example.data.model.SurahPlayStats>> = quranDao.getAllSurahStatsFlow()

    suspend fun incrementSurahStats(surahNumber: Int, nameEnglish: String, durationDelta: Long, ayahsDelta: Int) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val existing = quranDao.getSurahStats(surahNumber)
        if (existing != null) {
            val updated = existing.copy(
                totalDurationSeconds = existing.totalDurationSeconds + durationDelta,
                totalAyahsRead = existing.totalAyahsRead + ayahsDelta
            )
            quranDao.insertSurahStats(updated)
        } else {
            val newStats = com.example.data.model.SurahPlayStats(
                surahNumber = surahNumber,
                surahNameEnglish = nameEnglish,
                totalDurationSeconds = durationDelta,
                totalAyahsRead = ayahsDelta
            )
            quranDao.insertSurahStats(newStats)
        }
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                if (capabilities != null) {
                    capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } else {
                    @Suppress("DEPRECATION")
                    val activeNetworkInfo = cm.activeNetworkInfo
                    activeNetworkInfo != null && activeNetworkInfo.isConnected
                }
            } else {
                false
            }
        } catch (e: Exception) {
            true // fallback to true to allow api attempts on exception
        }
    }
}
