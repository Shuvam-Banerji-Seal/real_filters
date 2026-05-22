package com.realfilters.app.data.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

data class FilterExport(
    @SerializedName("name")
    val name: String = "",
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("type")
    val type: String = "",
    @SerializedName("layers")
    val layers: List<FilterLayerExport> = emptyList()
)

data class FilterLayerExport(
    @SerializedName("name")
    val name: String = "",
    @SerializedName("type")
    val type: String = "",
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("matrix_values")
    val matrixValues: FloatArray? = null,
    @SerializedName("kernel_values")
    val kernelValues: FloatArray? = null,
    @SerializedName("kernel_width")
    val kernelWidth: Int = 0,
    @SerializedName("kernel_height")
    val kernelHeight: Int = 0,
    @SerializedName("kernel_divisor")
    val kernelDivisor: Float = 1f,
    @SerializedName("kernel_offset")
    val kernelOffset: Float = 0f,
    @SerializedName("opacity")
    val opacity: Float = 1f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterLayerExport) return false
        return name == other.name &&
                type == other.type &&
                enabled == other.enabled &&
                matrixValues.contentEquals(other.matrixValues) &&
                kernelValues.contentEquals(other.kernelValues) &&
                kernelWidth == other.kernelWidth &&
                kernelHeight == other.kernelHeight &&
                kernelDivisor == other.kernelDivisor &&
                kernelOffset == other.kernelOffset &&
                opacity == other.opacity
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (matrixValues?.contentHashCode() ?: 0)
        result = 31 * result + (kernelValues?.contentHashCode() ?: 0)
        result = 31 * result + kernelWidth
        result = 31 * result + kernelHeight
        result = 31 * result + kernelDivisor.hashCode()
        result = 31 * result + kernelOffset.hashCode()
        result = 31 * result + opacity.hashCode()
        return result
    }
}

object FilterSerializer {
    private val gson = GsonBuilder().create()

    fun toJson(export: FilterExport): String = gson.toJson(export)

    fun fromJson(json: String): FilterExport? {
        if (json.isBlank()) return null
        return try {
            val result = gson.fromJson(json, FilterExport::class.java)
            if (result.layers.isEmpty() && result.name.isEmpty()) null else result
        } catch (e: Exception) {
            null
        }
    }
}
