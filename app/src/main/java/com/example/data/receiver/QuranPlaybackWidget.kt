package com.example.data.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.service.QuranPlayerService
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

class QuranPlaybackWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_PLAY_PAUSE_WIDGET || action == ACTION_PREV_WIDGET || action == ACTION_NEXT_WIDGET) {
            val serviceIntent = Intent(context, QuranPlayerService::class.java).apply {
                this.action = when (action) {
                    ACTION_PLAY_PAUSE_WIDGET -> QuranPlayerService.ACTION_PLAY_PAUSE
                    ACTION_PREV_WIDGET -> QuranPlayerService.ACTION_PREV
                    else -> QuranPlayerService.ACTION_NEXT
                }
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                // Fallback attempt to standard startService
                try {
                    context.startService(serviceIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val ACTION_PLAY_PAUSE_WIDGET = "com.example.alquran.widget.PLAY_PAUSE"
        private const val ACTION_PREV_WIDGET = "com.example.alquran.widget.PREV"
        private const val ACTION_NEXT_WIDGET = "com.example.alquran.widget.NEXT"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_playback_control)

            // Setup Deep link to open the app
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_badge_container, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_badge_image, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_subtitle, openPendingIntent)

            // Setup button intents
            val playIntent = Intent(context, QuranPlaybackWidget::class.java).apply { action = ACTION_PLAY_PAUSE_WIDGET }
            val prevIntent = Intent(context, QuranPlaybackWidget::class.java).apply { action = ACTION_PREV_WIDGET }
            val nextIntent = Intent(context, QuranPlaybackWidget::class.java).apply { action = ACTION_NEXT_WIDGET }

            views.setOnClickPendingIntent(
                R.id.btn_widget_play,
                PendingIntent.getBroadcast(context, 10, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_prev,
                PendingIntent.getBroadcast(context, 20, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_next,
                PendingIntent.getBroadcast(context, 30, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Dynamic theme assignment
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val db = com.example.data.db.QuranDatabase.getDatabase(context)
                    val settings = db.quranDao().getUserSettingsDirect()
                    val widgetStyle = settings?.widgetStyle ?: "classic_green"
                    withContext(Dispatchers.Main) {
                        try {
                            applyThemeToWidget(context, views, widgetStyle)
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        } catch (e: Exception) {
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                } catch (e: Exception) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        fun updateAllWidgets(context: Context, isPlaying: Boolean, surahName: String?, ayahIndex: Int) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, QuranPlaybackWidget::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (allWidgetIds.isEmpty()) return

                for (widgetId in allWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_playback_control)
                    if (surahName != null) {
                        views.setTextViewText(R.id.widget_title, surahName)
                        val statusText = if (isPlaying) "তেলাওয়াত চলছে..." else "পড়া বন্ধ আছে"
                        if (ayahIndex >= 0) {
                            views.setTextViewText(R.id.widget_subtitle, "আয়াত ${ayahIndex + 1} • $statusText")
                        } else {
                            views.setTextViewText(R.id.widget_subtitle, statusText)
                        }
                    } else {
                        views.setTextViewText(R.id.widget_title, "পবিত্র কুরআনুল কারিম")
                        views.setTextViewText(R.id.widget_subtitle, "আজকের আয়াত তেলাওয়াত করুন")
                    }

                    views.setImageViewResource(
                        R.id.btn_widget_play,
                        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                    )

                    // Maintain bindings
                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val openPendingIntent = PendingIntent.getActivity(
                        context, 0, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_badge_container, openPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_badge_image, openPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_title, openPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_subtitle, openPendingIntent)

                    val playIntent = Intent(context, QuranPlaybackWidget::class.java).apply { action = ACTION_PLAY_PAUSE_WIDGET }
                    val prevIntent = Intent(context, QuranPlaybackWidget::class.java).apply { action = ACTION_PREV_WIDGET }
                    val nextIntent = Intent(context, QuranPlaybackWidget::class.java).apply { action = ACTION_NEXT_WIDGET }

                    views.setOnClickPendingIntent(
                        R.id.btn_widget_play,
                        PendingIntent.getBroadcast(context, 10, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_prev,
                        PendingIntent.getBroadcast(context, 20, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )
                    views.setOnClickPendingIntent(
                        R.id.btn_widget_next,
                        PendingIntent.getBroadcast(context, 30, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )

                    // Dynamic theme assignment
                    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val db = com.example.data.db.QuranDatabase.getDatabase(context)
                            val settings = db.quranDao().getUserSettingsDirect()
                            val widgetStyle = settings?.widgetStyle ?: "classic_green"
                            withContext(Dispatchers.Main) {
                                try {
                                    applyThemeToWidget(context, views, widgetStyle)
                                    appWidgetManager.updateAppWidget(widgetId, views)
                                } catch (e: Exception) {
                                    appWidgetManager.updateAppWidget(widgetId, views)
                                }
                            }
                        } catch (e: Exception) {
                            appWidgetManager.updateAppWidget(widgetId, views)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore any widget updating errors
            }
        }

        private fun applyThemeToWidget(context: Context, views: RemoteViews, widgetStyle: String) {
            val bgRes = when (widgetStyle) {
                "classic_green" -> R.drawable.widget_bg_green
                "minimal_dark" -> R.drawable.widget_bg_minimal_dark
                "calm_azure" -> R.drawable.widget_bg_calm_azure
                "royal_gold" -> R.drawable.widget_bg_royal_gold
                "dark_slate" -> R.drawable.widget_bg_dark_slate
                else -> R.drawable.widget_bg_green
            }
            
            val primaryColor = when (widgetStyle) {
                "classic_green" -> 0xFFFFFFFF.toInt()
                "minimal_dark" -> 0xFFECEFF1.toInt()
                "calm_azure" -> 0xFFFFFFFF.toInt()
                "royal_gold" -> 0xFFFFFFFF.toInt()
                "dark_slate" -> 0xFFFFFFFF.toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            
            val secondaryColor = when (widgetStyle) {
                "classic_green" -> 0xFFC2F0C2.toInt()
                "minimal_dark" -> 0xFFB0BEC5.toInt()
                "calm_azure" -> 0xFFB2EBF2.toInt()
                "royal_gold" -> 0xFFFFF59D.toInt()
                "dark_slate" -> 0xFFB2DFDB.toInt()
                else -> 0xFFC2F0C2.toInt()
            }
            
            views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
            views.setTextColor(R.id.widget_title, primaryColor)
            views.setTextColor(R.id.widget_subtitle, secondaryColor)
            
            // Icon button tints
            val iconTint = when (widgetStyle) {
                "classic_green" -> 0xFFFFF9C4.toInt()
                "minimal_dark" -> 0xFFCFD8DC.toInt()
                "calm_azure" -> 0xFFE0F7FA.toInt()
                "royal_gold" -> 0xFFFFD54F.toInt()
                "dark_slate" -> 0xFFCFD8DC.toInt()
                else -> 0xFFFFF9C4.toInt()
            }
            views.setInt(R.id.btn_widget_prev, "setColorFilter", iconTint)
            views.setInt(R.id.btn_widget_next, "setColorFilter", iconTint)
            
            // Play Button Internal Icon tint inside the raw white background circular button
            val playIconColor = when (widgetStyle) {
                "classic_green" -> 0xFF0B562F.toInt()
                "minimal_dark" -> 0xFF121212.toInt()
                "calm_azure" -> 0xFF024D7A.toInt()
                "royal_gold" -> 0xFF4D3B0B.toInt()
                "dark_slate" -> 0xFF112424.toInt()
                else -> 0xFF0B562F.toInt()
            }
            views.setInt(R.id.btn_widget_play, "setColorFilter", playIconColor)
        }
    }
}
