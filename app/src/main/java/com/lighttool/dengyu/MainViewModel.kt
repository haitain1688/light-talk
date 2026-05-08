package com.lighttool.dengyu

import android.app.Application
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lighttool.dengyu.data.AppPage
import com.lighttool.dengyu.data.LampUiState
import com.lighttool.dengyu.data.MessageMode
import com.lighttool.dengyu.reader.CenterLightSample
import com.lighttool.dengyu.reader.LightSignalDecoder
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
    private val readerTransitionHoldMs = 90L
    private var readerDecoder = createReaderDecoder(200f, 600f, 200f, 700f)

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
    private var lastReaderLightDetected = false
    private var lastReaderUiSampleMs = 0L
    private var pendingReaderLevel = false
    private var pendingReaderLevelSinceMs = 0L

    init {
        readerDecoder = createReaderDecoder(
            _uiState.value.shortOnMs,
            _uiState.value.longOnMs,
            _uiState.value.gapMs,
            _uiState.value.wordGapMs
        )
    }

    fun setCurrentPage(page: AppPage) {
        _uiState.update { it.copy(currentPage = page) }
    }

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
        resetReaderDecoder()
    }

    fun setLongOn(value: Float) {
        _uiState.update { it.copy(longOnMs = value) }
        resetReaderDecoder()
    }

    fun setGap(value: Float) {
        _uiState.update { it.copy(gapMs = value) }
        resetReaderDecoder()
    }

    fun setWordGap(value: Float) {
        _uiState.update { it.copy(wordGapMs = value) }
        resetReaderDecoder()
    }

    fun setReaderThreshold(value: Float) {
        _uiState.update {
            it.copy(readerState = it.readerState.copy(detectionThreshold = value))
        }
    }

    fun applySuggestedReaderThreshold() {
        _uiState.update {
            val suggested = it.readerState.suggestedThreshold.coerceIn(6f, 60f)
            it.copy(
                readerState = it.readerState.copy(detectionThreshold = suggested),
                statusMessage = "已按当前环境自动校准识别阈值"
            )
        }
    }

    fun startReadingLight() {
        resetReaderDecoder(clearUi = true)
        _uiState.update {
            it.copy(
                currentPage = AppPage.READER,
                readerState = it.readerState.copy(
                    isReading = true,
                    guideMessage = "读取中，请保持光源位于取景框中央"
                ),
                statusMessage = "读灯语模式已启动"
            )
        }
    }

    fun stopReadingLight() {
        _uiState.update {
            it.copy(
                readerState = it.readerState.copy(
                    isReading = false,
                    currentPulseMs = 0L,
                    guideMessage = "读取已停止，可重新开始或保留当前结果"
                ),
                statusMessage = "读灯语模式已停止"
            )
        }
    }

    fun clearReaderResult() {
        resetReaderDecoder(clearUi = true)
        _uiState.update {
            it.copy(
                readerState = it.readerState.copy(
                    guideMessage = "结果已清空，请把光源对准取景框后重新开始"
                ),
                statusMessage = "读灯语结果已清空"
            )
        }
    }

    fun onReaderSample(sample: CenterLightSample) {
        val state = _uiState.value
        val threshold = state.readerState.detectionThreshold
        val lightDetected = stabilizeReaderLevel(sample.signalStrength, threshold, sample.timestampMs)
        val shouldUpdateUi = sample.timestampMs - lastReaderUiSampleMs >= 80L ||
            lightDetected != lastReaderLightDetected

        var symbols = state.readerState.decodedSymbols
        var message = state.readerState.decodedMessage
        var pulseMs = if (lightDetected) state.readerState.currentPulseMs else 0L
        if (state.readerState.isReading) {
            val snapshot = readerDecoder.onSample(lightDetected, sample.timestampMs)
            symbols = snapshot.symbols
            message = snapshot.message
            pulseMs = snapshot.currentPulseMs
        }

        if (shouldUpdateUi || _uiState.value.readerState.isReading) {
            _uiState.update {
                val guide = when {
                    !_uiState.value.readerState.isReading -> "先观察信号强度和建议阈值，确认稳定后再开始读取"
                    lightDetected -> "已稳定捕获亮灯信号，继续保持对准"
                    symbols.isBlank() && sample.signalStrength < sample.suggestedThreshold -> "光源偏弱，可把光源靠近或点自动校准"
                    symbols.isBlank() -> "等待下一次亮灯"
                    else -> "已读取到灯语，继续保持稳定直到结束"
                }
                it.copy(
                    readerState = it.readerState.copy(
                        signalStrength = sample.signalStrength,
                        centerBrightness = sample.centerBrightness,
                        surroundBrightness = sample.surroundBrightness,
                        noiseFloor = sample.noiseFloor,
                        suggestedThreshold = sample.suggestedThreshold,
                        lightDetected = lightDetected,
                        decodedSymbols = symbols,
                        decodedMessage = message,
                        guideMessage = guide,
                        currentPulseMs = pulseMs
                    )
                )
            }
            lastReaderUiSampleMs = sample.timestampMs
        }
        lastReaderLightDetected = lightDetected
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

    private fun resetReaderDecoder(clearUi: Boolean = false) {
        readerDecoder = createReaderDecoder(
            _uiState.value.shortOnMs,
            _uiState.value.longOnMs,
            _uiState.value.gapMs,
            _uiState.value.wordGapMs
        )
        readerDecoder.reset()
        lastReaderLightDetected = false
        lastReaderUiSampleMs = 0L
        pendingReaderLevel = false
        pendingReaderLevelSinceMs = 0L
        if (clearUi) {
            _uiState.update {
                it.copy(
                    readerState = it.readerState.copy(
                        signalStrength = 0f,
                        centerBrightness = 0f,
                        surroundBrightness = 0f,
                        noiseFloor = 0f,
                        lightDetected = false,
                        decodedSymbols = "",
                        decodedMessage = "",
                        currentPulseMs = 0L
                    )
                )
            }
        }
    }

    private fun createReaderDecoder(
        shortOnMs: Float?,
        longOnMs: Float?,
        gapMs: Float?,
        wordGapMs: Float?
    ): LightSignalDecoder {
        val short = shortOnMs?.toLong() ?: 200L
        val long = longOnMs?.toLong() ?: 600L
        val gap = gapMs?.toLong() ?: 200L
        val wordGap = wordGapMs?.toLong() ?: 700L
        return LightSignalDecoder(
            dotDashBoundaryMs = ((short + long) / 2L).coerceAtLeast(150L),
            letterGapBoundaryMs = (gap * 2L).coerceAtLeast(250L),
            wordGapBoundaryMs = wordGap.coerceAtLeast(gap * 4L)
        )
    }

    private fun stabilizeReaderLevel(
        signalStrength: Float,
        threshold: Float,
        timestampMs: Long
    ): Boolean {
        val onThreshold = threshold
        val offThreshold = (threshold * 0.58f).coerceAtLeast(4f)
        val targetLevel = when {
            lastReaderLightDetected -> signalStrength > offThreshold
            else -> signalStrength >= onThreshold
        }

        if (targetLevel == lastReaderLightDetected) {
            pendingReaderLevel = targetLevel
            pendingReaderLevelSinceMs = timestampMs
            return lastReaderLightDetected
        }

        if (pendingReaderLevel != targetLevel) {
            pendingReaderLevel = targetLevel
            pendingReaderLevelSinceMs = timestampMs
            return lastReaderLightDetected
        }

        val heldMs = timestampMs - pendingReaderLevelSinceMs
        return if (heldMs >= readerTransitionHoldMs) {
            pendingReaderLevel = targetLevel
            pendingReaderLevelSinceMs = timestampMs
            targetLevel
        } else {
            lastReaderLightDetected
        }
    }
}
