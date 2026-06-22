package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FavoriteSurah
import com.example.data.model.FormattedSurah
import com.example.data.model.LastPlayed
import com.example.data.model.SurahModel
import com.example.data.model.UserSettings
import com.example.data.repository.QuranRepository
import com.example.data.service.QuranPlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// বাল্ক ডাউনলোডের ডেটা স্ট্রাকচার (অগ্রগতি অ্যানিমেশনের জন্য percentage-কে Float করা হয়েছে, compat bounds as 0f..100f)
data class BulkDownloadState(
    val isDownloading: Boolean = false,
    val type: String = "TEXT", // "TEXT" অথবা "AUDIO"
    val currentSurah: Int = 0,
    val totalSurahs: Int = 114,
    val currentSurahName: String = "",
    val percentage: Float = 0f, // 0.0f থেকে 100.0f
    val error: String? = null
)

class QuranViewModel(private val repository: QuranRepository) : ViewModel() {

    // Network connectivity tracking
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Service bridge
    private val _playerService = MutableStateFlow<QuranPlayerService?>(null)
    val playerService = _playerService.asStateFlow()

    // Surah list loader
    private val _surahListState = MutableStateFlow<UiState<List<SurahModel>>>(UiState.Idle)
    val surahListState: StateFlow<UiState<List<SurahModel>>> = _surahListState.asStateFlow()

    // Current reading surah loader
    private val _readingSurahState = MutableStateFlow<UiState<FormattedSurah>>(UiState.Idle)
    val readingSurahState: StateFlow<UiState<FormattedSurah>> = _readingSurahState.asStateFlow()

    // Search query StateFlow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Bulk download state
    private val _bulkDownloadState = MutableStateFlow(BulkDownloadState())
    val bulkDownloadState = _bulkDownloadState.asStateFlow()

    private var statsTrackerJob: Job? = null

