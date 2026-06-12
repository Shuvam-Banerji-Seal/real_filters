package com.realfilters.app.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.caverock.androidsvg.SVG
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLoader @Inject constructor() {

    companion object {
        private const val TAG = "ImageLoader"
    }

    enum class ImageFormat {
        JPEG, PNG, GIF, BMP, WEBP, TIFF, HEIF, SVG, UNKNOWN
    }

    fun detectFormat(context: Context, uri: Uri): ImageFormat {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> ImageFormat.JPEG
            mimeType?.contains("png") == true -> ImageFormat.PNG
            mimeType?.contains("gif") == true -> ImageFormat.GIF
            mimeType?.contains("bmp") == true -> ImageFormat.BMP
            mimeType?.contains("webp") == true -> ImageFormat.WEBP
            mimeType?.contains("tiff") == true || mimeType?.contains("tif") == true -> ImageFormat.TIFF
            mimeType?.contains("heif") == true || mimeType?.contains("heic") == true -> ImageFormat.HEIF
            mimeType?.contains("svg") == true -> ImageFormat.SVG
            else -> ImageFormat.UNKNOWN
        }
    }

    fun loadImage(context: Context, uri: Uri, maxWidth: Int = 2048, maxHeight: Int = 2048): Bitmap? {
        val format = detectFormat(context, uri)
        Log.d(TAG, "loadImage: format=$format")

        return when (format) {
            ImageFormat.SVG -> loadSvg(context, uri, maxWidth, maxHeight)
            else -> loadStandard(context, uri, maxWidth, maxHeight)
        }
    }

    private fun loadStandard(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                Log.d(TAG, "ImageDecoder: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
                try {
                    ensureMutable(bitmap, maxWidth, maxHeight)
                } catch (t: Throwable) {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    throw t
                }
            } catch (e: Exception) {
                Log.e(TAG, "ImageDecoder failed, trying BitmapFactory", e)
                loadWithBitmapFactory(context, uri, maxWidth, maxHeight)
            }
        } else {
            loadWithBitmapFactory(context, uri, maxWidth, maxHeight)
        }
    }

    private fun loadWithBitmapFactory(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e(TAG, "Image has zero dimensions: ${options.outWidth}x${options.outHeight}")
                return null
            }

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (bitmap != null) {
                Log.d(TAG, "BitmapFactory: ${bitmap.width}x${bitmap.height}")
                try {
                    ensureMutable(bitmap, maxWidth, maxHeight)
                } catch (t: Throwable) {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    throw t
                }
            } else {
                Log.e(TAG, "BitmapFactory returned null")
                null
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "BitmapFactory OOM", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "BitmapFactory failed", e)
            null
        }
    }

    private fun ensureMutable(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val needsConversion = !bitmap.isMutable ||
                bitmap.config == Bitmap.Config.HARDWARE ||
                bitmap.config == null

        val converted = if (needsConversion) {
            Log.d(TAG, "Converting bitmap: mutable=${bitmap.isMutable}, config=${bitmap.config}")
            val copy = try {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Bitmap.copy OOM", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Bitmap.copy failed", e)
                null
            }
            if (copy != null && copy !== bitmap) {
                bitmap.recycle()
            }
            copy ?: bitmap // fallback to original if copy failed
        } else {
            bitmap
        }

        return scaleBitmap(converted, maxWidth, maxHeight)
    }

    private fun loadSvg(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val svg = SVG.getFromInputStream(inputStream)
                // Use viewBox dimensions when documentWidth/Height are -1 (unspecified).
                val rawW = if (svg.documentWidth > 0) svg.documentWidth.toFloat() else 512f
                val rawH = if (svg.documentHeight > 0) svg.documentHeight.toFloat() else 512f
                val width = (rawW * 2).toInt().coerceIn(1, maxWidth)
                val height = (rawH * 2).toInt().coerceIn(1, maxHeight)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = android.graphics.Canvas(bitmap)
                    svg.renderToCanvas(canvas)
                    Log.d(TAG, "SVG loaded: ${width}x${height}")
                    bitmap
                } catch (t: Throwable) {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    throw t
                }
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "SVG OOM", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "SVG load failed", e)
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            Log.w(TAG, "scaleBitmap: zero-dimension bitmap, returning as-is")
            return bitmap
        }
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)
        Log.d(TAG, "Scaling ${width}x${height} -> ${newWidth}x${newHeight}")
        val scaled = try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "createScaledBitmap OOM", e)
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "createScaledBitmap failed", e)
            return bitmap
        }
        if (scaled !== null && scaled !== bitmap) {
            bitmap.recycle()
        }
        return scaled ?: bitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
