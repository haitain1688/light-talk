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

data class LampUiState(
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
    val statusMessage: String = "准备就绪"
)
