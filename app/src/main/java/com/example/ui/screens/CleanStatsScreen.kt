package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.UiState

@Composable
fun CleanStatsScreen(viewModel: QuranViewModel) {
    val quranColors = LocalQuranColors.current
    val statsList by viewModel.allStats.collectAsStateWithLifecycle()
    val allSurahStats by viewModel.allSurahStats.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val installationDate = remember(settings) { settings.installationDate }
    val haptic = LocalHapticFeedback.current

    // Expansion tracking
    var expandedSurahNumber by remember { mutableStateOf<Int?>(null) }
    var selectedDayIndex by remember { mutableStateOf(6) }

    // Animations block
    var startAnim by remember { mutableStateOf(false) }
    val entryAlpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, tween(1000), label = "alpha")
    val entryOffset by animateFloatAsState(targetValue = if (startAnim) 0f else 40f, spring(stiffness = Spring.StiffnessLow), label = "offset")
    val animationScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    LaunchedEffect(Unit) { startAnim = true }

    // Helpers
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

    val formatBanglaDate = remember {
        { dateStr: String ->
            if (dateStr.isEmpty()) {
                "জানা নেই"
            } else {
                try {
                    val sdfIn = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val dateObj = sdfIn.parse(dateStr) ?: dateStr
                    val calendar = java.util.Calendar.getInstance()
                    if (dateObj is java.util.Date) {
                        calendar.time = dateObj
                        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        val month = calendar.get(java.util.Calendar.MONTH) // 0-indexed
                        val year = calendar.get(java.util.Calendar.YEAR)

                        val monthsBng = listOf(
                            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
                            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
                        )
                        
                        val dayBng = toBanglaDigits(day.toString())
                        val monthBng = monthsBng[month]
                        val yearBng = toBanglaDigits(year.toString())
                        
                        "$dayBng $monthBng, $yearBng"
                    } else {
                        dateStr
                    }
                } catch (e: Exception) {
                    dateStr
                }
            }
        }
    }

    val getActiveDaysCount = remember {
        { installationDateStr: String ->
            if (installationDateStr.isEmpty()) {
                1
            } else {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val installDate = sdf.parse(installationDateStr) ?: sdf.parse(sdf.format(java.util.Date()))!!
                    val today = sdf.parse(sdf.format(java.util.Date()))!!
                    val diffInMillis = today.time - installDate.time
                    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                    (diffInDays.toInt() + 1).coerceAtLeast(1)
                } catch (e: Exception) {
                    1
                }
            }
        }
    }

    val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val todayStats = remember(statsList, todayStr) {
        statsList.find { it.dateStr == todayStr } ?: com.example.data.model.QuranStats(todayStr, 0L, 0)
    }

    val totalAyahsRead = remember(statsList) {
        statsList.sumOf { it.ayahsReadCount }
    }

    val totalDurationSeconds = remember(statsList) {
        statsList.sumOf { it.durationSeconds }
    }

    val totalHours = totalDurationSeconds / 3600

    val pastSevenDays = remember(statsList) {
        val sdfDateLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val sdfDayLocal = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
        val bngDays = mapOf(
            "Sat" to "শনি",
            "Sun" to "রবি",
            "Mon" to "সোম",
            "Tue" to "মঙ্গল",
            "Wed" to "বুধ",
            "Thu" to "বৃহ",
            "Fri" to "শুক্র"
        )
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
        
        val days = ArrayList<Triple<String, String, Long>>()
        for (i in 0..6) {
            val date = calendar.time
            val dStr = sdfDateLocal.format(date)
            val dEng = sdfDayLocal.format(date)
            val dBng = bngDays[dEng] ?: dEng
            
            val duration = statsList.find { it.dateStr == dStr }?.durationSeconds ?: 0L
            days.add(Triple(dStr, dBng, duration))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    val maxDurationSeconds = remember(pastSevenDays) {
        val maxVal = pastSevenDays.maxOfOrNull { it.third } ?: 0L
        if (maxVal == 0L) 300L else maxVal
    }

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

    // BG Mesh & Orbs
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(quranColors.background)
    ) {
        // Subtle animated glow orb
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
            label = "gs"
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .size(220.dp)
                .scale(glowScale)
                .blur(100.dp)
                .background(quranColors.primary.copy(alpha = 0.12f), CircleShape)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { 
                    alpha = entryAlpha
                    translationY = entryOffset 
                },
            contentPadding = PaddingValues(bottom = 120.dp, top = 24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "তেলাওয়াত অগ্রগতি",
                        color = quranColors.textMain,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "আপনার প্রতিদিনের আল-কুরআন তেলাওয়াত ও শোনার লাইভ হিসাব সমূহ",
                        color = quranColors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Streak & Badge Overlay Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                    border = BorderStroke(1.dp, quranColors.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        quranColors.primary.copy(alpha = 0.08f),
                                        quranColors.accent.copy(alpha = 0.03f)
                                    )
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(quranColors.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFFF8C00),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ধারাবাহিক তেলাওয়াত",
                                    color = quranColors.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (continuousStreak > 0) toBanglaDigits("$continuousStreak") + " দিন ইন-শা-আল্লাহ!" else "ধারাবাহিকতা শুরু করুন!",
                                    color = quranColors.textMain,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            if (continuousStreak >= 3) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "PREMIUM",
                                        color = Color(0xFFD4AF37),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Beautiful Account Details Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                    border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "ব্যবহার বিবরণী",
                                tint = quranColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "অ্যাপ ব্যবহার বিবরণী",
                                color = quranColors.textMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Date
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "প্রথম ব্যবহার",
                                    color = quranColors.textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatBanglaDate(installationDate),
                                    color = quranColors.textMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(quranColors.borderColor.copy(alpha = 0.5f))
                            )
                            
                            // Today Date
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(
                                    text = "আজকের তারিখ",
                                    color = quranColors.textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatBanglaDate(todayStr),
                                    color = quranColors.textMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(quranColors.borderColor.copy(alpha = 0.5f))
                            )
                            
                            // Total Days
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "মোট দিন",
                                    color = quranColors.textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val totalDays = getActiveDaysCount(installationDate)
                                Text(
                                    text = toBanglaDigits("$totalDays দিন"),
                                    color = quranColors.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Stat Cards Rows (Glow styled)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val mostPlayedSurah by viewModel.mostPlayedSurah.collectAsStateWithLifecycle()
                    val surahName = mostPlayedSurah?.surahNameEnglish ?: "---"
                    
                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Favorite,
                        iconTint = Color(0xFFFF4E50),
                        title = surahName,
                        subtitle = "প্রিয় সূরা",
                        quranColors = quranColors
                    )

                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.AutoStories,
                        iconTint = Color(0xFFF9A826),
                        title = toBanglaDigits("$totalAyahsRead টি"),
                        subtitle = "পঠিত আয়াত",
                        quranColors = quranColors
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val sMinutes = totalDurationSeconds / 60
                    val hourRepresentation = if (totalHours > 0) "$totalHours ঘণ্টা ${sMinutes % 60}মি." else "$sMinutes মিনিট"

                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Headphones,
                        iconTint = quranColors.primary,
                        title = toBanglaDigits(hourRepresentation),
                        subtitle = "সর্বমোট শ্রবণ",
                        quranColors = quranColors
                    )

                    StatGlowCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.LocalFireDepartment,
                        iconTint = Color(0xFFFF6B6B),
                        title = toBanglaDigits("$continuousStreak দিন"),
                        subtitle = "ধারাবাহিকতা",
                        quranColors = quranColors
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Elegant Weekly Chart with Interactive Information Panel
            item {
                Text(
                    text = "সাপ্তাহিক বিশ্লেষণ",
                    color = quranColors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "নিচের বারগুলোতে ক্লিক করে যেকোনো দিনের বিস্তারিত তথ্য ও মিনিট দেখুন",
                    color = quranColors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )

                // Render Chart
                PremiumWeeklyChart(
                    pastSevenDays = pastSevenDays,
                    maxDurationSeconds = maxDurationSeconds,
                    todayStr = todayStr,
                    selectedDayIndex = selectedDayIndex,
                    animationScale = animationScale,
                    quranColors = quranColors,
                    haptic = haptic,
                    onSelectDay = { selectedDayIndex = it }
                )

                // Explicit Day Details Card under weekly charts
                val selectedDayTriple = pastSevenDays.getOrNull(selectedDayIndex)
                if (selectedDayTriple != null) {
                    val (sDateStr, sDayBng, sDurSeconds) = selectedDayTriple
                    val sMins = sDurSeconds / 60
                    val sSecs = sDurSeconds % 60
                    val sHours = sDurSeconds / 3600
                    val sMinsRemainder = (sDurSeconds % 3600) / 60
                    
                    val sStatObj = statsList.find { it.dateStr == sDateStr }
                    val sAyahsCount = sStatObj?.ayahsReadCount ?: 0

                    val bngMonths = mapOf(
                        "01" to "জানুয়ারি", "02" to "ফেব্রুয়ারি", "03" to "মার্চ", "04" to "এপ্রিল",
                        "05" to "মে", "06" to "জুন", "07" to "জুলাই", "08" to "আগস্ট",
                        "09" to "সেপ্টেম্বর", "10" to "অক্টোবর", "11" to "নভেম্বর", "12" to "ডিসেম্বর"
                    )
                    val formattedDayString = remember(sDateStr) {
                        val parts = sDateStr.split("-")
                        if (parts.size == 3) {
                            val day = parts[2].toInt().toString()
                            val month = bngMonths[parts[1]] ?: parts[1]
                            "$day $month ($sDayBng)"
                        } else {
                            "$sDateStr ($sDayBng)"
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .shadow(2.dp, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                        border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = quranColors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "দৈনিক বিবরণী",
                                        color = quranColors.textMain,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                
                                Text(
                                    text = toBanglaDigits(formattedDayString),
                                    color = quranColors.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Total Duration Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = quranColors.background.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = quranColors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val timeText = if (sDurSeconds > 0) {
                                            if (sHours > 0) "$sHours ঘণ্টা $sMinsRemainder মি." else "$sMins মি. $sSecs সে."
                                        } else {
                                            "০ সেকেন্ড"
                                        }
                                        Text(
                                            text = toBanglaDigits(timeText),
                                            color = quranColors.textMain,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "মোট শ্রবণ সময়",
                                            color = quranColors.textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Total Ayahs Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = quranColors.background.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = Color(0xFFD4AF37),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = toBanglaDigits("$sAyahsCount টি আয়াত"),
                                            color = quranColors.textMain,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "পঠিত আয়াত",
                                            color = quranColors.textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Motivation Status Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sDurSeconds > 0) quranColors.primary.copy(alpha = 0.08f) else quranColors.textMuted.copy(alpha = 0.06f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (sDurSeconds > 0) Icons.Default.AutoAwesome else Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = if (sDurSeconds > 0) quranColors.primary else quranColors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (sDurSeconds == 0L) {
                                            "“হে মুমিনগণ! চলুন আজ অন্তত এক আয়াত পড়ে বা শুনে দিনের সূচনা বরকতময় করি।”"
                                        } else if (sDurSeconds < 300L) {
                                            "“মাশাআল্লাহ! আপনি কুরআন শোনার ধারাবাহিকতা বজায় রেখেছেন।”"
                                        } else {
                                            "“আলহামদুলিল্লাহ! আপনি দীর্ঘ সময় আল্লাহর বাণী শ্রবণ ও অনুধাবন করেছেন।”"
                                        },
                                        color = if (sDurSeconds > 0) quranColors.textMain else quranColors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        style = androidx.compose.ui.text.TextStyle(lineHeight = 16.sp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Central Premium Daily Goal Ring Layout
            item {
                Text(
                    text = "আজকের লক্ষ্য অগ্রগতি",
                    color = quranColors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val dailyGoalMinutes = 15
                val currentMinutes = todayStats.durationSeconds / 60
                val goalPercentage = ((currentMinutes.toFloat() / dailyGoalMinutes) * 100).toInt().coerceIn(0, 100)
                val goalSweepProgress by animateFloatAsState(
                    targetValue = (goalPercentage.toFloat() / 100f) * animationScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
                    label = "sweep"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, quranColors.primary.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.4f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    color = quranColors.primarySoft,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Goal",
                                        tint = quranColors.primary,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "দৈনিক লক্ষ্য",
                                    color = quranColors.textMain,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "আপনার প্রতিদিনের কুরআন অধ্যয়নের লক্ষ্য হল অন্তত $dailyGoalMinutes মিনিট তিলাওয়াত শোনা।",
                                color = quranColors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = toBanglaDigits("$goalPercentage"),
                                    color = quranColors.primary,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "% সম্পন্ন হয়েছে",
                                    color = quranColors.primary.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(90.dp)) {
                                drawArc(
                                    color = quranColors.borderColor.copy(alpha = 0.4f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.linearGradient(
                                        colors = listOf(quranColors.primary, quranColors.accent)
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * goalSweepProgress,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Icon(
                                imageVector = if (goalPercentage >= 100) Icons.Default.Stars else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Advanced Surah-by-Surah Analysis List Section
            item {
                Text(
                    text = "সূরাভিত্তিক অগ্রগতি ও বিশ্লেষণ",
                    color = quranColors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "যে সকল সূরা আপনি শুনেছেন বা পড়েছেন, সেগুলোর ওপর ক্লিক করে সেশনের সূক্ষ্ম বিশ্লেষণ ও পূর্ণাঙ্গ ট্র্যাকিং বিবরণ দেখুন।",
                    color = quranColors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                )
            }

            if (allSurahStats.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = quranColors.textMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "কোনো সূরাভিত্তিক রেকর্ড নেই",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "হোম পেজের তালিকায় থাকা যেকোনো সূরা প্লে করে শোনা বা পড়া শুরু করুন। আপনার তেলাওয়াতের লাইভ হিসাব এখানে পাওয়া যাবে।",
                                color = quranColors.textMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                val allSurahsList = (viewModel.surahListState.value as? UiState.Success)?.data

                items(allSurahStats, key = { it.surahNumber }) { stat ->
                    val isExpanded = expandedSurahNumber == stat.surahNumber
                    val matchingSurah = allSurahsList?.find { it.number == stat.surahNumber }
                    val totalAyahs = matchingSurah?.numberOfAyahs ?: 0
                    val surahBngName = matchingSurah?.name ?: ""

                    Spacer(modifier = Modifier.height(2.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expandedSurahNumber = if (isExpanded) null else stat.surahNumber
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) quranColors.primary.copy(alpha = 0.03f) else quranColors.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isExpanded) 1.5.dp else 1.dp,
                            color = if (isExpanded) quranColors.primary else quranColors.borderColor.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Circular index badge
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(quranColors.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = toBanglaDigits(stat.surahNumber.toString()),
                                            color = quranColors.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = stat.surahNameEnglish,
                                            color = quranColors.textMain,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (surahBngName.isNotEmpty()) {
                                            Text(
                                                text = surahBngName,
                                                color = quranColors.textMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = FontFamily.Serif
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val sMins = stat.totalDurationSeconds / 60
                                    val sSecs = stat.totalDurationSeconds % 60
                                    val timeLabel = if (sMins > 0) "${sMins}মি. ${sSecs}সে." else "${sSecs}সে."
                                    Text(
                                        text = toBanglaDigits(timeLabel),
                                        color = quranColors.textMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand details",
                                        tint = quranColors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    HorizontalDivider(
                                        color = quranColors.borderColor.copy(alpha = 0.4f),
                                        thickness = 1.dp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Detailed grid stats row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = quranColors.background.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "মোট শ্রবণকাল",
                                                    color = quranColors.textMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = toBanglaDigits("${stat.totalDurationSeconds} সেকেন্ড"),
                                                    color = quranColors.textMain,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = quranColors.background.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "পঠিত আয়াত সংখ্যা",
                                                    color = quranColors.textMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = toBanglaDigits("${stat.totalAyahsRead} টি আয়াত"),
                                                    color = quranColors.textMain,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (totalAyahs > 0) {
                                        val completionPercent = ((stat.totalAyahsRead.toFloat() / totalAyahs.toFloat()) * 100).toInt().coerceIn(0, 100)
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "সুরাটির তেলাওয়াত লক্ষ্য",
                                            color = quranColors.textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = toBanglaDigits("${stat.totalAyahsRead} / $totalAyahs আয়াত সম্পন্ন"),
                                                color = quranColors.textMain,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = toBanglaDigits("$completionPercent%"),
                                                color = quranColors.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { stat.totalAyahsRead.toFloat() / totalAyahs.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = quranColors.primary,
                                            trackColor = quranColors.borderColor.copy(alpha = 0.4f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Direct play deep link CTA button
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.loadSurahReadingView(stat.surahNumber)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = quranColors.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "সূরাটি পড়তে ও শুনতে ক্লিক করুন",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
        modifier = modifier.height(115.dp),
        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(
                    text = title, 
                    color = quranColors.textMain, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle, 
                    color = quranColors.textMuted, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PremiumWeeklyChart(
    pastSevenDays: List<Triple<String, String, Long>>,
    maxDurationSeconds: Long,
    todayStr: String,
    selectedDayIndex: Int,
    animationScale: Float,
    quranColors: QuranColors,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onSelectDay: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(16.dp)
        ) {
            // Horizontal grid reference background lines
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(quranColors.borderColor.copy(alpha = 0.25f))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                pastSevenDays.forEachIndexed { index, triple ->
                    val (dateStr, bngDay, durSeconds) = triple
                    val isToday = dateStr == todayStr
                    val isSelected = index == selectedDayIndex
                    val mins = durSeconds / 60
                    
                    val animatedWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 14.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "width"
                    )
                    
                    val barHeightFactor = (durSeconds.toFloat() / maxDurationSeconds.toFloat() * animationScale).coerceIn(0.05f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectDay(index)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Rounded track background
                            Box(
                                modifier = Modifier
                                    .width(animatedWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(quranColors.borderColor.copy(alpha = 0.3f))
                            )

                            // Interactive filled bar progress
                            Box(
                                modifier = Modifier
                                    .width(animatedWidth)
                                    .fillMaxHeight(barHeightFactor)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = if (isSelected) {
                                                listOf(quranColors.primary, quranColors.accent)
                                            } else if (isToday) {
                                                listOf(quranColors.primary, quranColors.primarySoft)
                                            } else {
                                                listOf(
                                                    quranColors.primarySoft.copy(alpha = 0.85f),
                                                    quranColors.primarySoft.copy(alpha = 0.4f)
                                                )
                                            }
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bngDay,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) quranColors.primary else if (isToday) quranColors.accent else quranColors.textMain
                        )
                        
                        Text(
                            text = if (durSeconds > 0) {
                                val englishNum = if (mins > 0) "$mins" else "০"
                                englishNum.map { if (it in '0'..'9') "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("") + "মি"
                            } else "০",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) quranColors.primary else if (durSeconds > 0) quranColors.textMain else quranColors.textMuted
                        )
                    }
                }
            }
        }
    }
}
