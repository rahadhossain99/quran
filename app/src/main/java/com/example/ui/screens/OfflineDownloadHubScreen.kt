package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SurahModel
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.QuranViewModel.DownloadedSurahItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDownloadHubScreen(
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    onClose: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val downloadState by viewModel.bulkDownloadState.collectAsStateWithLifecycle()
    val downloadedList by viewModel.downloadedSurahsList.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val filteredDownloadedList = remember(downloadedList, searchQuery) {
        if (searchQuery.isBlank()) {
            downloadedList
        } else {
            downloadedList.filter {
                it.englishName.contains(searchQuery, ignoreCase = true) ||
                        it.number.toString() == searchQuery.trim()
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "মুছে ফেলুন",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "সংরক্ষণাগার খালি করুন",
                        color = quranColors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    "আপনি কি নিশ্চিত যে আপনার ডাউনলোড করা সকল সূরার অডিও এবং অনুবাদ ক্যাশ ফাইল মুছে ফেলতে চান? এর ফলে সম্পূর্ণ অফলাইন এক্সেস বন্ধ হয়ে যাবে।",
                    color = quranColors.textMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteAllDownloadedSurahs()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("মুছে ফেলুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("বাতিল", color = quranColors.textMuted)
                }
            },
            containerColor = quranColors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = quranColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Storage subheader
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = null,
                        tint = quranColors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সংরক্ষণাগার ব্যবস্থাপনা ও ক্যাশ",
                        color = quranColors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // General status card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                    border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট ডাউনলোডকৃত সূরা:",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${downloadedList.size} টি সূূরা",
                                color = quranColors.primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .background(quranColors.primarySoft, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (downloadedList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDeleteConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f),
                                    contentColor = Color.Red
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ডাউনলোড ডেটা চিরতরে মুছে ফেলুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Real-time bulk progress or downloader options
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                    border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ওয়ান-ক্লিক অফলাইন ডাউনলোড",
                            color = quranColors.textMain,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "একটি ক্লিকে ১১৪ টি সূরার ডেটা ডাউনলোড করে অফলাইন করে নিন। ওয়াইফাই সংযোগ ব্যবহারের পরামর্শ দেওয়া হচ্ছে।",
                            color = quranColors.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (downloadState.isDownloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(quranColors.background, RoundedCornerShape(14.dp))
                                    .border(BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (downloadState.type == "TEXT") "অনুবাদ ডাউনলোড হচ্ছে..." else "অডিও ডাউনলোড হচ্ছে...",
                                        color = quranColors.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${downloadState.percentage}%",
                                        color = quranColors.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { downloadState.percentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = quranColors.primary,
                                    trackColor = quranColors.borderColor.copy(alpha = 0.3f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "সূরা: ${downloadState.currentSurahName} (${downloadState.currentSurah}/114)",
                                    color = quranColors.textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.cancelBulkDownload(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ডাউনলোড বাতিল করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        } else {
                            if (downloadState.error != null) {
                                Text(
                                    text = "ত্রুটি ঘটেছে: ${downloadState.error}",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.startBulkDownload(context, "TEXT")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = quranColors.primarySoft, contentColor = quranColors.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("অনুবাদ শুধুমাত্র", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.startBulkDownload(context, "AUDIO")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = quranColors.primary, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("অডিওসহ সম্পূর্ণ", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Search bar for storage list
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("সংরক্ষণাগার খুঁজুন (নাম বা নম্বর)", color = quranColors.textMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = quranColors.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = quranColors.surface,
                        unfocusedContainerColor = quranColors.surface,
                        focusedBorderColor = quranColors.primary,
                        unfocusedBorderColor = quranColors.borderColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // List of items
            if (filteredDownloadedList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = quranColors.textMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "সংরক্ষণাগারে কোনো সূরা ডাউনলোড করা নেই" else "ফলাফল পাওয়া যায়নি",
                            color = quranColors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(filteredDownloadedList, key = { it.number }) { surahItem ->
                    val isSingleLoading = ongoingDownloads.containsKey(surahItem.number)
                    val progressVal = ongoingDownloads[surahItem.number] ?: 0f

                    val cardColor = if (surahItem.hasAudio) {
                        quranColors.primarySoft.copy(alpha = 0.85f)
                    } else {
                        quranColors.surface
                    }

                    val borderC = if (surahItem.hasAudio) {
                        BorderStroke(1.dp, quranColors.primary.copy(alpha = 0.4f))
                    } else {
                        BorderStroke(1.dp, quranColors.borderColor)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardColor)
                            .border(borderC, RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.loadSurahReadingView(surahItem.number)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (surahItem.hasAudio) quranColors.primarySoft else quranColors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = surahItem.number.toString(),
                                color = if (surahItem.hasAudio) quranColors.primary else quranColors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surahItem.englishName,
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (surahItem.hasAudio) "অনুবাদ ও অডিও অফলাইনে প্রস্তুত" else "অনুবাদ প্রস্তুত (অডিও ডাউনলোড বাকি)",
                                color = if (surahItem.hasAudio) quranColors.primary else quranColors.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!isOnline && !surahItem.hasAudio) {
                            // Offline and no audio, cannot download
                        } else {
                            if (isSingleLoading) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { progressVal },
                                        color = quranColors.primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else if (!surahItem.hasAudio) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        ongoingDownloads[surahItem.number] = 0.01f
                                        viewModel.downloadSurahOffline(
                                            surahNumber = surahItem.number,
                                            qari = settings.selectedQari,
                                            onProgress = { prg -> ongoingDownloads[surahItem.number] = prg },
                                            onCompleted = { success -> ongoingDownloads.remove(surahItem.number) }
                                        )
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "অডিও ডাউনলোড",
                                        tint = quranColors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.playSpecificSurah(surahItem.number)
                                        onClose()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "শুনুন",
                                        tint = quranColors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteDownloadedSurah(surahItem.number)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "মুছে ফেলুন",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
