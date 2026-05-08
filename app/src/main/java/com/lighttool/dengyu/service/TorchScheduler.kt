package com.lighttool.dengyu.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class TorchScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleTurnOff(delayMinutes: Int) {
        schedule(delayMinutes, TorchAlarmReceiver.ACTION_TURN_OFF, REQUEST_OFF)
    }

    fun cancelTurnOff() {
        val pendingIntent = pendingIntent(TorchAlarmReceiver.ACTION_TURN_OFF, REQUEST_OFF)
        alarmManager.cancel(pendingIntent)
    }

    private fun schedule(delayMinutes: Int, action: String, requestCode: Int) {
        if (delayMinutes <= 0) return
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        val pendingIntent = pendingIntent(action, requestCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun pendingIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, TorchAlarmReceiver::class.java).apply {
                this.action = action
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_OFF = 102
    }
}
