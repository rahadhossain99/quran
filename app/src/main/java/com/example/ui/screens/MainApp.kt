package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.isSystemInDarkTheme


// Color themes mapping
data class QuranColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val primarySoft: Color,
    val accent: Color,
    val textMain: Color,
    val textMuted: Color,
    val borderColor: Color,
    val isDark: Boolean
)

val LightQuranColors = QuranColors(
    background = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    primary = Color(0xFF0F9F59),
    primarySoft = Color(0xFFE6F5EC),
    accent = Color(0xFFFF9500),
    textMain = Color(0xFF1C1C1E),
    textMuted = Color(0xFF8E8E93),
    borderColor = Color(0xFFE5E5EA),
    isDark = false
)

val SepiaQuranColors = QuranColors(
    background = Color(0xFFF4ECD8),
    surface = Color(0xFFFAF6EB),
    primary = Color(0xFFD28F33),
    primarySoft = Color(0xFFF9EEDD),
    accent = Color(0xFFC25B3E),
    textMain = Color(0xFF5C4B37),
    textMuted = Color(0xFF8C7B66),
    borderColor = Color(0xFFE8DECA),
    isDark = false
)

val DarkQuranColors = QuranColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    primary = Color(0xFF30D158),
    primarySoft = Color(0xFF0C3817),
    accent = Color(0xFFFF9F0A),
    textMain = Color(0xFFFFFFFF),
    textMuted = Color(0xFF8E8E93),
    borderColor = Color(0xFF38383A),
    isDark = true
)

val LocalQuranColors = staticCompositionLocalOf { LightQuranColors }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainApp(
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val quranColors = when (settings.theme) {
        "dark" -> DarkQuranColors
        "sepia" -> SepiaQuranColors
        else -> LightQuranColors
    }

    val context = LocalContext.current
    LaunchedEffect(settings.theme) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val view = window.decorView
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            val isDark = settings.theme == "dark"
            
            // Set appropriate status/navigation bar icon tint (light icons on dark background, dark on light/sepia)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
            
            // Make system bars completely transparent so they blend seamlessly with the app's dynamic background
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    CompositionLocalProvider(LocalQuranColors provides quranColors) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = LocalQuranColors.current.background
        ) {
            val scope = rememberCoroutineScope()
            var currentTab by remember { mutableStateOf("home") } // "home", "favorites", "settings"
            var isDownloadHubOpen by remember { mutableStateOf(false) }
            val ongoingDownloads = remember { androidx.compose.runtime.mutableStateMapOf<Int, Float>() }
            val readingSurahState by viewModel.readingSurahState.collectAsStateWithLifecycle()
            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

            // Connect player service
            val playerService by viewModel.playerService.collectAsStateWithLifecycle()
            val isPlaying = playerService?.isPlaying?.collectAsStateWithLifecycle()?.value ?: false
            val serviceSurah = playerService?.currentSurah?.collectAsStateWithLifecycle()?.value
            val serviceAyahIndex = playerService?.currentAyahIndex?.collectAsStateWithLifecycle()?.value ?: -1

            // Expanded Player toggle
            var isPlayerExpanded by remember { mutableStateOf(false) }

            // Back navigation handlers
            BackHandler(enabled = isPlayerExpanded) {
                isPlayerExpanded = false
            }
            BackHandler(enabled = isDownloadHubOpen && (readingSurahState is UiState.Idle)) {
                isDownloadHubOpen = false
            }
            BackHandler(enabled = readingSurahState !is UiState.Idle) {
                viewModel.closeSurahReadingView()
            }
            BackHandler(enabled = (readingSurahState is UiState.Idle) && !isDownloadHubOpen && currentTab != "home") {
                currentTab = "home"
            }

            val cleanNeoEnabled = settings.cleanNeoEnabled
            val dotColor = quranColors.primary
            val dotAlpha = if (settings.theme == "dark") 0.07f else 0.12f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (cleanNeoEnabled) {
                            val dotRadius = 1.35.dp.toPx()
                            val gap = 18.dp.toPx()
                            val columns = (size.width / gap).toInt() + 1
                            val rows = (size.height / gap).toInt() + 1
                            for (c in 0 until columns) {
                                for (r in 0 until rows) {
                                    drawCircle(
                                        color = dotColor,
                                        alpha = dotAlpha,
                                        radius = dotRadius,
                                        center = androidx.compose.ui.geometry.Offset(c * gap, r * gap)
                                    )
                                }
                            }
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Custom Premium Header
                    HeaderBlock(
                        title = if (readingSurahState is UiState.Success) {
                            (readingSurahState as UiState.Success<FormattedSurah>).data.englishName
                        } else if (isDownloadHubOpen) {
                            "অফলাইন ডাউনলোড হাব"
                        } else {
                            "আল-কুরআন"
                        },
                        showBackButton = (readingSurahState is UiState.Success) || isDownloadHubOpen,
                        onBackClicked = {
                            if (readingSurahState is UiState.Success) {
                                viewModel.closeSurahReadingView()
                            } else if (isDownloadHubOpen) {
                                isDownloadHubOpen = false
                            }
                        }
                    )

                    if (!isOnline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(quranColors.accent.copy(alpha = 0.09f))
                                .border(
                                    width = 1.dp,
                                    color = quranColors.accent.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "অফলাইন",
                                tint = quranColors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "অফলাইন লাইব্রেরি মোড সক্রিয়",
                                color = quranColors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "ডাউনলোডকৃত সূচি",
                                color = quranColors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = readingSurahState,
                            transitionSpec = {
                                if (targetState is UiState.Success) {
                                    (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                            (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                            (slideOutHorizontally { width -> width / 3 } + fadeOut())
                                }
                            },
                            label = "screen_navigation"
                        ) { readingState ->
                            when (readingState) {
                                is UiState.Idle -> {
                                    // Main Navigation Tabs
                                    if (isDownloadHubOpen) {
                                        OfflineDownloadHubScreen(
                                            viewModel = viewModel,
                                            ongoingDownloads = ongoingDownloads,
                                            onClose = { isDownloadHubOpen = false }
                                        )
                                    } else {
                                        when (currentTab) {
                                            "home" -> {
                                                if (cleanNeoEnabled) {
                                                    CleanHomeScreen(
                                                        viewModel = viewModel,
                                                        ongoingDownloads = ongoingDownloads,
                                                        onOpenDownloadHub = { isDownloadHubOpen = true }
                                                    )
                                                } else {
                                                    HomeScreen(
                                                        viewModel = viewModel,
                                                        ongoingDownloads = ongoingDownloads,
                                                        onOpenDownloadHub = { isDownloadHubOpen = true }
                                                    )
                                                }
                                            }
                                            "favorites" -> FavoritesScreen(
                                                viewModel = viewModel,
                                                ongoingDownloads = ongoingDownloads,
                                                onOpenDownloadHub = { isDownloadHubOpen = true }
                                            )
                                            "stats" -> StatsScreen(viewModel)
                                            "settings" -> SettingsScreen(
                                                viewModel = viewModel,
                                                ongoingDownloads = ongoingDownloads,
                                                onOpenDownloadHub = { isDownloadHubOpen = true }
                                            )
                                        }
                                    }
                                }
                                is UiState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = LocalQuranColors.current.primary)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "আয়াত প্রস্তুত করা হচ্ছে...",
                                                color = LocalQuranColors.current.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    SurahReadingScreen(
                                        viewModel = viewModel,
                                        formattedSurah = readingState.data,
                                        isPlaying = isPlaying && serviceSurah?.number == readingState.data.number,
                                        activeAyahIndex = if (serviceSurah?.number == readingState.data.number) serviceAyahIndex else -1
                                    )
                                }
                                is UiState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Error",
                                                tint = Color.Red,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                readingState.message,
                                                color = LocalQuranColors.current.textMain,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { viewModel.closeSurahReadingView() },
                                                colors = ButtonDefaults.buttonColors(containerColor = LocalQuranColors.current.primary)
                                            ) {
                                                Text("ফিরে যান", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Navigation Spacer if mini player is showing
                    val bottomPadding = if (serviceSurah != null) 160.dp else 80.dp
                    Spacer(modifier = Modifier.height(bottomPadding))
                }

                // Global Mini Floating Player
                AnimatedVisibility(
                    visible = serviceSurah != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = if (readingSurahState is UiState.Success) 38.dp else 102.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    if (serviceSurah != null) {
                        MiniPlayerBlock(
                            surah = serviceSurah,
                            isPlaying = isPlaying,
                            currentAyahIndex = serviceAyahIndex,
                            progress = playerService?.progress?.collectAsStateWithLifecycle()?.value ?: 0,
                            duration = playerService?.duration?.collectAsStateWithLifecycle()?.value ?: 1,
                            onPlayPauseClicked = { playerService?.togglePlayPause() },
                            onExpandClicked = { isPlayerExpanded = true }
                        )
                    }
                }

                // Bottom Navigation (Hidden inside Reading View to maximize focus)
                AnimatedVisibility(
                    visible = readingSurahState is UiState.Idle,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }

                // Expanded Player Sheet Overlay (iOS Modal transition)
                AnimatedVisibility(
                    visible = isPlayerExpanded && serviceSurah != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f))
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(350, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f))
                    ) + fadeOut()
                ) {
                    if (serviceSurah != null) {
                        ExpandedPlayerView(
                            surah = serviceSurah,
                            isPlaying = isPlaying,
                            currentAyahIndex = serviceAyahIndex,
                            progress = playerService?.progress?.collectAsStateWithLifecycle()?.value ?: 0,
                            duration = playerService?.duration?.collectAsStateWithLifecycle()?.value ?: 1,
                            loopMode = playerService?.loopMode?.collectAsStateWithLifecycle()?.value ?: 0,
                            settings = settings,
                            onCloseRequested = { isPlayerExpanded = false },
                            onPlayPauseClicked = { playerService?.togglePlayPause() },
                            onNextClicked = { playerService?.playNext() },
                            onPrevClicked = { playerService?.playPrev() },
                            onLoopClicked = {
                                val currentMode = playerService?.loopMode?.value ?: 0
                                playerService?.setLoopMode((currentMode + 1) % 3)
                            },
                            onSeekIndexChanged = { index ->
                                playerService?.playAyah(index)
                            },
                            onProgressSeek = { ms ->
                                playerService?.seekTo(ms)
                            },
                            onGoToSurahClicked = {
                                isPlayerExpanded = false
                                viewModel.loadSurahReadingView(serviceSurah.number)
                            }
                        )
                    }
                }
            }
        }
    }
}

