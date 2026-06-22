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

    val finalDisplayList = remember(filteredList, selectedFilter, downloadedJson) {
        filteredList.filter { surah ->
            when (selectedFilter) {
                "meccan" -> surah.revelationType.lowercase() == "meccan"
                "medinan" -> surah.revelationType.lowercase() == "medinan"
                "offline" -> downloadedJson.contains(",${surah.number},")
                else -> true
            }
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

    // Parallax & immersive brushes
    val bgBrush = Brush.linearGradient(
        colors = listOf(
            quranColors.background,
            if (quranColors.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            quranColors.background
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Decorative glowing orbs (Redesigned uniqueness)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-40).dp)
                .size(250.dp)
                .blur(80.dp)
                .background(quranColors.primary.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = (-100).dp)
                .size(200.dp)
                .blur(60.dp)
                .background(quranColors.accent.copy(alpha = 0.1f), CircleShape)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp)
        ) {
            // Header Section
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = celestialGreeting.first,
                        color = quranColors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = celestialGreeting.second,
                        color = quranColors.textMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 30.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Minimalist Search
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("কী খুঁজছেন?", color = quranColors.textMuted.copy(alpha = 0.6f), fontSize = 15.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = quranColors.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close, "Clear",
                                    modifier = Modifier.clickable { viewModel.setSearchQuery("") },
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
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, quranColors.primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Player / Quick Action
            if (serviceSurah != null) {
                item {
                    val surahNumStr = serviceSurah.number.toString().map { "০১২৩৪৫৬৭৮৯"[it - '0'] }.joinToString("")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = quranColors.primary.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, quranColors.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pulsing play disc
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(quranColors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                LiveAudioWaveform(Color.White, Modifier.height(14.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = serviceSurah.englishName,
                                    color = quranColors.textMain,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "বর্তমানে চলছে • সূরা $surahNumStr",
                                    color = quranColors.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // Controls
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { playerService?.playPrev() }, modifier = Modifier.size(36.dp).background(quranColors.surface, CircleShape)) {
                                    Icon(Icons.Rounded.SkipPrevious, null, tint = quranColors.textMain, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { playerService?.togglePlayPause() }, modifier = Modifier.size(36.dp).background(quranColors.primary, CircleShape)) {
                                    Icon(if (isServicePlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { playerService?.playNext() }, modifier = Modifier.size(36.dp).background(quranColors.surface, CircleShape)) {
                                    Icon(Icons.Rounded.SkipNext, null, tint = quranColors.textMain, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Segmented Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { opt ->
                        val selected = selectedFilter == opt.second
                        val bg = if (selected) quranColors.primary else quranColors.surface.copy(alpha = 0.5f)
                        val textColor = if (selected) Color.White else quranColors.textMuted
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.dp, if (selected) quranColors.primary else quranColors.borderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedFilter = opt.second
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(opt.first, color = textColor, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // List Content
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Rounded.SearchOff, null, tint = quranColors.textMuted.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("কিছু পাওয়া যায়নি", color = quranColors.textMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        items(finalDisplayList, key = { it.number }) { surah ->
                            val isPlaying = isServicePlaying && serviceSurah?.number == surah.number
                            val isAudioDownloaded = downloadedAudioSet.contains(surah.number)
                            val isTextDownloaded = downloadedJson.contains(",${surah.number},")

                            CleanSurahCardRow(
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
