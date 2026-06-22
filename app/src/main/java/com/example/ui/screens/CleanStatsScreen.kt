package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.QuranViewModel
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun CleanStatsScreen(viewModel: QuranViewModel) {
    val quranColors = LocalQuranColors.current
    val statsList by viewModel.allStats.collectAsStateWithLifecycle()
    val allSurahStats by viewModel.allSurahStats.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Animations block
    var startAnim by remember { mutableStateOf(false) }
    val entryAlpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, tween(1000))
    val entryOffset by animateFloatAsState(targetValue = if (startAnim) 0f else 50f, spring(stiffness = Spring.StiffnessLow))
    
    LaunchedEffect(Unit) { startAnim = true }

    // Helpers
    val toBanglaDigits = { input: String -> input.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("") }
    
    val continuousStreak = remember(statsList) {
        var streak = 0
        val calendar = java.util.Calendar.getInstance()
        val sdfLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        while (true) {
            val checkStr = sdfLocal.format(calendar.time)
            val found = statsList.find { it.dateStr == checkStr }
            if (found != null && found.durationSeconds > 0) {
                streak++
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                if (streak == 0) {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = sdfLocal.format(calendar.time)
                    val foundYest = statsList.find { it.dateStr == yesterdayStr }
                    if (foundYest != null && foundYest.durationSeconds > 0) {
                        streak++
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                }
                break
            }
        }
        streak
    }

    val totalDurationSeconds = remember(statsList) { statsList.sumOf { it.durationSeconds } }
    val totalHours = totalDurationSeconds / 3600
    val totalAyahsRead = remember(statsList) { statsList.sumOf { it.ayahsReadCount } }
    
    val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val todayStats = remember(statsList, todayStr) { statsList.find { it.dateStr == todayStr } ?: com.example.data.model.QuranStats(todayStr, 0L, 0) }

    // BG Mesh
    Box(
        modifier = Modifier.fillMaxSize().background(quranColors.background)
    ) {
        // Subtle animated glow orb
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowScale by infiniteTransition.animateFloat(1f, 1.3f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "gs")
        
        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 60.dp, y = (-50).dp).size(200.dp).scale(glowScale).blur(100.dp).background(quranColors.primary.copy(alpha=0.15f), CircleShape))

        LazyColumn(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = entryAlpha; translationY = entryOffset },
            contentPadding = PaddingValues(bottom = 120.dp, top = 24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("আপনার অগ্রগতি", color = quranColors.textMain, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("আল্লাহর পথে আপনার ধারাবাহিকতা", color = quranColors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Central Premium Ring Chart
            item {
                PremiumDailyGoalRing(todayStats, quranColors)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Main Stat Cards
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.LocalFireDepartment,
                        iconTint = Color(0xFFFF6B6B),
                        title = toBanglaDigits("$continuousStreak দিন"),
                        subtitle = "ধারাবাহিকতা",
                        quranColors = quranColors
                    )
                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Headphones,
                        iconTint = quranColors.primary,
                        title = toBanglaDigits("$totalHours ঘণ্টা"),
                        subtitle = "মোট অডিও শ্রবণ",
                        quranColors = quranColors
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                 Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val mostPlayedSurah by viewModel.mostPlayedSurah.collectAsStateWithLifecycle()
                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Favorite,
                        iconTint = Color(0xFFFF4E50),
                        title = mostPlayedSurah?.surahNameEnglish ?: "---",
                        subtitle = "প্রিয় সূরা",
                        quranColors = quranColors
                    )
                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.AutoStories,
                        iconTint = Color(0xFFF9A826),
                        title = toBanglaDigits("$totalAyahsRead টি"),
                        subtitle = "পঠিত আয়াত",
                        quranColors = quranColors
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Elegant Weekly Chart
            item {
                Text("সাপ্তাহিক বিশ্লেষণ", color = quranColors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))
                PremiumWeeklyChart(statsList, quranColors, haptic)
            }
        }
    }
}

@Composable
fun PremiumDailyGoalRing(todayStats: com.example.data.model.QuranStats, quranColors: QuranColors) {
    val goalMinutes = 15
    val currentMinutes = todayStats.durationSeconds / 60
    val progressRatio = (currentMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
    
    var startAnim by remember { mutableStateOf(false) }
    val animatedRatio by animateFloatAsState(targetValue = if (startAnim) progressRatio else 0f, spring(dampingRatio = 0.5f, stiffness = 50f))
    
    LaunchedEffect(Unit) { startAnim = true }

    val toBanglaDigits = { input: String -> input.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("") }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val outerKnob = quranColors.primary
        val innerCircle = quranColors.accent

        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            // Background shadow ring
            Box(modifier = Modifier.size(200.dp).shadow(24.dp, CircleShape).background(quranColors.surface, CircleShape))

            Canvas(modifier = Modifier.size(170.dp)) {
                // Background Track
                drawArc(
                    color = quranColors.borderColor.copy(alpha=0.3f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
                // Fill Track
                drawArc(
                    brush = Brush.sweepGradient(listOf(innerCircle, outerKnob, innerCircle)),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedRatio,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(toBanglaDigits("$currentMinutes"), color = quranColors.textMain, fontSize = 42.sp, fontWeight = FontWeight.Black)
                Text("আজকের মিনিট", color = quranColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun StatGlowCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    quranColors: QuranColors
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha=0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(alpha=0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(title, color = quranColors.textMain, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = quranColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PremiumWeeklyChart(statsList: List<com.example.data.model.QuranStats>, quranColors: QuranColors, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val bngDays = mapOf("Sat" to "শনি", "Sun" to "রবি", "Mon" to "সোম", "Tue" to "মঙ্গল", "Wed" to "বুধ", "Thu" to "বৃহ", "Fri" to "শুক্র")
    val pastSevenDays = remember(statsList) {
        val sdfDateLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfDayLocal = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
        
        List(7) { 
            val date = calendar.time
            val dStr = sdfDateLocal.format(date)
            val dur = statsList.find { it.dateStr == dStr }?.durationSeconds ?: 0L
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            Triple(dStr, bngDays[sdfDayLocal.format(date)] ?: "", dur)
        }
    }

    val maxDuration = remember(pastSevenDays) { pastSevenDays.maxOfOrNull { it.third }?.coerceAtLeast(300L) ?: 300L }

    var selectedIndex by remember { mutableStateOf(6) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = quranColors.surface.copy(alpha=0.8f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, quranColors.borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            pastSevenDays.forEachIndexed { index, triple ->
                val isSelected = index == selectedIndex
                val ratio = (triple.third.toFloat() / maxDuration).coerceIn(0.05f, 1f)
                
                var barEntry by remember { mutableStateOf(false) }
                val animHeight by animateFloatAsState(targetValue = if (barEntry) ratio else 0f, spring(dampingRatio = 0.6f, stiffness = 100f))
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 50L); barEntry = true }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).clickable { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedIndex = index 
                    }
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 16.dp)
                                .fillMaxHeight(animHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Brush.verticalGradient(listOf(quranColors.primary, quranColors.accent)) else Brush.verticalGradient(listOf(quranColors.primary.copy(alpha=0.3f), quranColors.primary.copy(alpha=0.1f))))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(triple.second, color = if (isSelected) quranColors.primary else quranColors.textMuted, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium)
                }
            }
        }
    }
}
