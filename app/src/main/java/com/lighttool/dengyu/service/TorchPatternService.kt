package com.lighttool.dengyu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lighttool.dengyu.MainActivity
import com.lighttool.dengyu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TorchPatternService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var torchController: TorchController
    private var activeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        torchController = TorchController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopPattern(turnOffTorch = true)
            ACTION_STOP_KEEP_TORCH -> stopPattern(turnOffTorch = false)
            ACTION_FLASH -> {
                val interval = intent.getLongExtra(EXTRA_INTERVAL_MS, 500L).coerceAtLeast(80L)
                startFlashing(interval)
            }
            ACTION_PATTERN -> {
                val pulses = intent.getLongArrayExtra(EXTRA_PATTERN) ?: longArrayOf(200L, 700L)
                val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 1).coerceAtLeast(1)
                val loopGapMs = intent.getLongExtra(EXTRA_LOOP_GAP_MS, 1500L).coerceAtLeast(200L)
                startPattern(pulses.toList(), repeatCount, loopGapMs)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        activeJob?.cancel()
        activeJob = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startFlashing(intervalMs: Long) {
        startForeground(NOTIFICATION_ID, buildNotification())
        activeJob?.cancel()
        torchController.setTorch(false)
        activeJob = scope.launch {
            while (isActive) {
                torchController.setTorch(true)
                delay(intervalMs)
                torchController.setTorch(false)
                delay(intervalMs)
            }
        }
    }

    private fun startPattern(pulses: List<Long>, repeatCount: Int, loopGapMs: Long) {
        startForeground(NOTIFICATION_ID, buildNotification())
        activeJob?.cancel()
        torchController.setTorch(false)
        activeJob = scope.launch {
            repeat(repeatCount) { round ->
                pulses.forEachIndexed { index, duration ->
                    if (!isActive) return@launch
                    val isOnPhase = index % 2 == 0
                    torchController.setTorch(isOnPhase)
                    delay(duration.coerceAtLeast(50L))
                }
                if (!isActive) return@launch
                torchController.setTorch(false)
                if (round < repeatCount - 1) {
                    delay(loopGapMs)
                }
            }
            stopPattern(turnOffTorch = false)
        }
    }

    private fun stopPattern(turnOffTorch: Boolean) {
        activeJob?.cancel()
        activeJob = null
        if (turnOffTorch) {
            torchController.setTorch(false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "torch_pattern_channel"
        private const val NOTIFICATION_ID = 301

        const val ACTION_FLASH = "com.lighttool.dengyu.action.FLASH"
        const val ACTION_PATTERN = "com.lighttool.dengyu.action.PATTERN"
        const val ACTION_STOP = "com.lighttool.dengyu.action.STOP"
        const val ACTION_STOP_KEEP_TORCH = "com.lighttool.dengyu.action.STOP_KEEP_TORCH"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val EXTRA_PATTERN = "extra_pattern"
        const val EXTRA_REPEAT_COUNT = "extra_repeat_count"
        const val EXTRA_LOOP_GAP_MS = "extra_loop_gap_ms"

        fun flashIntent(context: Context, intervalMs: Long): Intent =
            Intent(context, TorchPatternService::class.java).apply {
                action = ACTION_FLASH
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }

        fun patternIntent(
            context: Context,
            pulses: LongArray,
            repeatCount: Int,
            loopGapMs: Long
        ): Intent =
            Intent(context, TorchPatternService::class.java).apply {
                action = ACTION_PATTERN
                putExtra(EXTRA_PATTERN, pulses)
                putExtra(EXTRA_REPEAT_COUNT, repeatCount)
                putExtra(EXTRA_LOOP_GAP_MS, loopGapMs)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, TorchPatternService::class.java).apply {
                action = ACTION_STOP
            }

        fun stopKeepTorchIntent(context: Context): Intent =
            Intent(context, TorchPatternService::class.java).apply {
                action = ACTION_STOP_KEEP_TORCH
            }
    }
}
