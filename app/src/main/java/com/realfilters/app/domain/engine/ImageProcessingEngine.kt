package com.realfilters.app.domain.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
data class ColorMatrix(
    val values: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ),
    val name: String = "Identity"
) {
    init {
        require(values.size == 20) { "ColorMatrix requires exactly 20 values (4x5), got ${values.size}" }
    }

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
                sb.append(String.format(java.util.Locale.US, "%6.3f", v))
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

@Immutable
data class ConvolutionKernel(
    val values: FloatArray,
    val width: Int,
    val height: Int,
    val name: String = "Custom",
    val divisor: Float = 1f,
    val offset: Float = 0f
) {
    init {
        require(width > 0) { "ConvolutionKernel width must be > 0, got $width" }
        require(height > 0) { "ConvolutionKernel height must be > 0, got $height" }
        require(values.size == width * height) {
            "ConvolutionKernel values size (${values.size}) must equal width*height (${width * height})"
        }
    }

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
                sb.append(String.format(java.util.Locale.US, "%6.2f", get(x, y)))
                if (x < width - 1) sb.append("  ")
            }
            if (y < height - 1) sb.append("\n")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConvolutionKernel) return false
        return values.contentEquals(other.values) &&
            width == other.width &&
            height == other.height &&
            divisor == other.divisor &&
            offset == other.offset
    }

    override fun hashCode(): Int {
        var result = values.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + divisor.hashCode()
        result = 31 * result + offset.hashCode()
        return result
    }
}

@Immutable
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

    companion object {
        const val MAX_LAYERS = 32
    }

    suspend fun applyColorMatrix(bitmap: Bitmap, matrix: ColorMatrix): Bitmap =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val pixels = IntArray(width * height)
                val outPixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                coroutineContext.ensureActive()

                for (i in pixels.indices) {
                    if ((i and 0x3FFFF) == 0) coroutineContext.ensureActive()
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
            } catch (t: Throwable) {
                if (!result.isRecycled) result.recycle()
                throw t
            }
        }

    suspend fun applyConvolution(bitmap: Bitmap, kernel: ConvolutionKernel): Bitmap =
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("Cannot apply convolution to bitmap with zero dimensions: ${width}x${height}")
            }
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val pixels = IntArray(width * height)
                val outPixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                coroutineContext.ensureActive()

                val kHalfW = kernel.width / 2
                val kHalfH = kernel.height / 2
                val div = if (kernel.divisor == 0f) 1f else kernel.divisor

                for (y in 0 until height) {
                    if ((y and 0x3F) == 0) coroutineContext.ensureActive()
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
            } catch (t: Throwable) {
                if (!result.isRecycled) result.recycle()
                throw t
            }
        }

    suspend fun applyFilterLayers(bitmap: Bitmap, layers: List<FilterLayer>): Bitmap =
        withContext(Dispatchers.Default) {
            require(layers.count { it.enabled } <= MAX_LAYERS) {
                "Too many enabled layers: ${layers.count { it.enabled }} (max $MAX_LAYERS)"
            }
            val config = if (bitmap.config == Bitmap.Config.HARDWARE) Bitmap.Config.ARGB_8888
                         else (bitmap.config ?: Bitmap.Config.ARGB_8888)
            val initialCopy = try {
                bitmap.copy(config, true)
            } catch (_: Throwable) {
                null
            } ?: try {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } catch (_: Throwable) {
                null
            }
            var current = initialCopy ?: return@withContext bitmap

            try {
                for (layer in layers.filter { it.enabled }) {
                    coroutineContext.ensureActive()
                    val processed = when {
                        layer.colorMatrix != null -> applyColorMatrix(current, layer.colorMatrix)
                        layer.convolutionKernel != null -> applyConvolution(current, layer.convolutionKernel)
                        else -> current
                    }

                    if (layer.opacity < 1f && processed !== current) {
                        val oldCurrent = current
                        current = blendBitmaps(current, processed, layer.opacity)
                        if (!processed.isRecycled) processed.recycle()
                        if (oldCurrent !== bitmap && !oldCurrent.isRecycled) oldCurrent.recycle()
                    } else if (processed !== current) {
                        val oldCurrent = current
                        current = processed
                        if (oldCurrent !== bitmap && !oldCurrent.isRecycled) oldCurrent.recycle()
                    }
                }
                current
            } catch (t: Throwable) {
                if (current !== bitmap && !current.isRecycled) current.recycle()
                throw t
            }
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
