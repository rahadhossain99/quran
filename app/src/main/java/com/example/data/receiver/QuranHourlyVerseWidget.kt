package com.example.data.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.ui.screens.HourlyVersesList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

class QuranHourlyVerseWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.ACTION_COPY_VERSE") {
            try {
                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val hourlyVerse = HourlyVersesList[currentHour % HourlyVersesList.size]
                
                val copyText = "আয়াতের অর্থ ও আরবি পাঠ:\n\n${hourlyVerse.arabicText}\n\nঅনুবাদ: ${hourlyVerse.bengaliText}\n[${hourlyVerse.surahName} (${hourlyVerse.surahNumber}:${hourlyVerse.ayahNumber})]"
                
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Quran Verse", copyText)
                clipboard.setPrimaryClip(clip)
                
                android.widget.Toast.makeText(context, "আয়াত ও বাংলা অনুবাদ কপি হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private fun convertToBengaliDigits(number: Int): String {
            val englishDigits = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            val bengaliDigits = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            return number.toString().map { char ->
                val idx = englishDigits.indexOf(char)
                if (idx != -1) bengaliDigits[idx] else char
            }.joinToString("")
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_hourly_verse)

                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(java.util.Calendar.MINUTE)
                
                // Represent active timeslots beautifully - 100% accurate & interactive
                val currentBng = convertToBengaliDigits(currentHour)
                val nextBng = convertToBengaliDigits((currentHour + 1) % 24)
                
                val minutesStr = convertToBengaliDigits(currentMinute)
                val minBng = if (minutesStr.length < 2) "০$minutesStr" else minutesStr
                
                val slotText = "সময়কাল: $currentBng:০০ - $nextBng:০০  |  $currentBng:$minBng"

                val hourlyVerse = HourlyVersesList[currentHour % HourlyVersesList.size]

                // Set texts on views
                views.setTextViewText(R.id.widget_hourly_header, "${hourlyVerse.surahName} (${hourlyVerse.surahNumber}:${hourlyVerse.ayahNumber})")
                views.setTextViewText(R.id.widget_hourly_time, slotText)
                views.setTextViewText(R.id.widget_hourly_arabic, hourlyVerse.arabicText)
                views.setTextViewText(R.id.widget_hourly_bangla, hourlyVerse.bengaliText)
                views.setTextViewText(R.id.widget_hourly_citation, "সূরা: ${hourlyVerse.surahName}")

                // Setup deep link to play the specific Surah and Ayah!
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("PLAY_SURAH_NUMBER", hourlyVerse.surahNumber)
                    putExtra("PLAY_AYAH_INDEX", hourlyVerse.ayahIndex)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val openPendingIntent = PendingIntent.getActivity(
                    context, 200 + appWidgetId, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_hourly_root, openPendingIntent)

                // Copy button pending intent
                val copyIntent = Intent(context, QuranHourlyVerseWidget::class.java).apply {
                    action = "com.example.ACTION_COPY_VERSE"
                }
                val copyPendingIntent = PendingIntent.getBroadcast(
                    context, 400 + appWidgetId, copyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_copy, copyPendingIntent)

                // Dynamic theme assignment
                @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val db = com.example.data.db.QuranDatabase.getDatabase(context)
                        val settings = db.quranDao().getUserSettingsDirect()
                        val widgetStyle = settings?.widgetStyle ?: "classic_green"
                        withContext(Dispatchers.Main) {
                            try {
                                applyThemeToWidget(views, widgetStyle)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            } catch (e: Exception) {
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        }
                    } catch (e: Exception) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }

        private fun applyThemeToWidget(views: RemoteViews, widgetStyle: String) {
            val bgRes = when (widgetStyle) {
                "classic_green" -> R.drawable.widget_bg_green
                "minimal_dark" -> R.drawable.widget_bg_minimal_dark
                "calm_azure" -> R.drawable.widget_bg_calm_azure
                "royal_gold" -> R.drawable.widget_bg_royal_gold
                "dark_slate" -> R.drawable.widget_bg_dark_slate
                else -> R.drawable.widget_bg_green
            }

            val primaryColor = when (widgetStyle) {
                "classic_green" -> 0xFFE0BA51.toInt()
                "minimal_dark" -> 0xFFCFD8DC.toInt()
                "calm_azure" -> 0xFFFFFFFF.toInt()
                "royal_gold" -> 0xFFFFF9C4.toInt()
                "dark_slate" -> 0xFFE0F2F1.toInt()
                else -> 0xFFFFF9C4.toInt()
            }

            val secondaryColor = when (widgetStyle) {
                "classic_green" -> 0xFFC2F0C2.toInt()
                "minimal_dark" -> 0xFF90A4AE.toInt()
                "calm_azure" -> 0xFF80DEEA.toInt()
                "royal_gold" -> 0xFFFFF59D.toInt()
                "dark_slate" -> 0xFFB2DFDB.toInt()
                else -> 0xFFC2F0C2.toInt()
            }

            val textColorArabic = 0xFFFFFFFF.toInt()
            val textColorBangla = when (widgetStyle) {
                "classic_green" -> 0xFFECEFF1.toInt()
                "minimal_dark" -> 0xFFE0E0E0.toInt()
                "calm_azure" -> 0xFFB2EBF2.toInt()
                "royal_gold" -> 0xFFFFF59D.toInt()
                "dark_slate" -> 0xFFC2F0C2.toInt()
                else -> 0xFFFFFFFF.toInt()
            }

            views.setInt(R.id.widget_hourly_root, "setBackgroundResource", bgRes)
            views.setTextColor(R.id.widget_hourly_header, primaryColor)
            views.setTextColor(R.id.widget_hourly_time, secondaryColor)
            views.setTextColor(R.id.widget_hourly_arabic, textColorArabic)
            views.setTextColor(R.id.widget_hourly_bangla, textColorBangla)
            views.setTextColor(R.id.widget_hourly_citation, secondaryColor)
            views.setInt(R.id.btn_widget_copy, "setColorFilter", primaryColor)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = android.content.ComponentName(context, QuranHourlyVerseWidget::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (allWidgetIds.isEmpty()) return
                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
