package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.UiState
import com.example.data.model.SurahModel
import com.example.data.model.UserSettings
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Juz mapping
private val juzToSurahs = mapOf(
    1 to listOf(1, 2),
    2 to listOf(2),
    3 to listOf(2, 3),
    4 to listOf(3, 4),
    5 to listOf(4),
    6 to listOf(4, 5),
    7 to listOf(5, 6),
    8 to listOf(6, 7),
    9 to listOf(7, 8),
    10 to listOf(8, 9),
    11 to listOf(9, 10, 11),
    12 to listOf(11, 12),
    13 to listOf(12, 13, 14),
    14 to listOf(15, 16),
    15 to listOf(17, 18),
    16 to listOf(18, 19, 20),
    17 to listOf(21, 22),
    18 to listOf(23, 24, 25),
    19 to listOf(25, 26, 27),
    20 to listOf(27, 28, 29),
    21 to listOf(29, 30, 31, 32, 33),
    22 to listOf(33, 34, 35, 36),
    23 to listOf(36, 37, 38, 39),
    24 to listOf(39, 40, 41),
    25 to listOf(41, 42, 43, 44, 45),
    26 to listOf(46, 47, 48, 49, 50, 51),
    27 to listOf(51, 52, 53, 54, 55, 56, 57),
    28 to listOf(58, 59, 60, 61, 62, 63, 64, 65, 66),
    29 to listOf(67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77),
    30 to (78..114).toList()
)

fun getBanglaOrdinalPara(juz: Int): String {
    val ord = when (juz) {
        1 -> "১ম"
        2 -> "২য়"
        3 -> "৩য়"
        4 -> "৪র্থ"
        5 -> "৫ম"
        6 -> "৬ষ্ঠ"
        7 -> "৭ম"
        8 -> " ৮ম"
        9 -> "৯ম"
        10 -> "১০ম"
        else -> {
            val engDigits = juz.toString()
            val bngDigits = engDigits.map { "০১২৩৪৫৬৭৮৯"[it - '0'] }.joinToString("")
            "${bngDigits}তম"
        }
    }
    return "$ord পারা"
}