    // Combined filtered Surah list (অ্যানিমেশন বান্ধব Flow)
    val filteredSurahs = combine(_surahListState, _searchQuery) { state, query ->
        if (state is UiState.Success) {
            if (query.isBlank()) state.data
            else {
                state.data.filter {
                    it.englishName.contains(query, ignoreCase = true) ||
                    it.name.contains(query) ||
                    it.englishNameTranslation.contains(query, ignoreCase = true)
                }
            }
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks direct Flow
    val favoriteSurahs: StateFlow<List<FavoriteSurah>> = repository.getFavoriteSurahs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History (Last played) direct Flow
    val lastPlayed: StateFlow<LastPlayed?> = repository.getLastPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User settings Flow (মেমোরি লিক এবং স্লাগিশ কম্বাইন লজিক ফিক্সড)
    val userSettings: StateFlow<UserSettings> = repository.getUserSettings()
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val allStats: StateFlow<List<com.example.data.model.QuranStats>> = repository.getAllStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSurahStats: StateFlow<List<com.example.data.model.SurahPlayStats>> = repository.getAllSurahStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSurah: StateFlow<com.example.data.model.SurahPlayStats?> = allSurahStats
        .map { list ->
            list.filter { it.totalDurationSeconds > 0 }.maxByOrNull { it.totalDurationSeconds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadSurahList()
        setupDefaultSettings()
        observePlayerPlaybackForStats()
    }

    fun updateNetworkStatus(online: Boolean) {
        _isOnline.value = online
        if (online && (_surahListState.value is UiState.Idle || _surahListState.value is UiState.Error)) {
            loadSurahList()
        }
    }

    private fun setupDefaultSettings() {
        viewModelScope.launch {
            repository.getUserSettings().collect { saved ->
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                if (saved == null) {
                    repository.saveUserSettings(UserSettings(installationDate = todayStr))
                } else if (saved.installationDate.isEmpty()) {
                    repository.saveUserSettings(saved.copy(installationDate = todayStr))
                }
            }
        }
    }

    // ব্যাকগ্রাউন্ড ব্যাটারি ড্রেন ও ট্র্যাকিং বাগ ফিক্স
    private fun observePlayerPlaybackForStats() {
        viewModelScope.launch {
            _playerService.collect { service ->
                if (service != null) {
                    service.isPlaying.collect { playing ->
                        if (playing) startStatsTracker() else stopStatsTracker()
                    }
                } else {
                    stopStatsTracker()
                }
            }
        }
    }

    private fun startStatsTracker() {
        if (statsTrackerJob?.isActive == true) return
        statsTrackerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _playerService.value?.let { service ->
                    if (service.isPlaying.value) {
                        service.currentSurah.value?.let { currentSurah ->
                            incrementSurahStats(currentSurah.number, currentSurah.englishName, 1L, 0)
                        }
                        incrementStats(1L, 0)
                    }
                }
            }
        }
    }

    private fun stopStatsTracker() {
        statsTrackerJob?.cancel()
    }

    fun incrementSurahStats(surahNumber: Int, nameEnglish: String, durationDelta: Long, ayahsDelta: Int) {
        viewModelScope.launch {
            try {
                repository.incrementSurahStats(surahNumber, nameEnglish, durationDelta, ayahsDelta)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun incrementStats(durationSecondsDelta: Long, ayahsDelta: Int) {
        viewModelScope.launch {
            try {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                repository.incrementStats(todayStr, durationSecondsDelta, ayahsDelta)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setPlayerService(service: QuranPlayerService?) {
        _playerService.value = service
        service?.onAyahChangedListener = { surahNum, index, enName, arName, itemInSurah ->
            saveLastPlayedTrack(surahNum, index, enName, arName, itemInSurah)
        }
        service?.onSurahFinishedListener = { playNextSurah() }
        service?.onNextSurahListener = { playNextSurah() }
        service?.onPrevSurahListener = { playPrevSurah() }
    }

    fun loadSurahList() {
        viewModelScope.launch {
            _surahListState.value = UiState.Loading
            try {
                val list = repository.getSurahs()
                _surahListState.value = UiState.Success(list)
            } catch (e: IOException) {
                _surahListState.value = UiState.Error("নেটওয়ার্ক কানেকশন চেক করুন।")
            } catch (e: Exception) {
                _surahListState.value = UiState.Error(e.localizedMessage ?: "ত্রুটি ঘটেছে।")
            }
        }
    }

    fun loadSurahReadingView(number: Int) {
        viewModelScope.launch {
            _readingSurahState.value = UiState.Loading
            try {
                val qari = userSettings.value.selectedQari
                val formattedSurah = repository.getSurahEditions(number, qari)
                _readingSurahState.value = UiState.Success(formattedSurah)
            } catch (e: Exception) {
                _readingSurahState.value = UiState.Error(e.localizedMessage ?: "সূরাটি লোড করতে ব্যর্থ হয়েছে")
            }
        }
    }

    fun closeSurahReadingView() {
        _readingSurahState.value = UiState.Idle
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoriteSurah(surah: SurahModel) {
        viewModelScope.launch {
            val isFav = favoriteSurahs.value.any { it.surahNumber == surah.number }
            if (isFav) {
                repository.deleteFavorite(surah.number)
            } else {
                repository.insertFavorite(
                    FavoriteSurah(
                        surahNumber = surah.number,
                        englishName = surah.englishName,
                        nameArabic = surah.name,
                        numberOfAyahs = surah.numberOfAyahs,
                        revelationType = surah.revelationType
                    )
                )
            }
        }
    }

    fun toggleFavoriteSurahFromFavorite(favorite: FavoriteSurah) {
        viewModelScope.launch { repository.deleteFavorite(favorite.surahNumber) }
    }

    fun updateArabicFontSize(size: Int) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(arabicFontSize = size)) }
    }

    fun updateTranslationFontSize(size: Int) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(translationFontSize = size)) }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(theme = themeName)) }
    }