// App Header Block
@Composable
fun HeaderBlock(
    title: String,
    showBackButton: Boolean,
    onBackClicked: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(quranColors.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(quranColors.background)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = quranColors.textMain
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(quranColors.primary, quranColors.accent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = quranColors.textMain,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// iOS Style bottom navigation
@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val quranColors = LocalQuranColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .background(quranColors.surface)
            .navigationBarsPadding(),
        color = quranColors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val tabs = listOf(
                Triple("home", Icons.Default.Home, "হোম"),
                Triple("favorites", Icons.Default.Bookmark, "বুকমার্ক"),
                Triple("stats", Icons.Default.TrendingUp, "অগ্রগতি"),
                Triple("settings", Icons.Default.Settings, "সেটিংস")
            )

            tabs.forEach { (tabId, icon, label) ->
                val isActive = currentTab == tabId
                val tint by animateColorAsState(
                    targetValue = if (isActive) quranColors.primary else quranColors.textMuted,
                    label = "tab_color"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.1f else 1.0f,
                    label = "tab_scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            onClick = { onTabSelected(tabId) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) quranColors.primarySoft else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Tab 1: Home List
@Composable
fun HomeScreen(
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    onOpenDownloadHub: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val surahListState by viewModel.surahListState.collectAsStateWithLifecycle()
    val filteredList by viewModel.filteredSurahs.collectAsStateWithLifecycle()
    val lastPlayed by viewModel.lastPlayed.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bookmarks by viewModel.favoriteSurahs.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val downloadedList by viewModel.downloadedSurahsList.collectAsStateWithLifecycle()
    val downloadedAudioSet = remember(downloadedList) { downloadedList.filter { it.hasAudio }.map { it.number }.toSet() }
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.surahNumber }.toSet() }
    val downloadedJson = settings.downloadedSurahsJson
    val haptic = LocalHapticFeedback.current

    val playerService by viewModel.playerService.collectAsStateWithLifecycle()
    val isServicePlaying = playerService?.isPlaying?.collectAsStateWithLifecycle()?.value ?: false
    val serviceSurah = playerService?.currentSurah?.collectAsStateWithLifecycle()?.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            // Welcome Card Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(quranColors.primary, Color(0xFF139755))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "আসসালামু আলাইকুম",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "পবিত্র কুরআনুল কারিম পড়ুন এবং শুনুন",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Custom search engine inside
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("সূরা অনুসন্ধান করুন...", color = Color.White.copy(alpha = 0.6f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.15f),
                            disabledContainerColor = Color.Black.copy(alpha = 0.15f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Revert to classic "সর্বশেষ পঠিত" (Last Played) card inside the app itself
        if (searchQuery.isEmpty() && lastPlayed != null) {
            item {
                Text(
                    "সর্বশেষ পঠিত",
                    color = quranColors.textMain,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.resumeLastPlayed(lastPlayed!!)
                        },
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                    border = BorderStroke(1.dp, quranColors.borderColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(quranColors.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lastPlayed!!.surahEnglishName,
                                color = quranColors.textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সংক্ষিপ্ত বিবরণ • আয়াত ${lastPlayed!!.ayahNumberInSurah}",
                                color = quranColors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Play arrow as resumption action
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(quranColors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Surah Grid title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "সকল সূরা",
                        color = quranColors.textMain,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "১১৪ টি",
                        color = quranColors.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(quranColors.primarySoft, RoundedCornerShape(99.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDownloadHub()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(quranColors.primarySoft)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "অফলাইন ডাউনলোড হাব",
                        tint = quranColors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        when (surahListState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = quranColors.primary)
                    }
                }
            }
            is UiState.Success -> {
                if (filteredList.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = quranColors.textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "কোনো সূরা পাওয়া যায়নি",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    itemsIndexed(filteredList, key = { _, surah -> surah.number }) { index, surah ->
                        val isBookmark = bookmarkedIds.contains(surah.number)
                        val isDownloaded = downloadedJson.contains(",${surah.number},")
                        val isAudioDownloaded = downloadedAudioSet.contains(surah.number)
                        val onSurahClick = remember(surah) { { viewModel.loadSurahReadingView(surah.number) } }
                        val onBookmarkClick = remember(surah) { { viewModel.toggleFavoriteSurah(surah) } }
                        val isPlaying = isServicePlaying && serviceSurah?.number == surah.number
                        SurahCardRow(
                            surah = surah,
                            isBookmark = isBookmark,
                            isDownloaded = isDownloaded,
                            isAudioDownloaded = isAudioDownloaded,
                            ongoingDownloads = ongoingDownloads,
                            isOfflineMode = !isOnline,
                            isPlaying = isPlaying,
                            onSurahClicked = onSurahClick,
                            onBookmarkClicked = onBookmarkClick,
                            viewModel = viewModel,
                            settings = settings
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                    ) {
                        Text(
                            (surahListState as UiState.Error).message,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadSurahList() },
                            colors = ButtonDefaults.buttonColors(containerColor = quranColors.primary)
                        ) {
                            Text("পুনরায় চেষ্টা করুন", color = Color.White)
                        }
                    }
                }
            }
            else -> {}
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Single Surah card block
@Composable
fun SurahCardRow(
    surah: SurahModel,
    isBookmark: Boolean,
    isDownloaded: Boolean = false,
    isAudioDownloaded: Boolean = false,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float> = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf() },
    isOfflineMode: Boolean = false,
    isPlaying: Boolean = false,
    onSurahClicked: () -> Unit,
    onBookmarkClicked: () -> Unit,
    viewModel: QuranViewModel? = null,
    settings: UserSettings? = null
) {
    val quranColors = LocalQuranColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showOfflineWarningDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    if (showOfflineWarningDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline Warning",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অফলাইন সতর্কতা",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = quranColors.textMain
                    )
                }
            },
            text = {
                Text(
                    text = "আপনি বর্তমানে কোনো ইন্টারনেট সংযোগ ছাড়াই অ্যাপটি ব্যবহার করছেন।\n\n'${surah.englishName}' সূরার আয়াত ও অডিও অফলাইনে শোনার জন্য প্রথমে ইন্টারনেট সংযোগ চালু করে ডাউনলোড আইকনে ক্লিক করে ডাউনলোড করে নিন। ধন্যবাদ!",
                    fontSize = 14.sp,
                    color = quranColors.textMuted,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showOfflineWarningDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = quranColors.primary)
                ) {
                    Text("বুঝতে পেরেছি", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = quranColors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        )
    }

    val finalOnClick: () -> Unit = if (isOfflineMode && !isDownloaded) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showOfflineWarningDialog = true
        }
    } else {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSurahClicked()
        }
    }

    val isCurrentDownloading = ongoingDownloads.containsKey(surah.number)
    val downloadProgress = ongoingDownloads[surah.number] ?: 0f

    val cardBg = when {
        isPlaying -> {
            quranColors.primary.copy(alpha = 0.05f + alpha * 0.06f)
        }
        isAudioDownloaded && isDownloaded -> {
            quranColors.primarySoft.copy(alpha = 0.75f)
        }
        isDownloaded -> {
            quranColors.surface
        }
        isOfflineMode && !isDownloaded -> {
            quranColors.surface.copy(alpha = 0.55f)
        }
        else -> quranColors.surface
    }

    val cardBorder = when {
        isPlaying -> {
            BorderStroke(2.dp, quranColors.primary.copy(alpha = alpha))
        }
        isAudioDownloaded && isDownloaded -> {
            BorderStroke(1.5.dp, quranColors.primary.copy(alpha = 0.6f))
        }
        isDownloaded -> {
            BorderStroke(1.5.dp, quranColors.accent.copy(alpha = 0.4f))
        }
        isOfflineMode && !isDownloaded -> {
            BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.35f))
        }
        else -> BorderStroke(1.dp, quranColors.borderColor)
    }

    val mainTextAlpha = if (isOfflineMode && !isDownloaded) 0.5f else 1f
    val mainTextColor = if (isAudioDownloaded && isDownloaded) {
        quranColors.primary
    } else quranColors.textMain.copy(alpha = mainTextAlpha)
    
    val mutedTextColor = quranColors.textMuted.copy(alpha = mainTextAlpha)

    Card(
        onClick = finalOnClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isAudioDownloaded && isDownloaded) quranColors.primarySoft
                        else quranColors.background
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    surah.number.toString(),
                    color = if (isAudioDownloaded && isDownloaded) quranColors.primary else quranColors.textMain.copy(alpha = mainTextAlpha),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        surah.englishName,
                        color = mainTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPlaying) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(quranColors.accent.copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "চলছে",
                            color = quranColors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Subtitle metadata descriptions with status icons
                Spacer(modifier = Modifier.height(2.dp))
                if (isCurrentDownloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(16.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                color = quranColors.primary,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "অডিও নামছে... ${(downloadProgress * 100).toInt()}%",
                            color = quranColors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${if (surah.revelationType == "Meccan") "মাক্কী" else "মাদানী"} • ${surah.numberOfAyahs} আয়াত",
                            color = mutedTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Status pill badges
                        if (isAudioDownloaded && isDownloaded) {
                            Text(
                                "অডিও ও অনুবাদ",
                                color = quranColors.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        quranColors.primarySoft,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        } else if (isDownloaded) {
                            Text(
                                "অনুবাদ মাত্র",
                                color = quranColors.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        quranColors.accent.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Text(
                surah.name,
                color = if (isAudioDownloaded && isDownloaded) quranColors.primary else quranColors.primary.copy(alpha = mainTextAlpha),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Dynamic Action Button (CloudDownload or Success state)
            if (isCurrentDownloading) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        color = quranColors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isAudioDownloaded) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = "ডাউনলোড হয়েছে",
                        tint = quranColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                if (!isOfflineMode) {
                    IconButton(
                        onClick = {
                            if (viewModel != null && settings != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ongoingDownloads[surah.number] = 0.01f
                                viewModel.downloadSurahOffline(
                                    surahNumber = surah.number,
                                    qari = settings.selectedQari,
                                    onProgress = { prg ->
                                        ongoingDownloads[surah.number] = prg
                                    },
                                    onCompleted = { success ->
                                        ongoingDownloads.remove(surah.number)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "অডিও ডাউনলোড করুন",
                            tint = quranColors.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "অফলাইন",
                            tint = quranColors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBookmarkClicked()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isBookmark) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmark) quranColors.accent else quranColors.textMuted.copy(alpha = mainTextAlpha),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Tab 2: Favorites Bookmarks
@Composable
fun FavoritesScreen(
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    onOpenDownloadHub: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val bookmarks by viewModel.favoriteSurahs.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val downloadedList by viewModel.downloadedSurahsList.collectAsStateWithLifecycle()
    val downloadedAudioSet = remember(downloadedList) { downloadedList.filter { it.hasAudio }.map { it.number }.toSet() }
    val downloadedJson = settings.downloadedSurahsJson

    val playerService by viewModel.playerService.collectAsStateWithLifecycle()
    val isServicePlaying = playerService?.isPlaying?.collectAsStateWithLifecycle()?.value ?: false
    val serviceSurah = playerService?.currentSurah?.collectAsStateWithLifecycle()?.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "বুকমার্ক সমূহ",
            color = quranColors.textMain,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "আপনার পছন্দের সংরক্ষিত সূরাগুলো",
            color = quranColors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = quranColors.textMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "কোনো বুকমার্ক নেই",
                        color = quranColors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "সূরা পড়ার সময় স্টারে ক্লিক করে সেভ করুন",
                        color = quranColors.textMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(bookmarks, key = { _, bookmark -> bookmark.surahNumber }) { _, bookmark ->
                    // Convert favorite Surah entity to standard surrogate for presentation with remember cache
                    val surahModelRepresentation = remember(bookmark) {
                        SurahModel(
                            number = bookmark.surahNumber,
                            name = bookmark.nameArabic,
                            englishName = bookmark.englishName,
                            englishNameTranslation = "",
                            numberOfAyahs = bookmark.numberOfAyahs,
                            revelationType = bookmark.revelationType
                        )
                    }
                    val onSurahClick = remember(bookmark) { { viewModel.loadSurahReadingView(bookmark.surahNumber) } }
                    val onBookmarkClick = remember(bookmark) { { viewModel.toggleFavoriteSurahFromFavorite(bookmark) } }
                    val isDownloaded = downloadedJson.contains(",${bookmark.surahNumber},")
                    val isAudioDownloaded = downloadedAudioSet.contains(bookmark.surahNumber)
                    val isPlaying = isServicePlaying && serviceSurah?.number == bookmark.surahNumber
                    SurahCardRow(
                        surah = surahModelRepresentation,
                        isBookmark = true,
                        isDownloaded = isDownloaded,
                        isAudioDownloaded = isAudioDownloaded,
                        ongoingDownloads = ongoingDownloads,
                        isOfflineMode = !isOnline,
                        isPlaying = isPlaying,
                        onSurahClicked = onSurahClick,
                        onBookmarkClicked = onBookmarkClick,
                        viewModel = viewModel,
                        settings = settings
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// REMOVED DhikrSimulatorWidget to fulfill user request 
// (Widget was previously here)

@Composable
fun StatsScreen(viewModel: QuranViewModel) {
    val quranColors = LocalQuranColors.current
    val statsList by viewModel.allStats.collectAsStateWithLifecycle()
    val allSurahStats by viewModel.allSurahStats.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val installationDate = remember(settings) { settings.installationDate }
    val haptic = LocalHapticFeedback.current
    var selectedDayIndex by remember { mutableStateOf(6) }
    var expandedSurahNumber by remember { mutableStateOf<Int?>(null) }

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

    // Animation mount state
    var startAnimation by remember { mutableStateOf(false) }
    val animationScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Helper date formatting
    val sdfDate = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
    val todayStr = remember { sdfDate.format(java.util.Date()) }

    // Aggregate statistics
    val todayStats = remember(statsList, todayStr) {
        statsList.find { it.dateStr == todayStr } ?: com.example.data.model.QuranStats(todayStr, 0L, 0)
    }

    val totalAyahsRead = remember(statsList) {
        statsList.sumOf { it.ayahsReadCount }
    }

    val totalDurationSeconds = remember(statsList) {
        statsList.sumOf { it.durationSeconds }
    }

    // Weekly reading analytics calculations
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
        
        val days = ArrayList<Triple<String, String, Long>>() // (dateStr, bngDay, durationSeconds)
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
        if (maxVal == 0L) 300L else maxVal // 5 minutes minimum baseline for graph scale
    }

    // Streak tracker calculation
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "তেলাওয়াত অগ্রগতি ও অ্যানালিটিক্স",
            color = quranColors.textMain,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "আপনার প্রতিদিনের আল-কুরআন তেলাওয়াত ও শোনার লাইভ হিসাব সমূহ",
            color = quranColors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Streak & Badge Overlay Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = quranColors.surface),
            border = BorderStroke(1.dp, quranColors.primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp)
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
                                "ধারাবাহিক তেলাওয়াত",
                                color = quranColors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (continuousStreak > 0) "$continuousStreak দিন ইন-শা-আল্লাহ!" else "ধারাবাহিকতা শুরু করুন!",
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

        Spacer(modifier = Modifier.height(8.dp))

        // Beautiful Account Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = quranColors.surface),
            border = BorderStroke(1.dp, quranColors.borderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                
                Spacer(modifier = Modifier.height(14.dp))
                
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

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Key Aggregations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Most Read/Listened Surah
            val mostPlayedSurah by viewModel.mostPlayedSurah.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, quranColors.borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Most Listened Surah",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        if (mostPlayedSurah != null) {
                            val surahMins = mostPlayedSurah!!.totalDurationSeconds / 60
                            val surahSecs = mostPlayedSurah!!.totalDurationSeconds % 60
                            val durText = if (surahMins > 0) "${surahMins}মি." else "${surahSecs}সে."
                            val ayahsText = "${mostPlayedSurah!!.totalAyahsRead} আয়াত"
                            
                            Text(
                                text = mostPlayedSurah!!.surahNameEnglish,
                                color = quranColors.textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "বেশি পড়া ($durText, $ayahsText)",
                                color = quranColors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "কোনো তথ্য নেই",
                                color = quranColors.textMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "সবচেয়ে বেশি পড়া",
                                color = quranColors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Card 2: Total Ayahs
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, quranColors.borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Total Ayahs",
                        tint = Color(0xFFE0A96D),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "${totalAyahsRead} টি আয়াত",
                            color = quranColors.textMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "মোট পঠিত আয়াত",
                            color = quranColors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Card 3: Total duration
            val totalHours = totalDurationSeconds / 3600
            val totalMinutes = (totalDurationSeconds % 3600) / 60
            val totalSecs = totalDurationSeconds % 60
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, quranColors.borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Total Time",
                        tint = Color(0xFF006699),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (totalHours > 0) "${totalHours}ঘণ্টা ${totalMinutes}মি." else if (totalMinutes > 0) "${totalMinutes}মি. ${totalSecs}সে." else "${totalSecs}সে.",
                            color = quranColors.textMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সর্বমোট সময়কাল",
                            color = quranColors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PREMIUM WEEKLY BAR GRAPH COMPONENT
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = quranColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, quranColors.borderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "সাপ্তাহিক সময়ের বিশ্লেষণ",
                            color = quranColors.textMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "নিচের বারগুলোতে ক্লিক করে যেকোনো দিনের বিস্তারিত তথ্য ও মিনিট দেখুন",
                            color = quranColors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "Stats",
                        tint = quranColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // The Interactive Chart Bar Layout with reference lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    // Thin horizontal background reference lines for premium feel
                    Column(
                        modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
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
                        for (index in pastSevenDays.indices) {
                            val triple = pastSevenDays[index]
                            val dateStr = triple.first
                            val dBng = triple.second
                            val durSeconds = triple.third
                            
                            val isToday = dateStr == todayStr
                            val isSelected = index == selectedDayIndex
                            val mins = durSeconds / 60
                            
                            val barWidth by animateDpAsState(
                                targetValue = if (isSelected) 22.dp else 14.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                            
                            val barHeightFactor = (durSeconds.toFloat() / maxDurationSeconds.toFloat() * animationScale).coerceIn(0.04f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedDayIndex = index
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Background path track
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(quranColors.borderColor.copy(alpha = 0.3f))
                                    )

                                    // Interactive filled bar
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(barHeightFactor)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = if (isSelected) {
                                                        listOf(quranColors.primary, quranColors.accent)
                                                    } else if (isToday) {
                                                        listOf(quranColors.primary, quranColors.primarySoft)
                                                    } else {
                                                        listOf(quranColors.primarySoft.copy(alpha = 0.85f), quranColors.primarySoft.copy(alpha = 0.5f))
                                                    }
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = dBng,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) quranColors.primary else if (isToday) quranColors.accent else quranColors.textMain
                                )
                                
                                Text(
                                    text = if (durSeconds > 0) "${mins}মি." else "০",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) quranColors.primary else if (durSeconds > 0) quranColors.textMain else quranColors.textMuted
                                )
                            }
                        }
                    }
                }

                // Detailed dynamic information panel when a day bar gets selected
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

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(quranColors.surface)
                            .border(1.dp, quranColors.borderColor, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
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
                                text = formattedDayString,
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
                                    Text(
                                        text = if (sDurSeconds > 0) {
                                            if (sHours > 0) "${sHours}ঘণ্টা ${sMinsRemainder}মি." else "${sMins}মি. ${sSecs}সে."
                                        } else {
                                            "০ সেকেন্ড"
                                        },
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
                                        text = "$sAyahsCount টি আয়াত",
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

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sDurSeconds > 0) quranColors.primary.copy(alpha = 0.1f) else quranColors.textMuted.copy(alpha = 0.1f))
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PREMIUM HABIT GOAL COMPLETION CARD
        val dailyGoalMinutes = 15
        val goalPercentage = remember(todayStats) {
            val mins = todayStats.durationSeconds.toFloat() / 60f
            ((mins / dailyGoalMinutes.toFloat()) * 100).toInt().coerceIn(0, 100)
        }

        val goalSweepProgress by animateFloatAsState(
            targetValue = goalPercentage.toFloat() / 100f * animationScale,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(quranColors.surface, quranColors.background.copy(alpha = 0.3f))
                    )
                )
                .border(2.dp, quranColors.primary.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
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
                            text = "লক্ষ্য অগ্রগতি",
                            color = quranColors.textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "আপনার লক্ষ্য হল অন্তত $dailyGoalMinutes মিনিট তিলাওয়াত শোনা। নিয়মিত তিলাওয়াত ঈমান বৃদ্ধি করে।",
                        color = quranColors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$goalPercentage",
                            color = quranColors.primary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "% সম্পন্ন",
                            color = quranColors.primary.copy(alpha = 0.7f),
                            fontSize = 14.sp,
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
                            color = quranColors.borderColor.copy(alpha = 0.5f),
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

        Spacer(modifier = Modifier.height(24.dp))

        // ADVANCED SURAH-BY-SURAH DETAILED ANALYSIS SECTION
        Text(
            text = "সূরাভিত্তিক অগ্রগতি ও বিশ্লেষণ",
            color = quranColors.textMain,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "যে সকল সূরা আপনি শুনেছেন বা পড়েছেন, সেগুলোর ওপর ক্লিক করে সেশনের সূক্ষ্ম বিশ্লেষণ ও পূর্ণাঙ্গ ট্র্যাকিং বিবরণ দেখুন।",
            color = quranColors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (allSurahStats.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
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
                    Text(
                        text = "হোম পেজের তালিকায় থাকা যেকোনো সূরা প্লে করে শোনা বা পড়া শুরু করুন। আপনার তেলাওয়াতের লাইভ হিসাব এখানে পাওয়া যাবে।",
                        color = quranColors.textMuted,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            val allSurahsList = (viewModel.surahListState.collectAsStateWithLifecycle().value as? UiState.Success)?.data

            allSurahStats.forEach { stat ->
                val isExpanded = expandedSurahNumber == stat.surahNumber
                val matchingSurah = allSurahsList?.find { it.number == stat.surahNumber }
                val totalAyahs = matchingSurah?.numberOfAyahs ?: 0
                val surahBngName = matchingSurah?.name ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
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
                        color = if (isExpanded) quranColors.primary else quranColors.borderColor
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
                                // Surah Number circular badge
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(quranColors.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stat.surahNumber.toString(),
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
                                Text(
                                    text = if (sMins > 0) "${sMins}মি. ${sSecs}সে." else "${sSecs}সে.",
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
                                HorizontalDivider(color = quranColors.borderColor.copy(alpha = 0.4f), thickness = 1.dp)
                                
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
                                                text = "${stat.totalDurationSeconds} সেকেন্ড",
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
                                                text = "${stat.totalAyahsRead} টি আয়াত",
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
                                            text = "${stat.totalAyahsRead} / $totalAyahs আয়াত সম্পন্ন",
                                            color = quranColors.textMain,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$completionPercent%",
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
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

// Tab 3: settings screen
@Composable
fun SettingsScreen(
    viewModel: QuranViewModel,
    ongoingDownloads: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    onOpenDownloadHub: () -> Unit
) {
    var showNotificationOptions by remember { mutableStateOf(false) }

    BackHandler(enabled = showNotificationOptions) {
        showNotificationOptions = false
    }

    AnimatedContent(
        targetState = showNotificationOptions,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "settings_navigation"
    ) { showNotifications ->
        if (showNotifications) {
            NotificationSettingsScreen(viewModel) {
                showNotificationOptions = false
            }
        } else {
            val quranColors = LocalQuranColors.current
            val settings by viewModel.userSettings.collectAsStateWithLifecycle()
            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val haptic = LocalHapticFeedback.current

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "অ্যাপ সেটিংস",
                        color = quranColors.textMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Notification Settings Card Link
                item {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { showNotificationOptions = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(quranColors.primarySoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = quranColors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "নোটিফিকেশন অপশন",
                                    color = quranColors.textMain,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = quranColors.textMuted
                            )
                        }
                    }
                }

                // FontSize Controller Card
                item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(quranColors.primarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "অক্ষরের আকার",
                            color = quranColors.textMain,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Arabic Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("আরবি ফন্ট", color = quranColors.textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${settings.arabicFontSize}px", color = quranColors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = settings.arabicFontSize.toFloat(),
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateArabicFontSize(it.toInt()) 
                            },
                            valueRange = 24f..70f,
                            colors = SliderDefaults.colors(
                                thumbColor = quranColors.primary,
                                activeTrackColor = quranColors.primary,
                                inactiveTrackColor = quranColors.borderColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Translation Slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("বাংলা ও উচ্চারণ", color = quranColors.textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${settings.translationFontSize}px", color = quranColors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = settings.translationFontSize.toFloat(),
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateTranslationFontSize(it.toInt()) 
                            },
                            valueRange = 14f..36f,
                            colors = SliderDefaults.colors(
                                thumbColor = quranColors.primary,
                                activeTrackColor = quranColors.primary,
                                inactiveTrackColor = quranColors.borderColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "লাইভ প্রিভিউ (Preview)",
                        color = quranColors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Card with preview text
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(quranColors.background, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                            fontSize = settings.arabicFontSize.sp,
                            fontFamily = FontFamily.Serif,
                            color = quranColors.textMain,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = (settings.arabicFontSize * 1.5).sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে।",
                            fontSize = settings.translationFontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = quranColors.textMain,
                            lineHeight = (settings.translationFontSize * 1.3).sp
                        )
                    }
                }
            }
        }



        // Neo Clean Mode Card Toggle
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(quranColors.primarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "নিও ক্লিন মোড",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "ডট টেক্সচার ও ক্লিন মিনিমালিস্ট ইন্টারফেস",
                                color = quranColors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Switch(
                        checked = settings.cleanNeoEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateCleanNeoEnabled(it) 
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = quranColors.primary,
                            checkedTrackColor = quranColors.primarySoft,
                            uncheckedThumbColor = quranColors.textMuted.copy(alpha = 0.5f),
                            uncheckedTrackColor = quranColors.background
                        )
                    )
                }
            }
        }



        // Themes Card
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(quranColors.primarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "অ্যাপ থিম",
                            color = quranColors.textMain,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val themes = listOf(
                            "light" to "লাইট",
                            "dark" to "ডার্ক",
                            "sepia" to "সেপিয়া"
                        )
                        themes.forEach { (themeId, label) ->
                            val isSelected = settings.theme == themeId
                            Button(
                                onClick = { viewModel.updateTheme(themeId) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) quranColors.primarySoft else quranColors.background,
                                    contentColor = if (isSelected) quranColors.primary else quranColors.textMuted
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                border = if (isSelected) BorderStroke(1.dp, quranColors.primary) else null
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Qaris custom list
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(quranColors.primarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = quranColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "তেলাওয়াতকারী নির্বাচন",
                            color = quranColors.textMain,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isOnline) {
                        Text(
                            text = "অফলাইন মোড: শুধুমাত্র প্রধান ক্বারী সক্রিয় আছেন।",
                            color = quranColors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    val displayQaris = if (isOnline) QARIS_LIST else listOf(QARIS_LIST.first())
                    displayQaris.forEach { qari ->
                        val isSelected = settings.selectedQari == qari.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) quranColors.primarySoft else Color.Transparent)
                                .clickable { viewModel.updateQari(qari.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isSelected) quranColors.primary else quranColors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    qari.name,
                                    color = quranColors.textMain,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    qari.englishName,
                                    color = quranColors.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(quranColors.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Download Manager Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            val downloadedList by viewModel.downloadedSurahsList.collectAsStateWithLifecycle()
            val downloadState by viewModel.bulkDownloadState.collectAsStateWithLifecycle()

            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                border = BorderStroke(1.dp, quranColors.borderColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(quranColors.primarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = quranColors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "অফলাইন ডাউনলোড হাব",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        if (downloadState.isDownloading) {
                            Text(
                                text = "ডাউনলোড হচ্ছে... ${downloadState.percentage}%",
                                color = quranColors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "সকল সূরা এক ক্লিকে অডিও ও অনুবাদসহ ডাউনলোড করে অফলাইনে সহজে ব্যবহার করুন। আপনার ক্যাশ ডেটা চিরতরে সংরক্ষিত থাকবে এবং অফলাইনে কোনো ইন্টারনেট লাগবে না।",
                        color = quranColors.textMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenDownloadHub()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = quranColors.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (downloadedList.isNotEmpty()) "সংরক্ষণাগার ও ডাউনলোড হাব খুলুন (${downloadedList.size} টি)" else "ডাউনলোড হাব খুলুন",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
        } // end form else
    } // end AnimatedContent
}

@Composable
fun NotificationSettingsScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("app_notifications", android.content.Context.MODE_PRIVATE) }
    
    // Master Switch - uses settings
    val isMasterEnabled = settings.dailyNotificationEnabled

    // Internal toggles (only changeable if master is enabled)
    var customReminderEnabled by remember { mutableStateOf(prefs.getBoolean("custom_reminder", true)) }
    var autoNotificationEnabled by remember { mutableStateOf(prefs.getBoolean("auto_notification", true)) }
    
    var autoMorningEnabled by remember { mutableStateOf(prefs.getBoolean("auto_morning", true)) }
    var auto10amEnabled by remember { mutableStateOf(prefs.getBoolean("auto_10am", true)) }
    var auto12pmEnabled by remember { mutableStateOf(prefs.getBoolean("auto_12pm", true)) }
    var auto4pmEnabled by remember { mutableStateOf(prefs.getBoolean("auto_4pm", true)) }
    var auto5pmEnabled by remember { mutableStateOf(prefs.getBoolean("auto_5pm", true)) }
    var auto9pmEnabled by remember { mutableStateOf(prefs.getBoolean("auto_9pm", true)) }
    var auto10pmEnabled by remember { mutableStateOf(prefs.getBoolean("auto_10pm", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // App bar like back header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack() 
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = quranColors.textMain)
            }
            Text("নোটিফিকেশন অপশন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = quranColors.textMain)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Master Switch Card
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "সকল নোটিফিকেশন",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = isMasterEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateDailyNotificationEnabled(context, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = quranColors.primary,
                                    uncheckedThumbColor = quranColors.textMuted,
                                    uncheckedTrackColor = quranColors.background
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "এই অপশনটি বন্ধ করলে অ্যাপের সকল রিমাইন্ডার এবং স্বয়ংক্রিয় নোটিফিকেশন বন্ধ হয়ে যাবে।",
                            color = quranColors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(visible = isMasterEnabled) {
                    Column {
                        // Customizable Daily Reminder Card
                        Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "রিমাইন্ডার নোটিফিকেশন",
                                        color = quranColors.textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "আপনার পছন্দ অনুযায়ী নির্দিষ্ট সময়ে মনে করিয়ে দেওয়ার জন্য।",
                                        color = quranColors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Switch(
                                    checked = customReminderEnabled,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        customReminderEnabled = it
                                        prefs.edit().putBoolean("custom_reminder", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = quranColors.primary,
                                        uncheckedThumbColor = quranColors.textMuted,
                                        uncheckedTrackColor = quranColors.background
                                    )
                                )
                            }

                            AnimatedVisibility(visible = customReminderEnabled) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(quranColors.borderColor))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    val displayTime = remember(settings.notificationHour, settings.notificationMinute) {
                                        val h = settings.notificationHour
                                        val m = settings.notificationMinute
                                        val amPm = if (h >= 12) "PM" else "AM"
                                        val hr = when {
                                            h == 0 -> 12
                                            h > 12 -> h - 12
                                            else -> h
                                        }
                                        String.format("%02d:%02d %s", hr, m, amPm)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "রিমাইন্ডার শিডিউল সময়:",
                                            color = quranColors.textMain,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            displayTime,
                                            color = quranColors.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            modifier = Modifier
                                                .background(quranColors.primarySoft, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Hour Adjuster
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ঘণ্টা (Hour)", color = quranColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${settings.notificationHour} টা", color = quranColors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = settings.notificationHour.toFloat(),
                                            onValueChange = { viewModel.updateNotificationTime(context, it.toInt(), settings.notificationMinute) },
                                            valueRange = 0f..23f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = quranColors.primary,
                                                activeTrackColor = quranColors.primary,
                                                inactiveTrackColor = quranColors.borderColor
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Minute Adjuster
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("মিনিট (Minute)", color = quranColors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${settings.notificationMinute} মি.", color = quranColors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = settings.notificationMinute.toFloat(),
                                            onValueChange = { viewModel.updateNotificationTime(context, settings.notificationHour, it.toInt()) },
                                            valueRange = 0f..59f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = quranColors.primary,
                                                activeTrackColor = quranColors.primary,
                                                inactiveTrackColor = quranColors.borderColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Auto App Notification Card
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "স্বয়ংক্রিয় নোটিফিকেশন",
                                        color = quranColors.textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "অ্যাপ থেকে প্রতিদিন নির্দিষ্ট ৭টি সময়ে ইসলামি রিমাইন্ডার পাঠানো হবে।",
                                        color = quranColors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = autoNotificationEnabled,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        autoNotificationEnabled = checked
                                        prefs.edit().putBoolean("auto_notification", checked).apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = quranColors.primary,
                                        uncheckedThumbColor = quranColors.textMuted,
                                        uncheckedTrackColor = quranColors.background
                                    )
                                )
                            }
                            
                            AnimatedVisibility(visible = autoNotificationEnabled) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(quranColors.borderColor.copy(alpha = 0.5f)))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    val autoSettings = listOf(
                                        Triple("auto_morning", "সকাল ৮:০০ টা", "রহমতের সুপ্রভাত") to autoMorningEnabled,
                                        Triple("auto_10am", "সকাল ১০:০০ টা", "কাজের ফাঁকে রিফ্রেশমেন্ট") to auto10amEnabled,
                                        Triple("auto_12pm", "দুপুর ১২:০০ টা", "দুপুরের প্রশান্তি") to auto12pmEnabled,
                                        Triple("auto_4pm", "বিকাল ৪:০০ টা", "অনুপ্রেরণামূলক আয়াত") to auto4pmEnabled,
                                        Triple("auto_5pm", "বিকাল ৫:০০ টা", "তিলওয়াতের লক্ষ্য") to auto5pmEnabled,
                                        Triple("auto_9pm", "রাত ৯:০০ টা", "রাতের সুন্নাহ আমল") to auto9pmEnabled,
                                        Triple("auto_10pm", "রাত ১০:০০ টা", "রাতের বিশেষ সূরা") to auto10pmEnabled
                                    )
                                    
                                    autoSettings.forEachIndexed { index, (info, isEnabled) ->
                                        val (key, timeStr, desc) = info
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    desc,
                                                    color = quranColors.textMain,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    timeStr,
                                                    color = quranColors.primary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            Switch(
                                                checked = isEnabled,
                                                onCheckedChange = { checked ->
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    prefs.edit().putBoolean(key, checked).apply()
                                                    when(key) {
                                                        "auto_morning" -> autoMorningEnabled = checked
                                                        "auto_10am" -> auto10amEnabled = checked
                                                        "auto_12pm" -> auto12pmEnabled = checked
                                                        "auto_4pm" -> auto4pmEnabled = checked
                                                        "auto_5pm" -> auto5pmEnabled = checked
                                                        "auto_9pm" -> auto9pmEnabled = checked
                                                        "auto_10pm" -> auto10pmEnabled = checked
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = quranColors.primary,
                                                    uncheckedThumbColor = quranColors.textMuted,
                                                    uncheckedTrackColor = quranColors.background
                                                ),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                        if (index < autoSettings.size - 1) {
                                            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(quranColors.borderColor.copy(alpha = 0.5f)))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notification Style Card restored
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = quranColors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(quranColors.primarySoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = quranColors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "নোটিফিকেশন স্টাইল ও টেস্ট",
                                        color = quranColors.textMain,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "প্লেয়ার ব্যাকগ্রাউন্ড ও নোটিফিকেশন টেস্ট",
                                        color = quranColors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(quranColors.borderColor))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "প্লেয়ার নোটিফিকেশন ডিজাইন স্টাইল:",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "অডিও প্লেয়ারের রানিং নোটিফিকেশনের ব্যাকগ্রাউন্ড রঙ নির্বাচন করুন।",
                                color = quranColors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            val styleChoices = listOf(
                                Triple("classic_green", 0xFF0B562F, "সবুজ"),
                                Triple("minimal_dark", 0xFF121212, "ডার্ক"),
                                Triple("calm_azure", 0xFF006699, "নীল"),
                                Triple("royal_gold", 0xFFB8860B, "সোনালী"),
                                Triple("dark_slate", 0xFF2F4F4F, "স্লেট")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                styleChoices.forEach { (styleName, colorHex, displayName) ->
                                    val isSelected = settings.notificationStyle == styleName
                                    val backgroundBrush = Color(colorHex)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) backgroundBrush else backgroundBrush.copy(alpha = 0.15f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) quranColors.primary else backgroundBrush.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.updateNotificationStyle(styleName)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(backgroundBrush)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = displayName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) {
                                                    if (styleName == "minimal_dark" || styleName == "dark_slate" || styleName == "classic_green") Color.White else quranColors.textMain
                                                } else {
                                                    quranColors.textMain
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(quranColors.borderColor))
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "হোম স্ক্রিন উইজেট থিম ও ডিজাইন:",
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "পবিত্র কুরআনের হোম স্ক্রিন উইজেটগুলোর জন্য আপনার পছন্দের কালার স্কিম ও নকশা নির্বাচন করুন।",
                                color = quranColors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            val widgetChoices = listOf(
                                Triple("classic_green", 0xFF0B562F, "সবুজ সোনালী"),
                                Triple("minimal_dark", 0xFF121212, "স্লেট ডার্ক"),
                                Triple("calm_azure", 0xFF024D7A, "শান্ত নীল"),
                                Triple("royal_gold", 0xFF8A6B1A, "রাজকীয় সোনা"),
                                Triple("dark_slate", 0xFF112424, "ডার্ক স্লেট")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                widgetChoices.forEach { (styleName, colorHex, displayName) ->
                                    val isSelected = settings.widgetStyle == styleName
                                    val backgroundBrush = Color(colorHex)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) backgroundBrush else backgroundBrush.copy(alpha = 0.15f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) quranColors.primary else backgroundBrush.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.updateWidgetStyle(styleName, context)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(backgroundBrush)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = displayName,
                                                fontSize = 10.sp,
                                                lineHeight = 11.sp,
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) {
                                                    if (styleName == "minimal_dark" || styleName == "dark_slate" || styleName == "classic_green") Color.White else quranColors.textMain
                                                } else {
                                                    quranColors.textMain
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    try {
                                        val intent = android.content.Intent(context, com.example.data.receiver.DailyReminderReceiver::class.java).apply {
                                            putExtra("reminder_type", "custom")
                                        }
                                        context.sendBroadcast(intent)
                                    } catch (e: Exception) {
                                        try {
                                            com.example.data.receiver.DailyReminderReceiver().onReceive(context, android.content.Intent().apply { putExtra("reminder_type", "custom") })
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = quranColors.primarySoft),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = quranColors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "একটি টেস্ট রিমাইন্ডার এখনই পাঠান",
                                    color = quranColors.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
    }
}

// Overlay View: Reading view
@Composable
fun SurahReadingScreen(
    viewModel: QuranViewModel,
    formattedSurah: FormattedSurah,
    isPlaying: Boolean,
    activeAyahIndex: Int
) {
    val quranColors = LocalQuranColors.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val bookmarks by viewModel.favoriteSurahs.collectAsStateWithLifecycle()
    val isFav = bookmarks.any { it.surahNumber == formattedSurah.number }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val isDownloaded = settings.downloadedSurahsJson.contains(",${formattedSurah.number},")
    val scope = rememberCoroutineScope()

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "ডাউনলোড মুছে ফেলবেন?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = quranColors.textMain
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে আপনি এই সূরার অফলাইন অডিও ফাইলগুলো মুছে ফেলতে চান?",
                    fontSize = 14.sp,
                    color = quranColors.textMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteDownloadedSurah(formattedSurah.number, settings.selectedQari)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F) // Deep Red for destructive action
                    )
                ) {
                    Text(
                        "হ্যাঁ, মুছুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text(
                        "না, থাক",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = quranColors.textMuted
                    )
                }
            },
            containerColor = quranColors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        )
    }

    // Automatic Scroll into view when an ayah is changed inside playback
    LaunchedEffect(activeAyahIndex) {
        if (activeAyahIndex in 0 until formattedSurah.ayahs.size) {
            // Scroll to center active index on playback change
            scrollState.animateScrollToItem(activeAyahIndex + 1) // index + 1 due to header card
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Main Surah Info Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(quranColors.primary, Color(0xFF139755))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${if (formattedSurah.revelationType == "Meccan") "মাক্কী" else "মাদানী"} • ${formattedSurah.numberOfAyahs} আয়াত",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(99.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (downloadProgress != null) {
                                CircularProgressIndicator(
                                    progress = downloadProgress ?: 0f,
                                    color = quranColors.accent,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${((downloadProgress ?: 0f) * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (isDownloaded) {
                                // Beautiful downloaded badge + separate confirmable delete button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = quranColors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ডাউনলোডকৃত",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { showDeleteConfirmation = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete downloaded Surah",
                                        tint = Color(0xFFFFD1D1), // Soft, elegant red
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                // Downloader trigger
                                IconButton(
                                    onClick = {
                                        viewModel.downloadSurahOffline(
                                            surahNumber = formattedSurah.number,
                                            qari = settings.selectedQari,
                                            onProgress = { progress ->
                                                downloadProgress = progress
                                            },
                                            onCompleted = { success ->
                                                downloadProgress = null
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Download Surah Offline",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    // Dummy model representing active view items
                                    val reference = SurahModel(
                                        number = formattedSurah.number,
                                        name = formattedSurah.name,
                                        englishName = formattedSurah.englishName,
                                        englishNameTranslation = formattedSurah.englishNameTranslation,
                                        numberOfAyahs = formattedSurah.numberOfAyahs,
                                        revelationType = formattedSurah.revelationType
                                    )
                                    viewModel.toggleFavoriteSurah(reference)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (isFav) quranColors.accent else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        formattedSurah.name,
                        fontFamily = FontFamily.Serif,
                        color = quranColors.accent,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formattedSurah.englishName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formattedSurah.englishNameTranslation,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big full audio start button
                    Button(
                        onClick = {
                            viewModel.playerService.value?.setSurahAndPlay(formattedSurah, 0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = quranColors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isDownloaded) "অফলাইন তেলাওয়াত শুরু করুন" else "সম্পূর্ণ তেলাওয়াত শুরু করুন",
                            color = quranColors.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Bismillah Banner (Omit for At-Tawbah 9 & Al-Fatihah 1)
        if (formattedSurah.number != 9 && formattedSurah.number != 1) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, quranColors.borderColor, RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        color = quranColors.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // List of Ayahs
        itemsIndexed(formattedSurah.ayahs, key = { _, ayah -> ayah.numberInSurah }) { index, ayah ->
            val isActive = activeAyahIndex == index

            Card(
                onClick = {
                    val service = viewModel.playerService.value
                    if (service != null && service.currentSurah.value?.number == formattedSurah.number && service.currentAyahIndex.value == index) {
                        service.togglePlayPause()
                    } else {
                        viewModel.playerService.value?.setSurahAndPlay(formattedSurah, index)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) quranColors.primarySoft.copy(alpha = 0.08f) else quranColors.surface
                ),
                border = BorderStroke(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) quranColors.accent.copy(alpha = 0.5f) else quranColors.borderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(quranColors.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ayah.numberInSurah.toString(),
                                color = quranColors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Play/Pause individual trigger button
                        IconButton(
                            onClick = {
                                val service = viewModel.playerService.value
                                if (service != null && service.currentSurah.value?.number == formattedSurah.number && service.currentAyahIndex.value == index) {
                                    service.togglePlayPause()
                                } else {
                                    viewModel.playerService.value?.setSurahAndPlay(formattedSurah, index)
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isActive && isPlaying) quranColors.primary else quranColors.background)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = if (isActive && isPlaying) Color.White else quranColors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Arabic Text block
                    Text(
                        text = ayah.arabicText,
                        fontSize = settings.arabicFontSize.sp,
                        fontFamily = FontFamily.Serif,
                        color = quranColors.textMain,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = (settings.arabicFontSize * 1.6).sp,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bengali Translation text block
                    Text(
                        text = ayah.bengaliText,
                        fontSize = settings.translationFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = quranColors.textMain,
                        lineHeight = (settings.translationFontSize * 1.35).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pronunciation / Transliteration phonetic block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(quranColors.background.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "উচ্চারণ",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = quranColors.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                        Text(
                            text = ayah.transliterationText,
                            fontSize = (settings.translationFontSize * 0.8).sp,
                            fontWeight = FontWeight.Medium,
                            color = quranColors.textMuted,
                            lineHeight = (settings.translationFontSize * 1.2).sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Mini Floating Audio Player
@Composable
fun MiniPlayerBlock(
    surah: FormattedSurah,
    isPlaying: Boolean,
    currentAyahIndex: Int,
    progress: Int,
    duration: Int,
    onPlayPauseClicked: () -> Unit,
    onExpandClicked: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val currentAyahNumber = if (currentAyahIndex in 0 until surah.ayahs.size) {
        surah.ayahs[currentAyahIndex].numberInSurah
    } else {
        1
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(quranColors.surface, RoundedCornerShape(24.dp))
            .clickable { onExpandClicked() },
        color = quranColors.surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Slider line progress on top bar
            val progressRatio = (progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                drawLine(
                    color = quranColors.borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = quranColors.primary,
                    start = Offset(0f, 0f),
                    end = Offset(size.width * progressRatio, 0f),
                    strokeWidth = 4.dp.toPx()
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah Index badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(quranColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    // Quick EQ animations
                    if (isPlaying) {
                        PlayEqAnimation(color = Color.White)
                    } else {
                        Text(
                            currentAyahNumber.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        surah.englishName,
                        color = quranColors.textMain,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isPlaying) "Playing Ayah $currentAyahNumber" else "Paused Ayah $currentAyahNumber",
                        color = quranColors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Play control
                IconButton(
                    onClick = onPlayPauseClicked,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(quranColors.background)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = quranColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = quranColors.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Fullscreen Slide up player
@Composable
fun ExpandedPlayerView(
    surah: FormattedSurah,
    isPlaying: Boolean,
    currentAyahIndex: Int,
    progress: Int,
    duration: Int,
    loopMode: Int,
    settings: UserSettings,
    onCloseRequested: () -> Unit,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPrevClicked: () -> Unit,
    onLoopClicked: () -> Unit,
    onSeekIndexChanged: (Int) -> Unit,
    onProgressSeek: (Int) -> Unit,
    onGoToSurahClicked: () -> Unit
) {
    val quranColors = LocalQuranColors.current
    val currentAyahNumber = if (currentAyahIndex in 0 until surah.ayahs.size) {
        surah.ayahs[currentAyahIndex].numberInSurah
    } else {
        1
    }
    val currentAyah = if (currentAyahIndex in 0 until surah.ayahs.size) {
        surah.ayahs[currentAyahIndex]
    } else {
        surah.ayahs.getOrNull(0)
    }

    val context = LocalContext.current

    Surface(
        color = quranColors.surface,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle / Header closing bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(quranColors.borderColor)
                )
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCloseRequested,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(quranColors.background)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = quranColors.textMain
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NOW PLAYING",
                        color = quranColors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        surah.englishName,
                        color = quranColors.textMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(
                    onClick = {
                        // Share via intents
                        if (currentAyah != null) {
                            val textStr = "${currentAyah.arabicText}\n\n${currentAyah.bengaliText}\n\n- সূরা ${surah.name} (${surah.englishName}), আয়াত ${currentAyah.numberInSurah}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, textStr)
                            }
                            context.startActivity(Intent.createChooser(intent, "আয়াত শেয়ার করুন"))
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(quranColors.background)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = quranColors.textMain
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large scripture display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, quranColors.borderColor, RoundedCornerShape(32.dp))
                    .background(quranColors.background.copy(alpha = 0.3f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentAyah != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        val qariName = QARIS_LIST.find { it.id == settings.selectedQari }?.englishName ?: "Quran Reciter"
                        Text(
                            text = qariName.uppercase(),
                            color = quranColors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = currentAyah.arabicText,
                            fontSize = (settings.arabicFontSize * 1.1).sp,
                            fontFamily = FontFamily.Serif,
                            color = quranColors.textMain,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = (settings.arabicFontSize * 1.8).sp,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = currentAyah.bengaliText,
                            fontSize = (settings.translationFontSize * 1.1).sp,
                            fontWeight = FontWeight.Bold,
                            color = quranColors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = (settings.translationFontSize * 1.5).sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player Seeker Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
            ) {
                // Verse Navigation Slider (Ayah jump slider)
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    val totalVerses = surah.ayahs.size
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "আয়াত $currentAyahNumber",
                            color = quranColors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "মোট $totalVerses আয়াত",
                            color = quranColors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Slider(
                        value = currentAyahIndex.coerceAtLeast(0).toFloat(),
                        onValueChange = { index ->
                            onSeekIndexChanged(index.toInt())
                        },
                        valueRange = 0f..(totalVerses - 1).toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = quranColors.primary,
                            activeTrackColor = quranColors.primary,
                            inactiveTrackColor = quranColors.borderColor
                        )
                    )
                }

                // Audio track position timeline (Time Seeker)
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    val formattedCurrent = formatProgressTime(progress)
                    val formattedDuration = formatProgressTime(duration)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(formattedCurrent, color = quranColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(formattedDuration, color = quranColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = { ms ->
                            onProgressSeek(ms.toInt())
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = quranColors.primary,
                            activeTrackColor = quranColors.primary,
                            inactiveTrackColor = quranColors.borderColor
                        )
                    )
                }

                // Control panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Loop Mode toggle with multiple icons indication
                    IconButton(
                        onClick = onLoopClicked,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (loopMode != 0) quranColors.primarySoft.copy(alpha = 0.2f) else Color.Transparent)
                            .size(48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (loopMode) {
                                    1 -> Icons.Default.Repeat
                                    2 -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Loop Mode",
                                tint = if (loopMode != 0) quranColors.primary else quranColors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = when (loopMode) {
                                    1 -> "Surah"
                                    2 -> "Ayah"
                                    else -> "Off"
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (loopMode != 0) quranColors.primary else quranColors.textMuted
                            )
                        }
                    }

                    // Skip previous
                    IconButton(
                        onClick = onPrevClicked,
                        enabled = currentAyahIndex > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (currentAyahIndex > 0) quranColors.textMain else quranColors.borderColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Large Play Pause Glow Button
                    Box(
                        modifier = Modifier
                            .shadow(12.dp, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(quranColors.primary, Color(0xFF139755))
                                ),
                                CircleShape
                            )
                            .size(76.dp)
                            .clickable { onPlayPauseClicked() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Skip next
                    IconButton(
                        onClick = onNextClicked,
                        enabled = currentAyahIndex < surah.ayahs.size - 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Ayah",
                            tint = if (currentAyahIndex < surah.ayahs.size - 1) quranColors.textMain else quranColors.borderColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Snaps to Surah List
                    IconButton(
                        onClick = onGoToSurahClicked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Go back to Quran",
                            tint = quranColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// Convert ms to string
private fun formatProgressTime(ms: Int): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%d:%02d", minutes, seconds)
}

// Custom EQ line animations
@Composable
fun PlayEqAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val heights = listOf(
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
            label = "h1"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
            label = "h2"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
            label = "h3"
        )
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .fillMaxHeight(anim.value)
                    .width(4.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

data class HourlyVerse(
    val surahNumber: Int,
    val ayahIndex: Int,
    val surahName: String,
    val ayahNumber: Int,
    val arabicText: String,
    val bengaliText: String
)

val HourlyVersesList = listOf(
    HourlyVerse(1, 1, "Al-Fatihah", 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "সমস্ত প্রশংসা আল্লাহর জন্য, যিনি সকল সৃষ্টির পালনকর্তা।"),
    HourlyVerse(2, 254, "Al-Baqarah", 255, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ", "আল্লাহ ছাড়া কোনো সত্য উপাস্য নেই, তিনি চিরঞ্জীব, সর্বসত্তার ধারক।"),
    HourlyVerse(2, 185, "Al-Baqarah", 186, "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ", "আর আমার বান্দারা যখন আপনার কাছে আমার ব্যাপারে জিজ্ঞাসা করে, আমি তো অবশ্যই নিকটবর্তী।"),
    HourlyVerse(2, 285, "Al-Baqarah", 286, "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "আল্লাহ কোনো প্রাণের ওপর তার সাধ্যের অতিরিক্ত দায়িত্ব চাপিয়ে দেন না।"),
    HourlyVerse(3, 158, "Ali 'Imran", 159, "فَإِذَا عَزَمْتَ فَتَوَكَّلْ عَلَى اللَّهِ", "অতঃপর যখন তুমি সিদ্ধান্ত গ্রহণ করবে, তখন আল্লাহর ওপর ভরসা করো।"),
    HourlyVerse(3, 190, "Ali 'Imran", 191, "رَبَّنَا مَا خَلَقْتَ هَٰذَا بَاطِلًا سُبْحَانَكَ", "হে আমাদের রব! আপনি এগুলো নিরর্থক সৃষ্টি করেননি, আপনি অত্যন্ত পবিত্র।"),
    HourlyVerse(6, 53, "Al-An'am", 54, "كَتَبَ رَبُّكُمْ عَلَىٰ نَفْسِهِ الرَّحْمَةَ", "তোমাদের রব তো নিজের ওপর দয়া প্রদর্শন করা লিখে নিয়েছেন।"),
    HourlyVerse(7, 55, "Al-A'raf", 56, "إِنَّ رَحْمَتَ اللَّهِ قَرِيبٌ مِنَ الْمُحْسِنِينَ", "নিশ্চয়ই আল্লাহর অনুগ্রহ সৎকর্মশীলদের অতি নিকটবর্তী।"),
    HourlyVerse(7, 203, "Al-A'raf", 204, "وَإِذَا قُرِئَ الْقُرْآنُ فَاسْتَمِعُوا لَهُ وَأَنْصِتُوا", "আর যখন কুরআন পাঠ করা হয়, তখন তোমরা মনোযোগ দিয়ে শোনো এবং চুপ থাকো।"),
    HourlyVerse(8, 1, "Al-Anfal", 2, "إِنَّمَا الْمُؤْمِنُونَ الَّذِينَ إِذَا ذُكِرَ اللَّهُ وَجِلَتْ قُلُوبُهُمْ", "মুমিন তো তারাই, আল্লাহর কথা স্মরণ করা হলে যাদের হৃদয় কেঁপে ওঠে।"),
    HourlyVerse(10, 56, "Yunus", 57, "قَدْ جَاءَتْكُمْ مَوْعِظَةٌ مِنْ رَبِّكُمْ وَشِفَاءٌ", "তোমাদের কাছে এসেছে তোমাদের রবের পক্ষ থেকে উপদেশ এবং অন্তরের ব্যাধির আরোগ্য।"),
    HourlyVerse(11, 114, "Hud", 115, "وَاصْبِرْ فَإِنَّ اللَّهَ لَا يُضِيعُ أَجْرَ الْمُحْسِنِينَ", "আর আপনি ধৈর্য ধরুন, নিশ্চয়ই আল্লাহ সৎকর্মশীলদের প্রতি প্রতিদান নষ্ট করেন না।"),
    HourlyVerse(13, 27, "Ar-Ra'd", 28, "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "জেনে রাখো, আল্লাহর স্মরণেই কেবল হৃদয়সমূহ শান্ত হয়।"),
    HourlyVerse(14, 6, "Ibrahim", 7, "لَئِنْ شَكَرْتُمْ لَأَزِيدَنَّكُمْ", "যদি তোমরা কৃতজ্ঞতা জ্ঞাপন করো, তবে আমি অবশ্যই তোমাদের বাড়িয়ে দেব।"),
    HourlyVerse(17, 81, "Al-Isra", 82, "وَنُنَزِّلُ مِنَ الْقُرْآنِ مَا هُوَ شِفَاءٌ وَرَحْمَةٌ", "আমি কুরআন নাজিল করেছি যা মুমিনদের জন্য আরোগ্য ও দয়া স্বরূপ।"),
    HourlyVerse(18, 108, "Al-Kahf", 109, "قُلْ لَوْ كَانَ الْبَحْرُ مِدَادًا لِكَلِمَاتِ رَبِّي لَنَفِدَ الْبَحْرُ", "বলুন: আমার রবের বাণীসমূহ লেখার জন্য যদি সমুদ্র কালি হয়, তবে তা শেষ হয়ে যাবে।"),
    HourlyVerse(20, 113, "Taha", 114, "وَقُلْ رَبِّ زِدْنِي عِلْمًا", "এবং বলুন: হে আমার রব! আমার জ্ঞান বৃদ্ধি করে দিন।"),
    HourlyVerse(21, 86, "Al-Anbiya", 87, "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ", "আপনি ছাড়া কোনো উপাস্য নেই, আপনি পবিত্র! নিশ্চয়ই আমি অপরাধীদের অন্তর্ভুক্ত ছিলাম।"),
    HourlyVerse(25, 73, "Al-Furqan", 74, "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ", "হে আমাদের রব! আমাদের স্ত্রীদের ও সন্তানদেরকে আমাদের চোখের শীতলতাস্বরূপ বানিয়ে দিন।"),
    HourlyVerse(29, 68, "Al-Ankabut", 69, "وَالَّذِينَ جَاهَدُوا فِينَا لَنَهْدِيَنَّهُمْ سُبُلَنَا", "আর যারা আমার পথে সর্বাত্মক প্রচেষ্টা চালায়, আমি অবশ্যই তাদেরকে আমার পথে পরিচালিত করব।"),
    HourlyVerse(39, 52, "Az-Zumar", 53, "لَا تَقْنَطُوا مِنْ رَحْمَةِ اللَّهِ", "তোমরা আল্লাহর রহমত হতে নিরাশ হয়ো না, নিশ্চয়ই আল্লাহ সব পাপ ক্ষমা করেন।"),
    HourlyVerse(40, 59, "Ghafir", 60, "وَقَالَ رَبُّكُمُ ادْعُونِي أَسْتَجِبْ لَكُمْ", "এবং তোমাদের রব বলেছেন: তোমরা আমাকে ডাকো, আমি তোমাদের ডাকে সাড়া দেব।"),
    HourlyVerse(93, 2, "Ad-Duha", 3, "مَا وَدَّعَكَ رَبُّكَ وَمَا قَلَىٰ", "আপনার রব আপনাকে পরিত্যাগ করেননি এবং অসন্তুষ্টও হননি।"),
    HourlyVerse(94, 4, "Al-Inshirah", 5, "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "নিশ্চয়ই কষ্টের সাথেই রয়েছে স্বস্তি।")
)