@Composable
fun VerticalParaScroller(
    selectedJuz: Int?,
    onJuzSelected: (Int?) -> Unit,
    toBanglaDigits: (String) -> String,
    quranColors: QuranColors,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "সব" item at the top
        item {
            val isSelected = selectedJuz == null
            val bg = if (isSelected) quranColors.primary else Color.Transparent
            val textColor = if (isSelected) Color.White else quranColors.textMain
            val border = if (isSelected) Color.Transparent else quranColors.borderColor.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(12.dp))
                    .clickable { onJuzSelected(null) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "সব পারা",
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 30 Paras (Juz)
        items((1..30).toList()) { juz ->
            val isSelected = selectedJuz == juz
            val bg = if (isSelected) quranColors.primary.copy(alpha = 0.95f) else Color.Transparent
            val textColor = if (isSelected) Color.White else quranColors.textMain
            val border = if (isSelected) Color.Transparent else quranColors.borderColor.copy(alpha = 0.4f)
            
            val ordName = remember(juz) { getBanglaOrdinalPara(juz) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(12.dp))
                    .clickable { onJuzSelected(juz) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = ordName.split(" ")[0], // "১ম", "২৫তম", etc.
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "পারা",
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else quranColors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CleanSurahCardRowCompact(
    surah: SurahModel,
    isPlaying: Boolean,
    isAudioDownloaded: Boolean,
    isTextDownloaded: Boolean,
    isOfflineMode: Boolean,
    settings: UserSettings,
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    quranColors: QuranColors,
    onSurahClicked: () -> Unit
) {
    val ongoingTextDownloads = remember { mutableStateMapOf<Int, Float>() }
    val isDownloadingAudio = ongoingDownloads.containsKey(surah.number)
    val audioProgress = ongoingDownloads[surah.number] ?: 0f

    val isDownloadingText = ongoingTextDownloads.containsKey(surah.number)
    val textProgress = ongoingTextDownloads[surah.number] ?: 0f

    val alphaMultiplier = if (isOfflineMode && !isTextDownloaded) 0.5f else 1f

    val typeBng = if (surah.revelationType.lowercase() == "meccan") "মাক্কী" else "মাদানী"
    val toBanglaDigits = { input: String ->
        input.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")
    }
    val ayahsBng = toBanglaDigits(surah.numberOfAyahs.toString())
    val numberBng = toBanglaDigits(surah.number.toString())

    val cardBg = if (isPlaying) quranColors.primary.copy(alpha = 0.04f) else quranColors.surface
    val cardBorder = if (isPlaying) quranColors.primary.copy(alpha = 0.25f) else quranColors.borderColor.copy(alpha = 0.4f)

    Card(
        onClick = {
            if (!isOfflineMode || isTextDownloaded) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSurahClicked()
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .graphicsLayer { alpha = alphaMultiplier }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Box
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlaying) quranColors.primary else quranColors.background)
                    .border(1.dp, if (isPlaying) Color.Transparent else quranColors.borderColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                     LiveAudioWaveform(Color.White, Modifier.height(11.dp))
                } else {
                    Text(numberBng, color = quranColors.textMain, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    surah.englishName,
                    color = quranColors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "$typeBng • $ayahsBng আয়াত",
                    color = quranColors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Downloads Compact
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadActionNodeCompact(
                    icon = Icons.Rounded.Article,
                    isDownloaded = isTextDownloaded,
                    isDownloading = isDownloadingText,
                    progress = textProgress,
                    isOfflineMode = isOfflineMode,
                    quranColors = quranColors,
                    haptic = haptic,
                    onDownloadRequest = {
                        ongoingTextDownloads[surah.number] = 0.1f
                        viewModel.downloadSurahTextOnly(
                            surahNumber = surah.number,
                            qari = settings.selectedQari,
                            onProgress = { prg -> ongoingTextDownloads[surah.number] = prg },
                            onCompleted = { ongoingTextDownloads.remove(surah.number) }
                        )
                    }
                )

                DownloadActionNodeCompact(
                    icon = Icons.Rounded.Headphones,
                    isDownloaded = isAudioDownloaded,
                    isDownloading = isDownloadingAudio,
                    progress = audioProgress,
                    isOfflineMode = isOfflineMode,
                    quranColors = quranColors,
                    haptic = haptic,
                    onDownloadRequest = {
                        ongoingDownloads[surah.number] = 0.01f
                        viewModel.downloadSurahAudioOnly(
                            surahNumber = surah.number,
                            qari = settings.selectedQari,
                            onProgress = { prg -> ongoingDownloads[surah.number] = prg },
                            onCompleted = { ongoingDownloads.remove(surah.number) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadActionNodeCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: Float,
    isOfflineMode: Boolean,
    quranColors: QuranColors,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDownloadRequest: () -> Unit
) {
    if (isDownloading) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            CircularProgressIndicator(progress = { progress }, color = quranColors.primary, strokeWidth = 1.5.dp, modifier = Modifier.fillMaxSize())
            Icon(icon, null, tint = quranColors.primary, modifier = Modifier.size(10.dp))
        }
    } else if (isDownloaded) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(quranColors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CheckCircle, "Downloaded", tint = quranColors.primary, modifier = Modifier.size(14.dp))
        }
    } else {
        if (!isOfflineMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(quranColors.surface)
                    .border(1.dp, quranColors.borderColor, CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDownloadRequest()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, "Download", tint = quranColors.textMuted, modifier = Modifier.size(11.dp))
            }
        } else {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(icon, "Offline", tint = quranColors.textMuted.copy(alpha=0.3f), modifier = Modifier.size(11.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanHomeScreen(
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    onOpenDownloadHub: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val surahListState by viewModel.surahListState.collectAsStateWithLifecycle()
    val filteredList by viewModel.filteredSurahs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val downloadedList by viewModel.downloadedSurahsList.collectAsStateWithLifecycle()
    val downloadedAudioSet = remember(downloadedList) { downloadedList.filter { it.hasAudio }.map { it.number }.toSet() }
    val downloadedJson = settings.downloadedSurahsJson
    val haptic = LocalHapticFeedback.current

    val playerService by viewModel.playerService.collectAsStateWithLifecycle()
    val isServicePlaying = playerService?.isPlaying?.collectAsStateWithLifecycle()?.value ?: false
    val serviceSurah = playerService?.currentSurah?.collectAsStateWithLifecycle()?.value

    var selectedFilter by remember { mutableStateOf("all") }
    val filterOptions = remember {
        listOf("সব সূরা" to "all", "মাক্কী" to "meccan", "মাদানী" to "medinan", "অফলাইন" to "offline")
    }

    var selectedJuz by remember { mutableStateOf<Int?>(null) }

    val toBanglaDigits = remember {
        { input: String ->
            input.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")
        }
    }

    val finalDisplayList = remember(filteredList, selectedFilter, downloadedJson, selectedJuz) {
        val baseList = filteredList.filter { surah ->
            when (selectedFilter) {
                "meccan" -> surah.revelationType.lowercase() == "meccan"
                "medinan" -> surah.revelationType.lowercase() == "medinan"
                "offline" -> downloadedJson.contains(",${surah.number},")
                else -> true
            }
        }
        if (selectedJuz != null) {
            val surahsInJuz = juzToSurahs[selectedJuz] ?: emptyList()
            baseList.filter { surahsInJuz.contains(it.number) }
        } else {
            baseList
        }
    }

    // Modern greeting
    val celestialGreeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> Pair("সুপ্রভাত", "কুরআনের আলোয় স্নিগ্ধ হোক সকাল")
            in 12..15 -> Pair("শুভ দুপুর", "তিলওয়াতে প্রশান্ত হোক কর্মব্যস্ত দুপুর")
            in 16..17 -> Pair("শুভ বিকেল", "স্নিগ্ধ বিকেলে রহমতের ছায়া পড়ুক")
            else -> Pair("শুভ রাত্রি", "কুরআনের সাকিনায় শান্ত হোক হৃদয়")
        }
    }

    // Parallax & immersive brushes - beautifully tailored to light, sepia, and dark themes
    val bgBrush = remember(quranColors) {
        val middleColor = if (quranColors.isDark) {
            Color(0xFF111827) // Soft dark indigo-slate for deep midnight feeling
        } else if (quranColors.background == Color(0xFFF4ECD8)) {
            Color(0xFFFAF2DE) // Warm glowing cream for Sepia theme (0xFFF4ECD8 is Sepia background)
        } else {
            Color(0xFFFFFFFF) // Pure sparkling white for Light theme
        }
        Brush.verticalGradient(
            colors = listOf(
                quranColors.background,
                middleColor,
                quranColors.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Decorative glowing orbs (Redesigned uniqueness for unified green theme)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .size(250.dp)
                .blur(80.dp)
                .background(quranColors.primary.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = (-100).dp)
                .size(200.dp)
                .blur(60.dp)
                .background(quranColors.primary.copy(alpha = 0.10f), CircleShape)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Header Section
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = celestialGreeting.first,
                        color = quranColors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = celestialGreeting.second,
                        color = quranColors.textMain,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 25.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Minimalist Search
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("কী খুঁজছেন?", color = quranColors.textMuted.copy(alpha = 0.6f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = quranColors.primary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setSearchQuery("") },
                                    tint = quranColors.textMuted
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = quranColors.surface,
                            unfocusedContainerColor = quranColors.surface.copy(alpha = 0.7f),
                            disabledContainerColor = quranColors.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = quranColors.textMain
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, quranColors.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Player / Quick Action
                if (serviceSurah != null) {
                    val surahNumStr = serviceSurah.number.toString().map { "০১২৩৪৫৬৭৮৯"[it - '0'] }.joinToString("")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = quranColors.primary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, quranColors.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pulsing play disc
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(quranColors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                LiveAudioWaveform(Color.White, Modifier.height(12.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = serviceSurah.englishName,
                                    color = quranColors.textMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "বর্তমানে চলছে • সূরা $surahNumStr",
                                    color = quranColors.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // Controls with premium tactile styling and expanded spacedBy for ultimate mobile responsiveness
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(quranColors.surface)
                                        .border(1.dp, quranColors.borderColor.copy(alpha = 0.6f), CircleShape)
                                        .clickable { playerService?.playPrev() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous Surah",
                                        tint = quranColors.textMain,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp) // Beautiful visual hierarchy with a slightly larger central Play action
                                        .clip(CircleShape)
                                        .background(quranColors.primary)
                                        .clickable { playerService?.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isServicePlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Toggle Play Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(quranColors.surface)
                                        .border(1.dp, quranColors.borderColor.copy(alpha = 0.6f), CircleShape)
                                        .clickable { playerService?.playNext() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next Surah",
                                        tint = quranColors.textMain,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Segmented Filters
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { opt ->
                        val selected = selectedFilter == opt.second
                        val bg = if (selected) quranColors.primary else quranColors.surface.copy(alpha = 0.5f)
                        val textColor = if (selected) Color.White else quranColors.textMuted
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(1.dp, if (selected) quranColors.primary else quranColors.borderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedFilter = opt.second
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(opt.first, color = textColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Split content: VerticalParaScroller (left) + Surah items (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                VerticalParaScroller(
                    selectedJuz = selectedJuz,
                    onJuzSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedJuz = it
                    },
                    toBanglaDigits = toBanglaDigits,
                    quranColors = quranColors,
                    modifier = Modifier
                        .width(78.dp)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(quranColors.borderColor.copy(alpha = 0.4f))
                )

                // Right Pane: list of Surahs matching JUZ filter & Segmented selector
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 6.dp)
                    ) {
                        // Small indicator of matching Para
                        if (selectedJuz != null) {
                            item {
                                val matchCountBng = toBanglaDigits(finalDisplayList.size.toString())
                                val paraNameStr = remember(selectedJuz) { getBanglaOrdinalPara(selectedJuz!!) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(quranColors.primary.copy(alpha = 0.05f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$paraNameStr • $matchCountBng সূরা",
                                        color = quranColors.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Clear Juz filter",
                                        tint = quranColors.primary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedJuz = null
                                            }
                                    )
                                }
                            }
                        }

                        when (surahListState) {
                            is UiState.Loading -> {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = quranColors.primary)
                                    }
                                }
                            }
                            is UiState.Success -> {
                                if (finalDisplayList.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Rounded.SearchOff, null, tint = quranColors.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("কোনো সূরা মেলেনি", color = quranColors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                } else {
                                    items(finalDisplayList, key = { it.number }) { surah ->
                                        val isPlaying = isServicePlaying && serviceSurah?.number == surah.number
                                        val isAudioDownloaded = downloadedAudioSet.contains(surah.number)
                                        val isTextDownloaded = downloadedJson.contains(",${surah.number},")

                                        CleanSurahCardRowCompact(
                                            surah = surah,
                                            isPlaying = isPlaying,
                                            isAudioDownloaded = isAudioDownloaded,
                                            isTextDownloaded = isTextDownloaded,
                                            isOfflineMode = !isOnline,
                                            settings = settings,
                                            viewModel = viewModel,
                                            ongoingDownloads = ongoingDownloads,
                                            haptic = haptic,
                                            quranColors = quranColors,
                                            onSurahClicked = { viewModel.loadSurahReadingView(surah.number) }
                                        )
                                    }
                                }
                            }
                            is UiState.Error -> {
                                item {
                                    Text(
                                        text = (surahListState as UiState.Error).message,
                                        color = quranColors.accent,
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveAudioWaveform(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val h1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "h1")
    val h2 by infiniteTransition.animateFloat(1f, 0.2f, infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse), label = "h2")
    val h3 by infiniteTransition.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "h3")

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).fillMaxHeight(h1).background(color, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).fillMaxHeight(h2).background(color, RoundedCornerShape(1.5.dp)))
        Box(Modifier.width(3.dp).fillMaxHeight(h3).background(color, RoundedCornerShape(1.5.dp)))
    }
}

@Composable
fun CleanSurahCardRow(
    surah: SurahModel,
    isPlaying: Boolean,
    isAudioDownloaded: Boolean,
    isTextDownloaded: Boolean,
    isOfflineMode: Boolean,
    settings: UserSettings,
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    quranColors: QuranColors,
    onSurahClicked: () -> Unit
) {
    val ongoingTextDownloads = remember { mutableStateMapOf<Int, Float>() }
    val isDownloadingAudio = ongoingDownloads.containsKey(surah.number)
    val audioProgress = ongoingDownloads[surah.number] ?: 0f

    val isDownloadingText = ongoingTextDownloads.containsKey(surah.number)
    val textProgress = ongoingTextDownloads[surah.number] ?: 0f

    val alphaMultiplier = if (isOfflineMode && !isTextDownloaded) 0.5f else 1f

    val typeBng = if (surah.revelationType.lowercase() == "meccan") "মাক্কী" else "মাদানী"
    val toBanglaDigits = { input: String ->
        input.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")
    }
    val ayahsBng = toBanglaDigits(surah.numberOfAyahs.toString())
    val numberBng = toBanglaDigits(surah.number.toString())

    val cardBg = if (isPlaying) quranColors.primary.copy(alpha = 0.05f) else quranColors.surface
    val cardBorder = if (isPlaying) quranColors.primary.copy(alpha = 0.3f) else quranColors.borderColor.copy(alpha = 0.5f)

    Card(
        onClick = {
            if (!isOfflineMode || isTextDownloaded) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSurahClicked()
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .graphicsLayer { alpha = alphaMultiplier }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant Number Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPlaying) quranColors.primary else quranColors.background)
                    .border(1.dp, if (isPlaying) Color.Transparent else quranColors.borderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                     LiveAudioWaveform(Color.White, Modifier.height(14.dp))
                } else {
                    Text(numberBng, color = quranColors.textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(surah.englishName, color = quranColors.textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(${surah.englishNameTranslation})", color = quranColors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill=false))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$typeBng • $ayahsBng আয়াত", color = quranColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Custom Download Buttons Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // TEXT Download Button
                DownloadActionNode(
                    icon = Icons.Rounded.Article,
                    isDownloaded = isTextDownloaded,
                    isDownloading = isDownloadingText,
                    progress = textProgress,
                    isOfflineMode = isOfflineMode,
                    quranColors = quranColors,
                    haptic = haptic,
                    onDownloadRequest = {
                        ongoingTextDownloads[surah.number] = 0.1f
                        viewModel.downloadSurahTextOnly(
                            surahNumber = surah.number,
                            qari = settings.selectedQari,
                            onProgress = { prg -> ongoingTextDownloads[surah.number] = prg },
                            onCompleted = { ongoingTextDownloads.remove(surah.number) }
                        )
                    }
                )

                // AUDIO Download Button
                DownloadActionNode(
                    icon = Icons.Rounded.Headphones,
                    isDownloaded = isAudioDownloaded,
                    isDownloading = isDownloadingAudio,
                    progress = audioProgress,
                    isOfflineMode = isOfflineMode,
                    quranColors = quranColors,
                    haptic = haptic,
                    onDownloadRequest = {
                        ongoingDownloads[surah.number] = 0.01f
                        viewModel.downloadSurahAudioOnly(
                            surahNumber = surah.number,
                            qari = settings.selectedQari,
                            onProgress = { prg -> ongoingDownloads[surah.number] = prg },
                            onCompleted = { ongoingDownloads.remove(surah.number) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadActionNode(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: Float,
    isOfflineMode: Boolean,
    quranColors: QuranColors,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDownloadRequest: () -> Unit
) {
    if (isDownloading) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            CircularProgressIndicator(progress = { progress }, color = quranColors.primary, strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
            Icon(icon, null, tint = quranColors.primary, modifier = Modifier.size(12.dp))
        }
    } else if (isDownloaded) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(quranColors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CheckCircle, "Downloaded", tint = quranColors.primary, modifier = Modifier.size(16.dp))
        }
    } else {
        if (!isOfflineMode) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(quranColors.surface)
                    .border(1.dp, quranColors.borderColor, CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDownloadRequest()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, "Download", tint = quranColors.textMuted, modifier = Modifier.size(14.dp))
            }
        } else {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(icon, "Offline", tint = quranColors.textMuted.copy(alpha=0.3f), modifier = Modifier.size(14.dp))
            }
        }
    }
}
