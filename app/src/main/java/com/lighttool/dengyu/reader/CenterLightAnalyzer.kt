package com.lighttool.dengyu.reader

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

data class CenterLightSample(
    val centerBrightness: Float,
    val frameBrightness: Float,
    val signalStrength: Float,
    val timestampMs: Long
)

class CenterLightAnalyzer(
    private val onSample: (CenterLightSample) -> Unit
) : ImageAnalysis.Analyzer {

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

        val fullAverage = averageLuma(
            buffer = buffer,
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = pixelStride,
            startX = 0,
            endX = width,
            startY = 0,
            endY = height,
            step = 8
        )
        val centerAverage = averageLuma(
            buffer = buffer,
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = pixelStride,
            startX = (width * 0.35f).toInt(),
            endX = (width * 0.65f).toInt(),
            startY = (height * 0.35f).toInt(),
            endY = (height * 0.65f).toInt(),
            step = 3
        )

        onSample(
            CenterLightSample(
                centerBrightness = centerAverage,
                frameBrightness = fullAverage,
                signalStrength = centerAverage - fullAverage,
                timestampMs = image.imageInfo.timestamp / 1_000_000L
            )
        )
        image.close()
    }

    private fun averageLuma(
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
    ): Float {
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
        return if (count == 0L) 0f else sum.toFloat() / count.toFloat()
    }
}
