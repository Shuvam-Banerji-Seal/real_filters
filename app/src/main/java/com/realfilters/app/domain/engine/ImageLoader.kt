package com.realfilters.app.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.caverock.androidsvg.SVG
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageLoader @Inject constructor() {

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

        return when (format) {
            ImageFormat.SVG -> loadSvg(context, uri, maxWidth, maxHeight)
            else -> loadStandard(context, uri, maxWidth, maxHeight)
        }
    }

    private fun loadStandard(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }.let { bitmap ->
                    scaleBitmap(bitmap, maxWidth, maxHeight)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadWithBitmapFactory(context, uri, maxWidth, maxHeight)
            }
        } else {
            loadWithBitmapFactory(context, uri, maxWidth, maxHeight)
        }
    }

    private fun loadWithBitmapFactory(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)?.let { bitmap ->
                scaleBitmap(bitmap, maxWidth, maxHeight)
            }
        }
    }

    private fun loadSvg(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val svg = SVG.getFromInputStream(inputStream)
            val width = (svg.documentWidth * 2).toInt().coerceAtMost(maxWidth)
            val height = (svg.documentHeight * 2).toInt().coerceAtMost(maxHeight)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            svg.renderToCanvas(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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