    fun updateCleanNeoEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(cleanNeoEnabled = enabled)) }
    }

    fun updateNotificationStyle(styleName: String) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(notificationStyle = styleName)) }
    }

    // Context-based setters required for widgets & notification scheduler
    fun updateWidgetStyle(styleName: String, context: android.content.Context) {
        viewModelScope.launch {
            repository.saveUserSettings(userSettings.value.copy(widgetStyle = styleName))
            com.example.data.receiver.QuranPlaybackWidget.updateAllWidgets(context.applicationContext, false, null, -1)
            com.example.data.receiver.QuranHourlyVerseWidget.updateAllWidgets(context.applicationContext)
        }
    }

    fun updateDailyNotificationEnabled(context: android.content.Context, enabled: Boolean) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(dailyNotificationEnabled = enabled)
            repository.saveUserSettings(updated)
            val appCtx = context.applicationContext
            if (enabled) {
                com.example.data.receiver.DailyReminderScheduler.scheduleNextDailyReminder(appCtx, updated.notificationHour, updated.notificationMinute)
                com.example.data.receiver.DailyReminderScheduler.scheduleAllDefaultAutos(appCtx)
            } else {
                com.example.data.receiver.DailyReminderScheduler.cancelReminder(appCtx)
            }
        }
    }

    fun updateNotificationTime(context: android.content.Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(notificationHour = hour, notificationMinute = minute)
            repository.saveUserSettings(updated)
            val appCtx = context.applicationContext
            if (updated.dailyNotificationEnabled) {
                com.example.data.receiver.DailyReminderScheduler.scheduleNextDailyReminder(appCtx, hour, minute)
            }
        }
    }

    // Context-less functions requested by user template
    fun updateWidgetStyleSetting(styleName: String) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(widgetStyle = styleName)) }
    }

    fun updateDailyNotificationEnabledSetting(enabled: Boolean) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(dailyNotificationEnabled = enabled)) }
    }

    fun updateNotificationTimeSetting(hour: Int, minute: Int) {
        viewModelScope.launch { repository.saveUserSettings(userSettings.value.copy(notificationHour = hour, notificationMinute = minute)) }
    }

    fun toggleOfflineSurah(surahNumber: Int) {
        viewModelScope.launch {
            val currentSetting = userSettings.value
            val list = currentSetting.downloadedSurahsJson
            val newList = if (list.contains(",$surahNumber,")) {
                list.replace(",$surahNumber,", "")
            } else {
                if (list.isEmpty()) ",$surahNumber," else "$list$surahNumber,"
            }
            repository.saveUserSettings(currentSetting.copy(downloadedSurahsJson = newList))
        }
    }

    fun updateQari(qariId: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveUserSettings(current.copy(selectedQari = qariId))
            
            val currentReading = _readingSurahState.value
            if (currentReading is UiState.Success) {
                loadSurahReadingView(currentReading.data.number)
            }

            _playerService.value?.let { service ->
                service.currentSurah.value?.let { playingSurah ->
                    val wasPlaying = service.isPlaying.value
                    val previousIndex = service.currentAyahIndex.value
                    try {
                        val updatedSurah = repository.getSurahEditions(playingSurah.number, qariId)
                        if (wasPlaying && previousIndex >= 0) {
                            service.setSurahAndPlay(updatedSurah, previousIndex)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    private fun saveLastPlayedTrack(surahNumber: Int, ayahIndex: Int, englishName: String, nameArabic: String, numberInSurah: Int) {
        viewModelScope.launch {
            incrementStats(0L, 1)
            incrementSurahStats(surahNumber, englishName, 0L, 1)
            repository.saveLastPlayed(
                LastPlayed(
                    surahNumber = surahNumber,
                    ayahIndex = ayahIndex,
                    surahEnglishName = englishName,
                    surahNameArabic = nameArabic,
                    ayahNumberInSurah = numberInSurah
                )
            )
        }
    }

    fun resumeLastPlayed(lastPlayed: LastPlayed) {
        viewModelScope.launch {
            loadSurahReadingView(lastPlayed.surahNumber)
            try {
                val qari = userSettings.value.selectedQari
                val data = repository.getSurahEditions(lastPlayed.surahNumber, qari)
                _playerService.value?.setSurahAndPlay(data, lastPlayed.ayahIndex)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun playNextSurah() {
        val current = _playerService.value?.currentSurah?.value
        val nextNum = if (current != null) {
            if (current.number < 114) current.number + 1 else 1
        } else {
            lastPlayed.value?.surahNumber?.let { if (it < 114) it + 1 else 1 } ?: 1
        }
        viewModelScope.launch {
            try {
                val qari = userSettings.value.selectedQari
                val data = repository.getSurahEditions(nextNum, qari)
                _playerService.value?.setSurahAndPlay(data, 0)
                if (_readingSurahState.value is UiState.Success) {
                    _readingSurahState.value = UiState.Success(data)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun playPrevSurah() {
        val current = _playerService.value?.currentSurah?.value
        val prevNum = if (current != null) {
            if (current.number > 1) current.number - 1 else 114
        } else {
            lastPlayed.value?.surahNumber?.let { if (it > 1) it - 1 else 114 } ?: 114
        }
        viewModelScope.launch {
            try {
                val qari = userSettings.value.selectedQari
                val data = repository.getSurahEditions(prevNum, qari)
                _playerService.value?.setSurahAndPlay(data, 0)
                if (_readingSurahState.value is UiState.Success) {
                    _readingSurahState.value = UiState.Success(data)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun playSpecificSurah(number: Int, index: Int = 0) {
        viewModelScope.launch {
            loadSurahReadingView(number)
            try {
                val qari = userSettings.value.selectedQari
                val data = repository.getSurahEditions(number, qari)
                _playerService.value?.setSurahAndPlay(data, index)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun downloadSurahTextOnly(
        surahNumber: Int,
        qari: String,
        onProgress: (Float) -> Unit,
        onCompleted: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress(0.1f)
                val surah = repository.getSurahEditions(surahNumber, qari)
                onProgress(1.0f)
                val currentSetting = userSettings.value
                if (!currentSetting.downloadedSurahsJson.contains(",$surahNumber,")) {
                    toggleOfflineSurah(surahNumber)
                }
                onCompleted(true)
            } catch (e: Exception) {
                onCompleted(false)
            }
        }
    }

    fun downloadSurahAudioOnly(
        surahNumber: Int,
        qari: String,
        onProgress: (Float) -> Unit,
        onCompleted: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val surah = repository.getSurahEditions(surahNumber, qari)
                val ayahs = surah.ayahs
                if (ayahs.isEmpty()) {
                    onProgress(1.0f)
                    onCompleted(true)
                    return@launch
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val total = ayahs.size
                    for (i in ayahs.indices) {
                        val url = ayahs[i].audioUrl
                        if (url.isNotBlank()) {
                            repository.downloadAudioFile(url)
                        }
                        val currentProgress = (i + 1).toFloat() / total
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onProgress(currentProgress)
                        }
                    }
                }
                onCompleted(true)
            } catch (e: Exception) {
                onCompleted(false)
            }
        }
    }

    fun downloadSurahOffline(
        surahNumber: Int,
        qari: String,
        onProgress: (Float) -> Unit,
        onCompleted: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onProgress(0.05f)
                val surah = repository.getSurahEditions(surahNumber, qari)
                val ayahs = surah.ayahs
                if (ayahs.isEmpty()) {
                    toggleOfflineSurah(surahNumber)
                    onProgress(1.0f)
                    onCompleted(true)
                    return@launch
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val total = ayahs.size
                    for (i in ayahs.indices) {
                        val url = ayahs[i].audioUrl
                        if (url.isNotBlank()) {
                            repository.downloadAudioFile(url)
                        }
                        val currentProgress = 0.05f + (0.95f * (i + 1).toFloat() / total)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onProgress(currentProgress)
                        }
                    }
                }

                val currentSetting = userSettings.value
                if (!currentSetting.downloadedSurahsJson.contains(",$surahNumber,")) {
                    toggleOfflineSurah(surahNumber)
                }
                onCompleted(true)
            } catch (e: Exception) {
                onCompleted(false)
            }
        }
    }

    fun deleteDownloadedSurah(surahNumber: Int, qari: String) {
        viewModelScope.launch {
            try {
                val surah = repository.getSurahEditions(surahNumber, qari)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    for (ayah in surah.ayahs) {
                        if (ayah.audioUrl.isNotBlank()) {
                            val file = repository.getCachedAudioFile(repository.context, ayah.audioUrl)
                            if (file != null && file.exists()) {
                                file.delete()
                            }
                        }
                    }
                    val cachedSurahFile = repository.getCachedSurahFile(surahNumber, qari)
                    if (cachedSurahFile.exists()) {
                        cachedSurahFile.delete()
                    }
                }
                val currentSetting = userSettings.value
                if (currentSetting.downloadedSurahsJson.contains(",$surahNumber,")) {
                    toggleOfflineSurah(surahNumber)
                }
            } catch (e: Exception) { /* Ignored */ }
        }
    }

    // অগ্রগতি ট্র্যাক মেকানিজম সংবলিত সম্পূর্ণ রিয়েল-টাইম বাল্ক ডাউনলোড লজিক (FIXED PROGRESS scaled to 0.0f..100.0f range)
    fun startBulkDownload(context: android.content.Context, type: String) {
        val qari = userSettings.value.selectedQari
        _bulkDownloadState.value = BulkDownloadState(
            isDownloading = true,
            type = type,
            currentSurah = 1,
            currentSurahName = "শুরু হচ্ছে...",
            percentage = 0.0f
        )

        viewModelScope.launch {
            try {
                val allSurahs = (surahListState.value as? UiState.Success)?.data ?: repository.getSurahs()
                val totalSurahs = 114

                for (num in 1..totalSurahs) {
                    if (!_bulkDownloadState.value.isDownloading) break

                    val surahMeta = allSurahs.find { it.number == num }
                    val name = surahMeta?.englishName ?: "সূরা $num"
                    val baseProgress = ((num - 1).toFloat() / totalSurahs.toFloat()) * 100f

                    _bulkDownloadState.value = _bulkDownloadState.value.copy(
                        currentSurah = num,
                        currentSurahName = name,
                        percentage = baseProgress
                    )
                    updateDownloadNotification(context, _bulkDownloadState.value)

                    val formattedSurah = repository.getSurahEditions(num, qari)

                    if (type == "AUDIO") {
                        val ayahs = formattedSurah.ayahs
                        val ayahCount = ayahs.size
                        if (ayahCount > 0) {
                            for (i in ayahs.indices) {
                                if (!_bulkDownloadState.value.isDownloading) break
                                val url = ayahs[i].audioUrl
                                if (url.isNotBlank()) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        repository.downloadAudioFile(url)
                                    }
                                }
                                val surahInternalProgress = (i + 1).toFloat() / ayahCount.toFloat()
                                val totalCurrentProgress = baseProgress + (surahInternalProgress * (100f / totalSurahs.toFloat()))
                                
                                _bulkDownloadState.value = _bulkDownloadState.value.copy(
                                    percentage = totalCurrentProgress.coerceIn(0.0f, 99.9f)
                                )
                                updateDownloadNotification(context, _bulkDownloadState.value)
                            }
                        }
                    } else {
                        val nextProgress = (num.toFloat() / totalSurahs.toFloat()) * 100f
                        _bulkDownloadState.value = _bulkDownloadState.value.copy(
                            percentage = nextProgress.coerceIn(0.0f, 99.9f)
                        )
                        updateDownloadNotification(context, _bulkDownloadState.value)
                    }

                    val currentSetting = userSettings.value
                    if (!currentSetting.downloadedSurahsJson.contains(",$num,")) {
                        toggleOfflineSurah(num)
                    }
                }

                if (_bulkDownloadState.value.isDownloading) {
                    _bulkDownloadState.value = _bulkDownloadState.value.copy(
                        isDownloading = false,
                        percentage = 100.0f,
                        currentSurah = 114,
                        currentSurahName = "সম্পন্ন"
                    )
                    completeDownloadNotification(context, type)
                }
            } catch (e: Exception) {
                _bulkDownloadState.value = _bulkDownloadState.value.copy(
                    isDownloading = false,
                    error = e.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে"
                )
                cancelDownloadNotification(context, e.localizedMessage ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun cancelBulkDownload(context: android.content.Context) {
        _bulkDownloadState.value = BulkDownloadState(isDownloading = false)
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(1001)
        } catch (e: Exception) {}
    }

    private fun updateDownloadNotification(context: android.content.Context, state: BulkDownloadState) {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "quran_downloads"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(channelId, "Quran Bulk Downloads", android.app.NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
            }
            val intProgress = state.percentage.toInt()
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.example.R.drawable.ic_quran_notification)
                .setContentTitle(if (state.type == "TEXT") "কুরআন টেক্সট ডাউনলোড হচ্ছে" else "কুরআন অডিও ডাউনলোড হচ্ছে")
                .setContentText("সূরা: ${state.currentSurahName} (${state.currentSurah}/114) • $intProgress%")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setProgress(100, intProgress, false)
                .setOngoing(true)

            notificationManager.notify(1001, builder.build())
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun completeDownloadNotification(context: android.content.Context, type: String) {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "quran_downloads"
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.example.R.drawable.ic_quran_notification)
                .setContentTitle("ডাউনলোড সম্পন্ন হয়েছে")
                .setContentText(if (type == "TEXT") "সকল সূরার টেক্সট অফলাইনে সংরক্ষিত হয়েছে।" else "সকল সূরার অডিও ও টেক্সট অফলাইনে সংরক্ষিত হয়েছে।")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setProgress(0, 0, false)
                .setOngoing(false)

            notificationManager.notify(1001, builder.build())
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun cancelDownloadNotification(context: android.content.Context, errorMsg: String) {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "quran_downloads"
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.example.R.drawable.ic_quran_notification)
                .setContentTitle("ডাউনলোড ব্যর্থ বা বাতিল হয়েছে")
                .setContentText("কারণ: $errorMsg")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setProgress(0, 0, false)
                .setOngoing(false)

            notificationManager.notify(1001, builder.build())
        } catch (e: Exception) { e.printStackTrace() }
    }

    data class DownloadedSurahItem(
        val number: Int,
        val englishName: String,
        val banglaName: String,
        val arabicName: String,
        val hasAudio: Boolean
    )

    val downloadedSurahsList: StateFlow<List<DownloadedSurahItem>> = combine(userSettings, surahListState) { settings, surahState ->
        val list = settings.downloadedSurahsJson
        val downloadedNums = list.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.distinct()

        if (surahState is UiState.Success) {
            val allSurahs = surahState.data
            downloadedNums.mapNotNull { num ->
                val meta = allSurahs.find { it.number == num }
                if (meta != null) {
                    val hasAudio = repository.hasSurahAudioCached(num, settings.selectedQari)
                    DownloadedSurahItem(
                        number = num,
                        englishName = meta.englishName,
                        banglaName = meta.englishNameTranslation,
                        arabicName = meta.name,
                        hasAudio = hasAudio
                    )
                } else null
            }
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownloadedSurah(surahNumber: Int) {
        viewModelScope.launch {
            val qari = userSettings.value.selectedQari
            val currentSetting = userSettings.value
            val newList = currentSetting.downloadedSurahsJson.replace(",$surahNumber,", "")
            
            repository.saveUserSettings(currentSetting.copy(downloadedSurahsJson = newList))
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.deleteCachedSurah(surahNumber, qari)
            }
        }
    }

    fun deleteAllDownloadedSurahs() {
        viewModelScope.launch {
            val qari = userSettings.value.selectedQari
            val list = userSettings.value.downloadedSurahsJson
            
            repository.saveUserSettings(userSettings.value.copy(downloadedSurahsJson = ""))
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                list.split(",").filter { it.isNotBlank() }.forEach { surahStr ->
                    surahStr.toIntOrNull()?.let { repository.deleteCachedSurah(it, qari) }
                }
            }
        }
    }
}

class QuranViewModelFactory(private val repository: QuranRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuranViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuranViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
