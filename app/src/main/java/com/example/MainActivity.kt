package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.db.QuranDatabase
import com.example.data.repository.QuranRepository
import com.example.data.service.QuranPlayerService
import com.example.ui.screens.MainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.QuranViewModel
import com.example.viewmodel.QuranViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val database by lazy { QuranDatabase.getDatabase(this) }
    private val repository by lazy { QuranRepository(database.quranDao(), applicationContext) }

    private val viewModel: QuranViewModel by viewModels {
        QuranViewModelFactory(repository)
    }

    private var playerService: QuranPlayerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? QuranPlayerService.PlayerBinder
            playerService = localBinder?.getService()
            isBound = true
            viewModel.setPlayerService(playerService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
            isBound = false
            viewModel.setPlayerService(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Monitor Network State dynamically
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val activeNet = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNet)
            val isInitialOnline = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            viewModel.updateNetworkStatus(isInitialOnline)

            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    runOnUiThread { viewModel.updateNetworkStatus(true) }
                }

                override fun onLost(network: android.net.Network) {
                    runOnUiThread { viewModel.updateNetworkStatus(false) }
                }
            }
            try {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            } catch (e: Exception) {
                // Keep default online true if callback registration fails
                viewModel.updateNetworkStatus(true)
            }
        } else {
            viewModel.updateNetworkStatus(true)
        }

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Force widgets update instantly on application startup
        try {
            com.example.data.receiver.QuranHourlyVerseWidget.updateAllWidgets(applicationContext)
            com.example.data.receiver.QuranPlaybackWidget.updateAllWidgets(applicationContext, false, null, -1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start and Bind Playback Service
        val serviceIntent = Intent(this, QuranPlayerService::class.java)
        try {
            startService(serviceIntent)
        } catch (e: Exception) {
            // Under aggressive OS, standard start can fail if not allowed, but we proceed to bind
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        lifecycleScope.launch {
            viewModel.userSettings.collect { settings ->
                if (settings.dailyNotificationEnabled) {
                    com.example.data.receiver.DailyReminderScheduler.scheduleNextDailyReminder(
                        applicationContext,
                        settings.notificationHour,
                        settings.notificationMinute
                    )
                    // Schedule the 5 automatic daily notifications for everyone (8am, 10am, 12pm, 5pm, 9pm)
                    com.example.data.receiver.DailyReminderScheduler.scheduleAllDefaultAutos(applicationContext)
                }
            }
        }

        // Process any launch intent from widgets
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val surahNum = intent.getIntExtra("PLAY_SURAH_NUMBER", -1)
        val ayahIndex = intent.getIntExtra("PLAY_AYAH_INDEX", 0)
        if (surahNum != -1) {
            viewModel.playSpecificSurah(surahNum, ayahIndex)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}
