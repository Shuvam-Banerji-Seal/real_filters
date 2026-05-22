package com.realfilters.app.domain.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ColorMatrix(
    val values: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ),
    val name: String = "Identity"
) {
    val size: Int get() = 5

    operator fun get(row: Int, col: Int): Float = values[row * 5 + col]

    operator fun set(row: Int, col: Int, value: Float) {
        values[row * 5 + col] = value
    }

    fun clone(): ColorMatrix = ColorMatrix(values = values.copyOf(), name = name)

    fun toDisplayString(): String {
        val sb = StringBuilder()
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                val v = get(row, col)
                sb.append(String.format("%6.3f", v))
                if (col < 4) sb.append("  ")
            }
            if (row < 3) sb.append("\n")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorMatrix) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}

data class ConvolutionKernel(
    val values: FloatArray,
    val width: Int,
    val height: Int,
    val name: String = "Custom",
    val divisor: Float = 1f,
    val offset: Float = 0f
) {
    operator fun get(x: Int, y: Int): Float = values[y * width + x]

    fun clone(): ConvolutionKernel = ConvolutionKernel(
        values = values.copyOf(),
        width = width,
        height = height,
        name = name,
        divisor = divisor,
        offset = offset
    )

    fun toDisplayString(): String {
        val sb = StringBuilder()
        for (y in 0 until height) {
            for (x in 0 until width) {
                sb.append(String.format("%6.2f", get(x, y)))
                if (x < width - 1) sb.append("  ")
            }
            if (y < height - 1) sb.append("\n")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConvolutionKernel) return false
        return values.contentEquals(other.values) && width == other.width && height == other.height
    }

    override fun hashCode(): Int = values.contentHashCode() * 31 + width * 31 + height
}

data class FilterLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val colorMatrix: ColorMatrix? = null,
    val convolutionKernel: ConvolutionKernel? = null,
    val name: String = "Layer",
    val enabled: Boolean = true,
    val opacity: Float = 1f
)

@Singleton
class ImageProcessingEngine @Inject constructor() {

    suspend fun applyColorMatrix(bitmap: Bitmap, matrix: ColorMatrix): Bitmap =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val outPixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = Color.alpha(pixel) / 255f
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f

                val nr = clamp(r * matrix[0, 0] + g * matrix[0, 1] + b * matrix[0, 2] + a * matrix[0, 3] + matrix[0, 4])
                val ng = clamp(r * matrix[1, 0] + g * matrix[1, 1] + b * matrix[1, 2] + a * matrix[1, 3] + matrix[1, 4])
                val nb = clamp(r * matrix[2, 0] + g * matrix[2, 1] + b * matrix[2, 2] + a * matrix[2, 3] + matrix[2, 4])
                val na = clamp(r * matrix[3, 0] + g * matrix[3, 1] + b * matrix[3, 2] + a * matrix[3, 3] + matrix[3, 4])

                outPixels[i] = Color.argb(
                    (na * 255).roundToInt(),
                    (nr * 255).roundToInt(),
                    (ng * 255).roundToInt(),
                    (nb * 255).roundToInt()
                )
            }

            result.setPixels(outPixels, 0, width, 0, 0, width, height)
            result
        }

    suspend fun applyConvolution(bitmap: Bitmap, kernel: ConvolutionKernel): Bitmap =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val outPixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val kHalfW = kernel.width / 2
            val kHalfH = kernel.height / 2

            for (y in 0 until height) {
                for (x in 0 until width) {
                    var rSum = 0f
                    var gSum = 0f
                    var bSum = 0f
                    var aSum = 0f

                    for (ky in 0 until kernel.height) {
                        for (kx in 0 until kernel.width) {
                            val px = clampInt(x + kx - kHalfW, 0, width - 1)
                            val py = clampInt(y + ky - kHalfH, 0, height - 1)
                            val pixel = pixels[py * width + px]
                            val kv = kernel[kx, ky]

                            aSum += Color.alpha(pixel) * kv
                            rSum += Color.red(pixel) * kv
                            gSum += Color.green(pixel) * kv
                            bSum += Color.blue(pixel) * kv
                        }
                    }

                    val div = if (kernel.divisor == 0f) 1f else kernel.divisor
                    outPixels[y * width + x] = Color.argb(
                        clampInt((aSum / div + kernel.offset).roundToInt(), 0, 255),
                        clampInt((rSum / div + kernel.offset).roundToInt(), 0, 255),
                        clampInt((gSum / div + kernel.offset).roundToInt(), 0, 255),
                        clampInt((bSum / div + kernel.offset).roundToInt(), 0, 255)
                    )
                }
            }

            result.setPixels(outPixels, 0, width, 0, 0, width, height)
            result
        }

    suspend fun applyFilterLayers(bitmap: Bitmap, layers: List<FilterLayer>): Bitmap =
        withContext(Dispatchers.Default) {
            val config = if (bitmap.config == Bitmap.Config.HARDWARE) Bitmap.Config.ARGB_8888
                         else (bitmap.config ?: Bitmap.Config.ARGB_8888)
            var current = bitmap.copy(config, true)

            for (layer in layers.filter { it.enabled }) {
                val processed = when {
                    layer.colorMatrix != null -> applyColorMatrix(current, layer.colorMatrix)
                    layer.convolutionKernel != null -> applyConvolution(current, layer.convolutionKernel)
                    else -> current
                }

                if (layer.opacity < 1f && processed !== current) {
                    val oldCurrent = current
                    current = blendBitmaps(current, processed, layer.opacity)
                    processed.recycle()
                    if (oldCurrent !== bitmap) oldCurrent.recycle()
                } else if (processed !== current) {
                    if (current !== bitmap) current.recycle()
                    current = processed
                }
            }

            current
        }

    fun multiplyMatrices(a: ColorMatrix, b: ColorMatrix): ColorMatrix {
        val result = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[row, k] * b[k, col]
                }
                if (col == 4) {
                    sum += a[row, 4]
                }
                result[row * 5 + col] = sum
            }
        }
        return ColorMatrix(result, "${a.name} × ${b.name}")
    }

    private fun blendBitmaps(base: Bitmap, overlay: Bitmap, opacity: Float): Bitmap {
        val width = base.width
        val height = base.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val basePixels = IntArray(width * height)
        val overlayPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)

        for (i in basePixels.indices) {
            val bp = basePixels[i]
            val op = overlayPixels[i]
            val inv = 1f - opacity
            outPixels[i] = Color.argb(
                clampInt((Color.alpha(bp) * inv + Color.alpha(op) * opacity).roundToInt(), 0, 255),
                clampInt((Color.red(bp) * inv + Color.red(op) * opacity).roundToInt(), 0, 255),
                clampInt((Color.green(bp) * inv + Color.green(op) * opacity).roundToInt(), 0, 255),
                clampInt((Color.blue(bp) * inv + Color.blue(op) * opacity).roundToInt(), 0, 255)
            )
        }

        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun clamp(value: Float): Float = max(0f, min(1f, value))
    private fun clampInt(value: Int, min: Int, max: Int): Int = max(min, min(max, value))
}
