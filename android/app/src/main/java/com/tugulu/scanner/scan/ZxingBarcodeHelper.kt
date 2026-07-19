package com.tugulu.scanner.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

/**
 * 参照 floatscan ScanActivity：
 * - 一维码优先（快递面单）
 * - TRY_HARDER / CHARACTER_SET
 * - 全幅 + 中心裁剪 + Hybrid/GlobalHistogram/反色多路尝试
 */
class ZxingBarcodeHelper {

    private val reader = MultiFormatReader()
    private val hints: EnumMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)

    init {
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.ITF,
            BarcodeFormat.CODABAR,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX
        )
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hints[DecodeHintType.TRY_HARDER] = true
        try {
            // ZXing 3.5+ 支持反色条码
            hints[DecodeHintType.ALSO_INVERTED] = true
        } catch (_: Throwable) {
            // ignore on older cores
        }
        reader.setHints(hints)
    }

    fun decodeNv21(nv21: ByteArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || nv21.isEmpty()) return null

        // 1) 全幅
        decodeSource(
            PlanarYUVLuminanceSource(nv21, width, height, 0, 0, width, height, false)
        )?.let { return it }

        // 2) 中心裁剪（快递条码常在画面中部）
        val cropW = (width * 0.72f).toInt().coerceAtLeast(64)
        val cropH = (height * 0.48f).toInt().coerceAtLeast(48)
        val left = ((width - cropW) / 2).coerceAtLeast(0)
        val top = ((height - cropH) / 2).coerceAtLeast(0)
        if (left + cropW <= width && top + cropH <= height) {
            decodeSource(
                PlanarYUVLuminanceSource(nv21, width, height, left, top, cropW, cropH, false)
            )?.let { return it }
        }

        // 3) 横向中带（一维码水平排布）
        val bandH = (height * 0.36f).toInt().coerceAtLeast(48)
        val bandTop = ((height - bandH) / 2).coerceAtLeast(0)
        if (bandTop + bandH <= height) {
            decodeSource(
                PlanarYUVLuminanceSource(nv21, width, height, 0, bandTop, width, bandH, false)
            )?.let { return it }
        }
        return null
    }

    private fun decodeSource(source: PlanarYUVLuminanceSource): String? {
        val attempts = arrayOf(
            { HybridBinarizer(source) },
            { GlobalHistogramBinarizer(source) },
            { HybridBinarizer(source.invert()) }
        )
        for (factory in attempts) {
            try {
                reader.reset()
                val result = reader.decodeWithState(BinaryBitmap(factory()))
                val text = result?.text
                if (!text.isNullOrBlank()) return text
            } catch (_: Exception) {
                // NotFound / checksum 等：继续下一策略
            }
        }
        return null
    }
}
