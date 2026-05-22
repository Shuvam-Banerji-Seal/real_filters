package com.realfilters.app.domain.filter

import com.realfilters.app.domain.engine.ColorMatrix
import com.realfilters.app.domain.engine.ConvolutionKernel

object PresetFilters {

    val identity = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Identity"
    )

    val sepia = ColorMatrix(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Sepia"
    )

    val grayscale = ColorMatrix(
        floatArrayOf(
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Grayscale"
    )

    val invert = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 1f,
            0f, -1f, 0f, 0f, 1f,
            0f, 0f, -1f, 0f, 1f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Invert"
    )

    val brightness = ColorMatrix(
        floatArrayOf(
            1.2f, 0f, 0f, 0f, 0.1f,
            0f, 1.2f, 0f, 0f, 0.1f,
            0f, 0f, 1.2f, 0f, 0.1f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Brightness +"
    )

    val contrast = ColorMatrix(
        floatArrayOf(
            1.5f, 0f, 0f, 0f, -0.25f,
            0f, 1.5f, 0f, 0f, -0.25f,
            0f, 0f, 1.5f, 0f, -0.25f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Contrast +"
    )

    val saturation = run {
        val Lu = 0.2126f; val Lg = 0.7152f; val Lb = 0.0722f; val s = 1.5f
        ColorMatrix(
            floatArrayOf(
                Lu + (1f - Lu) * s, Lg * (1f - s),       Lb * (1f - s),       0f, 0f,
                Lu * (1f - s),       Lg + (1f - Lg) * s,  Lb * (1f - s),       0f, 0f,
                Lu * (1f - s),       Lg * (1f - s),       Lb + (1f - Lb) * s,  0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ),
            name = "Saturation +"
        )
    }

    val redChannel = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Red Channel"
    )

    val greenChannel = ColorMatrix(
        floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Green Channel"
    )

    val blueChannel = ColorMatrix(
        floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Blue Channel"
    )

    val vintage = ColorMatrix(
        floatArrayOf(
            0.6f, 0.3f, 0.1f, 0f, 0.05f,
            0.2f, 0.7f, 0.1f, 0f, 0.02f,
            0.1f, 0.2f, 0.5f, 0f, 0.08f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Vintage"
    )

    val coolTone = ColorMatrix(
        floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 0.9f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0.1f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Cool Tone"
    )

    val warmTone = ColorMatrix(
        floatArrayOf(
            1.2f, 0f, 0f, 0f, 0.1f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        name = "Warm Tone"
    )

    val colorMatrices: List<ColorMatrix> = listOf(
        identity, sepia, grayscale, invert, brightness,
        contrast, saturation, redChannel, greenChannel,
        blueChannel, vintage, coolTone, warmTone
    )

    val sharpen = ConvolutionKernel(
        values = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        ),
        width = 3, height = 3,
        name = "Sharpen",
        divisor = 1f
    )

    val blur = ConvolutionKernel(
        values = floatArrayOf(
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 1f
        ),
        width = 3, height = 3,
        name = "Blur",
        divisor = 9f
    )

    val gaussianBlur = ConvolutionKernel(
        values = floatArrayOf(
            1f, 2f, 1f,
            2f, 4f, 2f,
            1f, 2f, 1f
        ),
        width = 3, height = 3,
        name = "Gaussian Blur",
        divisor = 16f
    )

    val edgeDetect = ConvolutionKernel(
        values = floatArrayOf(
            -1f, -1f, -1f,
            -1f, 8f, -1f,
            -1f, -1f, -1f
        ),
        width = 3, height = 3,
        name = "Edge Detect",
        divisor = 1f
    )

    val emboss = ConvolutionKernel(
        values = floatArrayOf(
            -2f, -1f, 0f,
            -1f, 1f, 1f,
            0f, 1f, 2f
        ),
        width = 3, height = 3,
        name = "Emboss",
        divisor = 1f
    )

    val edgeDetectHorizontal = ConvolutionKernel(
        values = floatArrayOf(
            0f, 0f, 0f,
            -1f, 1f, 0f,
            0f, 0f, 0f
        ),
        width = 3, height = 3,
        name = "Edge H",
        divisor = 1f
    )

    val edgeDetectVertical = ConvolutionKernel(
        values = floatArrayOf(
            0f, -1f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f
        ),
        width = 3, height = 3,
        name = "Edge V",
        divisor = 1f
    )

    val boxBlur = ConvolutionKernel(
        values = floatArrayOf(
            1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f
        ),
        width = 5, height = 5,
        name = "Box Blur 5x5",
        divisor = 25f
    )

    val convolutionKernels: List<ConvolutionKernel> = listOf(
        sharpen, blur, gaussianBlur, edgeDetect, emboss,
        edgeDetectHorizontal, edgeDetectVertical, boxBlur
    )
}
