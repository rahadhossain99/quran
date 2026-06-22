package com.example.data.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object DailyReminderScheduler {
    fun scheduleNextDailyReminder(context: Context, hour: Int, minute: Int) {
        scheduleAlarm(context, hour, minute, 1202, "custom")
    }

    fun scheduleAllDefaultAutos(context: Context) {
        // 1. Morning (8:00 AM)
        scheduleAlarm(context, 8, 0, 3001, "auto_morning")
        // 2. Mid-morning (10:00 AM)
        scheduleAlarm(context, 10, 0, 3002, "auto_10am")
        // 3. Noon (12:00 PM)
        scheduleAlarm(context, 12, 0, 3003, "auto_12pm")
        // 4. Afternoon (4:00 PM)
        scheduleAlarm(context, 16, 0, 3006, "auto_4pm")
        // 5. Late Afternoon (5:00 PM)
        scheduleAlarm(context, 17, 0, 3004, "auto_5pm")
        // 6. Night (9:00 PM)
        scheduleAlarm(context, 21, 0, 3005, "auto_9pm")
        // 7. Late Night (10:00 PM)
        scheduleAlarm(context, 22, 0, 3007, "auto_10pm")
    }

    private fun scheduleAlarm(context: Context, hour: Int, minute: Int, requestCode: Int, type: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra("reminder_type", type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time is in the past, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis() + 1000) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Allows running even in Sleep/Doze mode for premium accuracy
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Safe fallback if exact alarm permission is restricted on new OS levels
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1202,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
