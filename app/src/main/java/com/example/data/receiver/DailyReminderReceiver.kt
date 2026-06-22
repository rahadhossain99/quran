package com.example.data.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = try { goAsync() } catch (e: Exception) { null }
        val reminderType = intent.getStringExtra("reminder_type") ?: "custom"
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = QuranDatabase.getDatabase(context)
                val settings = db.quranDao().getUserSettings().firstOrNull()
                
                // Only send if notifications are enabled
                if (settings == null || settings.dailyNotificationEnabled) {
                    val prefs = context.getSharedPreferences("app_notifications", Context.MODE_PRIVATE)
                    val autoNotificationMaster = prefs.getBoolean("auto_notification", true)
                    
                    if (reminderType.startsWith("auto_") && !autoNotificationMaster) {
                        return@launch
                    }
                    
                    when (reminderType) {
                        "custom" -> {
                            if (prefs.getBoolean("custom_reminder", true)) showReminderNotification(context)
                        }
                        "auto_morning" -> {
                            if (prefs.getBoolean("auto_morning", true)) showAutoNotification(
                                context,
                                7701,
                                "একটি নতুন রহমতের সুপ্রভাত! 🌅",
                                "আস-সালামু আলাইকুম। কুরআনের পবিত্র আলো ও বরকত দিয়ে আপনার আজকের দিনটি সুন্দরভাবে শুরু হোক।"
                            )
                        }
                        "auto_10am" -> {
                            if (prefs.getBoolean("auto_10am", true)) showAutoNotification(
                                context,
                                7702,
                                "কাজের ফাঁকে দ্বীনি রিফ্রেশমেন্ট! ✨",
                                "সকাল ১০:০০ টা বেজে গিয়েছে। শত ব্যস্ততার মাঝেও চলুন আল্লাহর পবিত্র বাণী পড়া বা শোনার মাধ্যমে অন্তর শান্ত করি।"
                            )
                        }
                        "auto_12pm" -> {
                            if (prefs.getBoolean("auto_12pm", true)) showAutoNotification(
                                context,
                                7703,
                                "দুপুরের প্রশান্তি ও আল-কুরআন 📖",
                                "দিনের এই মধ্যভাগে কুরআনের তিলওয়াত ও অনুবাদ শুনলে মন ফ্রেশ ও হালকা হয়। চলুন তিলওয়াত শুনি।"
                            )
                        }
                        "auto_4pm" -> {
                            if (!prefs.getBoolean("auto_4pm", true)) return@launch
                            val ayahs = listOf(
                                Pair("বলুন, 'হে আমার বান্দাগণ যারা নিজেদের ওপর জুলুম করেছ, আল্লাহর রহমত থেকে নিরাশ হয়ো না।' ✨\n(সূরা আজ-জুমার, আয়াত: ৫৩)", "আজকের অনুপ্রেরণামূলক আয়াত 📖"),
                                Pair("নিশ্চয়ই কষ্টের সাথেই স্বস্তি রয়েছে। 🤲\n(সূরা আশ-শরহ, আয়াত: ৬)", "আজকের অনুপ্রেরণামূলক আয়াত 📖"),
                                Pair("তোমরা আমাকে স্মরণ করো, আমিও তোমাদের স্মরণ করব। 🕋\n(সূরা আল-বাকারাহ, আয়াত: ১৫২)", "আজকের অনুপ্রেরণামূলক আয়াত 📖"),
                                Pair("আর যখন আমার বান্দারা আমার সম্পর্কে আপনাকে জিজ্ঞাসা করে, আমি তো নিকটেই আছি। ❤️\n(সূরা আল-বাকারাহ, আয়াত: ১৮৬)", "আজকের অনুপ্রেরণামূলক আয়াত 📖"),
                                Pair("হে মুমিনগণ! তোমরা ধৈর্য ও সালাতের মাধ্যমে সাহায্য প্রার্থনা করো। নিশ্চই আল্লাহ ধৈর্যশীলদের সাথে আছেন। 🛡️\n(সূরা আল-বাকারাহ, আয়াত: ১৫৩)", "আজকের অনুপ্রেরণামূলক আয়াত 📖"),
                                Pair("আল্লাহ কাউকে তার সাধ্যের অতিরিক্ত দায়িত্ব অর্পণ করেন না। 🌱\n(সূরা আল-বাকারাহ, আয়াত: ২৮৬)", "আজকের অনুপ্রেরণামূলক আয়াত 📖"),
                                Pair("আল্লাহর রহমত সৎকর্মশীলদের নিকটবর্তী। 🌸\n(সূরা আল-আ'রাф, আয়াত: ৫৬)", "আজকের অনুপ্রেরণামূলক আয়াত 📖")
                            )
                            val calendar = java.util.Calendar.getInstance()
                            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
                            val idx = dayOfYear % ayahs.size
                            val selected = ayahs[idx]
                            showAutoNotification(
                                context,
                                7706,
                                selected.second,
                                selected.first
                            )
                        }
                        "auto_5pm" -> {
                            if (prefs.getBoolean("auto_5pm", true)) showAutoNotification(
                                context,
                                7704,
                                "তিলওয়াত ও আপনার আজকের লক্ষ্য 📈",
                                "বিকাল ৫:০০ টা! চলুন আজকের তিলওয়াতের অগ্রগতির গ্রাফগুলো দেখে লক্ষ্য পূরণ নিশ্চিত করি।"
                            )
                        }
                        "auto_9pm" -> {
                            if (prefs.getBoolean("auto_9pm", true)) showAutoNotification(
                                context,
                                7705,
                                "রাতের রিল্যাক্সেশন ও সুন্নাহ আমল 🌙",
                                "শোয়ার পূর্বে সূরা আল-মুলক পাঠ করার সুন্নাহ অভ্যাস গড়ে তুলুন, যা কবরের আজাব থেকে সুরক্ষা দান করে।"
                            )
                        }
                        "auto_10pm" -> {
                            if (!prefs.getBoolean("auto_10pm", true)) return@launch
                            val surahs = listOf(
                                Pair("সূরা আল-মুলক (Mulk)", "রাতের সুন্নাহ ও কবরের আজাব থেকে রক্ষার অন্যতম আমল। শোয়ার পূর্বে এটি শুনতে পারেন। 🌌"),
                                Pair("সূরা আর-রহমান (Ar-Rahman)", "আল্লাহ তায়ালার অফুরন্ত নেয়ামত ও করুণাকে স্মরণ করে পরম প্রশান্তিতে ঘুমাতে যান। 🕊️"),
                                Pair("সূরা আল-ওয়াকিয়াহ (Waqiah)", "রাসুলুল্লাহ (সা.) বলেছেন, যে ব্যক্তি প্রতি রাতে সূরা ওয়াকিয়াহ পড়বে, সে কখনও দারিদ্র্যে পড়বে না। 💰✨"),
                                Pair("সূরা আস-সাজদাহ (Sajdah)", "রাত্রে ঘুমানোর পূর্বে সূরা আল-মুলকের পাশাপাশি সূরা আস-সাজদাহ তেলাওয়াত করা অত্যন্ত ফজিলতপূর্ণ সুন্নাত। 📖🌙")
                            )
                            val calendar = java.util.Calendar.getInstance()
                            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
                            val idx = dayOfYear % surahs.size
                            val selected = surahs[idx]
                            showAutoNotification(
                                context,
                                7707,
                                "রাতের বিশেষ সূরা: ${selected.first} ✨",
                                selected.second
                            )
                        }
                    }
                }
                
                // Reschedule for next cycles
                when (reminderType) {
                    "custom" -> {
                        DailyReminderScheduler.scheduleNextDailyReminder(
                            context, 
                            settings?.notificationHour ?: 9, 
                            settings?.notificationMinute ?: 0
                        )
                    }
                    else -> {
                        DailyReminderScheduler.scheduleAllDefaultAutos(context)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    pendingResult?.finish()
                } catch (e: Exception) {
                    // Suppress
                }
            }
        }
    }

    private fun getBengaliDateAndDay(): String {
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeekNum = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dayOfMonthNum = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val monthNum = calendar.get(java.util.Calendar.MONTH) // 0-indexed

        val bngDigits = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯")
        fun toBngNum(num: Int): String {
            return num.toString().map { bngDigits[it.toString().toInt()] }.joinToString("")
        }

        val monthNames = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val dayNames = mapOf(
            java.util.Calendar.SATURDAY to "শনিবার",
            java.util.Calendar.SUNDAY to "রবিবার",
            java.util.Calendar.MONDAY to "সোমবার",
            java.util.Calendar.TUESDAY to "মঙ্গলবার",
            java.util.Calendar.WEDNESDAY to "বুধবার",
            java.util.Calendar.THURSDAY to "বৃহস্পতি",
            java.util.Calendar.FRIDAY to "শুক্রবার"
        )

        val bngDay = toBngNum(dayOfMonthNum)
        val bngMonth = monthNames.getOrElse(monthNum) { "" }
        val bngDayName = dayNames[dayOfWeekNum] ?: ""

        return "$bngDay $bngMonth, $bngDayName"
    }

    private fun showAutoNotification(context: Context, id: Int, title: String, message: String) {
        val channelId = "quran_daily_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "দৈনিক কুরআন রিমাইন্ডার"
            val desc = "প্রতিদিন পবিত্র কুরআন চর্চার চমৎকার রিমাইন্ডার"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.R.drawable.ic_quran_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(getBengaliDateAndDay())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF0F9F59.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(id, notification)
    }

    private fun showReminderNotification(context: Context) {
        val channelId = "quran_daily_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "দৈনিক কুরআন রিমাইন্ডার"
            val desc = "প্রতিদিন পবিত্র কুরআন চর্চার চমৎকার রিমাইন্ডার"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            202,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messages = listOf(
            "আস-সালামু আলাইকুম। আজ কি আল-কুরআন পাঠ করেছেন? চলুন আজকের সুন্দর দিনটি পবিত্র কুরআনের আলোয় শুরু করি।",
            "কুরআন পড়ুন, তা হৃদয়কে প্রশান্ত করে এবং দ্বীনের আলো দিয়ে জীবনকে সুন্দর করে সাজিয়ে তোলে। ✨",
            "আজকের হাজারো ব্যস্ততার মাঝেও কি কিছু মূহুর্ত আল্লাহর বাণী পড়ার জন্য বরাদ্দ করা যায় না? চলুন তিলওয়াত করি!",
            "পবিত্র কুরআন তিলওয়াত ও শ্রবণ করা আমাদের ঈমানকে বৃদ্ধি করে। চলুন আজ অন্তত ৫ মিনিট হলেও কুরআন পড়ি বা শুনি।"
        )
        val selectedMessage = messages.random()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.R.drawable.ic_quran_notification)
            .setContentTitle("কুরআন তিলওয়াতের স্মরণিকা ✨")
            .setContentText(selectedMessage)
            .setSubText(getBengaliDateAndDay())
            .setStyle(NotificationCompat.BigTextStyle().bigText(selectedMessage))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF0F9F59.toInt()) // Standard matching emerald primary color
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(7722, notification)
    }
}
