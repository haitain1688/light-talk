package com.lighttool.dengyu.data

data class LightPattern(
    val pulses: List<Long>,
    val repeat: Boolean
)

data class ScheduleState(
    val autoOffHours: Int = 0,
    val autoOffMinutes: Int = 25
)

enum class MessageMode {
    MORSE,
    CUSTOM
}

enum class AppPage {
    CONTROL,
    READER
}

data class ReaderHistoryItem(
    val id: Long,
    val timeLabel: String,
    val symbols: String,
    val message: String
)

data class ReaderUiState(
    val isReading: Boolean = false,
    val autoStartEnabled: Boolean = true,
    val signalStrength: Float = 0f,
    val centerBrightness: Float = 0f,
    val surroundBrightness: Float = 0f,
    val noiseFloor: Float = 0f,
    val detectionThreshold: Float = 18f,
    val suggestedThreshold: Float = 18f,
    val lightDetected: Boolean = false,
    val decodedSymbols: String = "",
    val decodedMessage: String = "",
    val guideMessage: String = "将光源对准取景框，先观察信号强度，再开始读取",
    val currentPulseMs: Long = 0L,
    val history: List<ReaderHistoryItem> = emptyList()
)

data class LampUiState(
    val currentPage: AppPage = AppPage.CONTROL,
    val isTorchOn: Boolean = false,
    val flashingEnabled: Boolean = false,
    val isPatternSending: Boolean = false,
    val intervalMs: Float = 500f,
    val scheduleState: ScheduleState = ScheduleState(),
    val autoOffRemainingSeconds: Long? = null,
    val messageMode: MessageMode = MessageMode.MORSE,
    val morseInput: String = "SOS",
    val morsePreview: String = "... --- ...",
    val customPatternText: String = ". - . .",
    val repeatCount: Int = 1,
    val shortOnMs: Float = 200f,
    val longOnMs: Float = 600f,
    val gapMs: Float = 200f,
    val wordGapMs: Float = 700f,
    val readerState: ReaderUiState = ReaderUiState(),
    val statusMessage: String = "准备就绪"
)
