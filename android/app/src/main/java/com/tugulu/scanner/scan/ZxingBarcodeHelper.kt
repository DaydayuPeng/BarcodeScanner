package com.tugulu.scanner.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 对齐 floatscan / 优化后的 scanner.html：
 * - 仅快递一维码：CODE_128 / CODE_39 / EAN_13 / ITF / CODABAR
 * - TRY_HARDER + UTF-8
 * - 交替全幅 / 中心横带，每帧最多两次轻量解码（后台线程可承受）
 */
class ZxingBarcodeHelper {

    private val reader = MultiFormatReader()
    private val hints: EnumMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
    private val tick = AtomicInteger(0)

    init {
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.CODABAR
        )
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hints[DecodeHintType.TRY_HARDER] = true
        reader.setHints(hints)
    }

    fun decodeNv21(nv21: ByteArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || nv21.isEmpty()) return null

        val useBand = tick.getAndIncrement() % 2 == 1
        if (useBand) {
            val bandH = (height * 0.42f).toInt().coerceAtLeast(48)
            val bandTop = ((height - bandH) / 2).coerceAtLeast(0)
            if (bandTop + bandH <= height) {
                decodeSource(
                    PlanarYUVLuminanceSource(nv21, width, height, 0, bandTop, width, bandH, false)
                )?.let { return it }
            }
        }

        return decodeSource(
            PlanarYUVLuminanceSource(nv21, width, height, 0, 0, width, height, false)
        )
    }

    private fun decodeSource(source: PlanarYUVLuminanceSource): String? {
        // Hybrid 优先；失败再试 GlobalHistogram（比每帧三路反色更稳、更快）
        val attempts = arrayOf(
            { HybridBinarizer(source) },
            { GlobalHistogramBinarizer(source) }
        )
        for (factory in attempts) {
            try {
                reader.reset()
                val result = reader.decodeWithState(BinaryBitmap(factory()))
                val text = result?.text
                if (!text.isNullOrBlank()) return text
            } catch (_: Exception) {
                // NotFound 等：继续
            }
        }
        return null
    }
}
