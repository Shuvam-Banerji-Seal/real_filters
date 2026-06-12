package com.realfilters.app.domain.engine

/**
 * Structured errors emitted by the image-processing pipeline.
 * The UI layer maps these to localized strings.
 */
sealed class EngineError(open val message: String) {
    data object ImageTooLarge : EngineError("Image is too large to process")
    data object ImageDecodeFailed : EngineError("Image could not be decoded")
    data class UnsupportedFormat(val format: String) : EngineError("Unsupported image format: $format")
    data object NoImageLoaded : EngineError("No image has been loaded")
    data class InvalidMatrixSize(val expected: Int, val actual: Int) :
        EngineError("Color matrix requires $expected values, got $actual")
    data class InvalidKernelSize(val width: Int, val height: Int, val values: Int) :
        EngineError("Convolution kernel size mismatch: ${width}x$height = ${width * height} expected, got $values")
    data object TooManyLayers : EngineError("Too many filter layers (max 32)")
    data class DiskFull(override val message: String) : EngineError(message)
    data class DatabaseBusy(override val message: String) : EngineError(message)
    data class OutOfMemory(override val message: String) : EngineError(message)
    data class InvalidJson(override val message: String) : EngineError(message)
    data class Unknown(override val message: String) : EngineError(message)
}
