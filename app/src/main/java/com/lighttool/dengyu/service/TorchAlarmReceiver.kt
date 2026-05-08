package com.lighttool.dengyu.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class TorchAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TURN_OFF -> {
                ContextCompat.startForegroundService(
                    context,
                    TorchPatternService.stopIntent(context)
                )
                TorchController(context).setTorch(false)
            }
        }
    }

    companion object {
        const val ACTION_TURN_OFF = "com.lighttool.dengyu.action.TURN_OFF"
    }
}
