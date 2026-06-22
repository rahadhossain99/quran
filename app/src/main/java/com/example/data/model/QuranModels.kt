package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

// REST API Models
@Immutable
@JsonClass(generateAdapter = true)
data class SurahListResponse(
    val code: Int,
    val status: String,
    val data: List<SurahModel>
)

@Immutable
@JsonClass(generateAdapter = true)
data class SurahModel(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

@Immutable
@JsonClass(generateAdapter = true)
data class SurahEditionsResponse(
    val code: Int,
    val status: String,
    val data: List<SurahEditionModel>
)

@Immutable
@JsonClass(generateAdapter = true)
data class SurahEditionModel(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val ayahs: List<AyahEditionModel>
)

@Immutable
@JsonClass(generateAdapter = true)
data class AyahEditionModel(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val audio: String? = null
)

// UI Friendly Unified Models
@Immutable
data class FormattedSurah(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val ayahs: List<FormattedAyah>
)

@Immutable
data class FormattedAyah(
    val numberInSurah: Int,
    val arabicText: String,
    val bengaliText: String,
    val transliterationText: String,
    val audioUrl: String
)

// Room Database Entities
@Immutable
@Entity(tableName = "favorite_surahs")
data class FavoriteSurah(
    @PrimaryKey val surahNumber: Int,
    val englishName: String,
    val nameArabic: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "last_played")
data class LastPlayed(
    @PrimaryKey val id: Int = 1,
    val surahNumber: Int,
    val ayahIndex: Int,
    val surahEnglishName: String,
    val surahNameArabic: String,
    val ayahNumberInSurah: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val theme: String = "light",
    val arabicFontSize: Int = 44,
    val translationFontSize: Int = 21,
    val selectedQari: String = "ar.alafasy",
    val dailyNotificationEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val downloadedSurahsJson: String = "",
    val notificationStyle: String = "classic_green",
    val widgetStyle: String = "classic_green",
    val cleanNeoEnabled: Boolean = false,
    val installationDate: String = ""
)

@Immutable
@Entity(tableName = "quran_stats")
data class QuranStats(
    @PrimaryKey val dateStr: String, // "yyyy-MM-dd"
    val durationSeconds: Long = 0L,
    val ayahsReadCount: Int = 0
)

@Immutable
@Entity(tableName = "surah_play_stats")
data class SurahPlayStats(
    @PrimaryKey val surahNumber: Int,
    val surahNameEnglish: String,
    val totalDurationSeconds: Long = 0L,
    val totalAyahsRead: Int = 0
)

// Qari Structure
@Immutable
data class Qari(
    val id: String,
    val name: String,
    val englishName: String
)

// List of all reciters (Qaris)
val QARIS_LIST = listOf(
    Qari("ar.alafasy", "মিশারি আল-আফাসি", "Mishary Rashid Alafasy"),
    Qari("ar.abdurrahmaansudais", "আব্দুর রহমান আস-সুদাইস", "Abdur-Rahman as-Sudais"),
    Qari("ar.abdulbasitmurattal", "আব্দুল বাসিত মুরাত্তাল", "AbdulBaset AbdulSamad"),
    Qari("ar.husary", "খলিল আল-হুসারি", "Mahmoud Khalil Al-Husary"),
    Qari("ar.saadalghamidi", "সাদ আল-গামদি", "Saad al-Ghamidi"),
    Qari("ar.mahermuaiqly", "মাহের আল মুআইক্লি", "Maher Al Muaiqly")
)
