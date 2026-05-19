package com.realfilters.app.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class FilterExport(
    @SerializedName("name")
    val name: String,
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("type")
    val type: String, // "color_matrix" or "convolution" or "composite"
    @SerializedName("layers")
    val layers: List<FilterLayerExport>
)

data class FilterLayerExport(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String, // "color_matrix" or "convolution"
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
        return name == other.name && type == other.type
    }

    override fun hashCode(): Int = name.hashCode() * 31 + type.hashCode()
}

object FilterSerializer {
    private val gson = Gson()

    fun toJson(export: FilterExport): String = gson.toJson(export)

    fun fromJson(json: String): FilterExport? = try {
        gson.fromJson(json, FilterExport::class.java)
    } catch (e: Exception) {
        null
    }
}
