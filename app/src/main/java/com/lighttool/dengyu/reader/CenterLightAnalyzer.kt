package com.lighttool.dengyu.reader

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

data class CenterLightSample(
    val centerBrightness: Float,
    val surroundBrightness: Float,
    val signalStrength: Float,
    val noiseFloor: Float,
    val suggestedThreshold: Float,
    val timestampMs: Long
)

class CenterLightAnalyzer(
    private val onSample: (CenterLightSample) -> Unit
) : ImageAnalysis.Analyzer {

    private var hasBaseline = false
    private var smoothedCenter = 0f
    private var smoothedSurround = 0f
    private var smoothedSignal = 0f
    private var noiseFloor = 8f

    override fun analyze(image: ImageProxy) {
        val plane = image.planes.firstOrNull()
        if (plane == null) {
            image.close()
            return
        }

        val width = image.width
        val height = image.height
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val focusStartX = (width * 0.18f).toInt()
        val focusEndX = (width * 0.82f).toInt()
        val focusStartY = (height * 0.18f).toInt()
        val focusEndY = (height * 0.82f).toInt()
        val centerStartX = (width * 0.35f).toInt()
        val centerEndX = (width * 0.65f).toInt()
        val centerStartY = (height * 0.35f).toInt()
        val centerEndY = (height * 0.65f).toInt()

        val centerStats = averageLumaStats(
            buffer = buffer,
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = pixelStride,
            startX = centerStartX,
            endX = centerEndX,
            startY = centerStartY,
            endY = centerEndY,
            step = 3
        )
        val focusStats = averageLumaStats(
            buffer = buffer,
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = pixelStride,
            startX = focusStartX,
            endX = focusEndX,
            startY = focusStartY,
            endY = focusEndY,
            step = 4
        )

        val surroundAverage = if (focusStats.count > centerStats.count) {
            ((focusStats.sum - centerStats.sum).toFloat() / (focusStats.count - centerStats.count).toFloat())
        } else {
            centerStats.average
        }
        val centerAverage = centerStats.average
        val rawSignal = centerAverage - surroundAverage

        if (!hasBaseline) {
            hasBaseline = true
            smoothedCenter = centerAverage
            smoothedSurround = surroundAverage
            smoothedSignal = rawSignal
        } else {
            smoothedCenter = smoothedCenter * 0.78f + centerAverage * 0.22f
            smoothedSurround = smoothedSurround * 0.82f + surroundAverage * 0.18f
            smoothedSignal = smoothedSignal * 0.70f + rawSignal * 0.30f
        }

        val deviation = kotlin.math.abs(rawSignal - smoothedSignal)
        noiseFloor = noiseFloor * 0.90f + deviation * 0.10f
        val suggestedThreshold = (noiseFloor * 2.8f).coerceIn(8f, 60f)

        onSample(
            CenterLightSample(
                centerBrightness = smoothedCenter,
                surroundBrightness = smoothedSurround,
                signalStrength = smoothedSignal,
                noiseFloor = noiseFloor,
                suggestedThreshold = suggestedThreshold,
                timestampMs = image.imageInfo.timestamp / 1_000_000L
            )
        )
        image.close()
    }

    private fun averageLumaStats(
        buffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        startX: Int,
        endX: Int,
        startY: Int,
        endY: Int,
        step: Int
    ): LumaStats {
        var sum = 0L
        var count = 0L
        val safeStartX = startX.coerceIn(0, width - 1)
        val safeEndX = endX.coerceIn(safeStartX + 1, width)
        val safeStartY = startY.coerceIn(0, height - 1)
        val safeEndY = endY.coerceIn(safeStartY + 1, height)

        var y = safeStartY
        while (y < safeEndY) {
            var x = safeStartX
            while (x < safeEndX) {
                val index = y * rowStride + x * pixelStride
                sum += buffer.get(index).toInt() and 0xFF
                count++
                x += step
            }
            y += step
        }
        return LumaStats(
            sum = sum,
            count = count,
            average = if (count == 0L) 0f else sum.toFloat() / count.toFloat()
        )
    }

    private data class LumaStats(
        val sum: Long,
        val count: Long,
        val average: Float
    )
}
