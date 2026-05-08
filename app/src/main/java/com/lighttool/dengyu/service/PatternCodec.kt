package com.lighttool.dengyu.service

import com.lighttool.dengyu.data.LightPattern

object PatternCodec {

    private val morseMap = mapOf(
        'A' to ".-",
        'B' to "-...",
        'C' to "-.-.",
        'D' to "-..",
        'E' to ".",
        'F' to "..-.",
        'G' to "--.",
        'H' to "....",
        'I' to "..",
        'J' to ".---",
        'K' to "-.-",
        'L' to ".-..",
        'M' to "--",
        'N' to "-.",
        'O' to "---",
        'P' to ".--.",
        'Q' to "--.-",
        'R' to ".-.",
        'S' to "...",
        'T' to "-",
        'U' to "..-",
        'V' to "...-",
        'W' to ".--",
        'X' to "-..-",
        'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----",
        '1' to ".----",
        '2' to "..---",
        '3' to "...--",
        '4' to "....-",
        '5' to ".....",
        '6' to "-....",
        '7' to "--...",
        '8' to "---..",
        '9' to "----."
    )

    fun encode(
        raw: String,
        shortOnMs: Long,
        longOnMs: Long,
        gapMs: Long,
        wordGapMs: Long
    ): LightPattern {
        val pulses = mutableListOf<Long>()
        raw.trim().forEachIndexed { index, char ->
            when (char) {
                '.' -> {
                    pulses += shortOnMs
                    pulses += gapMs
                }
                '-' -> {
                    pulses += longOnMs
                    pulses += gapMs
                }
                ' ', '/', '|' -> {
                    if (pulses.isNotEmpty()) {
                        pulses[pulses.lastIndex] = wordGapMs
                    }
                }
            }
            if (index == raw.lastIndex && pulses.isNotEmpty()) {
                pulses[pulses.lastIndex] = wordGapMs
            }
        }
        if (pulses.isEmpty()) {
            pulses += shortOnMs
            pulses += wordGapMs
        }
        return LightPattern(pulses = pulses, repeat = true)
    }

    fun encodeMorseMessage(
        raw: String,
        shortOnMs: Long,
        longOnMs: Long,
        gapMs: Long,
        letterGapMs: Long,
        wordGapMs: Long
    ): LightPattern {
        val pulses = mutableListOf<Long>()
        val normalized = raw.uppercase()
        normalized.forEachIndexed { index, char ->
            when {
                char == ' ' -> {
                    if (pulses.isNotEmpty()) {
                        pulses[pulses.lastIndex] = wordGapMs
                    }
                }
                morseMap.containsKey(char) -> {
                    val code = morseMap.getValue(char)
                    code.forEachIndexed { symbolIndex, symbol ->
                        pulses += if (symbol == '.') shortOnMs else longOnMs
                        pulses += if (symbolIndex == code.lastIndex) letterGapMs else gapMs
                    }
                }
            }
            if (index == normalized.lastIndex && pulses.isNotEmpty()) {
                pulses[pulses.lastIndex] = wordGapMs
            }
        }
        if (pulses.isEmpty()) {
            return encode(".", shortOnMs, longOnMs, gapMs, wordGapMs)
        }
        return LightPattern(pulses = pulses, repeat = true)
    }

    fun toMorsePreview(raw: String): String {
        return raw.uppercase()
            .mapNotNull { char ->
                when {
                    char == ' ' -> "/"
                    morseMap.containsKey(char) -> morseMap.getValue(char)
                    else -> null
                }
            }
            .joinToString(" ")
            .ifBlank { "..." }
    }
}
