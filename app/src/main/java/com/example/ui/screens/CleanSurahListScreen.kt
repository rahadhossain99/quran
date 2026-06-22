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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.UiState
import com.example.data.model.SurahModel
import com.example.data.model.UserSettings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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

    // Filtering Options: All, Meccan, Medinan, Offline
    var selectedFilter by remember { mutableStateOf("all") }
    val filterOptions = remember {
        listOf(
            "সব সূরা" to "all",
            "মাক্কী" to "meccan",
            "মাদানী" to "medinan",
            "অফলাইন" to "offline"
        )
    }

    // Dynamic filtering on top of search queries
    val finalDisplayList = remember(filteredList, selectedFilter, downloadedJson) {
        filteredList.filter { surah ->
            when (selectedFilter) {
                "meccan" -> surah.revelationType.lowercase() == "meccan"
                "medinan" -> surah.revelationType.lowercase() == "medinan"
                "offline" -> downloadedJson.contains("\"${surah.number}\"")
                else -> true
            }
        }
    }

    // Curated Ayat Quote
    val inspirationalQuotes = remember {
        listOf(
            "“হে আমার বান্দাগণ! আল্লাহর রহমত থেকে নিরাশ হয়ো না।” ✨\n— সূরা আজ-জুমার: ৫৩",
            "“আল্লাহ কাউকে তার সাধ্যের অতিরিক্ত দায়িত্ব অর্পণ করেন না।” 🌱\n— সূরা আল-বাকারাহ: ২৮৬",
            "“নিশ্চয়ই কষ্টের সাথেই স্বস্তি রয়েছে।” 🤲\n— সূরা আশ-শরহ: ৬",
            "“তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ করব।” 🕋\n— সূরা আল-বাকারাহ: ১৫২",
            "“আর যখন আমার বান্দারা জিজ্ঞাসা করে, নিশ্চয় আমি নিকটে আছি।” ❤️\n— সূরা আল-বাকারাহ: ১৮৬",
            "“ধৈর্য ও সালাতের মাধ্যমে আল্লাহর সাহায্য প্রার্থনা করো।” 🛡️\n— সূরা আল-বাকারাহ: ১৫৩"
        )
    }
    
    val selectedQuote = remember {
        val calendar = java.util.Calendar.getInstance()
        val index = calendar.get(java.util.Calendar.DAY_OF_YEAR) % inspirationalQuotes.size
        inspirationalQuotes[index]
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Celestial Premium Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = quranColors.primarySoft.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, quranColors.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(quranColors.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ক্লিন মোড",
                            color = quranColors.textMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "Tranquil",
                        color = quranColors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(quranColors.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Beautiful centered motivational quote
                Text(
                    text = selectedQuote,
                    color = quranColors.textMain.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Sleek Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("সূরা অনুসন্ধান করুন...", color = quranColors.textMuted.copy(alpha = 0.6f), fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = quranColors.background,
                    unfocusedContainerColor = quranColors.surface.copy(alpha = 0.8f),
                    disabledContainerColor = quranColors.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = quranColors.textMain,
                    unfocusedTextColor = quranColors.textMain
                ),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = quranColors.primary)
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        quranColors.borderColor.copy(alpha = 0.4f),
                        RoundedCornerShape(24.dp)
                    )
            )
        }

        // Luxurious Filter Pillar Bar (Sub-navigation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterOptions.forEach { opt ->
                val isSelected = selectedFilter == opt.second
                val chipBg = if (isSelected) quranColors.primary else quranColors.surface
                val chipText = if (isSelected) Color.White else quranColors.textMuted
                val chipBorder = if (isSelected) quranColors.primary else quranColors.borderColor

                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(18.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedFilter = opt.second
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt.first,
                        color = chipText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Surah List
        if (surahListState is com.example.viewmodel.UiState.Success) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp)
            ) {
                if (finalDisplayList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "কোনো সূরা পাওয়া যায়নি",
                                color = quranColors.textMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    items(finalDisplayList, key = { it.number }) { surah ->
                        val isPlaying = isServicePlaying && serviceSurah?.number == surah.number
                        val isAudioDownloaded = downloadedAudioSet.contains(surah.number)
                        val isDownloaded = downloadedJson.contains("\"${surah.number}\"")

                        CleanSurahCardRow(
                            surah = surah,
                            isPlaying = isPlaying,
                            isAudioDownloaded = isAudioDownloaded,
                            isDownloaded = isDownloaded,
                            isOfflineMode = !isOnline,
                            settings = settings,
                            viewModel = viewModel,
                            ongoingDownloads = ongoingDownloads,
                            onSurahClicked = {
                                viewModel.loadSurahReadingView(surah.number)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiveAudioWaveform(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(2.5.dp).fillMaxHeight(h1).background(color, RoundedCornerShape(1.dp)))
        Box(Modifier.width(2.5.dp).fillMaxHeight(h2).background(color, RoundedCornerShape(1.dp)))
        Box(Modifier.width(2.5.dp).fillMaxHeight(h3).background(color, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun CleanSurahCardRow(
    surah: SurahModel,
    isPlaying: Boolean,
    isAudioDownloaded: Boolean,
    isDownloaded: Boolean,
    isOfflineMode: Boolean,
    settings: UserSettings,
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    onSurahClicked: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val haptic = LocalHapticFeedback.current
    val isCurrentDownloading = ongoingDownloads.containsKey(surah.number)
    val downloadProgress = ongoingDownloads[surah.number] ?: 0f

    val alphaMultiplier = if (isOfflineMode && !isDownloaded) 0.4f else 1f

    val toBanglaDigits = remember {
        { input: String ->
            val english = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            val bangla = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            var result = input
            for (i in 0..9) {
                result = result.replace(english[i], bangla[i])
            }
            result
        }
    }

    val typeBng = if (surah.revelationType.lowercase() == "meccan") "মাক্কী" else "মাদানী"
    val ayahsBng = toBanglaDigits(surah.numberOfAyahs.toString())
    val numberBng = toBanglaDigits(surah.number.toString())

    Card(
        onClick = {
            if (!isOfflineMode || isDownloaded) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSurahClicked()
            }
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) quranColors.primary.copy(alpha = 0.08f) else quranColors.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isPlaying) quranColors.primary.copy(alpha = 0.4f) else quranColors.borderColor.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant star logo frame hosting number
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) quranColors.primary.copy(alpha = 0.15f) else quranColors.background)
                    .border(
                        1.dp,
                        if (isPlaying) quranColors.primary.copy(alpha = 0.3f) else quranColors.borderColor,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = numberBng,
                    color = if (isPlaying) quranColors.primary else quranColors.textMain.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Surah Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = surah.englishName,
                        color = quranColors.textMain.copy(alpha = alphaMultiplier),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${surah.englishNameTranslation})",
                        color = quranColors.textMuted.copy(alpha = 0.6f * alphaMultiplier),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$typeBng • $ayahsBng আয়াত",
                        color = quranColors.textMuted.copy(alpha = 0.75f * alphaMultiplier),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isCurrentDownloading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            color = quranColors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    } else if (isPlaying) {
                        Spacer(modifier = Modifier.width(10.dp))
                        LiveAudioWaveform(
                            color = quranColors.primary,
                            modifier = Modifier.height(10.dp)
                        )
                    }
                }
            }

            // Arabic Name
            Text(
                surah.name,
                color = if (isPlaying) quranColors.primary else quranColors.textMain.copy(alpha = alphaMultiplier),
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Right side visual (Action icon or Play state)
            if (isCurrentDownloading) {
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    color = quranColors.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isAudioDownloaded) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(quranColors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = "Downloaded",
                        tint = quranColors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else {
                if (!isOfflineMode) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(quranColors.background)
                            .border(1.dp, quranColors.borderColor, CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ongoingDownloads[surah.number] = 0.01f
                                viewModel.downloadSurahOffline(
                                    surahNumber = surah.number,
                                    qari = settings.selectedQari,
                                    onProgress = { prg -> ongoingDownloads[surah.number] = prg },
                                    onCompleted = { ongoingDownloads.remove(surah.number) }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download",
                            tint = quranColors.textMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        tint = quranColors.textMuted.copy(alpha = 0.2f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
