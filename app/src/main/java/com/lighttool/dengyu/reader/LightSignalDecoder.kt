package com.lighttool.dengyu.reader

import com.lighttool.dengyu.service.PatternCodec

data class DecodeSnapshot(
    val symbols: String,
    val message: String,
    val currentPulseMs: Long
)

class LightSignalDecoder(
    private val dotDashBoundaryMs: Long,
    private val letterGapBoundaryMs: Long,
    private val wordGapBoundaryMs: Long,
    private val minPulseMs: Long
) {

    private var hasStarted = false
    private var lastLevel = false
    private var lastTransitionTimeMs = 0L
    private var offGapStage = OffGapStage.NONE
    private val committedTokens = mutableListOf<String>()
    private val currentToken = StringBuilder()

    fun reset() {
        hasStarted = false
        lastLevel = false
        lastTransitionTimeMs = 0L
        offGapStage = OffGapStage.NONE
        committedTokens.clear()
        currentToken.clear()
    }

    fun onSample(level: Boolean, timestampMs: Long): DecodeSnapshot {
        if (!hasStarted) {
            hasStarted = true
            lastLevel = level
            lastTransitionTimeMs = timestampMs
            return snapshot(timestampMs)
        }

        if (level != lastLevel) {
            val duration = (timestampMs - lastTransitionTimeMs).coerceAtLeast(0L)
            if (lastLevel) {
                appendPulse(duration)
            }
            lastLevel = level
            lastTransitionTimeMs = timestampMs
            offGapStage = OffGapStage.NONE
            return snapshot(timestampMs)
        }

        if (!level) {
            val offDuration = (timestampMs - lastTransitionTimeMs).coerceAtLeast(0L)
            when {
                offDuration >= wordGapBoundaryMs && offGapStage != OffGapStage.WORD -> {
                    finalizeCurrentToken()
                    appendWordGap()
                    offGapStage = OffGapStage.WORD
                }

                offDuration >= letterGapBoundaryMs && offGapStage == OffGapStage.NONE -> {
                    finalizeCurrentToken()
                    offGapStage = OffGapStage.LETTER
                }
            }
        }

        return snapshot(timestampMs)
    }

    fun complete(timestampMs: Long): DecodeSnapshot {
        finalizeCurrentToken()
        return snapshot(timestampMs)
    }

    private fun appendPulse(durationMs: Long) {
        if (durationMs < minPulseMs) {
            return
        }
        currentToken.append(if (durationMs < dotDashBoundaryMs) '.' else '-')
    }

    private fun finalizeCurrentToken() {
        if (currentToken.isNotEmpty()) {
            committedTokens += currentToken.toString()
            currentToken.clear()
        }
    }

    private fun appendWordGap() {
        val hasRealToken = committedTokens.any { it.isNotBlank() && it != "/" }
        if (hasRealToken && committedTokens.lastOrNull() != "/") {
            committedTokens += "/"
        }
    }

    private fun snapshot(timestampMs: Long): DecodeSnapshot {
        val previewTokens = buildList {
            addAll(committedTokens)
            if (currentToken.isNotEmpty()) {
                add(currentToken.toString())
            }
        }
        val rawSymbols = previewTokens.joinToString(" ").trim()
        val visibleSymbols = previewTokens
            .filter { it.isNotBlank() && it != "/" }
            .joinToString(" ")
            .trim()
        return DecodeSnapshot(
            symbols = visibleSymbols,
            message = PatternCodec.decodeMorseSequence(rawSymbols),
            currentPulseMs = (timestampMs - lastTransitionTimeMs).coerceAtLeast(0L)
        )
    }

    private enum class OffGapStage {
        NONE,
        LETTER,
        WORD
    }
}
