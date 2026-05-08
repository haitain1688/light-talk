package com.lighttool.dengyu

import android.app.Application
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lighttool.dengyu.data.LampUiState
import com.lighttool.dengyu.data.MessageMode
import com.lighttool.dengyu.service.PatternCodec
import com.lighttool.dengyu.service.TorchController
import com.lighttool.dengyu.service.TorchPatternService
import com.lighttool.dengyu.service.TorchScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val torchController = TorchController(appContext)
    private val scheduler = TorchScheduler(appContext)
    private var autoOffCountdownJob: Job? = null
    private var patternStatusJob: Job? = null
    private val patternLoopGapMs = 1500L

    private val _uiState = MutableStateFlow(
        LampUiState(
            morsePreview = PatternCodec.toMorsePreview("SOS"),
            statusMessage = if (torchController.isFlashAvailable()) {
                "已检测到闪光灯"
            } else {
                "未检测到可用闪光灯"
            }
        )
    )
    val uiState: StateFlow<LampUiState> = _uiState.asStateFlow()

    fun toggleTorch() {
        if (!torchController.isFlashAvailable()) {
            setStatus("当前设备不支持闪光灯")
            return
        }
        val target = !_uiState.value.isTorchOn
        patternStatusJob?.cancel()
        if (target) {
            stopPatternServiceKeepTorch()
        } else {
            stopPatternService()
        }
        torchController.setTorch(target)
            .onSuccess {
                if (!target) {
                    cancelAutoOffCountdown()
                    scheduler.cancelTurnOff()
                }
                _uiState.update {
                    it.copy(
                        isTorchOn = target,
                        flashingEnabled = false,
                        isPatternSending = false,
                        autoOffRemainingSeconds = if (target) it.autoOffRemainingSeconds else null,
                        statusMessage = if (target) "常亮已开启" else "常亮已关闭"
                    )
                }
            }
            .onFailure { setStatus(it.message ?: "闪光灯控制失败") }
    }

    fun setInterval(value: Float) {
        _uiState.update { it.copy(intervalMs = value) }
    }

    fun setMessageMode(mode: MessageMode) {
        _uiState.update { it.copy(messageMode = mode) }
    }

    fun setMorseInput(value: String) {
        _uiState.update {
            it.copy(
                morseInput = value,
                morsePreview = PatternCodec.toMorsePreview(value)
            )
        }
    }

    fun setCustomPatternText(value: String) {
        _uiState.update { it.copy(customPatternText = value) }
    }

    fun setShortOn(value: Float) {
        _uiState.update { it.copy(shortOnMs = value) }
    }

    fun setLongOn(value: Float) {
        _uiState.update { it.copy(longOnMs = value) }
    }

    fun setGap(value: Float) {
        _uiState.update { it.copy(gapMs = value) }
    }

    fun setWordGap(value: Float) {
        _uiState.update { it.copy(wordGapMs = value) }
    }

    fun setRepeatCount(value: String) {
        _uiState.update {
            it.copy(repeatCount = (value.toIntOrNull() ?: 1).coerceIn(1, 20))
        }
    }

    fun setAutoOffHours(value: String) {
        _uiState.update {
            it.copy(
                scheduleState = it.scheduleState.copy(
                    autoOffHours = (value.toIntOrNull() ?: 0).coerceIn(0, 23)
                )
            )
        }
    }

    fun setAutoOffMinutes(value: String) {
        _uiState.update {
            it.copy(
                scheduleState = it.scheduleState.copy(
                    autoOffMinutes = (value.toIntOrNull() ?: 0).coerceIn(0, 59)
                )
            )
        }
    }

    fun startFlashing() {
        if (!torchController.isFlashAvailable()) {
            setStatus("当前设备不支持闪光灯")
            return
        }
        cancelAutoOffCountdown()
        scheduler.cancelTurnOff()
        patternStatusJob?.cancel()
        val interval = _uiState.value.intervalMs.toLong()
        startForegroundService(TorchPatternService.flashIntent(appContext, interval))
        _uiState.update {
            it.copy(
                isTorchOn = false,
                flashingEnabled = true,
                isPatternSending = false,
                autoOffRemainingSeconds = null,
                statusMessage = "闪灯模式运行中"
            )
        }
    }

    fun stopFlashing() {
        patternStatusJob?.cancel()
        stopPatternService()
        torchController.setTorch(false)
        cancelAutoOffCountdown()
        _uiState.update {
            it.copy(
                isTorchOn = false,
                flashingEnabled = false,
                isPatternSending = false,
                autoOffRemainingSeconds = null,
                statusMessage = "闪灯模式已停止"
            )
        }
    }

    fun sendPattern() {
        if (!torchController.isFlashAvailable()) {
            setStatus("当前设备不支持闪光灯")
            return
        }
        val state = _uiState.value
        patternStatusJob?.cancel()
        val short = state.shortOnMs.toLong()
        val long = state.longOnMs.toLong()
        val gap = state.gapMs.toLong()
        val wordGap = state.wordGapMs.toLong()
        scheduler.cancelTurnOff()
        cancelAutoOffCountdown()
        val pattern = when (state.messageMode) {
            MessageMode.MORSE -> PatternCodec.encodeMorseMessage(
                raw = state.morseInput,
                shortOnMs = short,
                longOnMs = long,
                gapMs = gap,
                letterGapMs = gap * 3,
                wordGapMs = wordGap
            )
            MessageMode.CUSTOM -> PatternCodec.encode(
                raw = state.customPatternText,
                shortOnMs = short,
                longOnMs = long,
                gapMs = gap,
                wordGapMs = wordGap
            )
        }
        startForegroundService(
            TorchPatternService.patternIntent(
                appContext,
                pattern.pulses.toLongArray(),
                state.repeatCount,
                patternLoopGapMs
            )
        )
        val preview = if (state.messageMode == MessageMode.MORSE) {
            state.morseInput.ifBlank { "SOS" }
        } else {
            state.customPatternText.ifBlank { "默认节奏" }
        }
        _uiState.update {
            it.copy(
                isTorchOn = false,
                flashingEnabled = false,
                isPatternSending = true,
                autoOffRemainingSeconds = null,
                statusMessage = "灯语发送中：$preview"
            )
        }
        schedulePatternCompletion(
            pattern.pulses.sum() * state.repeatCount + (state.repeatCount - 1) * patternLoopGapMs
        )
    }

    fun startTorchWithAutoOff() {
        if (!torchController.isFlashAvailable()) {
            setStatus("当前设备不支持闪光灯")
            return
        }
        val state = _uiState.value.scheduleState
        val delayMinutes = state.autoOffHours * 60 + state.autoOffMinutes
        stopPatternServiceKeepTorch()
        patternStatusJob?.cancel()
        torchController.setTorch(true)
            .onSuccess {
                scheduler.cancelTurnOff()
                if (delayMinutes > 0) {
                    scheduler.scheduleTurnOff(delayMinutes)
                    startAutoOffCountdown(delayMinutes * 60L)
                } else {
                    cancelAutoOffCountdown()
                }
                _uiState.update {
                    it.copy(
                        isTorchOn = true,
                        flashingEnabled = false,
                        isPatternSending = false,
                        autoOffRemainingSeconds = if (delayMinutes > 0) delayMinutes * 60L else null,
                        statusMessage = if (delayMinutes > 0) {
                            "已开灯，将在 ${state.autoOffHours} 小时 ${state.autoOffMinutes} 分钟后自动关闭"
                        } else {
                            "已开灯，当前未设置自动关灯"
                        }
                    )
                }
            }
            .onFailure { setStatus(it.message ?: "自动关灯设置失败") }
    }

    fun stopTorchNow() {
        scheduler.cancelTurnOff()
        cancelAutoOffCountdown()
        patternStatusJob?.cancel()
        stopPatternService()
        torchController.setTorch(false).onSuccess {
            _uiState.update {
                it.copy(
                    isTorchOn = false,
                    flashingEnabled = false,
                    isPatternSending = false,
                    autoOffRemainingSeconds = null,
                    statusMessage = "手电筒已关闭"
                )
            }
        }
    }

    fun stopPatternSending() {
        patternStatusJob?.cancel()
        stopPatternService()
        _uiState.update {
            it.copy(
                isPatternSending = false,
                flashingEnabled = false,
                statusMessage = "灯语发送已停止"
            )
        }
    }

    private fun stopPatternService() {
        appContext.startService(TorchPatternService.stopIntent(appContext))
    }

    private fun stopPatternServiceKeepTorch() {
        appContext.startService(TorchPatternService.stopKeepTorchIntent(appContext))
    }

    private fun startForegroundService(intent: android.content.Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(appContext, intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun startAutoOffCountdown(totalSeconds: Long) {
        autoOffCountdownJob?.cancel()
        autoOffCountdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining >= 0 && isActive) {
                _uiState.update { it.copy(autoOffRemainingSeconds = remaining) }
                if (remaining == 0L) {
                    _uiState.update {
                        it.copy(
                            isTorchOn = false,
                            autoOffRemainingSeconds = null,
                            statusMessage = "倒计时结束，手电筒已自动关闭"
                        )
                    }
                    break
                }
                delay(1000)
                remaining--
            }
        }
    }

    private fun cancelAutoOffCountdown() {
        autoOffCountdownJob?.cancel()
        autoOffCountdownJob = null
    }

    private fun schedulePatternCompletion(totalDurationMs: Long) {
        patternStatusJob?.cancel()
        patternStatusJob = viewModelScope.launch {
            delay(totalDurationMs + 300L)
            _uiState.update {
                it.copy(
                    isPatternSending = false,
                    statusMessage = "灯语发送完成"
                )
            }
        }
    }

    private fun setStatus(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = message) }
        }
    }
}
